package an.anelytrapilot.commend.cmds;

import an.anelytrapilot.commend.ANSharedStates;
import an.anelytrapilot.commend.cmdmanage.Command;
import an.anelytrapilot.fuction.EndElytraPilot;
import an.anelytrapilot.fuction.OverWorldElytraPilot;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.world.World;

import static an.anelytrapilot.util.MessageSend.sendMessage;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FlyToTarget extends Command {
    public FlyToTarget() {
        super("ANElytraPilot");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("Fly")
                .then(literal("ToTarget")
                        .executes(ctx -> {
                            if (setTarget.FlyState) {
                                ANSharedStates.OverWorldFlyCommend = true;
                                ANSharedStates.FlyStop = false;
                                OverWorldElytraPilot.stage = OverWorldElytraPilot.OverWorldFlyStage.ESCAPE;
                                sendMessage("-> 开始导航向目标点");
                            } else {
                                sendMessage("你需要先设置一个目标点");
                            }
                            return SINGLE_SUCCESS;
                        }))
                .then(literal("ToElytra")  // 子命令也改为小写
                        .executes(ctx -> {
                            if(mc.world.getRegistryKey() != World.END){
                                sendMessage("你只能在末地使用该指令");
                                return 0;
                            }
                            if (FileLoad.FileState && setTarget.FlyState) {  // 只需要检查文件状态
                                ANSharedStates.FlyCommend = true;
                                ANSharedStates.FlyStop = false;
                                EndElytraPilot.stage = EndElytraPilot.FlyStage.ESCAPE;
                                sendMessage("-> 开始导航向目标鞘翅");
                            } else {
                                sendMessage("你需要设置鞘翅坐标文件并且设置一个起点坐标");
                            }
                            return SINGLE_SUCCESS;
                        })));

    }
}
