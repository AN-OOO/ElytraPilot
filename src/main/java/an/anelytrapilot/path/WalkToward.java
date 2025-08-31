package an.anelytrapilot.path;

import an.anelytrapilot.commend.ANSharedStates;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

import static an.anelytrapilot.util.MessageSend.sendMessage;

public class WalkToward {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static List<BlockPos> path = null;
    private static int pathIndex = 0;
    private static boolean isActive = false;
    public static boolean IsArrived = false;

    public static void startPath(BlockPos target) {
        if (mc.player == null || mc.world == null) return;

        BlockPos start = mc.player.getBlockPos();
        start = findNearestWalkable(mc.world, start);
        target = findNearestWalkable(mc.world, target);

        path = ANPathfinder.findPath(mc.world, start, target);
        pathIndex = 0;
        isActive = path != null && !path.isEmpty();

        if (isActive) {
            //mc.player.sendMessage(Text.literal("开始自动寻路，目标步数: " + path.size()), false);
        } else {
            mc.player.sendMessage(Text.literal("无法生成路径"), false);
        }
    }

    public static BlockPos findNearestWalkable(World world, BlockPos pos) {
        int maxOffset = 20; // 向上和向下最多搜索多少格

        // 向下查找地面
        for (int dy = 0; dy < maxOffset; dy++) {
            BlockPos check = pos.down(dy);
            if (isSafeToStand(world, check)) {
                return check.up(); // 返回站在这个方块上方
            }
        }

        // 向上查找平台（比如站在柱子顶）
        for (int dy = 1; dy < maxOffset; dy++) {
            BlockPos check = pos.up(dy);
            if (isSafeToStand(world, check)) {
                return check.up(); // 返回站在这个方块上方
            }
        }

        // 找不到就返回原始位置
        return pos;
    }

    public static boolean isSafeToStand(World world, BlockPos pos) {
        BlockPos ground = pos;
        BlockPos head = pos.up();

        // 检查地面是否坚固（不能是空气/液体）
        boolean solidGround = !world.getBlockState(ground).getCollisionShape(world, ground).isEmpty();

        // 检查上方是否有足够空间（不能有方块阻挡）
        boolean spaceAbove = world.getBlockState(head).getCollisionShape(world, head).isEmpty();

        return solidGround && spaceAbove;
    }

    public static void WalkToElytra(BlockPos ElytraPos) {

        if (ElytraPos!= null) {
            startPath(ElytraPos);
            ANSharedStates.shouldStartWalking = false;
            ANSharedStates.walkTarget = null;
        }

        if (!isActive || mc.player == null || path == null || pathIndex >= path.size()) {
            return;
        }

        if (isActive && path != null && mc.world != null) {
            for (int i = 0; i < path.size(); i++) {
                BlockPos p = path.get(i);
                Vec3d pos = new Vec3d(p.getX() + 0.5, p.getY() -0.5, p.getZ() + 0.5);

                DustParticleEffect particle = new DustParticleEffect(DustParticleEffect.RED, 1.0f);
                mc.world.addParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
        ClientPlayerEntity player = mc.player;
        BlockPos target = path.get(pathIndex);
        Vec3d playerPos = player.getPos();

        double dx = target.getX() + 0.5 - playerPos.x;
        double dz = target.getZ() + 0.5 - playerPos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.6) {
            pathIndex++;
            if (pathIndex >= path.size()) {
                mc.options.forwardKey.setPressed(false);
                //sendMessage("已到达指引点");
                isActive = false;
                IsArrived = true;
                return;
            }
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        player.setYaw(yaw);
        if (player.isOnGround()) {
            player.setSprinting(true); // 强制冲刺
        }
        if (shouldJump(player)) {
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
        }

        mc.options.forwardKey.setPressed(true);
    }


    public static void tick() {

        if (ANSharedStates.shouldStartWalking && ANSharedStates.walkTarget != null) {
            startPath(ANSharedStates.walkTarget);
            ANSharedStates.shouldStartWalking = false;
            ANSharedStates.walkTarget = null;
        }

        if (!isActive || mc.player == null || path == null || pathIndex >= path.size()) {
            return;
        }
        if(ANSharedStates.FlyStop)return;

        if (isActive && path != null && mc.world != null) {
            for (int i = 0; i < path.size(); i++) {
                BlockPos p = path.get(i);
                Vec3d pos = new Vec3d(p.getX() + 0.5, p.getY() -0.5, p.getZ() + 0.5);

                DustParticleEffect particle = new DustParticleEffect(DustParticleEffect.RED, 1.0f);
                mc.world.addParticle(particle, pos.x, pos.y, pos.z, 0, 0, 0);
            }
        }
        ClientPlayerEntity player = mc.player;
        BlockPos target = path.get(pathIndex);
        Vec3d playerPos = player.getPos();

        double dx = target.getX() + 0.5 - playerPos.x;
        double dz = target.getZ() + 0.5 - playerPos.z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 0.6) {
            pathIndex++;
            if (pathIndex >= path.size()) {
                mc.options.forwardKey.setPressed(false);
                sendMessage("已到达指引点");
                isActive = false;
                return;
            }
            return;
        }

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        player.setYaw(yaw);
        if (player.isOnGround()) {
            player.setSprinting(true); // 强制冲刺
        }
        if (shouldJump(player)) {
            mc.options.jumpKey.setPressed(true);
        } else {
            mc.options.jumpKey.setPressed(false);
        }

        mc.options.forwardKey.setPressed(true);
    }

    private static boolean shouldJump(ClientPlayerEntity player) {
        Vec3d facing = player.getRotationVec(1.0f);
        BlockPos front = BlockPos.ofFloored(
                player.getX() + facing.x,
                player.getY(),
                player.getZ() + facing.z
        );

        // 检查玩家正前方一格是否有方块
        return !mc.world.getBlockState(front).getCollisionShape(mc.world, front).isEmpty();
    }
}
