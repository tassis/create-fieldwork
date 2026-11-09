package dev.tassis.createfieldwork.wireless.publication;

import dev.tassis.createfieldwork.wireless.scope.Scope;
import dev.tassis.createfieldwork.wireless.scope.ScopeResolver;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.Create;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator;
import dev.tassis.createfieldwork.isolator.lifecycle.TransitionCoordinator.Position;
import dev.tassis.createfieldwork.isolator.state.CraftKey;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.tassis.createfieldwork.platform.FieldworkServerState;
import dev.tassis.createfieldwork.wireless.refresh.WirelessRefresh;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** Cold-path index for loaded transmitter blocks whose network registration has not happened yet. */
public final class SourcePublications {
    private final FieldworkServerState state;
    private final Map<Position, Boolean> pending = new HashMap<>(); // true while the block write is in progress
    private boolean publicUnknown;

    public SourcePublications(FieldworkServerState state) {
        this.state = state;
    }

    public static boolean transmitter(BlockState block) {
        return block.is(AllBlocks.REDSTONE_LINK.get()) && !block.getValue(RedstoneLinkBlock.RECEIVER);
    }

    public void expect(ServerLevel level, BlockPos pos, boolean beforeWrite) {
        state.checkThread();
        pending.put(new Position(level.dimension(), pos), beforeWrite);
        state.invalidateVisibility();
    }

    public void written(ServerLevel level, BlockPos pos) {
        state.checkThread();
        var key = new Position(level.dimension(), pos);
        if (pending.containsKey(key)) {
            pending.put(key, false);
            state.invalidateVisibility();
        }
    }

    public void published(ServerLevel level, BlockPos pos) {
        state.checkThread();
        if (pending.remove(new Position(level.dimension(), pos)) != null) {
            state.invalidateVisibility();
        }
    }

    public boolean pendingWorld() {
        state.checkThread();
        return publicUnknown;
    }

    public boolean unresolvedIn(ServerLevel level, Set<CraftKey> crafts, Set<Position> positions) {
        state.checkThread();

        for (var position : pending.keySet()) {
            if (!position.dimension().equals(level.dimension())) {
                continue;
            }
            if (WirelessRefresh.involved(level, position.pos(), crafts, positions)) {
                return true;
            }

            // A reserved position whose owner has not been published cannot be
            // confidently excluded from a craft operation in this dimension.
            if (Sable.HELPER.getContaining(level, position.pos()) == null
                && SubLevelContainer.getContainer(level).inBounds(position.pos())
                && crafts.stream().anyMatch(key -> key.dimension().equals(level.dimension()))) {
                return true;
            }
        }

        return false;
    }

    public void unloaded(ServerLevel level, ChunkPos chunk) {
        state.checkThread();
        boolean changed = pending.keySet().removeIf(position -> position.dimension().equals(level.dimension())
            && (position.pos().getX() >> 4) == chunk.x && (position.pos().getZ() >> 4) == chunk.z);
        if (changed) {
            state.invalidateVisibility();
        }
    }

    public void trackChunk(ServerLevel level, LevelChunk chunk) {
        state.checkThread();

        var sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            var section = sections[i];
            if (section.hasOnlyAir() || !section.maybeHas(SourcePublications::transmitter)) {
                continue;
            }

            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        if (!transmitter(section.getBlockState(x, y, z))) {
                            continue;
                        }

                        var pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                        var entity = chunk.getBlockEntity(pos);
                        var behaviour = entity instanceof RedstoneLinkBlockEntity link
                            ? link.getBehaviour(LinkBehaviour.TYPE)
                            : null;
                        var bucket = behaviour == null
                            ? null
                            : Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(level).get(behaviour.getNetworkKey());

                        if (bucket != null && bucket.contains(behaviour)) {
                            published(level, pos);
                        } else {
                            expect(level, pos, false);
                        }
                    }
                }
            }
        }
    }

    /** Called on authoritative changes, not in a wireless/condition query. */
    public void reclassify() {
        reclassify(null);
    }

    /** Prepare the post-commit classification while the transaction's barrier is still held. */
    public void reclassify(TransitionCoordinator.View view) {
        state.checkThread();

        publicUnknown = false;
        Iterator<Map.Entry<Position, Boolean>> entries = pending.entrySet().iterator();

        while (entries.hasNext()) {
            var entry = entries.next();
            var level = state.server().getLevel(entry.getKey().dimension());
            var pos = entry.getKey().pos();
            if (level == null || (!entry.getValue() && (!level.hasChunkAt(pos) || !transmitter(level.getBlockState(pos))))) {
                entries.remove();
                continue;
            }

            var scope = ScopeResolver.at(level, pos, view);
            if (scope == Scope.WORLD || scope == Scope.PENDING) {
                publicUnknown = true;
            }
        }
    }

    public void clear() {
        state.checkThread();
        pending.clear();
        publicUnknown = false;
    }
}
