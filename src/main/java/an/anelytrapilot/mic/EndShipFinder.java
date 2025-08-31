/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the MIT License.
 */

package an.anelytrapilot.mic;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class EndShipFinder {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static ElytraTarget currentTarget = null;

    public static class ElytraTarget {
        public final ItemFrameEntity frame;
        public final BlockPos elytraPos;
        public final BlockPos deckPos;

        public ElytraTarget(ItemFrameEntity frame, BlockPos elytraPos, BlockPos deckPos) {
            this.frame = frame;
            this.elytraPos = elytraPos;
            this.deckPos = deckPos;
        }
    }

    // 主方法：扫描最近 Elytra 展示框
    public static ElytraTarget findNearestElytraFrame() {
        ItemFrameEntity bestFrame = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ItemFrameEntity frame)) continue;
            ItemStack stack = frame.getHeldItemStack();
            if (stack.getItem() != Items.ELYTRA) continue;

            double dist = mc.player.squaredDistanceTo(frame);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestFrame = frame;
            }
        }

        if (bestFrame == null) return null;

        BlockPos elytraPos = bestFrame.getBlockPos(); // 鞘翅展示框坐标
        Direction facing = bestFrame.getHorizontalFacing(); // 展示框朝向（面对 Elytra 的方向）

        // 方向定义
        Direction left = facing.rotateYCounterclockwise(); // 玩家面朝 Elytra 左边是 rotateYCounterclockwise
        Direction tail = facing.getOpposite();             // 船尾方向是背对 Elytra 的方向

        // 降落点： Elytra 上方 3 格，左偏 2 格，向船尾方向退 7 格
        BlockPos deckPos = elytraPos
                .up(3)
                .offset(left, -2)
                .offset(tail, -4);
        currentTarget = new ElytraTarget(bestFrame, elytraPos, deckPos);
        return currentTarget;

    }

    public static BlockPos findDragonHeadAttachedBlock(int radius) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return null;

        BlockPos origin = mc.player.getBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos check = origin.add(x, y, z);
                    Block block = mc.world.getBlockState(check).getBlock();

                    if (block == Blocks.DRAGON_HEAD || block == Blocks.DRAGON_WALL_HEAD) {
                        return GetStartFly(check);
                    }
                }
            }
        }

        return null;
    }

    public static BlockPos GetStartFly(BlockPos dragonHeadPos) {
        if (mc.world == null) return null;

        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockPos offset = dragonHeadPos.offset(dir);
            BlockState state = mc.world.getBlockState(offset);

            if (!state.isAir()) {
                return offset;
            }
        }

        return null; // 没有发现附着的方块
    }

}
