package com.bigmoon77.maidmind.integration.tlm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable result of a block-only ray test.
 *
 * Use trace() with the actual projectile launch position and intended target
 * point when evaluating ranged attacks. Entity collision and projectile
 * trajectory/gravity are intentionally handled by the caller.
 */
public record LineOfSightObservation(
        boolean clear,
        double distance,
        double hitX,
        double hitY,
        double hitZ,
        ResourceLocation blockingBlockId
) {
    public static LineOfSightObservation capture(Entity observer, Entity target) {
        return trace(observer.level(), observer.getEyePosition(), target.getEyePosition(), observer);
    }

    public static LineOfSightObservation trace(Level level, Vec3 start, Vec3 end, Entity context) {
        BlockHitResult hit = level.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                context
        ));

        boolean clear = hit.getType() == BlockHitResult.Type.MISS;
        ResourceLocation blockId = null;
        if (!clear) {
            BlockPos blockPos = hit.getBlockPos();
            blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(blockPos).getBlock());
        }

        return new LineOfSightObservation(
                clear,
                start.distanceTo(end),
                hit.getLocation().x,
                hit.getLocation().y,
                hit.getLocation().z,
                blockId
        );
    }
}
