/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the MIT License.
 */

package an.anelytrapilot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ANPilotConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 创建 .minecraft/ANElytraPilot/ 文件夹
    public static final Path CONFIG_DIR =
            FabricLoader.getInstance().getGameDir().resolve("ANElytraPilot");
    // 两个配置文件
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("ANElytraPilot.json");
    private static final Path ELYTRA_FOUND_FILE = CONFIG_DIR.resolve("ANElytraFound.json");


    private static String mode = "normal"; // 默认模式
    private static String customMessage = "";

    public static int targetX = 0;
    public static int targetY = 140;
    public static int targetZ = 0;

    // 设置飞行模式并保存
    public static void setMode(String newMode) {
        mode = newMode;
        save();
    }

    public static String getCustomMessage() {
        return customMessage;
    }

    public static void setCustomMessage(String message) {
        customMessage = message;
        save();
    }


    public static String getMode() {
        return mode;
    }

    // 加载配置
    public static void load() {

        if (CONFIG_FILE.toFile().exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE.toFile())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                if (json.has("mode")) mode = json.get("mode").getAsString();
                if (json.has("targetX")) targetX = json.get("targetX").getAsInt();
                if (json.has("targetY")) targetY = json.get("targetY").getAsInt();
                if (json.has("targetZ")) targetZ = json.get("targetZ").getAsInt();
                if (json.has("customMessage")) {
                    customMessage = json.get("customMessage").getAsString();
                }

            } catch (IOException e) {
                System.err.println("[ANElytraPilot] 配置读取失败: " + e.getMessage());
            }
        } else {
            System.out.println("[ANElytraPilot] 未找到配置文件，正在创建默认配置...");
            save(); // 创建默认配置文件
        }

        // 👉 创建 ElytraFound 文件（如果不存在）
        if (!ELYTRA_FOUND_FILE.toFile().exists()) {
            try {
                if (ELYTRA_FOUND_FILE.toFile().createNewFile()) {
                    System.out.println("[ANElytraPilot] 已创建 ElytraFound 文件: " + ELYTRA_FOUND_FILE);
                    try (FileWriter writer = new FileWriter(ELYTRA_FOUND_FILE.toFile())) {
                        writer.write("{}");
                    }
                }
            } catch (IOException e) {
                System.err.println("[ANElytraPilot] 无法创建 ElytraFound 文件: " + e.getMessage());
            }
        }
    }

    // 保存配置
    public static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("mode", mode);
        json.addProperty("targetX", targetX);
        json.addProperty("targetY", targetY);
        json.addProperty("targetZ", targetZ);
        json.addProperty("Log原因", customMessage);

        try {
            try {
                Files.createDirectories(CONFIG_DIR);
                System.out.println("文件夹已创建: " + CONFIG_DIR);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {

        }
    }

    // 获取导航坐标
    public static BlockPos getTargetPos() {
        return new BlockPos(targetX, targetY, targetZ);
    }

    public static void setTarget(int x, int y, int z) {
        targetX = x;
        targetY = y;
        targetZ = z;
        save();
    }

    // 访问配置文件路径（可用于加载/导出等）
    public static File getConfigFile() {
        return CONFIG_FILE.toFile();
    }

    public static File getElytraFoundFile() {
        return ELYTRA_FOUND_FILE.toFile();
    }

}
