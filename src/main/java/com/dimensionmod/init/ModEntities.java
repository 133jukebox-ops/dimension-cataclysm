package com.dimensionmod.init;

import com.dimensionmod.DimensionMod;
import com.dimensionmod.entity.boss.AbyssalDevourer;
import com.dimensionmod.entity.boss.InfernoAncient;
import com.dimensionmod.entity.boss.VoidArbiter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY =
            DeferredRegister.create(Registries.ENTITY_TYPE, DimensionMod.MODID);

    public static final RegistryObject<EntityType<AbyssalDevourer>> ABYSSAL_DEVOURER =
            REGISTRY.register("abyssal_devourer", () ->
                    EntityType.Builder.of(AbyssalDevourer::new, MobCategory.MONSTER)
                            .sized(1.6f, 3.2f)
                            .clientTrackingRange(16)
                            .updateInterval(3)
                            .fireImmune()
                            .build("abyssal_devourer")
            );

    public static final RegistryObject<EntityType<InfernoAncient>> INFerno_ANCIENT =
            REGISTRY.register("inferno_ancient", () ->
                    EntityType.Builder.of(InfernoAncient::new, MobCategory.MONSTER)
                            .sized(2.0f, 3.5f)
                            .clientTrackingRange(16)
                            .updateInterval(3)
                            .fireImmune()
                            .build("inferno_ancient")
            );

    public static final RegistryObject<EntityType<VoidArbiter>> VOID_ARBITER =
            REGISTRY.register("void_arbiter", () ->
                    EntityType.Builder.of(VoidArbiter::new, MobCategory.MONSTER)
                            .sized(1.8f, 3.8f)
                            .clientTrackingRange(20)
                            .updateInterval(2)
                            .fireImmune()
                            .build("void_arbiter")
            );

    public static void registerSpawnPlacements() {
        // Spawn placements handled via Forge's SpawnPlacementRegister event
    }
}
