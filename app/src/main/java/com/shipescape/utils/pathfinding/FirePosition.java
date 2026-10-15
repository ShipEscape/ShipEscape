package com.shipescape.utils.pathfinding;

import java.util.Arrays;

//使用手写二叉堆的方式实现优先队列，而不是PriorityQueue<Entry>，防止占用内存过大
//调用方法    public void applyTo(NavGraph graph, float intensity)
//实现效果：修改原graph中的cost数组，使其符合火灾通行情况
//需调参数：BASE_RADIUS_CELLS、PEAK_EXTRA_COST，后续根据实际情况进行调整


public class FirePosition {
    /**
     *
     */


    private static final float BASE_RADIUS_CELLS = 120f;
    /**
     * 火灾中心处的代价
     */


    private static final float PEAK_EXTRA_COST = 10_000f;

    /**
     * 堆的初始容量，按需翻倍。
     *
     */
    private static final int INITIAL_HEAP_CAPACITY = 1024;


    private static final int[] DX = {1, 0, -1, 0, 1, 1, -1, -1};
    private static final int[] DY = {0, 1, 0, -1, 1, -1, -1, 1};


    private final int x;
    private final int y;

    public FirePosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "FirePosition(" + x + ", " + y + ")";
    }

    /**
     * 判断 (x0,y0) 到 (x1,y1) 的直线是否畅通
     */
    private static boolean lineOfSight(NavGraph g, int x0, int y0, int x1, int y1) {
        final int dx = Math.abs(x1 - x0);
        final int dy = Math.abs(y1 - y0);
        final int sx = x0 < x1 ? 1 : -1;
        final int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int cx = x0;
        int cy = y0;

        while (cx != x1 || cy != y1) {
            final int e2 = 2 * err;
            int stepX = 0;
            int stepY = 0;
            if (e2 > -dy) {
                err -= dy;
                stepX = sx;
            }
            if (e2 < dx) {
                err += dx;
                stepY = sy;
            }

            if (stepX != 0 && stepY != 0) {
                if (!g.isWalkableAt(cx + stepX, cy) || !g.isWalkableAt(cx, cy + stepY)) {
                    return false;
                }
            }
            cx += stepX;
            cy += stepY;
            if (!g.isWalkableAt(cx, cy)) return false;
        }
        return true;
    }

    /**
     * 实现效果：修改原graph中的cost数组，使其符合火灾通行情况
     *
     * @param graph     传入的原图
     * @param intensity 火灾的烈度 代表火灾影响距离为 sqrt(intensity)*BASE_RADIUS_CELLS
     */
    public void applyTo(NavGraph graph, float intensity) {
        if (graph == null) {
            throw new IllegalArgumentException("graph 不能为 null");
        }
        if (intensity <= 0) {
            return; // 没有火，不动图
        }

        final int width = graph.getWidth();
        final int height = graph.getHeight();

        final int startIdx = graph.indexOf(x, y);
        if (startIdx < 0) {
            return; // 火心落在图外，没有任何影响
        }

        //    烈度 → 影响半径。
        //    热辐射点源模型：q = Q / (4πd²)，达到同一伤害阈值所需的距离 d ∝ √Q。
        //    所以半径随烈度的平方根增长 —— 烈度翻 4 倍，范围才翻 1 倍。
        final float radius = BASE_RADIUS_CELLS * (float) Math.sqrt(intensity);

        //    只在外接矩形内工作。
        //    因为「沿路距离 >= 直线距离」恒成立，所以沿路距离小于 radius 的格子
        //    必然也落在以火心为心、radius 为半径的矩形内。
        //    把工作区局部化，小火灾就不必为整张 222 万格的图分配数组
        final int r = (int) Math.ceil(radius);
        final int minX = Math.max(0, x - r);
        final int maxX = Math.min(width - 1, x + r);
        final int minY = Math.max(0, y - r);
        final int maxY = Math.min(height - 1, y + r);
        final int bw = maxX - minX + 1;      // 工作区宽（列数）

        // ③ 有界 Dijkstra 的工作数组。索引一律用「工作区局部索引」：
        //       localIdx = (globalY - minY) * bw + (globalX - minX)
        final float[] dist = new float[bw * (maxY - minY + 1)];
        Arrays.fill(dist, Float.POSITIVE_INFINITY);
        final boolean[] settled = new boolean[dist.length];

        final int[] parent = new int[dist.length];

        final MinHeap open = new MinHeap(INITIAL_HEAP_CAPACITY);

        final int startLocal = (y - minY) * bw + (x - minX);
        dist[startLocal] = 0f;
        open.push(startLocal, 0f);

        while (!open.isEmpty()) {
            final int cur = open.pop();
            if (settled[cur]) continue;
            settled[cur] = true;

            final float d = dist[cur];
            final int curX = minX + (cur % bw);
            final int curY = minY + (cur / bw);

            final float hazard = hazardAt(d, radius);
            final int globalIdx = curY * width + curX;
            if (hazard > graph.costAt(globalIdx)) {
                graph.setCostAt(globalIdx, hazard);
            }

            if (d >= radius) continue;

            final int p = parent[cur];
            final int pX = minX + (p % bw);
            final int pY = minY + (p / bw);

            for (int dir = 0; dir < 8; dir++) {
                final int nx = curX + DX[dir];
                final int ny = curY + DY[dir];
                if (nx < minX || nx > maxX || ny < minY || ny > maxY) continue;

                final int nLocal = (ny - minY) * bw + (nx - minX);
                if (settled[nLocal]) continue;
                if (!graph.isWalkableAt(nx, ny)) continue;

                if (DX[dir] != 0 && DY[dir] != 0) {
                    if (!graph.isWalkableAt(nx, curY) || !graph.isWalkableAt(curX, ny)) continue;
                }

                float nd;
                int cand;
                // Theta*：父节点能直接看到邻居，就跳过 cur，用父节点到邻居的欧氏距离
                if (p != cur && lineOfSight(graph, pX, pY, nx, ny)) {
                    final float ddx = nx - pX;
                    final float ddy = ny - pY;
                    nd = dist[p] + (float) Math.sqrt(ddx * ddx + ddy * ddy);
                    cand = p;
                } else {
                    nd = d + NavGraph.stepCost(dir);
                    cand = cur;
                }

                if (nd < dist[nLocal]) {
                    dist[nLocal] = nd;
                    parent[nLocal] = cand;
                    open.push(nLocal, nd);
                }
            }
        }
    }


    private static float hazardAt(float distance, float radius) {
        if (distance >= radius) return 1f;
        final float t = distance / radius;
        final float falloff = (1f - t) * (1f - t);
        return 1f + PEAK_EXTRA_COST * falloff;
    }


    private static final class MinHeap {
        private int[] cells;
        private float[] keys;
        private int size;

        MinHeap(int capacity) {
            cells = new int[capacity];
            keys = new float[capacity];
        }

        boolean isEmpty() {
            return size == 0;
        }

        void push(int cell, float key) {
            if (size == cells.length) {
                final int capacity = cells.length * 2;
                cells = Arrays.copyOf(cells, capacity);
                keys = Arrays.copyOf(keys, capacity);
            }
            int i = size++;
            cells[i] = cell;
            keys[i] = key;

            // 上浮
            while (i > 0) {
                final int parent = (i - 1) >>> 1;
                if (keys[parent] <= keys[i]) break;
                swap(i, parent);
                i = parent;
            }
        }

        /**
         * 调用前必须确保堆非空。
         */
        int pop() {
            final int top = cells[0];
            final int last = --size;
            cells[0] = cells[last];
            keys[0] = keys[last];

            // 下沉
            int i = 0;
            while (true) {
                final int left = 2 * i + 1;
                if (left >= size) break;
                final int right = left + 1;
                final int smaller = (right < size && keys[right] < keys[left]) ? right : left;
                if (keys[i] <= keys[smaller]) break;
                swap(i, smaller);
                i = smaller;
            }
            return top;
        }

        private void swap(int a, int b) {
            final int tc = cells[a];
            cells[a] = cells[b];
            cells[b] = tc;
            final float tk = keys[a];
            keys[a] = keys[b];
            keys[b] = tk;
        }
    }
}
