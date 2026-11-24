package com.bigmoon77.maidmind.integration.tlm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Immutable, value-only view of a maid's state.
 *
 * Capture this on the logical server thread. The resulting object can safely be
 * handed to later AI code without retaining a live entity or inventory handler.
 */
public record MaidSnapshot(
        UUID uuid,
        double x,
        double y,
        double z,
        ResourceLocation taskId,
        float health,
        List<InventoryEntry> inventory
) {
    public MaidSnapshot {
        inventory = List.copyOf(inventory);
    }

    public static MaidSnapshot capture(EntityMaid maid) {
        ItemStackHandler handler = maid.getMaidInv();
        List<InventoryEntry> inventory = new ArrayList<>(handler.getSlots());

        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            ResourceLocation itemId = stack.isEmpty()
                    ? BuiltInRegistries.ITEM.getKey(Items.AIR)
                    : BuiltInRegistries.ITEM.getKey(stack.getItem());

            inventory.add(new InventoryEntry(slot, itemId, stack.getCount()));
        }

        return new MaidSnapshot(
                maid.getUUID(),
                maid.getX(),
                maid.getY(),
                maid.getZ(),
                maid.getTask().getUid(),
                maid.getHealth(),
                inventory
        );
    }

    public record InventoryEntry(int slot, ResourceLocation itemId, int count) {
    }
}
