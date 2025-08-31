package an.anelytrapilot.fuction;

import an.anelytrapilot.commend.ANSharedStates;
import an.anelytrapilot.mic.PlaceModule;
import an.anelytrapilot.util.Timer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static an.anelytrapilot.util.MessageSend.sendMessage;

public class ElytraCollect {

    private static final Timer delayTimer = new Timer();
    private static int ShulerSlot;
    protected enum InteractMode {
        Packet,
        Normal
    }

    private enum Mode {
        QUICK_MOVE, SWAP
    }

    private static boolean IsMoving = false;
    private static boolean IsEnderMoving = false;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static void tick() {
        if (ANSharedStates.TestCommend && mc.player != null) {
            ANSharedStates.TestCommend = false;
            ANSharedStates.FlyStop = false;
            //PlaceEnderChest();
            PlaceShuler();

        }

        if(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && IsEnderMoving){
            ShulerMove();
            IsEnderMoving = false;
        }
        if(ANSharedStates.FlyStop)return;
        //sendMessage(""+CountEmptySlot());
    }

    public static void clickSlot(int id, int button, SlotActionType type) {
        if (id == -1 || mc.interactionManager == null || mc.player == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, id, button, type, mc.player);
    }

    public static void PlaceShuler(){
        BlockPos placePos = mc.player.getBlockPos().offset(mc.player.getHorizontalFacing());
        Vec3d hitVec = new Vec3d(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5);

        if (IsFindShulker()) {
            boolean a = placeBlock(placePos, true, InteractMode.Normal, PlaceModule.Rotate.Default, ShulerSlot);
            if (a) sendMessage("潜影盒放置成功！");

            BlockHitResult hitResult = new BlockHitResult(
                    hitVec,
                    Direction.UP,
                    placePos,
                    false
            );

            mc.interactionManager.interactBlock(
                    mc.player,
                    Hand.MAIN_HAND,
                    hitResult
            );
        }
        // IsMoving = mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler;

    }

    public static void PlaceShuler(BlockPos Shuler){
        //BlockPos placePos = mc.player.getBlockPos().offset(mc.player.getHorizontalFacing());
        Vec3d hitVec = new Vec3d(Shuler.getX() + 0.5, Shuler.getY() + 0.5, Shuler.getZ() + 0.5);

        if (IsFindShulker()) {
            boolean a = placeBlock(Shuler, true, InteractMode.Normal, PlaceModule.Rotate.Default, ShulerSlot);
            if (a) sendMessage("潜影盒放置成功！");

            lookAt(hitVec);
            BlockHitResult hitResult = new BlockHitResult(
                    hitVec,
                    Direction.UP,
                    Shuler,
                    false
            );

            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                mc.execute(() -> {
                    mc.interactionManager.interactBlock(
                            mc.player,
                            Hand.MAIN_HAND,
                            hitResult
                    );

                    mc.player.swingHand(Hand.MAIN_HAND);
                });
            }).start();
        }

    }

    public static void lookAt(Vec3d target) {
        Vec3d eyes = mc.player.getEyePos();

        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));

        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

