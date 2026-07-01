package com.gregtechcomputronics.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PunchedCardItem extends Item {

    public static final String RESEARCH_ID = "ResearchID";
    public static final String START_SIGNAL = "StartSignal";
    public static final String TARGET_SIGNAL = "TargetSignal";
    public static final String COMPLETED = "Completed";

    private static final String DEFAULT_RESEARCH_ID = "basic_tabulation";
    private static final int DEFAULT_START_SIGNAL = 5;
    private static final int DEFAULT_TARGET_SIGNAL = 7;

    public PunchedCardItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static String getResearchId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(RESEARCH_ID)) {
            return DEFAULT_RESEARCH_ID;
        }
        return tag.getString(RESEARCH_ID);
    }

    public static int getStartSignal(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(START_SIGNAL)) {
            return DEFAULT_START_SIGNAL;
        }
        return tag.getInt(START_SIGNAL);
    }

    public static int getTargetSignal(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TARGET_SIGNAL)) {
            return DEFAULT_TARGET_SIGNAL;
        }
        return tag.getInt(TARGET_SIGNAL);
    }

    public static boolean isCompleted(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(COMPLETED);
    }

    public static ItemStack createCompletedCopy(ItemStack source) {
        ItemStack result = source.copyWithCount(1);
        CompoundTag tag = result.getOrCreateTag();
        tag.putString(RESEARCH_ID, getResearchId(source));
        tag.putInt(START_SIGNAL, getStartSignal(source));
        tag.putInt(TARGET_SIGNAL, getTargetSignal(source));
        tag.putBoolean(COMPLETED, true);
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gtcomputronics.punch_card.research", getResearchId(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.gtcomputronics.punch_card.signals",
                getStartSignal(stack), getTargetSignal(stack)).withStyle(ChatFormatting.GRAY));
        if (isCompleted(stack)) {
            tooltip.add(Component.translatable("item.gtcomputronics.punch_card.completed")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
