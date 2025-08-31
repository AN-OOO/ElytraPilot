/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the terms of the MIT License.
 */

package an.anelytrapilot.mic;

import an.anelytrapilot.mixin.IIllIIllIIlll;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceModule {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    static boolean IsPlaced = false;
    public static Map<BlockPos, Long> awaiting = new HashMap<>();
    public enum Interact {
        Vanilla,
        Strict,
        Legit,
        AirPlace
    }
    public enum PlaceMode {
        Packet,
        Normal
    }

    public enum Rotate {
        None,
        Default,
        Grim
    }

    public static boolean placeBlock(BlockPos bp, Rotate rotate, Interact interact, PlaceMode mode, int slot, boolean returnSlot, boolean ignoreEntities) {

        boolean result = placeBlock(bp, rotate, interact, mode, ignoreEntities);

        return result;
    }

    public static boolean placeBlock(BlockPos bp, Rotate rotate, Interact interact, PlaceMode mode, boolean ignoreEntities) {
        BlockHitResult result = getPlaceResult(bp, interact, ignoreEntities);
        if (result == null || mc.world == null || mc.interactionManager == null || mc.player == null) return false;

        boolean sprint = mc.player.isSprinting();

        if (sprint)
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));

        float[] angle = calculateAngle(result.getPos());

        switch (rotate) {
            case None -> {

            }
            case Default -> mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(angle[0], angle[1], true,true));
            case Grim -> mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), angle[0], angle[1], true,true));
        }

        if (mode == PlaceMode.Normal)
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);

        if (mode == PlaceMode.Packet)
            sendSequencedPacket(id -> new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, result, id));

        awaiting.put(bp, System.currentTimeMillis());

        if (rotate == Rotate.Grim)
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true,true));


        if (sprint)
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        return true;
    }

    public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = ((IIllIIllIIlll) mc.world).getPendingUpdateManager22().incrementSequence();) {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }

    @Nullable
    public static BlockHitResult getPlaceResult(@NotNull BlockPos bp, Interact interact, boolean ignoreEntities) {
        if (!ignoreEntities)
            for (Entity entity : new ArrayList<>(mc.world.getNonSpectatingEntities(Entity.class, new Box(bp))))
                if (!(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity))
                    return null;

        if (!mc.world.getBlockState(bp).isReplaceable())
            return null;

        ArrayList<BlockPosWithFacing> supports = getSupportBlocks(bp);
        for (BlockPosWithFacing support : supports) {
            if (interact != Interact.Vanilla) {
                @NotNull List<Direction> dirs = getStrictDirections(bp);
                if (dirs.isEmpty())
                    return null;

                if (!dirs.contains(support.facing))
                    continue;
            }

            BlockHitResult result = null;
            if (interact == Interact.Legit) {
                Vec3d p = getVisibleDirectionPoint(support.facing, support.position, 0, 6); //TODO Implement Range
                if (p != null)
                    return new BlockHitResult(p, support.facing, support.position, false);
            } else {
                Vec3d directionVec = new Vec3d(support.position.getX() + 0.5 + support.facing.getVector().getX() * 0.5, support.position.getY() + 0.5 + support.facing.getVector().getY() * 0.5, support.position.getZ() + 0.5 + support.facing.getVector().getZ() * 0.5);
                result = new BlockHitResult(directionVec, support.facing, support.position, false);
            }
            return result;
        }
        return null;
    }

    private static @NotNull Box getDirectionBox(Direction dir) {
        return switch (dir) {
            case UP -> new Box(.15f, 1f, .15f, .85f, 1f, .85f);
            case DOWN -> new Box(.15f, 0f, .15f, .85f, 0f, .85f);

            case EAST -> new Box(1f, .15f, .15f, 1f, .85f, .85f);
            case WEST -> new Box(0f, .15f, .15f, 0f, .85f, .85f);

            case NORTH -> new Box(.15f, .15f, 0f, .85f, .85f, 0f);
            case SOUTH -> new Box(.15f, .15f, 1f, .85f, .85f, 1f);
        };
    }

    private static boolean shouldSkipPoint(Vec3d point, BlockPos bp, Direction dir, float wallRange, float range) {
        RaycastContext context = new RaycastContext(getEyesPos(mc.player), point, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
        BlockHitResult result = mc.world.raycast(context);

        float dst = squaredDistanceFromEyes(point);

        if (result != null
                && result.getType() == HitResult.Type.BLOCK
                && !result.getBlockPos().equals(bp)
                && dst > wallRange * wallRange)
            return true;

        return dst > range * range;
    }
    public static float squaredDistanceFromEyes(@NotNull Vec3d vec) {
        double d0 = vec.x - mc.player.getX();
        double d1 = vec.z - mc.player.getZ();
        double d2 = vec.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        return (float) (d0 * d0 + d1 * d1 + d2 * d2);
    }

    public static @Nullable Vec3d getVisibleDirectionPoint(@NotNull Direction dir, @NotNull BlockPos bp, float wallRange, float range) {
        Box brutBox = getDirectionBox(dir);

        // EAST, WEST
        if (brutBox.maxX - brutBox.minX == 0)
            for (double y = brutBox.minY; y < brutBox.maxY; y += 0.1f)
                for (double z = brutBox.minZ; z < brutBox.maxZ; z += 0.1f) {
                    Vec3d point = new Vec3d(bp.getX() + brutBox.minX, bp.getY() + y, bp.getZ() + z);

                    if (shouldSkipPoint(point, bp, dir, wallRange, range))
                        continue;

                    return point;
                }


        // DOWN, UP
        if (brutBox.maxY - brutBox.minY == 0)
            for (double x = brutBox.minX; x < brutBox.maxX; x += 0.1f)
                for (double z = brutBox.minZ; z < brutBox.maxZ; z += 0.1f) {
                    Vec3d point = new Vec3d(bp.getX() + x, bp.getY() + brutBox.minY, bp.getZ() + z);

                    if (shouldSkipPoint(point, bp, dir, wallRange, range))
                        continue;

                    return point;
                }


        // NORTH, SOUTH
        if (brutBox.maxZ - brutBox.minZ == 0)
            for (double x = brutBox.minX; x < brutBox.maxX; x += 0.1f)
                for (double y = brutBox.minY; y < brutBox.maxY; y += 0.1f) {
                    Vec3d point = new Vec3d(bp.getX() + x, bp.getY() + y, bp.getZ() + brutBox.minZ);

                    if (shouldSkipPoint(point, bp, dir, wallRange, range))
                        continue;

                    return point;
                }


        return null;
    }

    public record BlockPosWithFacing(BlockPos position, Direction facing) {
    }
    public static float @NotNull [] calculateAngle(Vec3d to) {
        return calculateAngle(getEyesPos(mc.player), to);
    }

    public static float @NotNull [] calculateAngle(@NotNull Vec3d from, @NotNull Vec3d to) {
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * -1.0;
        double difZ = to.z - from.z;
        double dist = MathHelper.sqrt((float) (difX * difX + difZ * difZ));

        float yD = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - 90.0);
        float pD = (float) MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))), -90f, 90f);

        return new float[]{yD, pD};
    }

    public static @NotNull ArrayList<BlockPosWithFacing> getSupportBlocks(@NotNull BlockPos bp) {
        ArrayList<BlockPosWithFacing> list = new ArrayList<>();

        if (mc.world.getBlockState(bp.add(0, -1, 0)).isSolid() || awaiting.containsKey(bp.add(0, -1, 0)))
            list.add(new BlockPosWithFacing(bp.add(0, -1, 0), Direction.UP));

        if (mc.world.getBlockState(bp.add(0, 1, 0)).isSolid() || awaiting.containsKey(bp.add(0, 1, 0)))
            list.add(new BlockPosWithFacing(bp.add(0, 1, 0), Direction.DOWN));

        if (mc.world.getBlockState(bp.add(-1, 0, 0)).isSolid() || awaiting.containsKey(bp.add(-1, 0, 0)))
            list.add(new BlockPosWithFacing(bp.add(-1, 0, 0), Direction.EAST));

        if (mc.world.getBlockState(bp.add(1, 0, 0)).isSolid() || awaiting.containsKey(bp.add(1, 0, 0)))
            list.add(new BlockPosWithFacing(bp.add(1, 0, 0), Direction.WEST));

        if (mc.world.getBlockState(bp.add(0, 0, 1)).isSolid() || awaiting.containsKey(bp.add(0, 0, 1)))
            list.add(new BlockPosWithFacing(bp.add(0, 0, 1), Direction.NORTH));

        if (mc.world.getBlockState(bp.add(0, 0, -1)).isSolid() || awaiting.containsKey(bp.add(0, 0, -1)))
            list.add(new BlockPosWithFacing(bp.add(0, 0, -1), Direction.SOUTH));

        return list;
    }

    public static @NotNull List<Direction> getStrictDirections(@NotNull BlockPos bp) {
        List<Direction> visibleSides = new ArrayList<>();
        Vec3d positionVector = bp.toCenterPos();

        double westDelta = getEyesPos(mc.player).x - (positionVector.add(0.5, 0, 0).x);
        double eastDelta = getEyesPos(mc.player).x - (positionVector.add(-0.5, 0, 0).x);
        double northDelta = getEyesPos(mc.player).z - (positionVector.add(0, 0, 0.5).z);
        double southDelta = getEyesPos(mc.player).z - (positionVector.add(0, 0, -0.5).z);
        double upDelta = getEyesPos(mc.player).y - (positionVector.add(0, 0.5, 0).y);
        double downDelta = getEyesPos(mc.player).y - (positionVector.add(0, -0.5, 0).y);

        if (westDelta > 0 && isSolid(bp.west()))
            visibleSides.add(Direction.EAST);
        if (westDelta < 0 && isSolid(bp.east()))
            visibleSides.add(Direction.WEST);
        if (eastDelta < 0 && isSolid(bp.east()))
            visibleSides.add(Direction.WEST);
        if (eastDelta > 0 && isSolid(bp.west()))
            visibleSides.add(Direction.EAST);

        if (northDelta > 0 && isSolid(bp.north()))
            visibleSides.add(Direction.SOUTH);
        if (northDelta < 0 && isSolid(bp.south()))
            visibleSides.add(Direction.NORTH);
        if (southDelta < 0 && isSolid(bp.south()))
            visibleSides.add(Direction.NORTH);
        if (southDelta > 0 && isSolid(bp.north()))
            visibleSides.add(Direction.SOUTH);

        if (upDelta > 0 && isSolid(bp.down()))
            visibleSides.add(Direction.UP);
        if (upDelta < 0 && isSolid(bp.up()))
            visibleSides.add(Direction.DOWN);
        if (downDelta < 0 && isSolid(bp.up()))
            visibleSides.add(Direction.DOWN);
        if (downDelta > 0 && isSolid(bp.down()))
            visibleSides.add(Direction.UP);

        return visibleSides;
    }
    public static boolean isSolid(BlockPos bp) {
        return mc.world.getBlockState(bp).isSolid() || awaiting.containsKey(bp);
    }
    public static Vec3d getEyesPos(@NotNull Entity entity) {
        return entity.getPos().add(0, entity.getEyeHeight(entity.getPose()), 0);
    }

}