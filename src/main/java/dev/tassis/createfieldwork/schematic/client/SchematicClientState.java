package dev.tassis.createfieldwork.schematic.client;

import com.simibubi.create.content.schematics.client.SchematicHandler;
import dev.tassis.createfieldwork.mixin.schematic.client.SchematicHandlerAccessor;
import dev.tassis.createfieldwork.mixin.schematic.client.SchematicTransformationAccessor;
import dev.tassis.createfieldwork.schematic.BlueprintSyncEchoes;
import dev.tassis.createfieldwork.schematic.comparison.BuildStatePolicy;
import dev.tassis.createfieldwork.schematic.comparison.ComparisonBudget;
import dev.tassis.createfieldwork.schematic.comparison.ComparisonCycle;
import dev.tassis.createfieldwork.schematic.comparison.NativeSchematicTarget;
import dev.tassis.createfieldwork.schematic.comparison.WorldTargetSupport;
import dev.tassis.createfieldwork.schematic.mask.GhostMask;
import net.minecraft.core.Vec3i;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** One client/world cache. Rendering is handled by existing Outliner, never a world scan. */
public final class SchematicClientState {
    private static final SchematicClientState INSTANCE = new SchematicClientState();
    private record OutlineKey(BlockPos pos) {}
    private static final class Candidate {
        final BlockPos pos;
        final BlockState expected;
        final long sourcePosition;
        BuildStatePolicy.Result result = BuildStatePolicy.Result.UNVERIFIED;
        int checked;
        int comparableTick = Integer.MIN_VALUE;
        boolean queued;
        boolean ambiguous;
        boolean comparable;
        Candidate(NativeSchematicTarget.Expected target, long sourcePosition) { pos = target.position(); expected = target.state(); this.sourcePosition = sourcePosition; }
    }
    private NativeSchematicTarget.Template template;
    private final Candidate[] cache = new Candidate[ComparisonBudget.CACHE_POSITIONS];
    private final Map<BlockPos, Integer> slots = new HashMap<>();
    private final Set<BlockPos> outlines = new HashSet<>();
    private final ArrayDeque<Integer> dirty = new ArrayDeque<>();
    private final ComparisonCycle cycle = new ComparisonCycle();
    private final GhostMask ghostMask = new GhostMask();
    private ClientLevel world;
    private Object player;
    private ItemStack stack = ItemStack.EMPTY;
    private BlockPos anchor;
    private Rotation rotation;
    private Mirror mirror;
    private WorldTargetSupport.Snapshot occupancy;
    private int handSlot = -1, nextSlot, ticks;
    private boolean paused, ghostMaskSaturated;
    private String status = "inactive";
    private SchematicClientState() {}

    public static void capture(StructureTemplate source) {
        INSTANCE.resetCache();
        INSTANCE.template = NativeSchematicTarget.inspect(source);
        INSTANCE.status = INSTANCE.template == null ? "unsupported" : "partial";
    }
    public static void clear() {
        INSTANCE.resetCache(); INSTANCE.template = null; INSTANCE.world = null; INSTANCE.player = null;
        INSTANCE.occupancy = null; INSTANCE.paused = false; INSTANCE.status = "inactive";
    }
    public static void reload() { INSTANCE.resetCache(); }
    public static String status() { return INSTANCE.status; }
    public static int cached() { return INSTANCE.slots.size(); }
    public static int total() { return INSTANCE.template == null ? 0 : INSTANCE.template.blocks().size(); }
    public static Set<Long> hiddenGhosts(SchematicHandler handler) {
        var state = INSTANCE; var mc = Minecraft.getInstance(); var tx = handler.getTransformation();
        if (state.world != mc.level || state.player != mc.player || !handler.isDeployed() || !settled(handler) ||
            state.anchor == null || !state.anchor.equals(tx.getAnchor())) return Set.of();
        var settings = tx.toSettings();
        return state.rotation == settings.getRotation() && state.mirror == settings.getMirror() ? state.ghostMask.positions() : Set.of();
    }
    public static Vec3i templateSize() { return INSTANCE.template == null ? Vec3i.ZERO : INSTANCE.template.size(); }
    public static boolean supportsGhostHiding() { return !INSTANCE.ghostMaskSaturated && GhostMask.supported(templateSize()); }
    public static void tick(SchematicHandler handler) { INSTANCE.update(handler); }

