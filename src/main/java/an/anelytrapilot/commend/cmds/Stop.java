package an.anelytrapilot.commend.cmds;

import an.anelytrapilot.commend.ANSharedStates;
import an.anelytrapilot.commend.cmdmanage.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

import static an.anelytrapilot.util.MessageSend.sendMessage;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Stop extends Command {
    public Stop() {
        super("ANElytraPilot");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("Stop")
                .executes(ctx -> {
                    ANSharedStates.FlyStop = true;
                    sendMessage("->停止导航");
                    return SINGLE_SUCCESS;
                }));

    }
}
