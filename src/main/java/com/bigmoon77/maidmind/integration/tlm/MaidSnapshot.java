package com.bigmoon77.maidmind.integration.tlm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable, world-thread-created view of a maid's current state.
 *
 * The snapshot deliberately contains plain values instead of live inventory handlers,
 * so later AI and learning code can consume it without retaining mutable game objects.
 */
public record MaidSnapshot(
        UUID uuid,
        BlockPos position,
        ResourceLocation taskId,
        float health,
        List<InventoryEntry> inventory
) {
    public MaidSnapshot {
        position = position.immutable();
        inventory = List.copyOf(inventory);
    }

    public static MaidSnapshot capture(EntityMaid maid) {
        ItemStackHandler handler = maid.getMaidInv();
        List<InventoryEntry> inventory = new ArrayList<>(handler.getSlots());

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            ResourceLocation itemId = stack.isEmpty()
                    ? BuiltInRegistries.ITEM.getKey(net.minecraft.world.item.Items.AIR)
                    : BuiltInRegistries.ITEM.getKey(stack.getItem());

            inventory.add(new InventoryEntry(slot, itemId, stack.getCount()));
        }

        return new MaidSnapshot(
                maid.getUUID(),
                maid.blockPosition(),
                maid.getTask().getUid(),
                maid.getHealth(),
                inventory
        );
    }

    public record InventoryEntry(int slot, ResourceLocation itemId, int count) {
    }
}
