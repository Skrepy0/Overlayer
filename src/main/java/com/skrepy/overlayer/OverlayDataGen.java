package com.skrepy.overlayer;

import com.skrepy.overlayer.datagen.ENUSLanProvider;
import com.skrepy.overlayer.datagen.ZHCNLanProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Overlayer.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class OverlayDataGen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(event.includeClient(), new ENUSLanProvider(packOutput));
        generator.addProvider(event.includeClient(), new ZHCNLanProvider(packOutput));
    }
}
