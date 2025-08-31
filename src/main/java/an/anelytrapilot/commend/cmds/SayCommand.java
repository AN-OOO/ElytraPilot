package an.anelytrapilot.commend.cmds;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import an.anelytrapilot.commend.cmdmanage.Command;

import static an.anelytrapilot.util.MessageSend.sendMessage;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SayCommand extends Command {
    public SayCommand() {
        super("HHHHH");
    }

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(arg("message", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String message = StringArgumentType.getString(ctx, "message");
                    sendMessage("你好!我是AN-G: " + message);
                    return SINGLE_SUCCESS;
                }));
    }
}
