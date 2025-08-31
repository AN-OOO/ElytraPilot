package an.anelytrapilot.path;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class ANPathfinder {
    // 防止无限循环的最大迭代次数
    private static final int MAX_ITERATIONS = 10000;
    // 移动成本：水平移动、跳跃、下落
    private static final double MOVE_COST = 1.0;
    private static final double JUMP_COST = 1.5;
    private static final double FALL_COST = 1.0;


    public static class Node {
        public BlockPos pos; // 方块位置
        public double gCost; // 从起点到当前节点的实际成本
        public double hCost; // 从当前节点到目标的估计成本
        public double fCost; // f = g + h
        public Node parent; // 父节点，用于回溯路径

        public Node(BlockPos pos) {
            this.pos = pos;
        }

        // 计算 fCost
        public void calculateFCost() {
            fCost = gCost + hCost;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return pos.equals(node.pos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode();
        }
    }

    public static List<BlockPos> findPath(World world, BlockPos start, BlockPos end) {
        // 初始化开放列表（优先队列，按 fCost 排序）和关闭列表
        PriorityQueue<Node> openList = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Set<Node> closedList = new HashSet<>();

        // 初始化起点节点
        Node startNode = new Node(start);
        startNode.gCost = 0;
        startNode.hCost = heuristic(start, end);
        startNode.calculateFCost();
        openList.add(startNode);

        int iterations = 0;
        while (!openList.isEmpty() && iterations < MAX_ITERATIONS) {
            Node current = openList.poll();
            if (current.pos.equals(end)) {
                return reconstructPath(current);
            }
            closedList.add(current);

            for (BlockPos neighborPos : getNeighbors(world, current.pos)) {
                if (closedList.stream().anyMatch(n -> n.pos.equals(neighborPos))) continue;

                // 计算到邻居的暂定 gCost
                double tentativeGCost = current.gCost + calculateMoveCost(current.pos, neighborPos);
                Node neighbor = openList.stream()
                        .filter(n -> n.pos.equals(neighborPos))
                        .findFirst()
                        .orElse(new Node(neighborPos));

                if (tentativeGCost < neighbor.gCost || !openList.contains(neighbor)) {
                    neighbor.parent = current;
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = heuristic(neighborPos, end);
                    neighbor.calculateFCost();

                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
            iterations++;
        }
        // 无路径可达
        return Collections.emptyList();
    }


    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY()) + Math.abs(a.getZ() - b.getZ());
    }


    private static List<BlockPos> getNeighbors(World world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();

        BlockPos up = pos.up();
        BlockPos jumpTarget = up;

        if (isSolidGround(world, pos.down())
                && isStandable(world, jumpTarget)
                && world.getBlockState(jumpTarget.up()).getCollisionShape(world, jumpTarget.up()).isEmpty()) { // 头部空间
            neighbors.add(jumpTarget);
        }

        int[][] offsets = {
                {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
                {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
                {1, -1, 0}, {-1, -1, 0}, {0, -1, 1}, {0, -1, -1}
        };

        for (int[] offset : offsets) {
            BlockPos neighbor = pos.add(offset[0], offset[1], offset[2]);

            if (isPositionValid(world, neighbor)) {
                neighbors.add(neighbor);
            }
        }

        return neighbors;
    }


    private static boolean isPositionValid(World world, BlockPos pos) {
        BlockPos ground = pos.down();
        BlockPos head = pos.up();

        boolean solidGround = !world.getBlockState(ground).getCollisionShape(world, ground).isEmpty();
        boolean spaceForBody = world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
        boolean spaceForHead = world.getBlockState(head).getCollisionShape(world, head).isEmpty();

        return solidGround && spaceForBody && spaceForHead;
    }


    private static List<BlockPos> reconstructPath(Node node) {
        List<BlockPos> path = new ArrayList<>();
        while (node != null) {

            path.add(node.pos.up());
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private static double calculateMoveCost(BlockPos from, BlockPos to) {
        if (from.getY() < to.getY()) return JUMP_COST;
        if (from.getY() > to.getY()) return FALL_COST;
        return MOVE_COST;
    }

    private static boolean isSolidGround(World world, BlockPos pos) {
        return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    private static boolean isStandable(World world, BlockPos pos) {
        BlockPos ground = pos.down();
        BlockPos head = pos.up();

        boolean solidGround = !world.getBlockState(ground).getCollisionShape(world, ground).isEmpty();
        boolean bodyEmpty = world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
        boolean headEmpty = world.getBlockState(head).getCollisionShape(world, head).isEmpty();

        return solidGround && bodyEmpty && headEmpty;
    }

}