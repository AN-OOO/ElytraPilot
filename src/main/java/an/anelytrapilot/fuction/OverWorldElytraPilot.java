/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the terms of the MIT License.
 */

package an.anelytrapilot.fuction;

import an.anelytrapilot.commend.ANSharedStates;
import an.anelytrapilot.config.ANPilotConfig;
import an.anelytrapilot.config.PlayerDataSender;
import an.anelytrapilot.mic.ElytraObstacleAvoider;
import an.anelytrapilot.mic.EndCityShipExtractor;
import an.anelytrapilot.mic.EndShipFinder;
import an.anelytrapilot.path.WalkToward;
import an.anelytrapilot.util.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import static an.anelytrapilot.config.ANPilotConfig.setCustomMessage;
import static an.anelytrapilot.fuction.EndElytraPilot.useFirework;
import static an.anelytrapilot.mic.EndShipFinder.findDragonHeadAttachedBlock;
import static an.anelytrapilot.path.WalkToward.IsArrived;
import static an.anelytrapilot.path.WalkToward.WalkToElytra;
import static an.anelytrapilot.util.MessageSend.sendMessage;

public class OverWorldElytraPilot {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static BlockPos OverWorldFlyTargetPos;
    public enum OverWorldFlyStage { ESCAPE, ASCEND, ANNN, GLIDE, STOP }
    public static OverWorldFlyStage stage = OverWorldFlyStage.ESCAPE;

    private enum ANNNStage { ANNN1, ANNN2}
    private static ANNNStage stage2 = ANNNStage.ANNN1;


    private static float FlyPlayerYaw;
    private static int HorizontalDist;
    private static int GlideStartY;

    private static long lastFireworkTime = 0;
    private static long escapeStartTime = 0;

    private static final Timer EnderTimer = new Timer();

    public static void tick() {

        if (ANSharedStates.OverWorldFlyCommend && mc.player != null) {
            OverWorldFlyTargetPos = ANSharedStates.FlyTarget;
            if(OverWorldFlyTargetPos!=null)FlyToOverWorld(OverWorldFlyTargetPos);

        }

    }

