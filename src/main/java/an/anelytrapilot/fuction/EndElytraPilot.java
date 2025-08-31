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
import an.anelytrapilot.util.Timer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import static an.anelytrapilot.config.ANPilotConfig.setCustomMessage;
import static an.anelytrapilot.fuction.ElytraCollect.*;
import static an.anelytrapilot.mic.BreakModule.continueBreaking;
import static an.anelytrapilot.mic.BreakModule.startBreaking;
import static an.anelytrapilot.mic.EndCityShipExtractor.saveSingleExploredCoordinate;
import static an.anelytrapilot.mic.EndShipFinder.findDragonHeadAttachedBlock;
import static an.anelytrapilot.path.WalkToward.IsArrived;
import static an.anelytrapilot.path.WalkToward.WalkToElytra;
import static an.anelytrapilot.util.MessageSend.sendMessage;


public class EndElytraPilot {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // 配置（替代 Setting）
    private static int targetX = 0;
    private static int targetY = 120;
    private static int targetZ = 0;
    private static Mode mode = Mode.Normal;
    private static boolean highRequire = true;

    private enum Mode { Normal, End }

    private BlockPos targetPos = null;
    private float playerYawAvoid = 0f;
    private boolean isAvoid = false;
    private final float smoothingSpeed = 5f; // 朝向平滑速度
    private static int glideStartY = 0;
    private static int savedSlot = -1; // 保存切换前的物品栏槽位

    //独立
    private static int sequence = 0; // 放在类字段中保存状态
    private static boolean IsEnder = true;
    private static boolean IsBreakingShuler = false;
    private static boolean IsBreakingEnder = false;
    private static boolean IsCollect = false;
    private static boolean IsFireStop = false;

    private static BlockPos FlyTargetPos;
    public static BlockPos ElytraTargetPos;
    public static BlockPos ElytraChest1Pos;
    public static BlockPos ElytraChest2Pos;
    public static BlockPos ShulerPlacePos;
    public static BlockPos EnderPlacePos;

    public static ItemFrameEntity ElytraFrame;
    private static BlockPos FoundPos;
    private static BlockPos FoundPos1;

    private static BlockPos PilotLandPos;
    private static BlockPos StartFlyPos;


    private static float FlyPlayerYaw;
    private static int HorizontalDist;
    private static int HorizontalDist1;
    private static int GlideStartY;
    private static int ElytraCount;
    private static int ElytraCount1;

    private static int CoolDown;


    private static long lastFireworkTime = 0;
    private static long escapeStartTime = 0;
    private static final Timer CloseTimer = new Timer();
    private static final Timer EnderTimer = new Timer();

    public enum FlyStage { ESCAPE, ASCEND, ANNN, GLIDE, Correct, FIND, TAKE, Collect, Shuler, Shuler2, EXIT, STOP }
    public static FlyStage stage = FlyStage.ESCAPE;

    private enum ANNNStage { ANNN1, ANNN2}
    private static ANNNStage stage2 = ANNNStage.ANNN1;
    public static int i = 0;

