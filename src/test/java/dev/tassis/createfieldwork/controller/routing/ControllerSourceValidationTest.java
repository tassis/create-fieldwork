package dev.tassis.createfieldwork.controller.routing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerSourceValidationTest {
    @Test
    void equivalentCapturesCompareByServerProvenanceRatherThanWrapperIdentity() {
        UUID player = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        var before = source(player, session, device, InteractionHand.MAIN_HAND);
        var after = source(player, session, device, InteractionHand.MAIN_HAND);

        assertTrue(ControllerSourceValidation.sameProvenance(before, after));
    }

    @Test
    void sessionHandAndDeviceChangesInvalidateProvenance() {
        UUID player = UUID.randomUUID();
        UUID session = UUID.randomUUID();
        UUID device = UUID.randomUUID();
        var baseline = source(player, session, device, InteractionHand.MAIN_HAND);

        assertFalse(ControllerSourceValidation.sameProvenance(
            baseline,
            source(player, UUID.randomUUID(), device, InteractionHand.MAIN_HAND)
        ));
        assertFalse(ControllerSourceValidation.sameProvenance(
            baseline,
            source(player, session, device, InteractionHand.OFF_HAND)
        ));
        assertFalse(ControllerSourceValidation.sameProvenance(
            baseline,
            source(player, session, UUID.randomUUID(), InteractionHand.MAIN_HAND)
        ));
    }

    @Test
    void nullProvenanceOnlyMatchesNull() {
        var source = source(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            InteractionHand.MAIN_HAND
        );

        assertTrue(ControllerSourceValidation.sameProvenance(null, null));
        assertFalse(ControllerSourceValidation.sameProvenance(null, source));
        assertFalse(ControllerSourceValidation.sameProvenance(source, null));
    }

    private static ControllerSources.Source source(UUID player, UUID session, UUID device,
                                                   InteractionHand hand) {
        return new ControllerSources.Source(
            player,
            session,
            Level.OVERWORLD,
            BlockPos.ZERO,
            new WeakReference<BlockEntity>(null),
            device,
            7,
            ItemContainerContents.EMPTY,
            hand
        );
    }
}
