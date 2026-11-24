package com.bigmoon77.maidmind.integration.tlm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, value-only terrain grid centered on a maid.
 *
 * Capture this on the logical server thread. The returned grid contains no
 * Level, BlockState, BlockPos.MutableBlockPos, or entity references and can
 * therefore be passed to background AI code.
 */
public record TerrainObservation(
        int centerX,
        int centerY,
        int centerZ,
        int radius,
        int verticalRange,
        List<Cell> cells
) {
    public static final int DEFAULT_RADIUS = 8;
    public static final int DEFAULT_VERTICAL_RANGE = 4;
    public static final int NO_GROUND = Integer.MIN_VALUE;

    public TerrainObservation {
        cells = List.copyOf(cells);
    }

    public static TerrainObservation capture(EntityMaid maid) {
        return capture(maid, DEFAULT_RADIUS, DEFAULT_VERTICAL_RANGE);
    }

    public static TerrainObservation capture(EntityMaid maid, int radius, int verticalRange) {
        if (radius < 1 || radius > 16) {
            throw new IllegalArgumentException("radius must be between 1 and 16");
        }
        if (verticalRange < 1 || verticalRange > 16) {
            throw new IllegalArgumentException("verticalRange must be between 1 and 16");
        }

        Level level = maid.level();
        int centerX = BlockPos.containing(maid.getX(), maid.getY(), maid.getZ()).getX();
        int centerY = BlockPos.containing(maid.getX(), maid.getY(), maid.getZ()).getY() - 1;
        int centerZ = BlockPos.containing(maid.getX(), maid.getY(), maid.getZ()).getZ();

        List<Cell> cells = new ArrayList<>((radius * 2 + 1) * (radius * 2 + 1));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                cells.add(captureCell(level, pos, centerX + dx, centerY, centerZ + dz,
                        dx, dz, verticalRange, centerY));
            }
        }

        return new TerrainObservation(centerX, centerY, centerZ, radius, verticalRange, cells);
    }

    private static Cell captureCell(
            Level level,
            BlockPos.MutableBlockPos pos,
            int x,
            int referenceY,
            int z,
            int offsetX,
            int offsetZ,
            int verticalRange,
            int centerGroundY
    ) {
        for (int y = referenceY + verticalRange; y >= referenceY - verticalRange; y--) {
            pos.set(x, y, z);
            BlockState groundState = level.getBlockState(pos);

            if (groundState.getCollisionShape(level, pos).isEmpty()) {
                continue;
            }

            pos.set(x, y + 1, z);
            BlockState feetState = level.getBlockState(pos);
            FluidState feetFluid = feetState.getFluidState();
            boolean feetBlocked = !feetState.getCollisionShape(level, pos).isEmpty();

            pos.set(x, y + 2, z);
            BlockState headState = level.getBlockState(pos);
            boolean headBlocked = !headState.getCollisionShape(level, pos).isEmpty();

            boolean lava = feetFluid.is(FluidTags.LAVA)
                    || feetState.getFluidState().is(FluidTags.LAVA)
                    || headState.getFluidState().is(FluidTags.LAVA);
            boolean water = feetFluid.is(FluidTags.WATER)
                    || headState.getFluidState().is(FluidTags.WATER);

            return new Cell(
                    offsetX,
                    offsetZ,
                    y,
                    y - centerGroundY,
                    true,
                    !feetBlocked && !headBlocked && !lava,
                    feetBlocked,
                    headBlocked,
                    water,
                    lava,
                    BuiltInRegistries.BLOCK.getKey(groundState.getBlock()),
                    BuiltInRegistries.BLOCK.getKey(feetState.getBlock()),
                    BuiltInRegistries.BLOCK.getKey(headState.getBlock())
            );
        }

        return new Cell(
                offsetX,
                offsetZ,
                NO_GROUND,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.set(x, referenceY, z)).getBlock()),
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.set(x, referenceY + 1, z)).getBlock()),
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos.set(x, referenceY + 2, z)).getBlock())
        );
    }

    /**
     * Returns the cell at a relative horizontal coordinate, or null outside the grid.
     */
    public Cell cellAt(int offsetX, int offsetZ) {
        if (Math.abs(offsetX) > radius || Math.abs(offsetZ) > radius) {
            return null;
        }

        int width = radius * 2 + 1;
        return cells.get((offsetZ + radius) * width + offsetX + radius);
    }

    public record Cell(
            int offsetX,
            int offsetZ,
            int groundY,
            int heightDelta,
            boolean hasGround,
            boolean standable,
            boolean feetBlocked,
            boolean headBlocked,
            boolean water,
            boolean lava,
            ResourceLocation groundBlockId,
            ResourceLocation feetBlockId,
            ResourceLocation headBlockId
    ) {
    }
}
