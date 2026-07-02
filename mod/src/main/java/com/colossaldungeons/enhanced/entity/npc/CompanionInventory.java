package com.colossaldungeons.enhanced.entity.npc;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Simple 9-slot container for companion entity inventory.
 * Allows companions to carry items and equipment.
 * Serializable for world persistence via NBT.
 */
public class CompanionInventory implements Container {

    public static final int INVENTORY_SIZE = 9;
    private final NonNullList<ItemStack> items;

    public CompanionInventory() {
        this.items = NonNullList.withSize(INVENTORY_SIZE, ItemStack.EMPTY);
    }

    @Override
    public int getContainerSize() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= INVENTORY_SIZE) return ItemStack.EMPTY;
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return ContainerHelper.removeItem(items, slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot >= 0 && slot < INVENTORY_SIZE) {
            items.set(slot, stack);
            if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
                stack.setCount(getMaxStackSize());
            }
        }
    }

    @Override
    public void setChanged() {
        // Mark dirty for saving
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    /**
     * Tries to add an item to the first available slot.
     *
     * @param stack the item to add
     * @return true if the item was added successfully
     */
    public boolean addItem(ItemStack stack) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copy());
                return true;
            }
            // Try to stack with existing items
            ItemStack existing = items.get(i);
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, stack.getCount());
                existing.grow(toAdd);
                stack.shrink(toAdd);
                if (stack.isEmpty()) return true;
            }
        }
        return false;
    }

    /**
     * Serializes the inventory to NBT.
     *
     * @return the serialized tag
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag listTag = new ListTag();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (!items.get(i).isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                listTag.add(items.get(i).save(itemTag));
            }
        }
        tag.put("Items", listTag);
        return tag;
    }

    /**
     * Deserializes the inventory from NBT.
     *
     * @param tag the tag to read from
     */
    public void load(CompoundTag tag) {
        clearContent();
        ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot < INVENTORY_SIZE) {
                items.set(slot, ItemStack.parseOptional(null, itemTag));
            }
        }
    }

    /**
     * Gets the count of occupied slots.
     *
     * @return number of non-empty slots
     */
    public int getUsedSlots() {
        int count = 0;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) count++;
        }
        return count;
    }
}
