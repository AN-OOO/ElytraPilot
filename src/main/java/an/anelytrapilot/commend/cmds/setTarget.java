package an.anelytrapilot.commend.cmds;

import an.anelytrapilot.commend.ANSharedStates;
import an.anelytrapilot.commend.cmdmanage.Command;
import an.anelytrapilot.config.ANPilotConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.math.BlockPos;

import static an.anelytrapilot.util.MessageSend.sendMessage;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class setTarget extends Command {
    public setTarget() {
        super("ANElytraPilot");
    }

    public static boolean FlyState = false;

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
                literal("SetTarget")
                        .then(arg("x", IntegerArgumentType.integer())
                                .then(arg("y", IntegerArgumentType.integer())
                                        .then(arg("z", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                    if(Math.abs(x-mc.player.getX())>50000||Math.abs(z-mc.player.getZ())>50000){
                                                        sendMessage("你可以设置的目标点<X,Z>轴的距离不可以超过50000格-qwq");
                                                    }else{
                                                        FlyState = true;
                                                        ANPilotConfig.setTarget(x, y, z);
                                                        ANSharedStates.FlyTarget = new BlockPos(x, y, z);
                                                        sendMessage("已设置目标点: " + x + " " + y + " " + z);
                                                    }
                                                    return SINGLE_SUCCESS;
                                                })))));

    }
}
