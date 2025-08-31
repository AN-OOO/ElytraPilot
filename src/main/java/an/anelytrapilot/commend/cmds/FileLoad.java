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
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class FileLoad extends Command {
    public FileLoad() {
        super("ANElytraPilot");
    }

    public static boolean FileState = false;

    @Override
    public void executeBuild(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("File")
                .then(arg("File", StringArgumentType.string())
                        .suggests(FileLoad::suggestAvailableFiles)
                        .executes(ctx -> {
                            String fileName = StringArgumentType.getString(ctx, "File");
                            ANSharedStates.file = CONFIG_DIR.resolve(fileName + ".txt");
                            MessageSend.sendMessage(ANSharedStates.file.toString());
                            ANSharedStates.FileLoad = true;
                            FileState = true;
                            return SINGLE_SUCCESS;
                        }))
        );
    }

    private static CompletableFuture<Suggestions> suggestAvailableFiles(
            CommandContext<CommandSource> context,
            SuggestionsBuilder builder
    ) {
        if (!Files.exists(CONFIG_DIR)) return builder.buildFuture();

        try (Stream<Path> paths = Files.list(CONFIG_DIR)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".txt"))
                    .map(p -> p.getFileName().toString().replace(".txt", ""))
                    .forEach(builder::suggest);
        } catch (IOException ignored) {
        }

        return builder.buildFuture();
    }
}
