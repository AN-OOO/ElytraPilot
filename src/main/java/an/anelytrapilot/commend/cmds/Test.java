package an.anelytrapilot.commend.cmds;

import an.anelytrapilot.commend.ANSharedStates;
import an.anelytrapilot.commend.cmdmanage.Command;
import an.anelytrapilot.util.MessageSend;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static an.anelytrapilot.config.ANPilotConfig.CONFIG_DIR;
import static an.anelytrapilot.util.MessageSend.sendMessage;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class Test extends Command {
    public Test() {
        super("ANElytraPilot");
    }

    public static boolean FileState = false;

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("test")
                .executes(ctx -> {
                    ANSharedStates.TestCommend = true;
                    sendMessage("->测试功能");
                    return SINGLE_SUCCESS;
                }));

    }

}
