/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the MIT License.
 */

package an.anelytrapilot.mic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class ElytraObstacleAvoider {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static int avoidTicks = 0;
    private static float targetYaw;
    private static float targetPitch;

    public static void ANavoidObstacles() {

        if (mc.player == null || !mc.player.isLiving()) return;

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        if (avoidTicks > 0) {
            avoidTicks--;
            applyTargetControl();
            return;
        }

        // 前方检测障碍
        if (isObstacleAhead(currentYaw, currentPitch, 8.0)) {
            Vec2f bestOffset = findBestEscapeDirection(currentYaw, currentPitch);
            targetYaw = currentYaw + bestOffset.x;
            targetPitch = MathHelper.clamp(currentPitch + bestOffset.y, -60f, 45f);
            avoidTicks = 10;
        }
    }

    private static void applyTargetControl() {
        mc.player.setYaw(lerpYaw(targetYaw, 0.8f));
        mc.player.setPitch(lerpPitch(targetPitch, 0.8f));
    }

    public static boolean isObstacleAhead(float yaw, float pitch, double dist) {
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d dir = getLookVec(yaw, pitch);
        Vec3d end = start.add(dir.multiply(dist));

        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return hit.getType() == HitResult.Type.BLOCK;
    }

    public static Vec2f findBestEscapeDirection(float yaw, float pitch) {
        float[][] offsets = {
                {-30f, 0f}, {30f, 0f}, // 左右
                {0f, -20f}, {0f, 20f}, // 上下
                {-30f, -15f}, {30f, -15f},
                {-30f, 15f}, {30f, 15f}
        };

        float bestScore = -1f;
        Vec2f bestOffset = new Vec2f(0f, -10f); // 默认上扬

        for (float[] offset : offsets) {
            float score = getClearDistance(yaw + offset[0], pitch + offset[1], 6.0f);
            if (score > bestScore) {
                bestScore = score;
                bestOffset = new Vec2f(offset[0], offset[1]);
            }
        }

        return bestOffset;
    }

    private static float getClearDistance(float yaw, float pitch, float maxDist) {
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d dir = getLookVec(yaw, pitch);
        Vec3d end = start.add(dir.multiply(maxDist));

        BlockHitResult hit = mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));

        return hit.getType() == HitResult.Type.MISS
                ? maxDist
                : (float) hit.getPos().distanceTo(start);
    }

    private static Vec3d getLookVec(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float x = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        float y = -MathHelper.sin(pitchRad);
        float z = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);

        return new Vec3d(x, y, z);
    }

    public static float lerpYaw(float TargetYaw, float t) {

        float a = mc.player.getYaw();
        return a + (TargetYaw - a) * t;
    }

    public static float lerpPitch(float TargetPitch, float t) {

        float a = mc.player.getPitch();
        return a + (TargetPitch - a) * t;
    }
}
