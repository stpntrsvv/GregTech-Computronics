package com.gregtechcomputronics.data;

import com.gregtechcomputronics.ComputronicsMod;
import com.gregtechcomputronics.common.item.PunchedCardItem;
import com.tterrag.registrate.util.entry.ItemEntry;

public class CustomItems {

    public static final ItemEntry<PunchedCardItem> PUNCH_CARD = ComputronicsMod.REGISTRATE
            .item("punch_card", PunchedCardItem::new)
            .register();

    public static void init() {}

    private CustomItems() {}
}
