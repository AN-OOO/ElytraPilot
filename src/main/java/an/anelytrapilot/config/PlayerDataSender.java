/*
 * Copyright (C) 2025 AN-G
 * All rights reserved.
 * This code is part of the AnelytraPilot mod for Minecraft.
 * Redistribution and modification are permitted under the MIT License.
 */

package an.anelytrapilot.config;

import an.anelytrapilot.commend.ANSharedStates;
import an.anelytrapilot.util.MessageSend;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Enumeration;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJson;

public class PlayerDataSender  {

    private boolean hasSentData = false; // 防止重复发送
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static void sendPlayerData(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) {

            return;
        }

        String username = player.getName().getString();
        String uuid = player.getUuid().toString();
        String mac = getMacAddress();

        sendUserInfo(username, uuid, mac);
    }

    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }

    private static void sendUserInfo(String username, String uuid, String mac) {
        try {
            // 拼接 JSON 字符串
            String json = String.format("""
            {
                "playerName": "%s",
                "uuid": "%s",
                "macAddress": "%s"
            }
            """, escapeJson(username), escapeJson(uuid), escapeJson(mac));

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://101.32.73.57:8080/api/user/add"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {

                        if (response.statusCode() == 200) {
                            String body = response.body();

                            // 简单提取 success 和 message
                            boolean success = body.contains("\"success\":true");
                            String message = extractMessage(body);

                            if (success) {
                                MessageSend.sendMessage(message);
                                // 在这里执行后续逻辑，比如启动你的 Elytra 模块
                            } else {
                                MessageSend.sendMessage(message);

                                new Thread(() -> {
                                    try {
                                        Thread.sleep(5000); // 延迟 150 毫秒，大概 2 tick
                                    } catch (InterruptedException ignored) {
                                    }
                                   mc.world.disconnect();
                                }).start();
                                // 在这里可以禁用功能，或者提醒用户
                            }
                        } else {
                            MessageSend.sendMessage("数据异常！");
                        }
                    })
                    .exceptionally(throwable -> {

                        return null;
                    });

        } catch (Exception e) {
            System.err.println("构建请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String extractMessage(String json) {
        // 简易提取 message 字段
        int index = json.indexOf("\"message\":\"");
        if (index != -1) {
            int start = index + "\"message\":\"".length();
            int end = json.indexOf("\"", start);
            if (end != -1) {
                return json.substring(start, end);
            }
        }
        return "";
    }

}