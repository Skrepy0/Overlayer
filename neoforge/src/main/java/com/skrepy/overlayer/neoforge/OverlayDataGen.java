package com.skrepy.overlayer.neoforge;

import com.skrepy.overlayer.neoforge.datagen.ENUSLanProvider;
import com.skrepy.overlayer.neoforge.datagen.ZHCNLanProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = OverlayerNeoForge.MOD_ID)
public class OverlayDataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        event.addProvider(new ENUSLanProvider(packOutput));
        event.addProvider(new ZHCNLanProvider(packOutput));
    }
}
