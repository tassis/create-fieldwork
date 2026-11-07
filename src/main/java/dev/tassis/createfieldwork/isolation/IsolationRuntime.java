package dev.tassis.createfieldwork.isolation;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.tassis.createfieldwork.content.IsolatorBlock;
import dev.tassis.createfieldwork.registry.FieldworkContent;
import dev.tassis.createfieldwork.server.FieldworkStateAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Cold-path world integration. No block enumeration is performed by a radio scope query. */
public final class IsolationRuntime {
    private IsolationRuntime() {}

    public static CraftKey key(SubLevel craft) { return new CraftKey(craft.getLevel().dimension(), craft.getUniqueId()); }

    public static void markerChanged(ServerLevel level, BlockPos pos, boolean present) {
        var state = FieldworkStateAccess.get(level.getServer());
        if (state.transitions().active()) return; // outer transaction owns complete reconstruction
        // A callback may replace this position again before an older onRemove
        // or onLoad finishes. Never apply that stale event to a newer marker.
        if (level.getBlockState(pos).is(FieldworkContent.ISOLATOR.get()) != present) return;
        var craft = Sable.HELPER.getContaining(level, pos);
        if (craft == null) { visual(level, pos, false); return; }
        var key = key(craft);
        if (state.registry().snapshot(key).status() != IsolationRegistry.Status.READY) return;
        // Chunk mutation wrapper normally handles this; onLoad is an idempotent fallback.
        if (state.registry().snapshot(key).positions().contains(pos) == present) return;
        var beforeScope = state.registry().scope(key);
        var targets = state.refresh().affected(Set.of(level.dimension()), Set.of(key),
            Set.of(new TransitionCoordinator.Position(level.dimension(), pos)));
        state.registry().marker(key, pos, present);
        if (!beforeScope.equals(state.registry().scope(key))) state.refresh().refresh(null, targets);
        visual(level, pos, present);
    }

    public static void visual(ServerLevel level, BlockPos pos, boolean active) {
        var block = level.getBlockState(pos);
        if (block.is(FieldworkContent.ISOLATOR.get()) && block.getValue(IsolatorBlock.ACTIVE) != active)
            level.setBlock(pos, block.setValue(IsolatorBlock.ACTIVE, active), 2);
    }

    public static Set<BlockPos> markers(ServerSubLevel craft) {
        Set<BlockPos> result = new HashSet<>();
        for (var holder : craft.getPlot().getLoadedChunks()) {
            var chunk = holder.getChunk();
            var sections = chunk.getSections();
            for (int i = 0; i < sections.length; i++) {
                var section = sections[i];
                if (section.hasOnlyAir() || !section.maybeHas(block -> block.is(FieldworkContent.ISOLATOR.get()))) continue;
                int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++)
                    if (section.getBlockState(x, y, z).is(FieldworkContent.ISOLATOR.get()))
                        result.add(new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z));
            }
        }
        return Set.copyOf(result);
    }

    public static List<BlockPos> blocks(ServerSubLevel craft) {
        List<BlockPos> result = new ArrayList<>();
        for (var holder : craft.getPlot().getLoadedChunks()) {
            var box = holder.getBoundingBox();
            if (box == null) continue;
            for (int x = box.minX(); x <= box.maxX(); x++) for (int y = box.minY(); y <= box.maxY(); y++) for (int z = box.minZ(); z <= box.maxZ(); z++) {
                BlockPos pos = new BlockPos(holder.getPos().getMinBlockX() + x, y, holder.getPos().getMinBlockZ() + z);
                if (!craft.getLevel().getBlockState(pos).isAir()) result.add(pos);
            }
        }
        return result;
    }
}
