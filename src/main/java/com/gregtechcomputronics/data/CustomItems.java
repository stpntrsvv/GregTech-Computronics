package com.gregtechcomputronics.data;

import net.minecraft.world.item.Item;

import com.gregtechcomputronics.ComputronicsMod;
import com.tterrag.registrate.util.entry.ItemEntry;

public class CustomItems {

    public static final ItemEntry<Item> PUNCH_CARD = ComputronicsMod.REGISTRATE
            .item("punch_card", Item::new)
            .register();

    public static void init() {}

    private CustomItems() {}
}
