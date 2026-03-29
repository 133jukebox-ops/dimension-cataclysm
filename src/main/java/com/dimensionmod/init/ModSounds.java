package com.dimensionmod.init;

import com.dimensionmod.DimensionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(Registries.SOUND_EVENT, DimensionMod.MODID);

    public static final RegistryObject<SoundEvent> BOSS_DEATH =
            REGISTRY.register("boss.death", () ->
                    SoundEvent.createVariableRangeEvent(new ResourceLocation(DimensionMod.MODID, "boss.death"))
            );
}
