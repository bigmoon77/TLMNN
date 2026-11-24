package com.bigmoon77.maidmind.integration.tlm;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import net.minecraftforge.common.MinecraftForge;

@LittleMaidExtension
public final class MaidMindTlmExtension implements ILittleMaid {
    public MaidMindTlmExtension() {
        MinecraftForge.EVENT_BUS.register(new MaidMindTlmEvents());
    }
}
