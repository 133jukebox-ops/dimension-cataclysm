package com.dimensionmod.structure;

import com.dimensionmod.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.level.storage.loot.LootTables;

public class AbyssalRuinPieces {

    private static final BlockState CRACKED_STONE = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    private static final BlockState CHISELED_STONE = Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState SOUL_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();
    private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState();

    public static void startAbyssalRuin(StructurePiecesBuilder builder, RandomSource random, net.minecraft.world.level.ChunkPos chunkPos) {
        BlockPos center = new BlockPos(chunkPos.getMinBlockX() + 7, 30, chunkPos.getMinBlockZ() + 7);
        builder.addPiece(new AbyssalRuinPiece(random, center));
    }

    public static class AbyssalRuinPiece extends StructurePiece {
        private final int width = 15;
        private final int height = 8;
        private final int depth = 15;

        public AbyssalRuinPiece(RandomSource random, BlockPos center) {
            super(0, new StructureBoundingBox(
                    center.getX() - width / 2, center.getY() - 5,
                    center.getZ() - depth / 2,
                    center.getX() + width / 2, center.getY() + height,
                    center.getZ() + depth / 2
            ));
        }

        public AbyssalRuinPiece(int p_163454_, CompoundTag tag) {
            super(p_163454_, tag);
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        public void postProcess(Level level, StructureManager structureManager,
                                RandomSource random, StructureBoundingBox box,
                                net.minecraft.core.HolderLookup.Provider registries) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            int centerX = this.boundingBox.getCenter().getX();
            int centerZ = this.boundingBox.getCenter().getZ();
            int baseY = this.boundingBox.minY();

            // ===== Floor =====
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    int worldX = boundingBox.minX() + x;
                    int worldZ = boundingBox.minZ() + z;
                    BlockState state;
                    if ((x == 0 || z == 0 || x == width - 1 || z == depth - 1)) {
                        state = CRACKED_STONE; // border
                    } else if (x == width / 2 && z == depth / 2) {
                        state = CRYING_OBSIDIAN; // center altar
                    } else if (x % 3 == 0 && z % 3 == 0) {
                        state = BLACKSTONE; // pillar base
                    } else {
                        state = CHISELED_STONE;
                    }
                    setBlock(level, mutable.set(worldX, baseY, worldZ), state, box);
                }
            }

            // ===== Walls =====
            for (int y = 1; y <= height; y++) {
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < depth; z++) {
                        if (x == 0 || z == 0 || x == width - 1 || z == depth - 1) {
                            int worldX = boundingBox.minX() + x;
                            int worldY = baseY + y;
                            int worldZ = boundingBox.minZ() + z;
                            // door openings
                            boolean isDoor = (y <= 3 && x == width / 2) || (y <= 3 && z == depth / 2);
                            if (!isDoor) {
                                BlockState state = (y == height) ? CHISELED_STONE : CRACKED_STONE;
                                setBlock(level, mutable.set(worldX, worldY, worldZ), state, box);
                            }
                        }
                    }
                }
            }

            // ===== Pillars (4 corners) =====
            int[][] pillars = {{2, 2}, {width - 3, 2}, {2, depth - 3}, {width - 3, depth - 3}};
            for (int[] p : pillars) {
                for (int y = 1; y < height; y++) {
                    int worldX = boundingBox.minX() + p[0];
                    int worldY = baseY + y;
                    int worldZ = boundingBox.minZ() + p[1];
                    setBlock(level, mutable.set(worldX, worldY, worldZ), BLACKSTONE, box);
                }
                // Lantern on top
                int worldX = boundingBox.minX() + p[0];
                int worldY = baseY + height;
                int worldZ = boundingBox.minZ() + p[1];
                setBlock(level, mutable.set(worldX, worldY, worldZ), SOUL_LANTERN, box);
            }

            // ===== Soul fire surrounding altar =====
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || z != 0) {
                        int worldX = centerX + x;
                        int worldY = baseY + 1;
                        int worldZ = centerZ + z;
                        if (level.getBlockState(mutable.set(worldX, worldY, worldZ)).isAir()) {
                            setBlock(level, mutable.set(worldX, worldY, worldZ),
                                    Blocks.SOUL_FIRE.defaultBlockState(), box);
                        }
                    }
                }
            }
        }
    }
}