/*
    public static void ElytraMove(){
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA) {
                if (delayTimer.every(500)) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i+18, 1, SlotActionType.QUICK_MOVE, mc.player);
                }
            }
        }
    }*/

    public static void ElytraMove() {
        List<Pair<Integer, ItemStack>> elytraList = new ArrayList<>();

        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA) {
                elytraList.add(new Pair<>(i, stack));
            }
        }

        // 按耐久度从低到高排序（damage 高 → damage 低）
        elytraList.sort((a, b) -> Integer.compare(
                b.getRight().getDamage(),
                a.getRight().getDamage()
        ));

        // 移动 Elytra
        for (Pair<Integer, ItemStack> pair : elytraList) {
            int slot = pair.getLeft();

            if (delayTimer.every(200)) {
                mc.interactionManager.clickSlot(
                        mc.player.currentScreenHandler.syncId,
                        slot + 18,
                        1,
                        SlotActionType.QUICK_MOVE,
                        mc.player
                );
            }
        }
    }


    public static boolean IsShulkerBoxFull() {
        if (!(mc.player.currentScreenHandler instanceof ShulkerBoxScreenHandler handler)) {
            return false;
        }
        boolean isFull = true;

        // 潜影盒容量固定 27 个格子 (slots 0~26)
        for (int i = 0; i < 27; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) {
                isFull = false;
                break;
            }
        }

        return isFull;
    }

    public static void ShulerMove(){
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.SHULKER_BOX) {
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
                // 如果容器组件为空，则潜影盒是空的
                if (compoundTag.stream().toList().size() == 27) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i+54, 1, SlotActionType.QUICK_MOVE, mc.player);
                    //if(i>8)mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i+18, 1, SlotActionType.QUICK_MOVE, mc.player);

                    //sendMessage(""+compoundTag.stream().toList().size());
                    //if (delayTimer.every(20)) {

                    //}
                }
            }
        }
    }

    public static int countElytra() {
        ClientPlayerEntity player = mc.player;
        if (player == null) return 0;

        int count = 0;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA) {
                count++;
            }
        }
        return count;
    }

    static int CountEmptySlot() {
        int emptySlots = 0;

        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                emptySlots++;
            }
        }
        return emptySlots;
    }

    public static void PlaceEnderChest(){
        BlockPos placePos = mc.player.getBlockPos().offset(mc.player.getHorizontalFacing());
        Vec3d hitVec = new Vec3d(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5);

        if (IsFindEnderChest()){
            boolean a = placeBlock(placePos,true,InteractMode.Normal, PlaceModule.Rotate.Default,ShulerSlot);
            if(a)sendMessage("潜影盒放置成功！");

            BlockHitResult hitResult = new BlockHitResult(
                    hitVec,
                    Direction.UP,
                    placePos,
                    false
            );

            mc.interactionManager.interactBlock(
                    mc.player,
                    Hand.MAIN_HAND,
                    hitResult
            );
        }

        IsEnderMoving = true;

    }

    public static void PlaceEnderChest(BlockPos placePos){
        Vec3d hitVec = new Vec3d(placePos.getX() + 0.5, placePos.getY() + 0.5, placePos.getZ() + 0.5);

        if (IsFindEnderChest()){
            boolean a = placeBlock(placePos,true,InteractMode.Normal, PlaceModule.Rotate.Default,ShulerSlot);
            if(a)sendMessage("末影箱放置成功！");
            lookAt(hitVec);
            BlockHitResult hitResult = new BlockHitResult(
                    hitVec,
                    Direction.UP,
                    placePos,
                    false
            );

            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException e) {}
                mc.execute(() -> {
                    mc.interactionManager.interactBlock(
                            mc.player,
                            Hand.MAIN_HAND,
                            hitResult
                    );

                    mc.player.swingHand(Hand.MAIN_HAND);
                });
            }).start();

        }

    }

    protected static boolean placeBlock(BlockPos pos, boolean ignoreEntities, InteractMode mode, PlaceModule.Rotate rotate, int slot) {
        boolean validInteraction = false;

        if (mode == InteractMode.Packet) {
            validInteraction = PlaceModule.placeBlock(pos, rotate, PlaceModule.Interact.Strict, PlaceModule.PlaceMode.Packet, slot, true, ignoreEntities);
        }
        if (mode == InteractMode.Normal) {
            validInteraction = PlaceModule.placeBlock(pos, rotate, PlaceModule.Interact.Strict, PlaceModule.PlaceMode.Normal, slot, true, ignoreEntities);
        }

        if (validInteraction && mc.player != null) {
            mc.player.swingHand(Hand.MAIN_HAND);
        }

        return validInteraction;
    }

    private static boolean IsFindShulker() {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.SHULKER_BOX) {
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);

                if (compoundTag.stream().toList().size() < 27) {
                    mc.player.getInventory().selectedSlot = i;

                    return true;
                }
            }
        }
        return false;
    }

    public static boolean IsFullShulker() {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.SHULKER_BOX) {
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);

                if (compoundTag.stream().toList().size() == 27) return true;
            }
        }
        return false;
    }


    private static boolean IsFindEnderChest() {
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.ENDER_CHEST) {
                //mc.player.getInventory().selectedSlot = i;
                //boolean swap = true;
                //clickSlot(i, swap ? i : 0, swap ? SlotActionType.SWAP : SlotActionType.QUICK_MOVE);
                mc.player.getInventory().selectedSlot = i;

                return true;
            }
        }
        return false;
    }

}
