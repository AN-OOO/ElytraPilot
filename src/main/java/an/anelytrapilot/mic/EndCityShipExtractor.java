package an.anelytrapilot.mic;

import an.anelytrapilot.commend.ANSharedStates;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static an.anelytrapilot.util.MessageSend.sendMessage;

public class EndCityShipExtractor {

    private static final String EXPLORED_FILE_PATH = "ANElytraPilot/ANElytraFound.json";

    private static final List<BlockPos> shipCoordinates = new ArrayList<>();
    private static final Set<String> exploredCoordinates = new HashSet<>();
    private static int currentIndex = 0;
    private static boolean isLoaded = false;

    public static void loadShipCoordinates() {
        File file = ANSharedStates.file.toFile();

        shipCoordinates.clear();
        exploredCoordinates.clear();

        if (!file.exists()) {
            sendMessage("坐标文件不存在！");
            return;
        }

        // 加载已探索坐标
        loadExploredCoordinates();
        List<BlockPos> tempCoordinates = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean isDataSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("种子;结构;X;Z;详细信息")) {
                    isDataSection = true;
                    continue;
                }

                if (!isDataSection) {
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length != 5) {
                    continue;
                }

                String structure = parts[1];
                String details = parts[4];

                if (!structure.equals("end_city") || !details.contains(":ship")) {
                    continue;
                }

                try {
                    int x = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    tempCoordinates.add(new BlockPos(x, 140, z));
                } catch (NumberFormatException e) {
                    System.err.println("[AnelytraPilot] 无效坐标格式: " + line);
                }
            }

            // 对坐标排序（这里用你原来的 Z 带逻辑，也可以保留绝对值排序）
            int minZ = tempCoordinates.stream().mapToInt(BlockPos::getZ).min().orElse(0);
            int maxZ = tempCoordinates.stream().mapToInt(BlockPos::getZ).max().orElse(0);

            List<BlockPos> sorted = new ArrayList<>();
            boolean orderIsAsc = true;

            for (int zStart = (minZ / 1000) * 1000; zStart <= maxZ; zStart += 1000) {
                int zEnd = zStart + 999;
                final int currentZStart = zStart;
                final int currentZEnd = zEnd;

                List<BlockPos> band = tempCoordinates.stream()
                        .filter(pos -> pos.getZ() >= currentZStart && pos.getZ() <= currentZEnd)
                        .sorted(orderIsAsc
                                ? Comparator.comparingInt(BlockPos::getX)
                                : Comparator.comparingInt(BlockPos::getX).reversed()
                        )
                        .toList();

                sorted.addAll(band);
                orderIsAsc = !orderIsAsc;
            }

            // 如果想反转整个顺序，可保留这行
            Collections.reverse(sorted);

            shipCoordinates.addAll(sorted);
            isLoaded = true;
            sendMessage("加载了 " + shipCoordinates.size() + " 个末地城坐标");
        } catch (IOException e) {
            sendMessage("加载坐标文件失败: " + e.getMessage());
        }
    }

    /**
     * 保存单个坐标到文件，每行一个 JSON
     */
    public static void saveSingleExploredCoordinate(BlockPos pos) {
        if (pos == null) return;

        String coordKey = pos.getX() + "," + pos.getZ();
        if (exploredCoordinates.contains(coordKey)) {
            sendMessage("坐标已存在: " + coordKey);
            return;
        }

        exploredCoordinates.add(coordKey);

        // 直接追加写入单行 JSON
        File file = new File(EXPLORED_FILE_PATH);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file, true)) {
            Gson gson = new Gson();
            Coordinate coord = new Coordinate(pos.getX(), pos.getZ());
            writer.write(gson.toJson(coord));
            writer.write("\n");
        } catch (IOException e) {
            sendMessage("保存已探索坐标失败: " + e.getMessage());
        }

        sendMessage("保存坐标到已探索文件: " + coordKey);
    }

    /**
     * 从文件加载所有已探索坐标
     */
    private static void loadExploredCoordinates() {
        File file = new File(EXPLORED_FILE_PATH);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            Gson gson = new Gson();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                Coordinate coord = gson.fromJson(line, Coordinate.class);
                exploredCoordinates.add(coord.x + "," + coord.z);
            }
        } catch (IOException e) {
            sendMessage("加载已探索坐标失败: " + e.getMessage());
            resetExploredFile(file, "读取已探索坐标文件失败，重置为空");
        } catch (com.google.gson.JsonSyntaxException e) {
            sendMessage("已探索坐标文件格式无效: " + e.getMessage());
            resetExploredFile(file, "已探索坐标文件格式无效，重置为空");
        }
    }

    private static void resetExploredFile(File file, String message) {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            // 清空文件
        } catch (IOException e) {
            sendMessage("重置已探索坐标文件失败: " + e.getMessage());
        }
        sendMessage(message);
    }

    public static BlockPos getNextShipCoordinate() {
        if (!isLoaded || shipCoordinates.isEmpty()) {
            return null;
        }

        while (currentIndex < shipCoordinates.size()) {
            BlockPos pos = shipCoordinates.get(currentIndex);
            String coordKey = pos.getX() + "," + pos.getZ();
            currentIndex++;
            if (!exploredCoordinates.contains(coordKey)) {
                return pos;
            }
        }

        sendMessage("没有更多未探索的末地城坐标！");
        return null;
    }

    public static void resetIndex() {
        currentIndex = 0;
        sendMessage("已重置末地城坐标索引");
    }

    public static int getShipCount() {
        return shipCoordinates.size();
    }

    public static boolean isLoaded() {
        return isLoaded;
    }

    /**
     * 用于序列化的简单类
     */
    private static class Coordinate {
        int x;
        int z;

        Coordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }
    }
}
