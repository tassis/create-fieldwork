package dev.tassis.createfieldwork.schematic.comparison;

import dev.tassis.createfieldwork.mixin.schematic.StructureTemplateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Collections;
import java.util.List;

/** Native per-block placement, not hand-written rotation math or lerped render coordinates.
 * Keeps a read-only view of the exact template already loaded by Create's renderer.
 */
public final class NativeSchematicTarget {
    public record Template(List<StructureTemplate.StructureBlockInfo> blocks, Vec3i size) {}
    public record Expected(BlockPos position, BlockState state) {}
    private NativeSchematicTarget() {}
    public static Template inspect(StructureTemplate template) {
        if (template == null) return null;
        var size = template.getSize();
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0 || size.getX() > ComparisonBudget.MAX_TEMPLATE_EXTENT ||
            size.getY() > ComparisonBudget.MAX_TEMPLATE_EXTENT || size.getZ() > ComparisonBudget.MAX_TEMPLATE_EXTENT) return null;
        var palettes = ((StructureTemplateAccessor) template).fieldwork$palettes();
        if (palettes.size() != 1 || palettes.getFirst().blocks().size() > ComparisonBudget.MAX_TEMPLATE_BLOCKS) return null;
        return new Template(Collections.unmodifiableList(palettes.getFirst().blocks()), new Vec3i(size.getX(), size.getY(), size.getZ()));
    }
    public static Expected sample(Template template, int index, BlockPos anchor, StructurePlaceSettings settings) {
        if (!settings.getProcessors().isEmpty()) return null;
        var info = template.blocks.get(index);
        var pos = info.pos();
        var size = template.size;
        if (pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0 || pos.getX() >= size.getX() || pos.getY() >= size.getY() || pos.getZ() >= size.getZ()) return null;
        var state = info.state().mirror(settings.getMirror()).rotate(settings.getRotation());
        return new Expected(StructureTemplate.calculateRelativePosition(settings, pos).offset(anchor).immutable(), state);
    }
}
