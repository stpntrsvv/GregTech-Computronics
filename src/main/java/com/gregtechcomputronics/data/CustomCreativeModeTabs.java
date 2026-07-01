package com.gregtechcomputronics.data;

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.gregtechcomputronics.ComputronicsMod;
import com.tterrag.registrate.util.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;

public class CustomCreativeModeTabs {

    public static final RegistryEntry<CreativeModeTab> MAIN = ComputronicsMod.REGISTRATE.defaultCreativeTab("main",
            builder -> builder.displayItems(new DisplayItemsGenerator("main", ComputronicsMod.REGISTRATE))
                    .icon(() -> new ItemStack(Items.PAPER))
                    .title(Component.translatable("itemGroup.gtcomputronics.main"))
                    .build())
            .register();

    public static void init() {
        ComputronicsMod.REGISTRATE.creativeModeTab(MAIN);
    }

    private CustomCreativeModeTabs() {}

    private record DisplayItemsGenerator(String name, GTRegistrate registrate)
            implements CreativeModeTab.DisplayItemsGenerator {

        @Override
        public void accept(@NotNull CreativeModeTab.ItemDisplayParameters itemDisplayParameters,
                           @NotNull CreativeModeTab.Output output) {
            var tab = registrate.get(name, Registries.CREATIVE_MODE_TAB);
            for (var entry : registrate.getAll(Registries.ITEM)) {
                if (!registrate.isInCreativeTab(entry, tab)) {
                    continue;
                }
                Item item = entry.get();
                output.accept(item);
            }
        }
    }
}
