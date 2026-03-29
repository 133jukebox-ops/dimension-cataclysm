package com.dimensionmod.init;

import com.dimensionmod.DimensionMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTRY =
            DeferredRegister.create(Registries.MOB_EFFECT, DimensionMod.MODID);

    public static final RegistryObject<MobEffect> VOID_TOUCH =
            REGISTRY.register("void_touch", () ->
                    new net.minecraft.world.effect.MobEffect(
                            net.minecraft.core.MobCategories.MISC, 300, 0x220033, false, true
                    )
            );
}
