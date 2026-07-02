package com.gregtechcomputronics.data;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.gregtechcomputronics.ComputronicsMod;
import com.tterrag.registrate.util.entry.BlockEntry;

public class CustomBlocks {

    public static final BlockEntry<Block> ANALOG_COMPUTING_FRAMEWORK = ComputronicsMod.REGISTRATE
            .block("analog_computing_framework", Block::new)
            .initialProperties(() -> Blocks.COBBLESTONE)
            .simpleItem()
            .register();

    public static void init() {}

    private CustomBlocks() {}
}
