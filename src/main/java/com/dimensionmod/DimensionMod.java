package com.dimensionmod;

import com.dimensionmod.init.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(DimensionMod.MODID)
public class DimensionMod {
    public static final String MODID = "dimensionmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(DimensionMod.class);

    public DimensionMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        ModSounds.REGISTRY.register(modBus);
        ModEffects.REGISTRY.register(modBus);
        ModEntities.REGISTRY.register(modBus);
        ModStructures.REGISTRY.register(modBus);

        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Dimension Cataclysm loaded! Initializing bosses and structures...");
        ModStructures.registerStructures();
        event.enqueueWork(() -> {
            ModEntities.registerSpawnPlacements();
        });
    }
}
