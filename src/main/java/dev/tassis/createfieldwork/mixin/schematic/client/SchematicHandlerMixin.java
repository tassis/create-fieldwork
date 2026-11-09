package dev.tassis.createfieldwork.mixin.schematic.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import dev.tassis.createfieldwork.schematic.input.BlueprintSelection;
import dev.tassis.createfieldwork.schematic.BlueprintSyncEchoes;
import dev.tassis.createfieldwork.schematic.client.SchematicClientState;
import dev.tassis.createfieldwork.schematic.client.ControllerGesture;
import dev.tassis.createfieldwork.schematic.client.OffhandProjectionBuffer;
import dev.tassis.createfieldwork.schematic.client.SchematicGhostRenderer;
import net.minecraft.world.level.block.Mirror;
import java.util.Set;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Independent native hand selection; no Traveler source or double-patch ordering assumption. */
@Mixin(value = SchematicHandler.class, remap = false)
public abstract class SchematicHandlerMixin {
    @Shadow private int activeHotbarSlot;
    @Shadow private ItemStack activeSchematicItem;
    @Shadow private boolean active;
    @Shadow private int syncCooldown;
    @Shadow @org.spongepowered.asm.mixin.Final private SchematicRenderer[] renderers;
    @Unique private int fieldwork$previousSlot = -1;
    @Unique private ItemStack fieldwork$previous = ItemStack.EMPTY;
    @Unique private Level fieldwork$world;
    @Unique private Player fieldwork$player;
    @Unique private final BlueprintSyncEchoes fieldwork$echoes = new BlueprintSyncEchoes();

    @Inject(method = "findBlueprintInHand", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void fieldwork$select(Player player, CallbackInfoReturnable<ItemStack> cir) {
        var minecraft = Minecraft.getInstance();
        var selected = BlueprintSelection.select(player);
        boolean worldChanged = fieldwork$world != minecraft.level || fieldwork$player != player;
        if (worldChanged) fieldwork$echoes.clear();
        fieldwork$world = minecraft.level; fieldwork$player = player;
        if (selected == null) {
            activeSchematicItem = null; activeHotbarSlot = 0; active = false; syncCooldown = 0;
            fieldwork$previousSlot = -1; fieldwork$previous = ItemStack.EMPTY; fieldwork$echoes.clear();
            cir.setReturnValue(null); return;
        }
        var snapshot = BlueprintSyncEchoes.normalized(selected.stack());
        boolean changed = !ItemStack.matches(fieldwork$previous, snapshot);
        boolean echoed = changed && fieldwork$echoes.consume(selected.slot(), selected.stack());
        if (worldChanged || fieldwork$previousSlot != selected.slot() || changed && !echoed) {
            active = false; syncCooldown = 0; // initialize same-name but distinct deployment, don't sync old state into new slot
        }
        activeSchematicItem = selected.stack(); activeHotbarSlot = selected.slot();
        fieldwork$previousSlot = selected.slot(); fieldwork$previous = snapshot;
        cir.setReturnValue(selected.stack());
    }
    @Inject(method = "sync", at = @At("HEAD"), require = 1, allow = 1)
    private void fieldwork$expectEcho(CallbackInfo ci) {
        var handler = (SchematicHandler) (Object) this;
        if (BlueprintSelection.valid(activeSchematicItem) && handler.getTransformation() != null)
            fieldwork$echoes.expect(activeHotbarSlot, activeSchematicItem, handler.getTransformation().getAnchor(), handler.getTransformation().toSettings(), handler.isDeployed());
    }
    @WrapOperation(method = "setupRenderer", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/schematics/SchematicItem;loadSchematic(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;"), require = 1, allow = 1)
    private StructureTemplate fieldwork$capture(Level level, ItemStack stack, Operation<StructureTemplate> upstream) {
        var template = upstream.call(level, stack);
        // Only offhand assistance consumes the template; main hand retains its
        // native preview without comparison discovery or extra HUD state.
        if (activeHotbarSlot == BlueprintSelection.OFFHAND_SLOT) SchematicClientState.capture(template);
        else SchematicClientState.clear();
        return template;
    }
    @Inject(method = "onMouseInput", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void fieldwork$controllerFirst(int button, boolean pressed, CallbackInfoReturnable<Boolean> cir) {
        if (OffhandProjectionBuffer.activeOffhand((SchematicHandler) (Object) this) && button == 1 && pressed && ControllerGesture.preferNative()) cir.setReturnValue(false);
    }
    @WrapOperation(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/createmod/catnip/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/schematics/client/SchematicRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/createmod/catnip/render/SuperRenderTypeBuffer;)V"), require = 3, allow = 3)
    private void fieldwork$offhandOpacity(SchematicRenderer renderer, PoseStack pose, SuperRenderTypeBuffer buffers, Operation<Void> upstream) {
        boolean offhand = OffhandProjectionBuffer.activeOffhand((SchematicHandler) (Object) this);
        Mirror variant = renderer == renderers[1] ? Mirror.FRONT_BACK : renderer == renderers[2] ? Mirror.LEFT_RIGHT : Mirror.NONE;
        ((SchematicGhostRenderer) renderer).fieldwork$visibility(offhand ? SchematicClientState.hiddenGhosts((SchematicHandler) (Object) this) : Set.of(), variant, SchematicClientState.templateSize());
        upstream.call(renderer, pose, OffhandProjectionBuffer.forGhost((SchematicHandler) (Object) this, buffers));
    }
}