    private static void FlyToEndCity(BlockPos EndCityPos){

        if(hasFirework()<20){

            //ANSharedStates.FlyStop = true;
            IsFireStop = true;
            if(i==0){
                sendMessage("你的烟花火箭已不足20个,将在下一个目标点进行Log！");
                i =1;
            }
        }else {
            i =0;
            IsFireStop = false;
        }

        if (mc.world.getRegistryKey() == World.END && mc.player.getY() <= 50) {
            ANSharedStates.FlyStop = true;
            setCustomMessage("意外事件，Y高度低于了50，log坐标为: "+mc.player.getX()+" "+mc.player.getY()+" "+mc.player.getZ());
            mc.world.disconnect();
            //sendMessage("末地高度过低，断开连接");
            return;
        }

        FlyPlayerYaw = getYawTo(EndCityPos.getX(),EndCityPos.getZ());
        int dx = (int) (EndCityPos.getX() + 0.5 - mc.player.getX());
        int dz = (int) (EndCityPos.getZ()+ 0.5 - mc.player.getZ());
        long correct = (long) dx * dx + (long) dz * dz;
        HorizontalDist = (int) Math.sqrt(correct);

        // 计算滑翔起始高度
        GlideStartY = Math.min(EndCityPos.getY() + 10 + (int)(HorizontalDist * 0.12), 400);
        long currentTime = mc.world.getTime();

        switch (stage){
            case ESCAPE -> {
                mc.player.setPitch(lerpPitch(-10.0f,0.2f));

                if(escapeStartTime==0)escapeStartTime = mc.world.getTime();
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                    new Thread(() -> {
                        try {
                            Thread.sleep(200); // 延迟 150 毫秒，大概 2 tick
                        } catch (InterruptedException ignored) {
                        }
                        sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        useFirework();

                        lastFireworkTime = mc.world.getTime();
                    }).start();
                }

                if ((currentTime - escapeStartTime) >= 80) {

                    stage = FlyStage.ASCEND;
                    escapeStartTime = 0;
                }
            }

            case ASCEND -> {
                ElytraObstacleAvoider.ANavoidObstacles();
                mc.player.setPitch(lerpPitch(-30.0f,0.2f));
                mc.player.setYaw(lerpYaw(FlyPlayerYaw,0.2f));

                if ((currentTime - lastFireworkTime) >= 80) {
                    useFirework();
                    lastFireworkTime = currentTime;
                }

                ElytraObstacleAvoider.ANavoidObstacles();
                if (mc.player.getY() >= GlideStartY) {
                    stage = FlyStage.ANNN;
                    sendMessage("达到滑翔高度，开始滑翔");
                }
            }

            case ANNN -> {
                mc.player.setYaw(lerpYaw(FlyPlayerYaw, 0.2f));
                if(HorizontalDist>1000){
                    if(mc.player.getY()>400){
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

                if(HorizontalDist<1000)stage =FlyStage.GLIDE;

            }

            case GLIDE -> {

                mc.player.setYaw(lerpYaw(FlyPlayerYaw,0.2f));
                ElytraObstacleAvoider.ANavoidObstacles();
                if ((getPitchToTarget(GlideStartY, 1000f, EndCityPos)) <= -12 ) {

                    mc.player.setPitch(lerpPitch((getPitchToTarget(GlideStartY, 1000f, EndCityPos)),0.2f));
                    if ((currentTime - lastFireworkTime) >= 80) {
                        useFirework();

                        lastFireworkTime = currentTime;
                    }
                } else {
                    mc.player.setPitch(lerpPitch((getPitchToTarget(GlideStartY, 1000f, EndCityPos)),0.2f));

                }
                if(HorizontalDist <= 100){

                    if(EndShipFinder.findNearestElytraFrame()!=null){
                        ElytraTargetPos = EndShipFinder.findNearestElytraFrame().elytraPos;
                        PilotLandPos = EndShipFinder.findNearestElytraFrame().deckPos;
                        ElytraFrame = EndShipFinder.findNearestElytraFrame().frame;

                        findChestsAndFront(ElytraTargetPos);
                        sendMessage("鞘翅坐标:"+ElytraTargetPos);
                        if(mc.player.getY()-ElytraTargetPos.getY()<20) stage = FlyStage.Correct;
                        else stage = FlyStage.FIND;
                    }
                }

                if(HorizontalDist <= 10 && EndShipFinder.findNearestElytraFrame()==null ){
                    if(IsFireStop)stage = FlyStage.EXIT;
                    if(EndCityShipExtractor.getNextShipCoordinate()!=null){
                        BlockPos flyTargetPos1 = EndCityShipExtractor.getNextShipCoordinate();
                        ANPilotConfig.setTarget(flyTargetPos1.getX(), flyTargetPos1.getY(), flyTargetPos1.getZ());
                        sendMessage("未找到鞘翅！下个目标点: "+flyTargetPos1);
                        FoundPos = flyTargetPos1;
                        saveSingleExploredCoordinate(FoundPos);
                        stage = FlyStage.ESCAPE;
                    }else{
                        ANSharedStates.FlyStop = true;
                        setCustomMessage("坐标已全部探索！log坐标为: "+(int)mc.player.getX()+" "+(int)mc.player.getY()+" "+(int)mc.player.getZ()+"   烟花数量:"+hasFirework());
                        mc.world.disconnect();
                    }


                }
            }

            case EXIT -> {
                ANSharedStates.FlyStop = true;
                setCustomMessage("烟花火箭数量不足，log坐标为: "+(int)mc.player.getX()+" "+(int)mc.player.getY()+" "+(int)mc.player.getZ()+"   烟花数量:"+hasFirework());
                mc.world.disconnect();
            }

            case Correct -> {
                mc.player.setPitch(lerpPitch(-90.0f,0.4f));
                if ((currentTime - lastFireworkTime) >= 80) {
                    useFirework();
                    lastFireworkTime = currentTime;
                }
                if(mc.player.getY()-ElytraTargetPos.getY()>20) stage = FlyStage.FIND;
            }

            case FIND -> {

                int dx1 = (int) (PilotLandPos.getX() + 0.5 - mc.player.getX());
                int dz1 = (int) (PilotLandPos.getZ()+ 0.5 - mc.player.getZ());
                HorizontalDist1 = (int) Math.sqrt(dx1 * dx1 + dz1 * dz1);
                GlideStartY = Math.min(PilotLandPos.getY() + 20 + (int)(HorizontalDist1 * 0.12), 300);

                if(mc.player.getY()-PilotLandPos.getY()>5&&mc.player.isOnGround()){
                    stage = FlyStage.ESCAPE;
                }
                FlyPlayerYaw = getYawTo(PilotLandPos.getX(),PilotLandPos.getZ());
                mc.player.setYaw(FlyPlayerYaw);
                mc.player.setPitch(getPitchToTarget(GlideStartY, 1000f, PilotLandPos));

                ElytraCount = countElytra();
                if(mc.player.isOnGround()&&(mc.player.getY()-PilotLandPos.getY())<5)stage = FlyStage.TAKE;

            }

            case TAKE -> {

                mc.player.setPitch(lerpPitch(30.0f,0.2f));
                WalkToElytra(ElytraTargetPos);
                if(IsArrived&&!isShulkerPresent(ElytraTargetPos)) {
                    IsArrived = false;
                    mc.interactionManager.attackEntity(mc.player, ElytraFrame);
                    mc.player.swingHand(Hand.MAIN_HAND);

                    //mc.interactionManager.attackEntity(mc.player, ElytraFrame);
                    //mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

                }

                ElytraCount1 = countElytra();
                if (ElytraCount1 > ElytraCount) {
                    // 鞘翅数量增加了
                    sendMessage("鞘翅+1");
                    if(IsFireStop)stage = FlyStage.EXIT;
                    saveSingleExploredCoordinate(FoundPos);
                    ElytraTargetPos = null;
                    CoolDown = 10;
                    if(CountEmptySlot()<2){
                        if(mc.player.isOnGround()){
                            PlaceShuler(ShulerPlacePos);
                            IsCollect = true;
                            stage = FlyStage.Collect;
                        }
                    }else stage = FlyStage.STOP;
                }

                if(findDragonHeadAttachedBlock(20)!=null){
                    StartFlyPos = findDragonHeadAttachedBlock(20).up(5);
                }
            }

            case Collect -> {

                if(CountEmptySlot()>2 && !IsCollect) {
                    stage = FlyStage.Shuler2;
                }

                if(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler){
                    CoolDown--;
                    if(CoolDown<=0)ElytraMove();
                    if (countElytra()<5 || IsShulkerBoxFull()){
                        mc.player.closeHandledScreen();
                        IsBreakingShuler = true;
                    }
                }

                if(IsBreakingShuler){
                    // 第一次调用
                    startBreaking(ShulerPlacePos);
                    continueBreaking();
                    if(mc.world.getBlockState(ShulerPlacePos).isAir()){
                        WalkToElytra(ShulerPlacePos);
                        if(IsArrived){
                            IsArrived=false;
                            if (CloseTimer.every(3000)) {
                                IsBreakingShuler=false;
                                IsCollect = false;
                                EnderTimer.reset();
                                stage = FlyStage.Shuler2;
                            }
                        }
                    }
                }
            }

            case Shuler2 -> {
                if (IsFullShulker()){
                    if(mc.player.isOnGround()){
                        CloseTimer.reset();
                        PlaceEnderChest(EnderPlacePos);
                        CoolDown = 10;
                        stage = FlyStage.Shuler;
                    }
                }
                if (EnderTimer.every(4000)){
                    // sendMessage("hhhh");
                    stage = FlyStage.STOP;
                }

            }

            case Shuler -> {
                if(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler){
                    CoolDown--;
                    if(CoolDown<=0)ShulerMove();
                    if (CloseTimer.every(3000)) {
                        mc.player.closeHandledScreen();
                        IsBreakingEnder = true;
                    }
                }

                if(IsBreakingEnder){

                    if(IsEnder){
                        startBreaking(EnderPlacePos);
                        continueBreaking();
                    }

                    if(mc.world.getBlockState(EnderPlacePos).isAir()){
                        IsEnder = false;
                        WalkToElytra(EnderPlacePos);
                        if(IsArrived) {
                            IsArrived = false;
                            if (CloseTimer.passedMs(3000)) {
                                IsBreakingEnder = false;
                                stage = FlyStage.STOP;
                            }

                        }

                    }
                }

            }

            case STOP -> {
                WalkToElytra(StartFlyPos);
                IsEnder = true;
                if(IsArrived) {
                    IsArrived = false;
                    if(EndCityShipExtractor.getNextShipCoordinate()!=null){
                        BlockPos flyTargetPos1 = EndCityShipExtractor.getNextShipCoordinate();
                        ANPilotConfig.setTarget(flyTargetPos1.getX(), flyTargetPos1.getY(), flyTargetPos1.getZ());
                        sendMessage("下个目标点: "+flyTargetPos1);
                        FoundPos = flyTargetPos1;
                        stage = FlyStage.ESCAPE;
                    }else{
                        ANSharedStates.FlyStop = true;
                        setCustomMessage("坐标已全部探索！log坐标为: "+(int)mc.player.getX()+" "+(int)mc.player.getY()+" "+(int)mc.player.getZ()+"   烟花数量:"+hasFirework());
                        mc.world.disconnect();
                    }
                }

                //breakTwoBlocksInFront();
            }

        }

    }

    public static boolean isShulkerPresent(BlockPos elytraPos) {
        if (mc.world == null) return false;

        int radiusXZ = 2;
        int radiusY = 1;

        Box searchBox = new Box(
                elytraPos.getX() - radiusXZ, elytraPos.getY() - radiusY, elytraPos.getZ() - radiusXZ,
                elytraPos.getX() + radiusXZ + 1, elytraPos.getY() + radiusY + 1, elytraPos.getZ() + radiusXZ + 1
        );

        List<ShulkerEntity> shulkers = mc.world.getEntitiesByClass(
                ShulkerEntity.class,
                searchBox,
                shulker -> true
        );

        return !shulkers.isEmpty();
    }


    public static List<BlockPos> findNearbyChests(BlockPos elytraPos) {
        List<BlockPos> chestPositions = new ArrayList<>();

        int radius = 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = elytraPos.add(dx, dy, dz);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.CHEST) {
                        chestPositions.add(pos);
                    }
                }
            }
        }
        return chestPositions;
    }

    public static void findChestsAndFront(BlockPos elytraPos) {
        List<BlockPos> chests = findNearbyChests(elytraPos);

        // 距离 Elytra 最近的两个箱子
        chests.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(elytraPos.toCenterPos())));
        ElytraChest1Pos = chests.get(0);
        ElytraChest2Pos = chests.get(1);

        ShulerPlacePos = getChestFrontAir(ElytraChest1Pos);
        EnderPlacePos = getChestFrontAir(ElytraChest2Pos);

        //sendMessage("Chest1 = " + ElytraChest1Pos + ", 前方空气: " + ShulerPlacePos);
        //sendMessage("Chest2 = " + ElytraChest2Pos + ", 前方空气: " + EnderPlacePos);
    }

    private static BlockPos getChestFrontAir(BlockPos chestPos) {
        BlockState state = mc.world.getBlockState(chestPos);
        Direction facing = state.contains(Properties.HORIZONTAL_FACING)
                ? state.get(Properties.HORIZONTAL_FACING)
                : Direction.NORTH;

        BlockPos front = chestPos.offset(facing);
        if (mc.world.getBlockState(front).isAir()) {
            return front;
        } else {
            return null; // 不为空可做其他逻辑
        }
    }


