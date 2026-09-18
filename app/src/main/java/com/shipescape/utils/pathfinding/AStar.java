package com.shipescape.utils.pathfinding;

import java.util.Arrays;
import java.util.PriorityQueue;
/*
调用方法：
int[] findPath(NavGraph graph, int startIdx, int goalIdx)
return path[]数组,数组存放从起到到终点的结果的路径的索引
如[0,258,516,517,774]#257*135,
即[(0,0),(1,1),(2,2),(3,2),(3,3)]
遍历数组即可得到路径
*/

public final class AStar {

    /*不可达的统一返回值。零长数组，调用方无法通过它拿到任何数*/
    private static final int[] EMPTY_PATH = new int[0];

    private AStar() {
        // 工具类，禁止实例化
    }


    public static int[] findPath(NavGraph graph, int startIdx, int goalIdx) {
        if (graph == null) {
            throw new IllegalArgumentException("graph 不能为 null");
        }



        //越界。必须同时检查上界：只查 < 0 会让 idx == cellCount() 漏过去，
        //最终在 gScore[cellCount()] 处抛 ArrayIndexOutOfBoundsException。
        final int n = graph.cellCount();
        if (startIdx < 0 || startIdx >= n || goalIdx < 0 || goalIdx >= n) return EMPTY_PATH;

        //不可通行。必须在「起点等于终点」之前判断，否则终点是墙时
        //会返回一个长度为 1 的「路径」，让调用方误以为找到了出口。
        if (!graph.isWalkableAt(startIdx) || !graph.isWalkableAt(goalIdx)) return EMPTY_PATH;

        // 3) 起点即终点。
        if (startIdx == goalIdx) return new int[]{startIdx};

        return search(graph, startIdx, goalIdx);
    }


    private static int[] search(NavGraph graph, int startIdx, int goalIdx) {
        final int n = graph.cellCount();

        final float[] gScore = new float[n];
        // 用 POSITIVE_INFINITY 而不是 Float.MAX_VALUE：Float.MAX_VALUE + 1.0f 因浮点舍入
        // 结果还是 Float.MAX_VALUE，会把「从没到达过的节点做松弛」这类 bug 伪装成一次无操作。
        Arrays.fill(gScore, Float.POSITIVE_INFINITY);

        final int[] cameFrom = new int[n];
        Arrays.fill(cameFrom, -1);

        final boolean[] closed = new boolean[n];

        final int goalX = graph.xOf(goalIdx);
        final int goalY = graph.yOf(goalIdx);

        final PriorityQueue<Entry> open = new PriorityQueue<>();

        final float startH = heuristic(graph.xOf(startIdx), graph.yOf(startIdx), goalX, goalY);
        gScore[startIdx] = 0.0f;
        open.add(Entry.of(startIdx, 0.0f, startH));

        while (!open.isEmpty()) {
            final Entry current = open.poll();
            final int cur = current.index;


            if (closed[cur]) continue;
            if (cur == goalIdx) return reconstruct(cameFrom, goalIdx);
            closed[cur] = true;

            final float gCur = gScore[cur];

            for (int dir = 0; dir < NavGraph.DIRECTION_COUNT; dir++) {
                final int next = graph.neighbor(cur, dir);
                if (next < 0 || closed[next]) continue;


                final float step = NavGraph.stepCost(dir)
                        * 0.5f * (graph.costAt(cur) + graph.costAt(next));
                final float tentative = gCur + step;

                if (tentative < gScore[next]) {
                    gScore[next] = tentative;
                    cameFrom[next] = cur;
                    final float h = heuristic(graph.xOf(next), graph.yOf(next), goalX, goalY);
                    open.add(Entry.of(next, tentative, h));
                }
            }
        }

        return EMPTY_PATH;   // 开集空了仍未抵达终点 → 不连通
    }

    /*
     octile 距离：8 邻域栅格上无障碍时的精确最短距离。
     <p>因为它是精确值，在无障碍区域里最优路径上每个节点的 {@code f} 都相等，
     所以 {@link Entry} 的 tie-break 不是可有可无的优化。
     */
    private static float heuristic(int x, int y, int goalX, int goalY) {
        final int dx = Math.abs(x - goalX);
        final int dy = Math.abs(y - goalY);
        final int min = Math.min(dx, dy);
        final int max = Math.max(dx, dy);
        return (max - min) + NavGraph.DIAGONAL_COST * min;
    }

    /*
     沿camefrom回溯出路径。先数长度再分配正好的数组，
     保证返回的首元素是起点、末元素是终点
     */
    private static int[] reconstruct(int[] cameFrom, int goalIdx) {
        int length = 0;
        for (int i = goalIdx; i != -1; i = cameFrom[i]) length++;

        final int[] path = new int[length];
        int node = goalIdx;
        for (int k = length - 1; k >= 0; k--) {
            path[k] = node;
            node = cameFrom[node];
        }
        return path;
    }

    /**
     * 开集条目。每次松弛都会 new 一个，所以刻意用普通类而不是 {@code record}——
     * 后者自动生成的 {@code equals/hashCode/toString} 在这里纯属浪费。
     *
     * <p>{@code compareTo} 在 {@code f} 与 {@code h} 都相等时返回 0，这是安全的，
     * <b>但前提是开集用堆实现</b>（{@link PriorityQueue} 允许重复元素）。
     * 若将来把开集换成 {@code TreeSet} 之类基于比较去重的容器，
     * 相等条目会被静默吞掉，搜索结果随之出错。
     */
    private static final class Entry implements Comparable<Entry> {
        final int index;
        final float f;
        final float h;

        /** 用 {@code g} 和 {@code h} 构造，{@code f} 由二者相加得出，避免调用方漏算。 */
        static Entry of(int index, float g, float h) {
            return new Entry(index, g + h, h);
        }

        private Entry(int index, float f, float h) {
            this.index = index;
            this.f = f;
            this.h = h;
        }

        @Override
        public int compareTo(Entry other) {
            final int byF = Float.compare(f, other.f);
            if (byF != 0) return byF;
            // f 相同时优先 h 更小的（更接近终点）：扩展的节点更少，路径也更直。
            return Float.compare(h, other.h);
        }
    }
}
