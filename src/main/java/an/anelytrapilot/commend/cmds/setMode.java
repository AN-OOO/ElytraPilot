package an.anelytrapilot.commend.cmds;

import an.anelytrapilot.commend.cmdmanage.Command;
import an.anelytrapilot.config.ANPilotConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;

import static an.anelytrapilot.util.MessageSend.sendMessage;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class setMode extends Command {
    public setMode() {
        super("ANElytraPilot");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("SetMode")
                .then(literal("OverWorld")
                        .executes(ctx -> {
                            ANPilotConfig.setMode("overWorld");
                            sendMessage("已设置为主世界导航模式");
                            return SINGLE_SUCCESS;
                        }))
                .then(literal("End")
                        .executes(ctx -> {
                            ANPilotConfig.setMode("end");
                            sendMessage("已设置为末地导航模式");
                            return SINGLE_SUCCESS;
                        })));

    }
}
