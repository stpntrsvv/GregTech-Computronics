package com.gregtechcomputronics.common.machine;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import com.gregtechcomputronics.common.item.PunchedCardItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalogTabulatorMachine extends MetaMachine implements IFancyUIMachine, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AnalogTabulatorMachine.class, MetaMachine.MANAGED_FIELD_HOLDER);

    private static final int CAPACITOR_THRESHOLD = 4;
    private static final int MAX_SIGNAL = 64;

    private final int gridWidth;
    private final int gridHeight;
    private final int startY;
    private final int targetY;

    @Persisted
    protected final NotifiableItemStackHandler cardInput;
    @Persisted
    protected final NotifiableItemStackHandler circuitInventory;
    @Persisted
    protected final NotifiableItemStackHandler cardOutput;

    @DescSynced
    private int currentOutputSignal;
    @DescSynced
    private String status = "Open circuit";

    public AnalogTabulatorMachine(IMachineBlockEntity holder, int gridWidth, int gridHeight) {
        super(holder);
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.startY = gridHeight / 2;
        this.targetY = gridHeight / 2;
        this.cardInput = new NotifiableItemStackHandler(this, 1, IO.IN)
                .setFilter(stack -> stack.getItem() instanceof PunchedCardItem);
        this.circuitInventory = new NotifiableItemStackHandler(this, gridWidth * gridHeight, IO.BOTH)
                .setFilter(stack -> CircuitComponent.fromStack(stack) != CircuitComponent.NONE);
        this.cardOutput = new NotifiableItemStackHandler(this, 1, IO.OUT)
                .setFilter(stack -> false);

        this.cardInput.addChangedListener(this::onItemsChanged);
        this.circuitInventory.addChangedListener(this::onItemsChanged);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    private void onItemsChanged() {
        checkCircuit();
        markDirty();
    }

    public void checkCircuit() {
        ItemStack card = cardInput.getStackInSlot(0);
        if (!(card.getItem() instanceof PunchedCardItem)) {
            currentOutputSignal = 0;
            status = "Insert punch card";
            return;
        }

        int startSignal = PunchedCardItem.getStartSignal(card);
        int targetSignal = PunchedCardItem.getTargetSignal(card);
        CircuitResult result = solve(startSignal, targetSignal);
        currentOutputSignal = result.signal();
        status = result.message();

        if (result.success() && cardOutput.getStackInSlot(0).isEmpty()) {
            cardOutput.storage.setStackInSlot(0, PunchedCardItem.createCompletedCopy(card));
            consumeCircuitPath(result.path());
            card.shrink(1);
            cardInput.storage.setStackInSlot(0, card);
            status = "Research complete";
        }
    }

    private CircuitResult solve(int startSignal, int targetSignal) {
        ArrayDeque<Node> queue = new ArrayDeque<>();
        Set<Node> visited = new HashSet<>();
        queue.add(new Node(0, startY, startSignal, 1, 0, List.of(slotIndex(0, startY))));

        Integer reachedTargetSignal = null;
        while (!queue.isEmpty()) {
            Node node = queue.removeFirst();
            if (!visited.add(node)) {
                continue;
            }

            CircuitComponent component = componentAt(node.x(), node.y());
            int nextSignal = component.apply(node.signal(), node.dx(), node.dy());
            if (nextSignal <= 0 || nextSignal > MAX_SIGNAL) {
                continue;
            }

            if (node.x() == gridWidth - 1 && node.y() == targetY) {
                reachedTargetSignal = nextSignal;
                if (nextSignal == targetSignal) {
                    return new CircuitResult(true, nextSignal, "Matched target", node.path());
                }
                continue;
            }

            enqueue(queue, node, node.x() + 1, node.y(), nextSignal, 1, 0);
            enqueue(queue, node, node.x() - 1, node.y(), nextSignal, -1, 0);
            enqueue(queue, node, node.x(), node.y() + 1, nextSignal, 0, 1);
            enqueue(queue, node, node.x(), node.y() - 1, nextSignal, 0, -1);
        }

        if (reachedTargetSignal == null) {
            return new CircuitResult(false, 0, "Open circuit", List.of());
        }
        return new CircuitResult(false, reachedTargetSignal, "Signal mismatch", List.of());
    }

    private CircuitComponent componentAt(int x, int y) {
        return CircuitComponent.fromStack(circuitInventory.getStackInSlot(slotIndex(x, y)));
    }

    private void enqueue(ArrayDeque<Node> queue, Node source, int x, int y, int signal, int dx, int dy) {
        if (x >= 0 && x < gridWidth && y >= 0 && y < gridHeight) {
            queue.add(new Node(x, y, signal, dx, dy, appendPath(source.path(), slotIndex(x, y))));
        }
    }

    private List<Integer> appendPath(List<Integer> path, int slot) {
        return java.util.stream.Stream.concat(path.stream(), java.util.stream.Stream.of(slot)).toList();
    }

    private int slotIndex(int x, int y) {
        return y * gridWidth + x;
    }

    private void consumeCircuitPath(List<Integer> path) {
        for (int slot : new HashSet<>(path)) {
            ItemStack stack = circuitInventory.storage.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                circuitInventory.storage.setStackInSlot(slot, stack);
            }
        }
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 176, 158);
        group.setBackground(GuiTextures.BACKGROUND);

        group.addWidget(new LabelWidget(38, 8, "Analog Research: LV Tier"));

        int gridStartX = 38;
        int gridStartY = 26;
        int slot = 0;
        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                group.addWidget(
                        new SlotWidget(circuitInventory.storage, slot++, gridStartX + x * 18, gridStartY + y * 18,
                                true, true).setBackgroundTexture(GuiTextures.SLOT));
            }
        }

        group.addWidget(new LabelWidget(5, 45, "Start"));
        group.addWidget(new LabelWidget(6, 56, "Signal"));
        group.addWidget(new LabelWidget(13, 70, () -> getStartSignalText()));
        group.addWidget(new LabelWidget(146, 45, "Target"));
        group.addWidget(new LabelWidget(147, 56, "Signal"));
        group.addWidget(new LabelWidget(154, 70, () -> getTargetSignalText()));

        group.addWidget(new LabelWidget(18, 112, "Punched"));
        group.addWidget(new LabelWidget(12, 123, "Research Card"));
        group.addWidget(new SlotWidget(cardInput.storage, 0, 54, 116, true, true)
                .setBackgroundTexture(GuiTextures.SLOT));
        group.addWidget(new LabelWidget(78, 120, "->"));
        group.addWidget(new SlotWidget(cardOutput.storage, 0, 104, 116, true, false)
                .setBackgroundTexture(GuiTextures.SLOT));
        group.addWidget(new LabelWidget(128, 112, "Processed"));
        group.addWidget(new LabelWidget(126, 123, "Schematic"));

        group.addWidget(new LabelWidget(8, 144, () -> "Out: " + currentOutputSignal));
        group.addWidget(new LabelWidget(66, 144, () -> "Status: " + status));
        return group;
    }

    private String getStartSignalText() {
        ItemStack card = cardInput.getStackInSlot(0);
        if (!(card.getItem() instanceof PunchedCardItem)) {
            return "-";
        }
        return String.valueOf(PunchedCardItem.getStartSignal(card));
    }

    private String getTargetSignalText() {
        ItemStack card = cardInput.getStackInSlot(0);
        if (!(card.getItem() instanceof PunchedCardItem)) {
            return "-";
        }
        return String.valueOf(PunchedCardItem.getTargetSignal(card));
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(cardInput.storage);
        clearInventory(circuitInventory.storage);
        clearInventory(cardOutput.storage);
    }

    private enum CircuitComponent {

        NONE,
        RESISTOR,
        DIODE,
        CAPACITOR,
        VACUUM_TUBE;

        int apply(int signal, int dx, int dy) {
            return switch (this) {
                case NONE -> 0;
                case RESISTOR -> signal - 1;
                case DIODE -> dx == 1 && dy == 0 ? signal : 0;
                case CAPACITOR -> signal >= CAPACITOR_THRESHOLD ? signal : 0;
                case VACUUM_TUBE -> signal + 3;
            };
        }

        static CircuitComponent fromStack(@NotNull ItemStack stack) {
            if (stack.isEmpty()) {
                return NONE;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null || !"gtceu".equals(id.getNamespace())) {
                return NONE;
            }
            return switch (id.getPath()) {
                case "resistor", "smd_resistor", "advanced_smd_resistor" -> RESISTOR;
                case "diode", "smd_diode", "advanced_smd_diode" -> DIODE;
                case "capacitor", "smd_capacitor", "advanced_smd_capacitor", "tantalum_capacitor" -> CAPACITOR;
                case "vacuum_tube" -> VACUUM_TUBE;
                default -> NONE;
            };
        }
    }

    private record Node(int x, int y, int signal, int dx, int dy, List<Integer> path) {

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof Node other)) {
                return false;
            }
            return x == other.x && y == other.y && signal == other.signal && dx == other.dx && dy == other.dy;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(x);
            result = 31 * result + Integer.hashCode(y);
            result = 31 * result + Integer.hashCode(signal);
            result = 31 * result + Integer.hashCode(dx);
            result = 31 * result + Integer.hashCode(dy);
            return result;
        }
    }

    private record CircuitResult(boolean success, int signal, String message, List<Integer> path) {}
}
