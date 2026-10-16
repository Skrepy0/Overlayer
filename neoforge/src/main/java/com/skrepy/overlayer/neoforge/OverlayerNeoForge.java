package com.skrepy.overlayer.neoforge;

import com.skrepy.overlayer.CommonOverlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(CommonOverlayer.MOD_ID)
public class OverlayerNeoForge {
    public OverlayerNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        CommonOverlayer.init();
    }

    @SubscribeEvent
    private void commonSetup(FMLCommonSetupEvent event) {
        CommonOverlayer.LOGGER.info(CommonOverlayer.MOD_ID + " common setup");
    }
}