/*
    public static void findChestCoordinates(BlockPos elytraPos) {
        // 检测桥的方向
        Direction bridgeDirection = detectBridgeDirection(elytraPos);

        // 根据桥的方向确定末地船朝向（船头朝向桥的相反方向）
        Direction shipFacing = bridgeDirection.getOpposite();

        if (shipFacing.getAxis() == Direction.Axis.Z) {
            // 船沿Z轴，箱子在X轴方向
            ElytraChest1Pos = elytraPos.offset(Direction.WEST, 1); // 右侧箱子
            ElytraChest2Pos = elytraPos.offset(Direction.EAST, 1); // 左侧箱子
            ShulerPlacePos = new BlockPos(ElytraChest1Pos.getX(),
                    ElytraChest1Pos.getY()-1,
                    ElytraChest1Pos.getZ()-1);
            EnderPlacePos = new BlockPos(ElytraChest2Pos.getX(),
                    ElytraChest2Pos.getY()-1,
                    ElytraChest2Pos.getZ()-1);


        } else {
            // 船沿X轴，箱子在Z轴方向
            ElytraChest1Pos= elytraPos.offset(Direction.SOUTH, 1); // 下方箱子
            ElytraChest2Pos= elytraPos.offset(Direction.NORTH, 1); // 上方箱子
            ShulerPlacePos = new BlockPos(ElytraChest1Pos.getX()+1,
                    ElytraChest1Pos.getY(),
                    ElytraChest1Pos.getZ());
            EnderPlacePos = new BlockPos(ElytraChest2Pos.getX(),
                    ElytraChest2Pos.getY()-1,
                    ElytraChest2Pos.getZ()-1);
        }

    }

 */

    private static int countElytra() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return 0;

        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA) {
                count++;
            }
        }
        return count;
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

    public static int hasFirework() {
        PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (inv.getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                count += stack.getCount();
            }
        }
        return count;
    }


    public static void tick() {

        if (ANSharedStates.FlyCommend && mc.player != null) {
            //FlyTargetPos = ANSharedStates.FlyTarget;
            FlyTargetPos = ANPilotConfig.getTargetPos();

            FlyToEndCity(FlyTargetPos);

        }

        if(ANSharedStates.FileLoad){
            EndCityShipExtractor.loadShipCoordinates();
            ANSharedStates.FileLoad = false;
        }

        if(!ANPilotConfig.getMode().equals("end"))return;

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

    private static float getPitchToTarget(float glideStartY, float maxGlideDist, BlockPos targetPos) {
        double dx = targetPos.getX() + 0.5 - mc.player.getX();
        double dz = targetPos.getZ() + 0.5 - mc.player.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float currentY = (float) mc.player.getY();
        float desiredY = targetPos.getY() + (glideStartY - targetPos.getY()) * ((float) horizontalDist / maxGlideDist);
        desiredY = Math.min(glideStartY, desiredY);
        float error = currentY - desiredY;
        float pitch = error * 0.6f + 5f;
        return MathHelper.clamp(pitch, -15f, 10f);
    }

    private BlockPos getObstacleBlockPos(double distance) {
        Vec3d start = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0f).normalize();
        Vec3d end = start.add(lookVec.multiply(distance));
        BlockHitResult result = mc.world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player
        ));
        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getBlockPos();
        }
        return null;
    }

    private boolean isObstacleFaceSealed(BlockPos obstacle, Vec3d look) {
        int dirX = (int) Math.round(look.x);
        int dirZ = (int) Math.round(look.z);
        BlockPos scanCenter = obstacle.add(dirX, 0, dirZ);
        int range = 2;
        for (int y = -range; y <= range; y++) {
            for (int offset = -range; offset <= range; offset++) {
                BlockPos check = Math.abs(dirX) == 1 ? scanCenter.add(0, y, offset) : scanCenter.add(offset, y, 0);
                if (mc.world.isAir(check)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void handleObstacleAvoid(BlockPos obstacle, Vec3d look) {
        int dirX = (int) Math.round(look.x);
        int dirZ = (int) Math.round(look.z);
        BlockPos faceCenter = obstacle.add(dirX, 0, dirZ);
        boolean isXDirection = Math.abs(dirX) == 1;
        Vec3i leftDir = getHorizontalLeftDir(look);
        Vec3i rightDir = getHorizontalRightDir(look);
        BlockPos leftFace = faceCenter.add(leftDir);
        BlockPos rightFace = faceCenter.add(rightDir);
        int leftAir = countAirInSide(leftFace, leftDir, isXDirection);
        int rightAir = countAirInSide(rightFace, rightDir, isXDirection);
        int upAir = countUpAirAbove(obstacle, 5, 3);

        if (leftAir == 0 && rightAir == 0) {
            if (upAir >= 8) {
                mc.player.setPitch(-70f);
                sendMessage("向上飞越障碍！");
            } else {
                sendMessage("前方完全封闭，停止导航");
            }
        } else if (leftAir >= rightAir) {
            playerYawAvoid = mc.player.getYaw() - 60f;
        } else {
            playerYawAvoid = mc.player.getYaw() + 60f;
        }
    }

    private Vec3i getHorizontalLeftDir(Vec3d look) {
        int x = (int) Math.round(look.x);
        int z = (int) Math.round(look.z);
        return new Vec3i(-z, 0, x);
    }

    private Vec3i getHorizontalRightDir(Vec3d look) {
        int x = (int) Math.round(look.x);
        int z = (int) Math.round(look.z);
        return new Vec3i(z, 0, -x);
    }

    private int countAirInSide(BlockPos faceCenter, Vec3i sideDir, boolean isXDirection) {
        int range = 2;
        int airCount = 0;
        for (int y = -range; y <= range; y++) {
            for (int offset = -range; offset <= range; offset++) {
                BlockPos check = isXDirection ? faceCenter.add(sideDir.getX(), y, offset) : faceCenter.add(offset, y, sideDir.getZ());
                if (mc.world.isAir(check)) {
                    airCount++;
                }
            }
        }
        return airCount;
    }

    private int countUpAirAbove(BlockPos base, int rangeXZ, int height) {
        int airCount = 0;
        for (int dx = -rangeXZ / 2; dx <= rangeXZ / 2; dx++) {
            for (int dz = -rangeXZ / 2; dz <= rangeXZ / 2; dz++) {
                for (int dy = 1; dy <= height; dy++) {
                    BlockPos check = base.add(dx, dy, dz);
                    if (mc.world.isAir(check)) {
                        airCount++;
                    }
                }
            }
        }
        return airCount;
    }

    public static void useFirework() {

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        ClientPlayNetworkHandler connection = mc.getNetworkHandler();

        if (player == null || connection == null) return;

        int selectedSlot = player.getInventory().selectedSlot;
        int fireworkSlot = -1;
        boolean fromInventory = false;

        // 查找烟花
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                fireworkSlot = i;
                break;
            }
        }

        if (fireworkSlot == -1) {
            for (int i = 9; i < player.getInventory().size(); i++) {
                if (player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                    mc.interactionManager.clickSlot(player.currentScreenHandler.syncId, i, selectedSlot, SlotActionType.SWAP, player);
                    connection.sendPacket(new CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId));
                    fireworkSlot = selectedSlot;
                    fromInventory = true;
                    break;
                }
            }
        }

        if (fireworkSlot != selectedSlot) {
            player.getInventory().selectedSlot = fireworkSlot;
        }

        // 🔧 使用本地维护的序列号
        connection.sendPacket(new PlayerInteractItemC2SPacket(
                Hand.MAIN_HAND,
                sequence++,
                player.getYaw(),
                player.getPitch()
        ));
        connection.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        if (fromInventory) {
            mc.interactionManager.clickSlot(player.currentScreenHandler.syncId, fireworkSlot, selectedSlot, SlotActionType.SWAP, player);
            connection.sendPacket(new CloseHandledScreenC2SPacket(player.currentScreenHandler.syncId));
        }
    }

    private static int findFireworkSlot() {
        ClientPlayerEntity player = mc.player;
        // 优先检查快捷栏
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        // 检查背包
        for (int i = 9; i < player.getInventory().main.size(); i++) {
            if (player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }
}