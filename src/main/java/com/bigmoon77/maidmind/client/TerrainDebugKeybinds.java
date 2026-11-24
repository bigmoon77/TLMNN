package com.bigmoon77.maidmind.client;

import com.bigmoon77.maidmind.MaidMind;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = MaidMind.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TerrainDebugKeybinds {
    public static final KeyMapping TOGGLE_TERRAIN_DEBUG = new KeyMapping(
            "key.maidmind.toggle_terrain_debug",
            GLFW.GLFW_KEY_K,
            "key.categories.misc"
    );
    public static final KeyMapping TOGGLE_LINE_OF_SIGHT_DEBUG = new KeyMapping(
            "key.maidmind.toggle_line_of_sight_debug",
            GLFW.GLFW_KEY_L,
            "key.categories.misc"
    );

    private TerrainDebugKeybinds() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_TERRAIN_DEBUG);
        event.register(TOGGLE_LINE_OF_SIGHT_DEBUG);
    }
}
