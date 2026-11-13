package dev.tassis.createfieldwork.acceptance;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.foundation.utility.CreatePaths;
import dev.tassis.createfieldwork.platform.registry.FieldworkContent;
import dev.tassis.createfieldwork.schematic.table.ScribingTableBlockEntity;
import dev.tassis.createfieldwork.schematic.table.ScribingTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

final class ScribingFixtures {
    static final BlockPos TABLE = new BlockPos(4096, 120, 4096);
    static final BlockPos ANCHOR = TABLE.offset(16, 0, 16);
    static final String OWNER = "fieldwork-scribing-acceptance";
    static final String FILE = "asymmetric.nbt";
    final ServerLevel level;
    private final java.util.List<ControllerAcceptance.Transport> transports = new java.util.ArrayList<>();
    private final java.util.Set<net.minecraft.world.level.ChunkPos> pinned = new java.util.HashSet<>();

    ScribingFixtures(ServerLevel level) { this.level = level; }

    ScribingTableBlockEntity table(BlockPos position) {
        level.setBlock(position, FieldworkContent.SCRIBING_TABLE.get().defaultBlockState(), 3);
        return (ScribingTableBlockEntity) level.getBlockEntity(position);
    }

    ServerPlayer actor(BlockPos position) {
        var actor = new ServerPlayer(level.getServer(), level,
            new GameProfile(UUID.randomUUID(), "ScribingAcceptance"), ClientInformation.createDefault());
        var transport = new ControllerAcceptance.Transport();
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(transport);
        transports.add(transport);
        new ServerGamePacketListenerImpl(level.getServer(), transport, actor,
            CommonListenerCookie.createInitial(actor.getGameProfile(), false));
        actor.setPos(position.getX() + .5, position.getY(), position.getZ() + 2);
        return actor;
    }

    void close() {
        transports.forEach(transport -> transport.embedded.finishAndReleaseAll());
        pinned.forEach(pos -> level.setChunkForced(pos.x, pos.z, false));
    }

    void pin(BlockPos position) {
        var chunk = new net.minecraft.world.level.ChunkPos(position);
        pinned.add(chunk);
        level.setChunkForced(chunk.x, chunk.z, true);
    }

    static void advance(ScribingTableBlockEntity table, int ticks) {
        for (int tick = 0; tick < ticks; tick++) table.serverTick();
    }

    static void complete(ScribingTableBlockEntity table) {
        advance(table, ScribingTableBlockEntity.PRINT_TICKS);
    }

    ScribingTableMenu menu(ScribingTableBlockEntity table, ServerPlayer player) {
        var menu = new ScribingTableMenu(17, player.getInventory(), table);
        player.containerMenu = menu;
        return menu;
    }

    ItemStack blueprint(BlockPos anchor) {
        ItemStack stack = AllItems.SCHEMATIC.asStack();
        stack.set(AllDataComponents.SCHEMATIC_OWNER, OWNER);
        stack.set(AllDataComponents.SCHEMATIC_FILE, FILE);
        stack.set(AllDataComponents.SCHEMATIC_BOUNDS, new Vec3i(3, 1, 3));
        stack.set(AllDataComponents.SCHEMATIC_ANCHOR, anchor);
        stack.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
        stack.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE);
        stack.set(AllDataComponents.SCHEMATIC_MIRROR, Mirror.NONE);
        return stack;
    }

    void writeTemplate() throws Exception {
        writeTemplate(false);
    }

    void writeTemplate(boolean dirt) throws Exception {
        BlockPos source = TABLE.offset(-48, 0, -48);
        for (BlockPos pos : BlockPos.betweenClosed(source, source.offset(2, 0, 2))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }
        var material = (dirt ? Blocks.DIRT : Blocks.STONE).defaultBlockState();
        level.setBlock(source, material, 2);
        level.setBlock(source.east(), material, 2);
        level.setBlock(source.south(2), material, 2);
        level.setBlock(source.offset(2, 0, 1), Blocks.OAK_STAIRS.defaultBlockState(), 2);
        var template = new StructureTemplate();
        var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, source.getX() + 1.5, source.getY(), source.getZ() + 1.5);
        level.addFreshEntity(stand);
        template.fillFromWorld(level, source, new Vec3i(3, 1, 3), true, Blocks.STRUCTURE_VOID);
        stand.discard();
        Path path = CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(OWNER).resolve(FILE);
        Files.createDirectories(path.getParent());
        NbtIo.writeCompressed(template.save(new CompoundTag()), path);
    }

    void deleteTemplate() throws Exception {
        Files.delete(CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(OWNER).resolve(FILE));
    }

    ItemStack nativeOutput(ItemStack blueprint, boolean clipboard, int mode, boolean replaceEntities, BlockPos cannonPos) {
        level.setBlock(cannonPos, AllBlocks.SCHEMATICANNON.getDefaultState(), 3);
        var cannon = (SchematicannonBlockEntity) level.getBlockEntity(cannonPos);
        cannon.replaceMode = mode;
        cannon.replaceBlockEntities = replaceEntities;
        cannon.printer.loadSchematic(blueprint, level, true);
        cannon.updateChecklist();
        return clipboard ? cannon.checklist.createWrittenClipboard() : cannon.checklist.createWrittenBook();
    }
}