    private void resetCache() {
        for (var pos : outlines) Outliner.getInstance().remove(new OutlineKey(pos));
        outlines.clear(); slots.clear(); dirty.clear(); Arrays.fill(cache, null);
        ghostMask.clear(); ghostMaskSaturated = false;
        cycle.reset(); nextSlot = 0; anchor = null; stack = ItemStack.EMPTY; handSlot = -1;
    }
    private void update(SchematicHandler handler) {
        ticks++;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !OffhandProjectionBuffer.activeOffhand(handler)) {
            // Leave main-hand/ordinary play idle: no template traversal, craft
            // bounds snapshot, world reads, extra outlines or repeated cache reset.
            if (world != null || template != null || !status.equals("inactive")) clear();
            return;
        }
        if (!handler.isActive() || !handler.isDeployed()) { if (!slots.isEmpty() || !outlines.isEmpty()) resetCache(); status = "inactive"; return; }
        if (template == null) { status = "unsupported"; return; }
        var tx = handler.getTransformation();
        if (tx == null) { resetCache(); status = "moving"; return; }
        if (!settled(handler)) { if (!paused) resetCache(); paused = true; status = "moving"; return; }
        paused = false;
        var settings = tx.toSettings(); var nextAnchor = tx.getAnchor().immutable();
        var nextStack = BlueprintSyncEchoes.normalized(handler.getActiveSchematicItem());
        int nextHand = ((SchematicHandlerAccessor) handler).fieldwork$slot();
        if (world != mc.level || player != mc.player || anchor == null || !anchor.equals(nextAnchor) ||
            rotation != settings.getRotation() || mirror != settings.getMirror() || handSlot != nextHand || !ItemStack.matches(stack, nextStack)) {
            resetCache(); world = mc.level; player = mc.player; anchor = nextAnchor; rotation = settings.getRotation(); mirror = settings.getMirror(); handSlot = nextHand; stack = nextStack;
        }
        occupancy = WorldTargetSupport.capture(world);
        if (!occupancy.ready()) { resetCache(); status = "world_unavailable"; return; }
        cycle.run(dirty, cache.length, template.blocks().size(), index -> {
            var candidate = cache[index];
            if (candidate != null) { candidate.queued = false; compare(candidate); }
        }, index -> discover(index, settings));
        updateGhostMask(); present(); status = "partial";
    }
    private static boolean settled(SchematicHandler handler) {
        var tx = handler.getTransformation();
        if (tx == null) return false;
        var animation = (SchematicTransformationAccessor) tx;
        var positionTarget = Vec3.atLowerCornerOf(animation.fieldwork$positionTarget());
        return animation.fieldwork$rotation().settled() && tx.getScaleFB().settled() && tx.getScaleLR().settled()
            && animation.fieldwork$position().distanceToSqr(positionTarget) < 0.0001
            && animation.fieldwork$previousPosition().distanceToSqr(positionTarget) < 0.0001;
    }
    private void updateGhostMask() {
        if (handSlot != 40 || !supportsGhostHiding()) { ghostMask.clear(); return; }
        Set<Long> confirmed = new HashSet<>();
        for (var candidate : cache) if (candidate != null && candidate.result == BuildStatePolicy.Result.MATCH &&
            ticks - candidate.checked <= ComparisonBudget.MAX_RESULT_AGE && inRange(candidate.pos) && comparable(candidate)) confirmed.add(candidate.sourcePosition);
        ghostMask.update(confirmed, ticks);
    }
    private boolean inRange(BlockPos pos) { return pos.distSqr(Minecraft.getInstance().player.blockPosition()) <= (double) ComparisonBudget.RANGE * ComparisonBudget.RANGE; }
    private void discover(int index, StructurePlaceSettings settings) {
        var target = NativeSchematicTarget.sample(template, index, anchor, settings);
        if (target == null || target.state().isAir() || !inRange(target.position())) return;
        var existing = slots.get(target.position());
        if (existing != null) {
            if (!cache[existing].expected.equals(target.state())) { cache[existing].ambiguous = true; cache[existing].result = BuildStatePolicy.Result.UNVERIFIED; }
            return;
        }
        if (!occupancy.comparable(target.position())) return;
        int slot = nextSlot; nextSlot = (nextSlot + 1) % cache.length;
        var old = cache[slot];
        if (old != null) {
            slots.remove(old.pos); removeOutline(old.pos);
            // A cycling partial cache would otherwise restore/re-hide evicted
            // matches and rebuild the entire native ghost on every tick.
            ghostMaskSaturated = true;
        }
        var candidate = new Candidate(target, template.blocks().get(index).pos().asLong()); cache[slot] = candidate; slots.put(candidate.pos, slot); compare(candidate);
    }
    private void compare(Candidate candidate) {
        candidate.checked = ticks;
        if (candidate.ambiguous || !inRange(candidate.pos) || !comparable(candidate)) {
            candidate.result = BuildStatePolicy.Result.UNVERIFIED; removeOutline(candidate.pos); return;
        }
        candidate.result = cycle.read() ? BuildStatePolicy.compare(candidate.expected, world.getBlockState(candidate.pos)) : BuildStatePolicy.Result.UNVERIFIED;
    }
    private void present() {
        Set<BlockPos> next = new HashSet<>();
        for (var candidate : cache) {
            if (candidate == null || !BuildStatePolicy.error(candidate.result) || ticks - candidate.checked > ComparisonBudget.MAX_RESULT_AGE || !inRange(candidate.pos)) continue;
            // Recheck spatial eligibility of cached error boxes, not block states.
            if (!comparable(candidate)) continue;
            if (!cycle.outline()) break;
            int color = candidate.result == BuildStatePolicy.Result.WRONG_BLOCK ? 0xE94B4B : 0xF0A13C;
            Outliner.getInstance().showAABB(new OutlineKey(candidate.pos), new AABB(candidate.pos).inflate(0.001)).colored(color).lineWidth(1f / 32f).clearTextures();
            next.add(candidate.pos);
        }
        for (var old : Set.copyOf(outlines)) if (!next.contains(old)) removeOutline(old);
        outlines.addAll(next);
    }
    private void removeOutline(BlockPos pos) { if (outlines.remove(pos)) Outliner.getInstance().remove(new OutlineKey(pos)); }
    private boolean comparable(Candidate candidate) {
        if (candidate.comparableTick != ticks) {
            candidate.comparable = occupancy.comparable(candidate.pos);
            candidate.comparableTick = ticks;
        }
        return candidate.comparable;
    }
    public static void changed(Level level, BlockPos pos) {
        var state = INSTANCE;
        if (state.world != level) return;
        var index = state.slots.get(pos);
        if (index == null) return;
        var candidate = state.cache[index]; candidate.result = BuildStatePolicy.Result.UNVERIFIED; state.removeOutline(pos);
        if (!candidate.queued && state.dirty.size() < ComparisonBudget.CACHE_POSITIONS) { candidate.queued = true; state.dirty.addLast(index); }
    }
    public static void unloaded(Level level, ChunkPos chunk) {
        var state = INSTANCE;
        if (state.world != level) return;
        for (var pos : Set.copyOf(state.slots.keySet())) if (new ChunkPos(pos).equals(chunk)) changed(level, pos);
    }
}
