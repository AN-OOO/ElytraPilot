/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the MIT License.
 */

package an.anelytrapilot.commend;

import an.anelytrapilot.config.ANPilotConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;



public class ANPilotCommend {

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getGameDir().resolve("ANElytraPilot");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess, registrationEnvironment) ->{
            registerCommands(commandDispatcher);
        });
    }


    private static CompletableFuture<Suggestions> suggestAvailableFiles(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
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

    private static void registerCommands(CommandDispatcher<ServerCommandSource> commandDispatcher) {

        commandDispatcher.register(CommandManager.literal("AnElytraPilot")
                .then(CommandManager.literal("fileLoad")
                        .then(CommandManager.argument("file", StringArgumentType.word())
                                .suggests(ANPilotCommend::suggestAvailableFiles)
                                .executes(ctx -> {

                                    String fileName = StringArgumentType.getString(ctx, "file");
                                    ANSharedStates.file = CONFIG_DIR.resolve(fileName + ".txt");
                                    ANSharedStates.FileLoad = true;
                                    return 1;
                                })
                        ))

                .then(CommandManager.literal("setMode")
                        .then(CommandManager.literal("normal")
                                .executes(ctx -> {
                                    ANPilotConfig.setMode("overWorld");
                                    ctx.getSource().sendFeedback(() ->
                                                    Text.literal("[ANElytraPilot] ")
                                                            .formatted(Formatting.GREEN)
                                                            .append(Text.literal("已设置为普通模式").formatted(Formatting.LIGHT_PURPLE)),
                                            false
                                    );                                    return 1;
                                }))
                        .then(CommandManager.literal("end")
                                .executes(ctx -> {
                                    ANPilotConfig.setMode("end");
                                    ctx.getSource().sendFeedback(() ->
                                                    Text.literal("[ANElytraPilot] ")
                                                            .formatted(Formatting.GREEN)
                                                            .append(Text.literal("已设置为末地模式").formatted(Formatting.LIGHT_PURPLE)),
                                            false
                                    );                                    return 1;
                                })))

                .then(CommandManager.literal("setTarget")
                        .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                    ANPilotConfig.setTarget(x,y,z);
                                                    ANSharedStates.FlyTarget = new BlockPos(x, y, z);
                                                    ctx.getSource().sendFeedback(() ->
                                                                    Text.literal("[ANElytraPilot] ")
                                                                            .formatted(Formatting.GREEN)
                                                                            .append(Text.literal("已设置目标点:"+ x + " " + y + " " + z).formatted(Formatting.LIGHT_PURPLE)),
                                                            false
                                                    );
                                                    return 1;
                                                })))))

                .then(CommandManager.literal("fly")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            ServerWorld world = ctx.getSource().getWorld();

                            if (player == null) return 0;
                            //if (!world.getRegistryKey().equals(World.END)) {
                            //  ctx.getSource().sendFeedback(() -> Text.literal("❌ 只能在末地中使用此命令！"), false);
                            //return 1;
                            //}

                            ANSharedStates.FlyCommend = true;
                            ctx.getSource().sendFeedback(() ->
                                            Text.literal("[ANElytraPilot] ")
                                                    .formatted(Formatting.GREEN)
                                                    .append(Text.literal("已启动末地飞行导航").formatted(Formatting.LIGHT_PURPLE)),
                                    false
                            );                             return 1;
                        }))

                .then(CommandManager.literal("Stop")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            ServerWorld world = ctx.getSource().getWorld();

                            if (player == null) return 0;

                            ANSharedStates.FlyStop = true;

                            ctx.getSource().sendFeedback(() ->
                                            Text.literal("[ANElytraPilot] ")
                                                    .formatted(Formatting.GREEN)
                                                    .append(Text.literal("已停止导航").formatted(Formatting.LIGHT_PURPLE)),
                                    false
                            );                             return 1;
                        }))


        );

        commandDispatcher.register(CommandManager.literal("walkto")
                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                        .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                            int z = IntegerArgumentType.getInteger(ctx, "z");

                                            // 标记变量，在客户端触发行为
                                            ANSharedStates.shouldStartWalking = true;
                                            ANSharedStates.walkTarget = new BlockPos(x, y, z);

                                            ctx.getSource().sendFeedback(() ->
                                                            Text.literal("[ANElytraPilot] ")
                                                                    .formatted(Formatting.GREEN)
                                                                    .append(Text.literal("已设置寻路目标点，客户端正在执行").formatted(Formatting.LIGHT_PURPLE)),
                                                    false
                                            );                                             return 1;
                                        })))));
    }

}
