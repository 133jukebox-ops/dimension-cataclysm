package com.dimensionmod.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

public class VoidAltarPieces {

    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
    private static final BlockState OBSIDIAN_SLAB = Blocks.OBSIDIAN_SLAB.defaultBlockState();
    private static final BlockState OBSIDIAN_STAIRS = Blocks.OBSIDIAN_STAIRS.defaultBlockState();
    private static final BlockState DRAGON_HEAD = Blocks.DRAGON_HEAD.defaultBlockState();
    private static final BlockState DRAGON_WALL_HEAD = Blocks.DRAGON_WALL_HEAD.defaultBlockState();
    private static final BlockState BEACON = Blocks.BEACON.defaultBlockState();
    private static final BlockState END_ROD = Blocks.END_ROD.defaultBlockState();

    public static void startVoidAltar(StructurePiecesBuilder builder, RandomSource random, net.minecraft.world.level.ChunkPos chunkPos) {
        BlockPos center = new BlockPos(chunkPos.getMinBlockX() + 7, 62, chunkPos.getMinBlockZ() + 7);
        builder.addPiece(new VoidAltarPiece(random, center));
    }

    public static class VoidAltarPiece extends StructurePiece {
        private final int width = 21;
        private final int height = 7;
        private final int depth = 21;

        public VoidAltarPiece(RandomSource random, BlockPos center) {
            super(0, new StructureBoundingBox(
                    center.getX() - width / 2, center.getY() - 2,
                    center.getZ() - depth / 2,
                    center.getX() + width / 2, center.getY() + height,
                    center.getZ() + depth / 2
            ));
        }

        public VoidAltarPiece(int p_163454_, CompoundTag tag) {
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
            int topY = baseY + height;

            // ===== End stone base platform =====
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    int worldX = boundingBox.minX() + x;
                    int worldZ = boundingBox.minZ() + z;
                    double dist = Math.sqrt(Math.pow(x - width / 2.0, 2) + Math.pow(z - depth / 2.0, 2));
                    if (dist <= 10) {
                        setBlock(level, mutable.set(worldX, baseY, worldZ), END_STONE, box);
                    }
                }
            }

            // ===== Ring of obsidian pillars =====
            int ringRadius = 8;
            for (int angle = 0; angle < 360; angle += 45) {
                double rad = Math.toRadians(angle);
                int px = (int) (centerX + Math.cos(rad) * ringRadius);
                int pz = (int) (centerZ + Math.sin(rad) * ringRadius);
                for (int y = 1; y <= 5; y++) {
                    setBlock(level, mutable.set(px, baseY + y, pz), OBSIDIAN, box);
                }
                // End rod on top
                setBlock(level, mutable.set(px, baseY + 6, pz), END_ROD, box);
                // Stairs pointing inward at mid height
                int stairAngle = (angle + 180) % 360;
                double sr = Math.toRadians(stairAngle);
                int sx = (int) (centerX + Math.cos(sr) * (ringRadius - 1));
                int sz = (int) (centerZ + Math.sin(sr) * (ringRadius - 1));
                setBlock(level, mutable.set(sx, baseY + 3, sz), OBSIDIAN_STAIRS, box);
            }

            // ===== Inner ring =====
            int innerRingRadius = 4;
            for (int angle = 0; angle < 360; angle += 90) {
                double rad = Math.toRadians(angle);
                int ix = (int) (centerX + Math.cos(rad) * innerRingRadius);
                int iz = (int) (centerZ + Math.sin(rad) * innerRingRadius);
                for (int y = 1; y <= 3; y++) {
                    setBlock(level, mutable.set(ix, baseY + y, iz), BLACKSTONE, box);
                }
            }

            // ===== Center platform =====
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    int worldX = centerX + x;
                    int worldZ = centerZ + z;
                    setBlock(level, mutable.set(worldX, baseY + 1, worldZ), OBSIDIAN, box);
                }
            }

            // ===== Center beacon / altar =====
            setBlock(level, mutable.set(centerX, baseY + 2, centerZ), BEACON, box);

            // ===== Dragon heads on 4 sides =====
            int[][] headPositions = {
                    {centerX, baseY + 2, centerZ - 3},
                    {centerX, baseY + 2, centerZ + 3},
                    {centerX - 3, baseY + 2, centerZ},
                    {centerX + 3, baseY + 2, centerZ},
            };
            int[] headDirections = {3, 2, 5, 4}; // 2=south, 3=north, 4=east, 5=west
            for (int i = 0; i < headPositions.length; i++) {
                int[] h = headPositions[i];
                setBlock(level, mutable.set(h[0], h[1], h[2]), DRAGON_WALL_HEAD.defaultBlockState().setValue(
                        net.minecraft.world.level.block.DirectionalBlock.FACING,
                        net.minecraft.core.Direction.from2DDataValue(headDirections[i])
                ), box);
            }

            // ===== Floating obsidian blocks (mysterious void effect) =====
            for (int i = 0; i < 6; i++) {
                int fx = centerX + (random.nextInt(12) - 6);
                int fy = baseY + 3 + random.nextInt(4);
                int fz = centerZ + (random.nextInt(12) - 6);
                double dist = Math.sqrt(Math.pow(fx - centerX, 2) + Math.pow(fz - centerZ, 2));
                if (dist > 3 && dist < 9) {
                    setBlock(level, mutable.set(fx, fy, fz), OBSIDIAN.defaultBlockState(), box);
                }
            }

            // ===== Obsidian stairs descending =====
            for (int s = 1; s <= 4; s++) {
                setBlock(level, mutable.set(centerX - 5 - s, baseY + 1 + s, centerZ), OBSIDIAN_STAIRS.defaultBlockState().setValue(
                        net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.WEST
                ), box);
                setBlock(level, mutable.set(centerX + 5 + s, baseY + 1 + s, centerZ), OBSIDIAN_STAIRS.defaultBlockState().setValue(
                        net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.EAST
                ), box);
                setBlock(level, mutable.set(centerX, baseY + 1 + s, centerZ - 5 - s), OBSIDIAN_STAIRS.defaultBlockState().setValue(
                        net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.NORTH
                ), box);
                setBlock(level, mutable.set(centerX, baseY + 1 + s, centerZ + 5 + s), OBSIDIAN_STAIRS.defaultBlockState().setValue(
                        net.minecraft.world.level.block.StairBlock.FACING, net.minecraft.core.Direction.SOUTH
                ), box);
            }
        }
    }
}
