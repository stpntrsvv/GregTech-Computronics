package com.gregtechcomputronics.common.machine;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

import com.gregtechcomputronics.common.item.PunchedCardItem;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalogTabulatorMachine extends MultiblockControllerMachine implements IFancyUIMachine, IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AnalogTabulatorMachine.class, MultiblockControllerMachine.MANAGED_FIELD_HOLDER);

    private static final int CAPACITOR_THRESHOLD = 4;
    private static final int MAX_SIGNAL = 64;
    private static final long UI_ENERGY_USAGE = 16L;

    private final int gridWidth;
    private final int gridHeight;
    private EnergyContainerList energyContainer;
    private TickableSubscription uiEnergySubscription;
    private int openInterfaceCount;

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
        this.cardInput = new NotifiableItemStackHandler(this, 1, IO.IN)
                .setFilter(stack -> stack.getItem() instanceof PunchedCardItem);
        this.circuitInventory = new CircuitInventoryHandler(this, gridWidth * gridHeight);
        this.cardOutput = new NotifiableItemStackHandler(this, 1, IO.OUT)
                .setFilter(stack -> false);

        this.cardInput.addChangedListener(this::onItemsChanged);
        this.circuitInventory.addChangedListener(this::onItemsChanged);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return isFormed();
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        energyContainer = createEnergyContainer();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        energyContainer = null;
        openInterfaceCount = 0;
        stopUiEnergySubscription();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        energyContainer = null;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        ModularUI ui = new ModularUI(176, 166, this, entityPlayer).widget(new FancyMachineUIWidget(this, 176, 166));
        if (!isRemote()) {
            openInterfaceCount++;
            startUiEnergySubscription();
            ui.registerCloseListener(() -> {
                openInterfaceCount = Math.max(0, openInterfaceCount - 1);
                if (openInterfaceCount == 0) {
                    stopUiEnergySubscription();
                }
            });
        }
        return ui;
    }

    private void onItemsChanged() {
        if (!hasUiPower()) {
            currentOutputSignal = 0;
            status = "Insufficient energy";
            markDirty();
            return;
        }
        checkCircuit();
        markDirty();
    }

    private EnergyContainerList createEnergyContainer() {
        List<IEnergyContainer> containers = getParts().stream()
                .flatMap(part -> part.getRecipeHandlers().stream())
                .filter(handlerList -> handlerList.isValid(IO.IN))
                .flatMap(handlerList -> handlerList.getCapability(EURecipeCapability.CAP).stream())
                .filter(IEnergyContainer.class::isInstance)
                .map(IEnergyContainer.class::cast)
                .toList();
        return new EnergyContainerList(containers);
    }

    private boolean hasUiPower() {
        return openInterfaceCount == 0 ||
                energyContainer != null && energyContainer.getEnergyStored() >= UI_ENERGY_USAGE;
    }

    private void startUiEnergySubscription() {
        if (uiEnergySubscription == null || !uiEnergySubscription.isStillSubscribed()) {
            uiEnergySubscription = subscribeServerTick(this::consumeUiEnergy);
        }
    }

    private void stopUiEnergySubscription() {
        if (uiEnergySubscription != null) {
            uiEnergySubscription.unsubscribe();
            uiEnergySubscription = null;
        }
    }

    private void consumeUiEnergy() {
        if (openInterfaceCount <= 0 || !isFormed()) {
            stopUiEnergySubscription();
            return;
        }
        if (energyContainer == null) {
            energyContainer = createEnergyContainer();
        }
        if (energyContainer.removeEnergy(UI_ENERGY_USAGE) < UI_ENERGY_USAGE) {
            currentOutputSignal = 0;
            status = "Insufficient energy";
        } else if ("Insufficient energy".equals(status)) {
            checkCircuit();
        }
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
        CircuitResult result = solve(card, startSignal, targetSignal);
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

    private CircuitResult solve(ItemStack card, int startSignal, int targetSignal) {
        int cardGridWidth = getCardGridWidth(card);
        int cardGridHeight = getCardGridHeight(card);
        int startX = PunchedCardItem.getStartX(card);
        int startY = PunchedCardItem.getStartY(card);
        int targetX = PunchedCardItem.getTargetX(card);
        int targetY = PunchedCardItem.getTargetY(card);
        if (!isUsableCircuitSlot(card, startX, startY) || !isUsableCircuitSlot(card, targetX, targetY)) {
            return new CircuitResult(false, 0, "Invalid card field", List.of());
        }

        ArrayDeque<Node> queue = new ArrayDeque<>();
        Set<Node> visited = new HashSet<>();
        queue.add(new Node(startX, startY, startSignal, 1, 0, List.of(slotIndex(startX, startY))));

        Integer reachedTargetSignal = null;
        while (!queue.isEmpty()) {
            Node node = queue.removeFirst();
            if (!visited.add(node)) {
                continue;
            }

            CircuitComponent component = componentAt(card, node.x(), node.y());
            int nextSignal = component.apply(node.signal(), node.dx(), node.dy());
            if (nextSignal <= 0 || nextSignal > MAX_SIGNAL) {
                continue;
            }

            if (node.x() == targetX && node.y() == targetY) {
                reachedTargetSignal = nextSignal;
                if (nextSignal == targetSignal) {
                    return new CircuitResult(true, nextSignal, "Matched target", node.path());
                }
                continue;
            }

            enqueue(queue, node, card, cardGridWidth, cardGridHeight, node.x() + 1, node.y(), nextSignal, 1, 0);
            enqueue(queue, node, card, cardGridWidth, cardGridHeight, node.x() - 1, node.y(), nextSignal, -1, 0);
            enqueue(queue, node, card, cardGridWidth, cardGridHeight, node.x(), node.y() + 1, nextSignal, 0, 1);
            enqueue(queue, node, card, cardGridWidth, cardGridHeight, node.x(), node.y() - 1, nextSignal, 0, -1);
        }

        if (reachedTargetSignal == null) {
            return new CircuitResult(false, 0, "Open circuit", List.of());
        }
        return new CircuitResult(false, reachedTargetSignal, "Signal mismatch", List.of());
    }

    private CircuitComponent componentAt(ItemStack card, int x, int y) {
        if (!isUsableCircuitSlot(card, x, y)) {
            return CircuitComponent.NONE;
        }
        CircuitComponent lockedComponent = CircuitComponent
                .fromCardComponent(PunchedCardItem.getLockedComponent(card, x, y));
        if (lockedComponent != CircuitComponent.NONE) {
            return lockedComponent;
        }
        return CircuitComponent.fromStack(circuitInventory.getStackInSlot(slotIndex(x, y)));
    }

    private void enqueue(ArrayDeque<Node> queue, Node source, ItemStack card, int cardGridWidth, int cardGridHeight,
                         int x, int y, int signal, int dx, int dy) {
        if (x >= 0 && x < cardGridWidth && y >= 0 && y < cardGridHeight && isUsableCircuitSlot(card, x, y)) {
            int slot = slotIndex(x, y);
            if (!source.path().contains(slot)) {
                queue.add(new Node(x, y, signal, dx, dy, appendPath(source.path(), slot)));
            }
        }
    }

    private List<Integer> appendPath(List<Integer> path, int slot) {
        return java.util.stream.Stream.concat(path.stream(), java.util.stream.Stream.of(slot)).toList();
    }

    private int slotIndex(int x, int y) {
        return y * gridWidth + x;
    }

    private int getCardGridWidth(ItemStack card) {
        return Math.min(gridWidth, PunchedCardItem.getGridWidth(card));
    }

    private int getCardGridHeight(ItemStack card) {
        return Math.min(gridHeight, PunchedCardItem.getGridHeight(card));
    }

    private boolean isUsableCircuitSlot(ItemStack card, int x, int y) {
        if (!(card.getItem() instanceof PunchedCardItem)) {
            return x >= 0 && x < gridWidth && y >= 0 && y < gridHeight;
        }
        return x >= 0 && x < getCardGridWidth(card) && y >= 0 && y < getCardGridHeight(card) &&
                !PunchedCardItem.isBurnedCell(card, x, y);
    }

    private boolean canPlaceCircuitComponent(int slot, ItemStack stack) {
        ItemStack card = cardInput.getStackInSlot(0);
        if (!(card.getItem() instanceof PunchedCardItem)) {
            return false;
        }
        int x = slot % gridWidth;
        int y = slot / gridWidth;
        return CircuitComponent.fromStack(stack) != CircuitComponent.NONE && isUsableCircuitSlot(card, x, y) &&
                !PunchedCardItem.isLockedCell(card, x, y);
    }

    private void consumeCircuitPath(List<Integer> path) {
        ItemStack card = cardInput.getStackInSlot(0);
        for (int slot : new HashSet<>(path)) {
            if (card.getItem() instanceof PunchedCardItem &&
                    PunchedCardItem.isLockedCell(card, slot % gridWidth, slot / gridWidth)) {
                continue;
            }
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
                        new SlotWidget(circuitInventory, slot++, gridStartX + x * 18, gridStartY + y * 18,
                                true, true).setBackgroundTexture(GuiTextures.SLOT));
                int cellX = x;
                int cellY = y;
                group.addWidget(new ImageWidget(gridStartX + x * 18 + 1, gridStartY + y * 18 + 1, 16, 16,
                        () -> new ItemStackTexture(getLockedComponentStack(cellX, cellY))));
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
        group.addWidget(new LabelWidget(54, 136, () -> "Field: " + getFieldText()));
        group.addWidget(new LabelWidget(112, 136, () -> "Route: " + getRouteText()));
        group.addWidget(new LabelWidget(8, 136, () -> "Locked: " + getLockedText()));
        group.addWidget(new LabelWidget(54, 148, () -> "Status: " + status));
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

    private String getFieldText() {
        ItemStack card = cardInput.getStackInSlot(0);
        if (!(card.getItem() instanceof PunchedCardItem)) {
            return "-";
        }
        return PunchedCardItem.getGridWidth(card) + "x" + PunchedCardItem.getGridHeight(card);
    }

    private String getRouteText() {
        ItemStack card = cardInput.getStackInSlot(0);
        if (!(card.getItem() instanceof PunchedCardItem)) {
            return "-";
        }
        return PunchedCardItem.getStartX(card) + "," + PunchedCardItem.getStartY(card) + ">" +
                PunchedCardItem.getTargetX(card) + "," + PunchedCardItem.getTargetY(card);
    }

    private String getLockedText() {
        ItemStack card = cardInput.getStackInSlot(0);
        if (!(card.getItem() instanceof PunchedCardItem)) {
            return "-";
        }
        return String.valueOf(PunchedCardItem.getLockedCellSlots(card).length);
    }

    private ItemStack getLockedComponentStack(int x, int y) {
        ItemStack card = cardInput.getStackInSlot(0);
        if (!(card.getItem() instanceof PunchedCardItem)) {
            return ItemStack.EMPTY;
        }
        return CircuitComponent.fromCardComponent(PunchedCardItem.getLockedComponent(card, x, y)).asDisplayStack();
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(cardInput.storage);
        clearInventory(circuitInventory.storage);
        clearInventory(cardOutput.storage);
    }

    private enum CircuitComponent {

        NONE,
        WIRE,
        RESISTOR,
        DIODE,
        CAPACITOR,
        VACUUM_TUBE;

        int apply(int signal, int dx, int dy) {
            return switch (this) {
                case NONE -> 0;
                case WIRE -> signal;
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
            if (isWireLike(id.getPath(), stack)) {
                return WIRE;
            }
            return switch (id.getPath()) {
                case "resistor", "smd_resistor", "advanced_smd_resistor" -> RESISTOR;
                case "diode", "smd_diode", "advanced_smd_diode" -> DIODE;
                case "capacitor", "smd_capacitor", "advanced_smd_capacitor", "tantalum_capacitor" -> CAPACITOR;
                case "vacuum_tube" -> VACUUM_TUBE;
                default -> NONE;
            };
        }

        static CircuitComponent fromCardComponent(int component) {
            return switch (component) {
                case PunchedCardItem.COMPONENT_WIRE -> WIRE;
                case PunchedCardItem.COMPONENT_RESISTOR -> RESISTOR;
                case PunchedCardItem.COMPONENT_DIODE -> DIODE;
                case PunchedCardItem.COMPONENT_CAPACITOR -> CAPACITOR;
                case PunchedCardItem.COMPONENT_VACUUM_TUBE -> VACUUM_TUBE;
                default -> NONE;
            };
        }

        ItemStack asDisplayStack() {
            ResourceLocation id = switch (this) {
                case WIRE -> new ResourceLocation("gtceu", "wire_single_copper");
                case RESISTOR -> new ResourceLocation("gtceu", "resistor");
                case DIODE -> new ResourceLocation("gtceu", "diode");
                case CAPACITOR -> new ResourceLocation("gtceu", "capacitor");
                case VACUUM_TUBE -> new ResourceLocation("gtceu", "vacuum_tube");
                case NONE -> null;
            };
            if (id == null) {
                return ItemStack.EMPTY;
            }
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null || item == Items.AIR) {
                return ItemStack.EMPTY;
            }
            return new ItemStack(item);
        }

        private static boolean isWireLike(String itemPath, ItemStack stack) {
            if (itemPath.contains("wiremill") || itemPath.contains("wire_cutter") ||
                    itemPath.contains("wirecutter") || itemPath.contains("wire_extruder_mold") ||
                    itemPath.contains("wireless")) {
                return false;
            }
            if (itemPath.startsWith("wire_") || itemPath.endsWith("_wire") || itemPath.contains("_wire_") ||
                    itemPath.startsWith("cable_") || itemPath.endsWith("_cable") || itemPath.contains("_cable_")) {
                return true;
            }
            return stack.getTags().anyMatch(tag -> {
                String tagPath = tag.location().getPath();
                return tagPath.contains("wire") || tagPath.contains("cable");
            });
        }
    }

    private class CircuitInventoryHandler extends NotifiableItemStackHandler {

        CircuitInventoryHandler(AnalogTabulatorMachine machine, int slots) {
            super(machine, slots, IO.BOTH);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return canPlaceCircuitComponent(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!canPlaceCircuitComponent(slot, stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public void setStackInSlot(int index, @NotNull ItemStack stack) {
            if (stack.isEmpty() || canPlaceCircuitComponent(index, stack)) {
                super.setStackInSlot(index, stack);
            }
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
