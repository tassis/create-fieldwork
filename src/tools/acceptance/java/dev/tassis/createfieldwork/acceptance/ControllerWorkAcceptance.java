package dev.tassis.createfieldwork.acceptance;

import com.simibubi.create.content.redstone.link.controller.LinkedControllerServerHandler;
import dev.tassis.createfieldwork.platform.FieldworkStateAccess;
import net.minecraft.server.level.ServerLevel;

import java.util.function.BiConsumer;

/** Red contract for Fieldwork's extra controller work, excluding Create's native TTL loop. */
final class ControllerWorkAcceptance {
    private final ServerLevel level;
    private final BiConsumer<String, Boolean> check;

    ControllerWorkAcceptance(ServerLevel level, BiConsumer<String, Boolean> check) {
        this.level = level;
        this.check = check;
    }

    void run() {
        var idle = ControllerWorkMetrics.measure(() -> LinkedControllerServerHandler.tick(level));
        check.accept("one idle Create controller tick performs no Fieldwork full sweep; observed=" + idle.fullSweeps(),
            idle.fullSweeps() == 0);
        check.accept("empty idle controller tick reconciles no endpoints; observed=" + idle.endpointReconciliations(),
            idle.endpointReconciliations() == 0);

        var controllers = FieldworkStateAccess.get(level.getServer()).controllers();
        int alignment = 0;
        ControllerWorkMetrics.Snapshot step;
        do {
            step = ControllerWorkMetrics.measure(controllers::postTick);
            alignment++;
        } while (step.fullSweeps() == 0 && alignment <= 200);
        check.accept("server-relative safety cadence reaches one sweep within 200 posts; posts=" + alignment,
            alignment <= 200 && step.fullSweeps() == 1);
        var first199 = ControllerWorkMetrics.measure(() -> {
            for (int i = 0; i < 199; i++) controllers.postTick();
        });
        check.accept("199 post ticks after a sweep perform no complete sweep; observed=" + first199.fullSweeps(),
            first199.fullSweeps() == 0);
        check.accept("199 empty post ticks reconcile no endpoints; observed=" + first199.endpointReconciliations(),
            first199.endpointReconciliations() == 0);
        var twoHundredth = ControllerWorkMetrics.measure(controllers::postTick);
        check.accept("post tick 200 performs exactly one complete safety sweep; observed=" + twoHundredth.fullSweeps(),
            twoHundredth.fullSweeps() == 1);
        check.accept("empty safety sweep visits no controller endpoints; observed=" + twoHundredth.endpointReconciliations(),
            twoHundredth.endpointReconciliations() == 0);
    }
}
