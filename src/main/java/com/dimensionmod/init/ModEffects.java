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

    // Reserved for custom effects if needed in the future
    public static final RegistryObject<MobEffect> VOID_TOUCH =
            REGISTRY.register("void_touch", () -> new net.minecraft.world.effect.MobEffect(
                    net.minecraft.core.MobCategories.MISC, 0x220033, false, true
            ).addAttributeModifier(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    "void_touch_debuff",
                    -2.0,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD
            ));
}
