package com.bigmoon77.maidmind.integration.tlm;

import com.bigmoon77.maidmind.MaidMind;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MaidMindTlmEvents {
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EntityMaid maid)) {
            return;
        }

        MaidSnapshot snapshot = MaidSnapshot.capture(maid);
        MaidMind.LOGGER.info(
                "Detected Touhou Little Maid: uuid={}, task={}, inventorySlots={}, occupiedSlots={}",
                snapshot.uuid(),
                snapshot.taskId(),
                snapshot.inventory().size(),
                snapshot.inventory().stream().filter(entry -> entry.count() > 0).count()
        );
    }
}
