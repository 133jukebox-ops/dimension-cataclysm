package com.dimensionmod.init;

import com.dimensionmod.DimensionMod;
import com.dimensionmod.structure.AbyssalRuinPieces;
import com.dimensionmod.structure.LavaTemplePieces;
import com.dimensionmod.structure.VoidAltarPieces;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorList;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredHolder;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Optional;

public class ModStructures {
    public static final DeferredRegister<StructureFeature<?>> STRUCTURE_REGISTRY =
            DeferredRegister.create(Registries.STRUCTURE_FEATURE, DimensionMod.MODID);

    public static final DeferredRegister<ConfiguredStructureFeature<?, ?>> CONFIGURED_REGISTRY =
            DeferredRegister.create(Registries.CONFIGURED_STRUCTURE_FEATURE, DimensionMod.MODID);

    // ============ Main World: Abyssal Ruin ============
    public static final RegistryObject<StructureFeature<JigsawConfiguration>> ABYSSAL_RUIN =
            STRUCTURE_REGISTRY.register("abyssal_ruin", () ->
                    new StructureFeature<JigsawConfiguration>(JigsawConfiguration.CODEC) {
                        @Override
                        public GenerationStep.Decoration step() {
                            return GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
                        }

                        @Override
                        public boolean validBiome() {
                            return true; // Handled in piece generator
                        }

                        @Override
                        public Optional<PieceGeneratorSupplier<JigsawConfiguration>> defaultSuppliers() {
                            return Optional.of((pContext) -> {
                                AbyssalRuinPieces.startAbyssalRuin(
                                        new StructurePiecesBuilder(),
                                        pContext.random(),
                                        pContext.chunkPos()
                                );
                                return new PieceGeneratorSupplier.ChunkPos() {
                                    @Override
                                    public net.minecraft.world.level.ChunkPos getChunkPos() {
                                        return pContext.chunkPos();
                                    }
                                };
                            });
                        }
                    }
            );

    // ============ Nether: Lava Temple ============
    public static final RegistryObject<StructureFeature<JigsawConfiguration>> LAVA_TEMPLE =
            STRUCTURE_REGISTRY.register("lava_temple", () ->
                    new StructureFeature<JigsawConfiguration>(JigsawConfiguration.CODEC) {
                        @Override
                        public GenerationStep.Decoration step() {
                            return GenerationStep.Decoration.UNDERGROUND_STRUCTURES;
                        }

                        @Override
                        public boolean validBiome() {
                            return true;
                        }

                        @Override
                        public Optional<PieceGeneratorSupplier<JigsawConfiguration>> defaultSuppliers() {
                            return Optional.of((pContext) -> {
                                LavaTemplePieces.startLavaTemple(
                                        new StructurePiecesBuilder(),
                                        pContext.random(),
                                        pContext.chunkPos()
                                );
                                return new PieceGeneratorSupplier.ChunkPos() {
                                    @Override
                                    public net.minecraft.world.level.ChunkPos getChunkPos() {
                                        return pContext.chunkPos();
                                    }
                                };
                            });
                        }
                    }
            );

    // ============ End: Void Altar ==========
    public static final RegistryObject<StructureFeature<JigsawConfiguration>> VOID_ALTAR =
            STRUCTURE_REGISTRY.register("void_altar", () ->
                    new StructureFeature<JigsawConfiguration>(JigsawConfiguration.CODEC) {
                        @Override
                        public GenerationStep.Decoration step() {
                            return GenerationStep.Decoration.END_DECORATION;
                        }

                        @Override
                        public boolean validBiome() {
                            return true;
                        }

                        @Override
                        public Optional<PieceGeneratorSupplier<JigsawConfiguration>> defaultSuppliers() {
                            return Optional.of((pContext) -> {
                                VoidAltarPieces.startVoidAltar(
                                        new StructurePiecesBuilder(),
                                        pContext.random(),
                                        pContext.chunkPos()
                                );
                                return new PieceGeneratorSupplier.ChunkPos() {
                                    @Override
                                    public net.minecraft.world.level.ChunkPos getChunkPos() {
                                        return pContext.chunkPos();
                                    }
                                };
                            });
                        }
                    }
            );

    // ============ Configured ============
    public static final RegistryObject<ConfiguredStructureFeature<?, ?>> CONFIGURED_ABYSSAL_RUIN =
            CONFIGURED_REGISTRY.register("configured_abyssal_ruin", () ->
                    ABYSSAL_RUIN.get().configured(new JigsawConfiguration(
                            net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement.legacy(
                                    "minecraft:empty",
                                    net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorLists.EMPTY
                            ), 0
                    ))
            );

    public static final RegistryObject<ConfiguredStructureFeature<?, ?>> CONFIGURED_LAVA_TEMPLE =
            CONFIGURED_REGISTRY.register("configured_lava_temple", () ->
                    LAVA_TEMPLE.get().configured(new JigsawConfiguration(
                            net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement.legacy(
                                    "minecraft:empty",
                                    net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorLists.EMPTY
                            ), 0
                    ))
            );

    public static final RegistryObject<ConfiguredStructureFeature<?, ?>> CONFIGURED_VOID_ALTAR =
            CONFIGURED_REGISTRY.register("configured_void_altar", () ->
                    VOID_ALTAR.get().configured(new JigsawConfiguration(
                            net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement.legacy(
                                    "minecraft:empty",
                                    net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorLists.EMPTY
                            ), 0
                    ))
            );

    public static final DeferredRegister<StructurePlacement<?>> PLACEMENT_REGISTRY =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, DimensionMod.MODID);

    public static final RegistryObject<StructurePlacement> ABYSSAL_RUIN_PLACEMENT =
            PLACEMENT_REGISTRY.register("abyssal_ruin_placement", () ->
                    new RandomSpreadStructurePlacement(0, 20, new java.util.Random(12345),
                            net.minecraft.world.level.levelgen.placement.NopePlacement.INSTANCE, 5)
            );

    public static void registerStructures() {
        DimensionMod.LOGGER.info("Dimension Cataclysm structures registered.");
    }
}
