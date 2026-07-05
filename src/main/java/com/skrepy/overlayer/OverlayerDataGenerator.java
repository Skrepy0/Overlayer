package com.skrepy.overlayer;

import com.skrepy.overlayer.datagen.ModENUSLangProvider;
import com.skrepy.overlayer.datagen.ModZHCNLangProvider;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class OverlayerDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ModENUSLangProvider::new);
        pack.addProvider(ModZHCNLangProvider::new);
    }
}
