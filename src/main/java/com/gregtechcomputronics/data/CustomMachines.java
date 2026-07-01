package com.gregtechcomputronics.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;

import com.gregtechcomputronics.ComputronicsMod;
import com.gregtechcomputronics.common.machine.AnalogTabulatorMachine;

public class CustomMachines {

    public static final MachineDefinition ANALOG_TABULATOR = ComputronicsMod.REGISTRATE
            .machine("analog_tabulator", holder -> new AnalogTabulatorMachine(holder, 5, 5))
            .rotationState(RotationState.ALL)
            .overlayTieredHullModel(GTCEu.id("block/machine/part/hull"))
            .tier(GTValues.LV)
            .langValue("Analog Tabulator")
            .register();

    public static void init() {}

    private CustomMachines() {}
}