    private static void FlyToOverWorld(BlockPos EndCityPos){

        if(!hasFirework()){
            ANSharedStates.FlyStop = true;
            sendMessage("你没有火箭了,导航结束");
            return;
        }

        if (mc.world.getRegistryKey() == World.END && mc.player.getY() <= 50) {
            setCustomMessage("意外事件，Y高度低于了50，log坐标为: "+mc.player.getX()+" "+mc.player.getY()+" "+mc.player.getZ());
            mc.world.disconnect();

            return;
        }

        if(mc.world.getRegistryKey()== World.END && mc.player.getY()<50)mc.world.disconnect();

        FlyPlayerYaw = getYawTo(EndCityPos.getX(),EndCityPos.getZ());
        int dx = (int) (EndCityPos.getX() + 0.5 - mc.player.getX());
        int dz = (int) (EndCityPos.getZ()+ 0.5 - mc.player.getZ());
        long correct = (long) dx * dx + (long) dz * dz;

        HorizontalDist = (int) Math.sqrt(correct);
        //float a = Math.sqrt(dx * dx + dz * dz);

        // 计算滑翔起始高度
        GlideStartY = Math.min(EndCityPos.getY() + 10 + (int)(HorizontalDist * 0.12), 300);
        long currentTime = mc.world.getTime();
        if(escapeStartTime==0)escapeStartTime = mc.world.getTime();

        switch (stage){
            case ESCAPE -> {
                mc.player.setPitch(lerpPitch(-10.0f,0.2f));
                ElytraObstacleAvoider.ANavoidObstacles();
                if (mc.player.isOnGround()) {
                    mc.player.jump(); // 模拟跳跃
                    new Thread(() -> {
                        try {
                            Thread.sleep(100); // 延迟 150 毫秒，大概 2 tick
                        } catch (InterruptedException ignored) {
                        }
                        sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        useFirework();
                        lastFireworkTime = mc.world.getTime();
                    }).start();
                }

                if ((currentTime - escapeStartTime) >= 80) {
                    stage = OverWorldFlyStage.ASCEND;
                    escapeStartTime = 0;
                }
            }

            case ASCEND -> {
                ElytraObstacleAvoider.ANavoidObstacles();
                mc.player.setPitch(lerpPitch(-25.0f,0.2f));
                mc.player.setYaw(lerpYaw(FlyPlayerYaw,0.2f));

                if ((currentTime - lastFireworkTime) >= 80) {
                    useFirework();
                    lastFireworkTime = currentTime;
                }

                ElytraObstacleAvoider.ANavoidObstacles();
                if (mc.player.getY() >= GlideStartY) {
                    stage = OverWorldFlyStage.ANNN;
                    sendMessage("达到滑翔高度，开始滑翔");
                }
            }

            case ANNN -> {
                mc.player.setYaw(lerpYaw(FlyPlayerYaw, 0.2f));
                if(HorizontalDist>1000){
                    if(mc.player.getY()>300){
                        stage2 = ANNNStage.ANNN1;
                    }
                    if(mc.player.getY()<230){
                        stage2 = ANNNStage.ANNN2;
                    }
                    switch (stage2){
                        case ANNN1 ->mc.player.setPitch(lerpPitch(10.0f,0.2f));

                        case ANNN2 -> {
                            mc.player.setPitch(lerpPitch(-25.0f,0.2f));
                            if(mc.player.getY()<300){
                                if(EnderTimer.every(4000)) useFirework();
                            }
                        }
                    }

                }

                if(HorizontalDist<1000)stage = OverWorldFlyStage.GLIDE;

            }

            case GLIDE -> {
                mc.player.setYaw(lerpYaw(FlyPlayerYaw, 0.2f));
                ElytraObstacleAvoider.ANavoidObstacles();
                /*
                if ((getPitchToTarget(GlideStartY, 1500f, EndCityPos)) <= -13) {
                    mc.player.setPitch(lerpPitch((getPitchToTarget(GlideStartY, 1000f, EndCityPos)), 0.2f));
                    if ((currentTime - lastFireworkTime) >= 80) {
                        useFirework();
                        lastFireworkTime = currentTime;
                    }
                } else {}*/
                    mc.player.setPitch(lerpPitch((getPitchToTarget(GlideStartY, 1000f, EndCityPos)), 0.2f));



                if (mc.player.isOnGround()) {
                    stage = OverWorldFlyStage.STOP;

                }
            }


            case STOP -> {
                ANSharedStates.FlyStop = true;
                //breakTwoBlocksInFront();
                sendMessage("到达目标点，导航结束");

            }

        }

    }
    protected static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static float lerpYaw(float TargetYaw, float t) {

        float a = mc.player.getYaw();
        return a + (TargetYaw - a) * t;
    }

    public static float lerpPitch(float TargetPitch, float t) {

        float a = mc.player.getPitch();
        return a + (TargetPitch - a) * t;
    }

    private static float getPitchToTarget(float glideStartY, float maxGlideDist, BlockPos targetPos) {
        double dx = targetPos.getX() + 0.5 - mc.player.getX();
        double dz = targetPos.getZ() + 0.5 - mc.player.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float currentY = (float) mc.player.getY();
        float desiredY = targetPos.getY() + (glideStartY - targetPos.getY()) * ((float) horizontalDist / maxGlideDist);
        desiredY = Math.min(glideStartY, desiredY);
        float error = currentY - desiredY;
        float pitch = error * 0.6f + 5f;
        return MathHelper.clamp(pitch, -15f, 30f);
    }

    private static float getYawTo(int x, int z) {
        double dx = x + 0.5 - mc.player.getX();
        double dz = z + 0.5 - mc.player.getZ();
        double yawRad = Math.atan2(dz, dx);
        float yawDeg = (float) Math.toDegrees(yawRad) - 90f;
        yawDeg %= 360f;
        if (yawDeg >= 180f) yawDeg -= 360f;
        if (yawDeg < -180f) yawDeg += 360f;
        return yawDeg;
    }

    public static boolean hasFirework() {
        PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                return true;
            }
        }
        return false;
    }

}