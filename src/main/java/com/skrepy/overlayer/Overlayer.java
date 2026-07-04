package com.skrepy.overlayer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.skrepy.overlayer.client.render.OverlayRenderer;
import com.skrepy.overlayer.manager.OverlayerManager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(value = Overlayer.MOD_ID, dist = Dist.CLIENT)
public class Overlayer {
    public static final String MOD_ID = "overlayer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Overlayer(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::addCreative);
        OverlayerManager.getInstance().load();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info(MOD_ID + " common setup");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    private void onClientSetup(FMLClientSetupEvent event) {
    }
}
