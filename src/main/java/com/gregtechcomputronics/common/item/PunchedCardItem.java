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
    public static final String GRID_WIDTH = "GridWidth";
    public static final String GRID_HEIGHT = "GridHeight";
    public static final String START_X = "StartX";
    public static final String START_Y = "StartY";
    public static final String TARGET_X = "TargetX";
    public static final String TARGET_Y = "TargetY";
    public static final String BURNED_CELLS = "BurnedCells";
    public static final String LOCKED_CELL_SLOTS = "LockedCellSlots";
    public static final String LOCKED_CELL_COMPONENTS = "LockedCellComponents";

    public static final int COMPONENT_NONE = 0;
    public static final int COMPONENT_WIRE = 1;
    public static final int COMPONENT_RESISTOR = 2;
    public static final int COMPONENT_DIODE = 3;
    public static final int COMPONENT_CAPACITOR = 4;
    public static final int COMPONENT_VACUUM_TUBE = 5;

    private static final String DEFAULT_RESEARCH_ID = "basic_tabulation";
    private static final int DEFAULT_START_SIGNAL = 5;
    private static final int DEFAULT_TARGET_SIGNAL = 7;
    private static final int DEFAULT_GRID_WIDTH = 5;
    private static final int DEFAULT_GRID_HEIGHT = 5;
    private static final int DEFAULT_START_X = 0;
    private static final int DEFAULT_START_Y = 2;
    private static final int DEFAULT_TARGET_X = 4;
    private static final int DEFAULT_TARGET_Y = 2;
    private static final int[] DEFAULT_LOCKED_CELL_SLOTS = { 12 };
    private static final int[] DEFAULT_LOCKED_CELL_COMPONENTS = { COMPONENT_CAPACITOR };

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

    public static int getGridWidth(ItemStack stack) {
        return getPositiveInt(stack, GRID_WIDTH, DEFAULT_GRID_WIDTH);
    }

    public static int getGridHeight(ItemStack stack) {
        return getPositiveInt(stack, GRID_HEIGHT, DEFAULT_GRID_HEIGHT);
    }

    public static int getStartX(ItemStack stack) {
        return getCoordinate(stack, START_X, DEFAULT_START_X, getGridWidth(stack));
    }

    public static int getStartY(ItemStack stack) {
        return getCoordinate(stack, START_Y, DEFAULT_START_Y, getGridHeight(stack));
    }

    public static int getTargetX(ItemStack stack) {
        return getCoordinate(stack, TARGET_X, DEFAULT_TARGET_X, getGridWidth(stack));
    }

    public static int getTargetY(ItemStack stack) {
        return getCoordinate(stack, TARGET_Y, DEFAULT_TARGET_Y, getGridHeight(stack));
    }

    public static int[] getBurnedCells(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(BURNED_CELLS)) {
            return new int[0];
        }
        return tag.getIntArray(BURNED_CELLS);
    }

    public static boolean isBurnedCell(ItemStack stack, int x, int y) {
        int slot = slotIndex(stack, x, y);
        for (int burnedCell : getBurnedCells(stack)) {
            if (burnedCell == slot) {
                return true;
            }
        }
        return false;
    }

    public static int getLockedComponent(ItemStack stack, int x, int y) {
        int slot = slotIndex(stack, x, y);
        int[] slots = getLockedCellSlots(stack);
        int[] components = getLockedCellComponents(stack);
        for (int i = 0; i < slots.length && i < components.length; i++) {
            if (slots[i] == slot) {
                return components[i];
            }
        }
        return COMPONENT_NONE;
    }

    public static boolean isLockedCell(ItemStack stack, int x, int y) {
        return getLockedComponent(stack, x, y) != COMPONENT_NONE;
    }

    public static int[] getLockedCellSlots(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(LOCKED_CELL_SLOTS)) {
            return DEFAULT_LOCKED_CELL_SLOTS;
        }
        return tag.getIntArray(LOCKED_CELL_SLOTS);
    }

    public static int[] getLockedCellComponents(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(LOCKED_CELL_COMPONENTS)) {
            return DEFAULT_LOCKED_CELL_COMPONENTS;
        }
        return tag.getIntArray(LOCKED_CELL_COMPONENTS);
    }

    public static int slotIndex(ItemStack stack, int x, int y) {
        return y * getGridWidth(stack) + x;
    }

    public static ItemStack createCompletedCopy(ItemStack source) {
        ItemStack result = source.copyWithCount(1);
        CompoundTag tag = result.getOrCreateTag();
        tag.putString(RESEARCH_ID, getResearchId(source));
        tag.putInt(START_SIGNAL, getStartSignal(source));
        tag.putInt(TARGET_SIGNAL, getTargetSignal(source));
        tag.putInt(GRID_WIDTH, getGridWidth(source));
        tag.putInt(GRID_HEIGHT, getGridHeight(source));
        tag.putInt(START_X, getStartX(source));
        tag.putInt(START_Y, getStartY(source));
        tag.putInt(TARGET_X, getTargetX(source));
        tag.putInt(TARGET_Y, getTargetY(source));
        tag.putIntArray(BURNED_CELLS, getBurnedCells(source));
        tag.putIntArray(LOCKED_CELL_SLOTS, getLockedCellSlots(source));
        tag.putIntArray(LOCKED_CELL_COMPONENTS, getLockedCellComponents(source));
        tag.putBoolean(COMPLETED, true);
        return result;
    }

    private static int getPositiveInt(ItemStack stack, String key, int fallback) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) {
            return fallback;
        }
        return Math.max(1, tag.getInt(key));
    }

    private static int getCoordinate(ItemStack stack, String key, int fallback, int axisSize) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) {
            return Math.min(fallback, axisSize - 1);
        }
        return Math.max(0, Math.min(axisSize - 1, tag.getInt(key)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.gtcomputronics.punch_card.research", getResearchId(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.gtcomputronics.punch_card.signals",
                getStartSignal(stack), getTargetSignal(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.gtcomputronics.punch_card.field",
                getGridWidth(stack), getGridHeight(stack)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.gtcomputronics.punch_card.route",
                getStartX(stack), getStartY(stack), getTargetX(stack), getTargetY(stack))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.gtcomputronics.punch_card.burned",
                getBurnedCells(stack).length).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.gtcomputronics.punch_card.locked",
                getLockedCellSlots(stack).length).withStyle(ChatFormatting.DARK_GRAY));
        if (isCompleted(stack)) {
            tooltip.add(Component.translatable("item.gtcomputronics.punch_card.completed")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
