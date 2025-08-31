package an.anelytrapilot.mic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import static an.anelytrapilot.fuction.EndElytraPilot.lerpPitch;
import static an.anelytrapilot.fuction.EndElytraPilot.lerpYaw;

public class BreakModule {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private static BlockPos targetBlock = null;


    public static void startBreaking(BlockPos pos) {
        if (mc.player == null || mc.world == null) return;

        targetBlock = pos;

        // 玩家视角对准方块
        Vec3d targetCenter = Vec3d.ofCenter(pos);
        float[] angles = getYawPitchTo(mc.player.getEyePos(), targetCenter);
        mc.player.setYaw(lerpYaw(angles[0], 0.2f));
        mc.player.setPitch(lerpPitch(angles[1], 0.2f));

        // 开始破坏
        mc.interactionManager.attackBlock(pos, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
    }


    public static void continueBreaking() {
        if (targetBlock == null || mc.player == null || mc.world == null) return;

        if (mc.world.isAir(targetBlock)) {
            // 方块已被破坏
            targetBlock = null;
            return;
        }

        ClientPlayerInteractionManager manager = mc.interactionManager;

        manager.updateBlockBreakingProgress(targetBlock, Direction.UP);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    public static void cancelBreaking() {
        targetBlock = null;
    }

    public static float[] getYawPitchTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (Math.toDegrees(-Math.atan2(dy, distXZ)));

        return new float[]{yaw, pitch};
    }
}
