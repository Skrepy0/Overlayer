package com.skrepy.overlayer.fabric;
import com.skrepy.overlayer.fabric.datagen.ModENUSLangProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class OverlayerDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ModENUSLangProvider::new);
        pack.addProvider(ModENUSLangProvider::new);
    }
}
