package dev.tassis.createfieldwork.isolator.binding;

import dev.tassis.createfieldwork.isolator.smart.BrassIsolatorBlockEntity;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.isolator.state.IsolatorBlocks;
import dev.tassis.createfieldwork.isolator.state.IsolationRegistry;
import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Loaded BE authority. Rebuilds only on cold mutations; radio queries are O(1). */
public final class BindingAuthority {
    private record Location(ResourceKey<Level> dimension, BlockPos pos) {}
    public record Grant(CraftKey craft, BrassIsolatorBlockEntity witness, long generation, boolean conflict) {}
    public record Candidate(Map<UUID, Grant> grants, Set<UUID> changedDevices) {
        public Candidate { grants = Map.copyOf(grants); changedDevices = Set.copyOf(changedDevices); }
    }
    private final FieldworkServerState state;
    private final Map<Location, BrassIsolatorBlockEntity> entries = new HashMap<>();
    private final Map<UUID, Grant> devices = new HashMap<>();
    private final Map<UUID, Long> revisions = new HashMap<>();
    private long sequence;
    public BindingAuthority(FieldworkServerState state) { this.state = state; }
    public long revision(UUID device) { state.checkThread(); return device == null ? 0 : revisions.getOrDefault(device, 0L); }

    public void track(BrassIsolatorBlockEntity be) {
        state.checkThread();
        if (!(be.getLevel() instanceof ServerLevel level) || level.getServer() != state.server()) return;
        var location = new Location(level.dimension(), be.getBlockPos());
        if (be.isRemoved() || !level.hasChunkAt(be.getBlockPos()) || level.getBlockEntity(be.getBlockPos()) != be) entries.remove(location);
        else entries.put(location, be);
        rebuild();
    }
    public void untrack(BrassIsolatorBlockEntity be) {
        state.checkThread();
        if (be.getLevel() != null) entries.remove(new Location(be.getLevel().dimension(), be.getBlockPos()), be);
        rebuild();
    }
    public void unloaded(ServerLevel level, ChunkPos chunk) {
        state.checkThread();
        entries.keySet().removeIf(location -> location.dimension.equals(level.dimension()) && new ChunkPos(location.pos).equals(chunk));
        rebuild();
    }
    public Set<UUID> rebuild() {
        var changed = rebuildInternal();
        notifyChanged(changed);
        return changed;
    }
    private Set<UUID> rebuildInternal() {
        state.checkThread();
        var next = new HashMap<UUID, Grant>();
        entries.entrySet().removeIf(entry -> entry.getValue().currentCraft() == null);
        for (var be : entries.values()) {
            var craft = be.currentCraft();
            if (craft == null || !craft.id().equals(be.instanceStamp())) continue;
            be.pruneRevoked(state.bindingRevocations((ServerLevel) be.getLevel()));
            be.forEachGrant((id, generation) -> {
                if (!state.bindingRevocations((ServerLevel) be.getLevel()).permits(id, generation)) return;
                var grant = new Grant(craft, be, generation, false);
                next.merge(id, grant, (a, b) -> !a.conflict && a.craft.equals(b.craft) ? a : new Grant(null, null, 0, true));
            });
        }
        Set<UUID> changed = new HashSet<>(devices.keySet()); changed.addAll(next.keySet());
        changed.removeIf(id -> sameTarget(devices.get(id), next.get(id)));
        devices.clear(); devices.putAll(next);
        if (changed.isEmpty()) return Set.of();
        for (var id : changed) revisions.put(id, ++sequence);
        state.invalidateVisibility();
        return Set.copyOf(changed);
    }
    private void notifyChanged(Set<UUID> changed) {
        if (changed.isEmpty()) return;
        if (state.transitions().active()) state.transitions().bindingDevices(changed);
        else state.controllers().changedDevices(changed);
    }
    /** Cold reconstruction paired with the marker candidate version, before its final refresh. */
    public Candidate candidate(Map<CraftKey, Set<BlockPos>> crafts, Set<TransitionCoordinator.Position> positions) {
        state.checkThread();
        for (var key : crafts.keySet()) {
            var level = state.server().getLevel(key.dimension());
            var craft = level == null ? null : dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level).getSubLevel(key.id());
            if (craft instanceof dev.ryanhcode.sable.sublevel.ServerSubLevel loaded && !loaded.isRemoved()) {
                for (var holder : loaded.getPlot().getLoadedChunks())
                    for (var entity : holder.getChunk().getBlockEntities().values())
                        if (entity instanceof BrassIsolatorBlockEntity be) entries.put(new Location(level.dimension(), be.getBlockPos()), be);
                // Sable holding/packed loads may leave BE NBT unmaterialized.
                // Resolve only the cold candidate marker positions, never on queries.
                for (var pos : crafts.get(key))
                    if (IsolatorBlocks.smart(level.getBlockState(pos)) &&
                        level.getBlockEntity(pos) instanceof BrassIsolatorBlockEntity be)
                        entries.put(new Location(level.dimension(), pos), be);
            }
        }
        for (var position : positions) {
            var level = state.server().getLevel(position.dimension());
            if (level != null && level.hasChunkAt(position.pos()) && level.getBlockEntity(position.pos()) instanceof BrassIsolatorBlockEntity be)
                entries.put(new Location(level.dimension(), be.getBlockPos()), be);
        }
        var changed = rebuildInternal();
        return new Candidate(devices, changed);
    }
    public void trackChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) {
        state.checkThread();
        for (var entity : chunk.getBlockEntities().values())
            if (entity instanceof BrassIsolatorBlockEntity be) entries.put(new Location(level.dimension(), be.getBlockPos()), be);
        rebuild();
    }
    public Scope scope(UUID id, ServerLevel source, TransitionCoordinator.View view) {
        state.checkThread();
        if (source == null) return Scope.PENDING;
        var grant = view == null ? devices.get(id) : view.grants().get(id);
        if (grant == null || grant.conflict || !grant.craft.dimension().equals(source.dimension()) ||
            !state.bindingRevocations().permits(id, grant.generation) || !grant.craft.equals(grant.witness.currentCraft()) ||
            !grant.craft.id().equals(grant.witness.instanceStamp()) || grant.witness.grantGeneration(id) != grant.generation) return Scope.PENDING;
        if ((view == null || !view.candidates().containsKey(grant.craft)) &&
            state.registry().snapshot(grant.craft).status() != IsolationRegistry.Status.READY) return Scope.PENDING;
        Scope target = ScopeResolver.craft(source, grant.craft, view);
        return target instanceof Scope.Private ? target : Scope.PENDING;
    }
    public CraftKey target(UUID id, ServerLevel source) {
        var scope = scope(id, source, null);
        return scope instanceof Scope.Private privateScope ? privateScope.craft() : null;
    }
    private static boolean sameTarget(Grant a, Grant b) {
        return a == b || a != null && b != null && Objects.equals(a.craft, b.craft) && a.generation == b.generation && a.conflict == b.conflict;
    }
    public void clear() { state.checkThread(); entries.clear(); devices.clear(); revisions.clear(); }
}
