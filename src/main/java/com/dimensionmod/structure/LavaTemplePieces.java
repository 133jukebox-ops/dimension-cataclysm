package com.dimensionmod.structure;

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

public class LavaTemplePieces {

    private static final BlockState NETHER_BRICKS = Blocks.NETHER_BRICK.defaultBlockState();
    private static final BlockState CRACKED_NETHER_BRICKS = Blocks.CRACKED_NETHER_BRICK.defaultBlockState();
    private static final BlockState NETHER_BRICK_FENCE = Blocks.NETHER_BRICK_FENCE.defaultBlockState();
    private static final BlockState LAVA = Blocks.LAVA.defaultBlockState();
    private static final BlockState GLOWSTONE = Blocks.GLOWSTONE.defaultBlockState();
    private static final BlockState BLACKSTONE_WALL = Blocks.BLACKSTONE_WALL.defaultBlockState();
    private static final BlockState SOUL_LANTERN = Blocks.SOUL_LANTERN.defaultBlockState();

    public static void startLavaTemple(StructurePiecesBuilder builder, RandomSource random, net.minecraft.world.level.ChunkPos chunkPos) {
        BlockPos center = new BlockPos(chunkPos.getMinBlockX() + 7, 35, chunkPos.getMinBlockZ() + 7);
        builder.addPiece(new LavaTemplePiece(random, center));
    }

    public static class LavaTemplePiece extends StructurePiece {
        private final int width = 17;
        private final int height = 10;
        private final int depth = 17;

        public LavaTemplePiece(RandomSource random, BlockPos center) {
            super(0, new StructureBoundingBox(
                    center.getX() - width / 2, center.getY() - 3,
                    center.getZ() - depth / 2,
                    center.getX() + width / 2, center.getY() + height,
                    center.getZ() + depth / 2
            ));
        }

        public LavaTemplePiece(int p_163454_, CompoundTag tag) {
            super(p_163454_, tag);
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {}

        @Override
        public void postProcess(Level level, StructureManager structureManager,
                                RandomSource random, StructureBoundingBox box,
                                net.minecraft.core.HolderLookup.Provider registries) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
            int baseY = this.boundingBox.minY();
            int centerX = this.boundingBox.getCenter().getX();
            int centerZ = this.boundingBox.getCenter().getZ();

            // ===== Lava pool (center) =====
            for (int x = 3; x < width - 3; x++) {
                for (int z = 3; z < depth - 3; z++) {
                    int worldX = boundingBox.minX() + x;
                    int worldZ = boundingBox.minZ() + z;
                    double dist = Math.sqrt(Math.pow(x - width / 2.0, 2) + Math.pow(z - depth / 2.0, 2));
                    int worldY = baseY;
                    if (dist < 4) {
                        setBlock(level, mutable.set(worldX, worldY, worldZ), LAVA, box);
                    } else {
                        setBlock(level, mutable.set(worldX, worldY, worldZ), CRACKED_NETHER_BRICKS, box);
                    }
                }
            }

            // ===== Floor ring around lava =====
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    int worldX = boundingBox.minX() + x;
                    int worldZ = boundingBox.minZ() + z;
                    double dist = Math.sqrt(Math.pow(x - width / 2.0, 2) + Math.pow(z - depth / 2.0, 2));
                    if (dist >= 4 && dist <= 7) {
                        setBlock(level, mutable.set(worldX, baseY, worldZ), NETHER_BRICKS, box);
                    }
                }
            }

            // ===== Walls =====
            for (int y = 1; y <= height; y++) {
                for (int x = 0; x < width; x++) {
                    for (int z = 0; z < depth; z++) {
                        boolean isOuter = x == 0 || z == 0 || x == width - 1 || z == depth - 1;
                        if (isOuter) {
                            int worldX = boundingBox.minX() + x;
                            int worldY = baseY + y;
                            int worldZ = boundingBox.minZ() + z;
                            boolean isDoor = (y <= 4 && x == width / 2) || (y <= 4 && z == depth / 2);
                            if (!isDoor) {
                                BlockState state = (y == 1 || y == height) ? GLOWSTONE : NETHER_BRICKS;
                                setBlock(level, mutable.set(worldX, worldY, worldZ), state, box);
                            }
                        }
                    }
                }
            }

            // ===== Pillars with banners =====
            int[][] pillars = {{3, 3}, {width - 4, 3}, {3, depth - 4}, {width - 4, depth - 4}};
            for (int i = 0; i < pillars.length; i++) {
                int[] p = pillars[i];
                for (int y = 1; y < height; y++) {
                    int worldX = boundingBox.minX() + p[0];
                    int worldY = baseY + y;
                    int worldZ = boundingBox.minZ() + p[1];
                    setBlock(level, mutable.set(worldX, worldY, worldZ), BLACKSTONE_WALL, box);
                }
                // Glowstone cap
                setBlock(level, mutable.set(boundingBox.minX() + p[0], baseY + height, boundingBox.minZ() + p[1]), GLOWSTONE, box);
                // Soul lantern on pillar
                setBlock(level, mutable.set(boundingBox.minX() + p[0], baseY + height - 1, boundingBox.minZ() + p[1]), SOUL_LANTERN, box);
            }

            // ===== Fence decoration =====
            for (int x = 4; x < width - 4; x += 2) {
                for (int z = 4; z < depth - 4; z += 2) {
                    double dist = Math.sqrt(Math.pow(x - width / 2.0, 2) + Math.pow(z - depth / 2.0, 2));
                    if (dist > 6 && dist < 8) {
                        setBlock(level, mutable.set(boundingBox.minX() + x, baseY + 1, boundingBox.minZ() + z), NETHER_BRICK_FENCE, box);
                    }
                }
            }
        }
    }
}
