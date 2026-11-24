package com.bigmoon77.maidmind.client;

import com.bigmoon77.maidmind.MaidMind;
import com.bigmoon77.maidmind.integration.tlm.TerrainObservation;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MaidMind.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class TerrainDebugRenderer {
    private static final int REFRESH_INTERVAL_TICKS = 10;
    private static final double SEARCH_RADIUS = 32.0;

    private static final Map<UUID, TerrainObservation> OBSERVATIONS = new HashMap<>();
    private static boolean enabled;
    private static int refreshTicks;

    private TerrainDebugRenderer() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        while (TerrainDebugKeybinds.TOGGLE_TERRAIN_DEBUG.consumeClick()) {
            enabled = !enabled;
            refreshTicks = REFRESH_INTERVAL_TICKS;

            if (!enabled) {
                OBSERVATIONS.clear();
                MaidMind.LOGGER.info("Terrain grid debug rendering disabled.");
            } else {
                MaidMind.LOGGER.info("Terrain grid debug rendering enabled.");
            }
        }

        if (!enabled || minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (++refreshTicks < REFRESH_INTERVAL_TICKS) {
            return;
        }
        refreshTicks = 0;

        Level level = minecraft.level;
        Player player = minecraft.player;
        Map<UUID, TerrainObservation> refreshed = new HashMap<>();

        for (EntityMaid maid : level.getEntitiesOfClass(
                EntityMaid.class,
                player.getBoundingBox().inflate(SEARCH_RADIUS)
        )) {
            if (!maid.isRemoved()) {
                refreshed.put(maid.getUUID(), TerrainObservation.capture(maid));
            }
        }

        OBSERVATIONS.clear();
        OBSERVATIONS.putAll(refreshed);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (!enabled
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || OBSERVATIONS.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        Camera camera = event.getCamera();
        var cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        VertexConsumer lines = minecraft.renderBuffers()
                .bufferSource()
                .getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
        RenderSystem.lineWidth(2.0F);

        for (TerrainObservation observation : OBSERVATIONS.values()) {
            drawObservation(poseStack, lines, observation);
        }

        poseStack.popPose();
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        RenderSystem.lineWidth(1.0F);
    }

    private static void drawObservation(
            PoseStack poseStack,
            VertexConsumer lines,
            TerrainObservation observation
    ) {
        for (TerrainObservation.Cell cell : observation.cells()) {
            if (!cell.hasGround()) {
                continue;
            }

            float red;
            float green;
            float blue;

            if (cell.lava()) {
                red = 1.0F;
                green = 0.12F;
                blue = 0.05F;
            } else if (cell.water()) {
                red = 0.1F;
                green = 0.45F;
                blue = 1.0F;
            } else if (cell.bodyBlocked()) {
                red = 1.0F;
                green = 0.25F;
                blue = 0.08F;
            } else if (!cell.standable()) {
                red = 1.0F;
                green = 0.62F;
                blue = 0.08F;
            } else if (cell.heightDelta() > 1) {
                red = 0.8F;
                green = 0.25F;
                blue = 1.0F;
            } else if (cell.heightDelta() < -1) {
                red = 0.15F;
                green = 0.75F;
                blue = 1.0F;
            } else {
                red = 0.15F;
                green = 1.0F;
                blue = 0.25F;
            }

            if (cell.offsetX() == 0 && cell.offsetZ() == 0) {
                red = 1.0F;
                green = 1.0F;
                blue = 1.0F;
            }

            double x = observation.centerX() + cell.offsetX();
            double y = cell.groundY() + 1.005;
            double z = observation.centerZ() + cell.offsetZ();
            AABB box = new AABB(x, y, z, x + 1.0, y + 0.025, z + 1.0);

            LevelRenderer.renderLineBox(
                    poseStack,
                    lines,
                    box,
                    red,
                    green,
                    blue,
                    0.95F
            );
        }
    }
}
