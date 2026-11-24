package com.bigmoon77.maidmind;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MaidMind.MOD_ID)
public final class MaidMind {
    public static final String MOD_ID = "maidmind";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MaidMind() {
        LOGGER.info("MaidMind is initializing.");
    }
}
