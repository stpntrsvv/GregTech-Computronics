package com.gregtechcomputronics.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;

import com.gregtechcomputronics.ComputronicsMod;
import com.gregtechcomputronics.common.machine.AnalogTabulatorMachine;

public class CustomMachines {

    public static final MachineDefinition ANALOG_TABULATOR = ComputronicsMod.REGISTRATE
            .multiblock("analog_tabulator", holder -> new AnalogTabulatorMachine(holder, 5, 5))
            .pattern(definition -> FactoryBlockPattern.start()
                    .aisle("EEE", "FSF")
                    .where('S', Predicates.controller(Predicates.blocks(definition.get())))
                    .where('F', Predicates.blocks(CustomBlocks.ANALOG_COMPUTING_FRAMEWORK.get()))
                    .where('E', Predicates.blocks(CustomBlocks.ANALOG_COMPUTING_FRAMEWORK.get())
                            .or(Predicates.ability(PartAbility.INPUT_ENERGY, GTValues.LV)
                                    .setMinGlobalLimited(1)
                                    .setMaxGlobalLimited(1)
                                    .setPreviewCount(1)))
                    .build())
            .rotationState(RotationState.ALL)
            .overlayTieredHullModel(GTCEu.id("block/machine/part/hull"))
            .tier(GTValues.LV)
            .langValue("Analog Tabulator")
            .register();

    public static void init() {}

    private CustomMachines() {}
}
