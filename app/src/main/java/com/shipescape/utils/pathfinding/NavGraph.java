package com.shipescape.utils.pathfinding;

import java.util.Arrays;

/*
public NavGraph(int width, int height) 初始化构建一个width*height的栅格图（如257*135）
通过boolen []walkable判断是否可通行（障碍物）#初始均为true
位置索引idx=y*width+x
支持的数据编辑方法
void setWalkableAt(int x, int y, boolean value)#单个像素格的可达性
public void fillFromMask(boolean[] mask)#支持同类型的boolen[]数组导入
public float setCostAt(int idx, float value)#某点的代价编辑
public void fillCosts(float[] costs)#支持已有的同类型的float[]数组导入
float cost[]代表每一格的代价（如火灾源可设为inf）
支持斜向行走（步长为DIAGONAL_COST = 1.41421356f
int indexOf(int x, int y)#根据x,y给出索引idx
int xOf(int idx)#给出x坐标
int yOf(int idx)#给出y坐标

*/



/*
后续计划：
1，增加障碍物的快速编辑
2.新增根据火源的cost快速修改
 */

public final class NavGraph {

    // 邻居方向数（8 邻域）
    public static final int DIRECTION_COUNT = 8;

    //斜向移动的代价倍率
    public static final float DIAGONAL_COST = 1.41421356f;

    //代价下限
    private static final float MIN_COST = 1.0f;

    //8 个方向：前4个正交，后4个斜向
    private static final int[] DX = {1, 0, -1, 0, 1, 1, -1, -1};
    private static final int[] DY = {0, 1, 0, -1, 1, -1, -1, 1};
    private static final float[] STEP_COST = {
            1.0f, 1.0f, 1.0f, 1.0f,
            DIAGONAL_COST, DIAGONAL_COST, DIAGONAL_COST, DIAGONAL_COST
    };

    private final int width;
    private final int height;
    private final boolean[] walkable;
    private final float[] cost;

//建一张全部可通行、代价均为 1.0 的空图。
//@throws IllegalArgumentException 宽或高不是正数
    public NavGraph(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "NavGraph 尺寸必须为正数，收到 " + width + " x " + height);
        }
        this.width = width;
        this.height = height;
        this.walkable = new boolean[width * height];
        this.cost = new float[width * height];
        Arrays.fill(this.walkable, true);
        Arrays.fill(this.cost, 1.0f);
    }


    //格子总数
    public int cellCount() {
        return walkable.length;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }


    public int indexOf(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return -1;
        return y * width + x;
    }

    public int xOf(int idx) {
        return idx % width;
    }

    public int yOf(int idx) {
        return idx / width;
    }


    public int nearestIndex(int x, int y) {
        final int cx = x < 0 ? 0 : Math.min(x, width - 1);
        final int cy = y < 0 ? 0 : Math.min(y, height - 1);
        return cy * width + cx;
    }


    /*该索引是否在范围内且可通行*/
    public boolean isWalkableAt(int idx) {
        return idx >= 0 && idx < walkable.length && walkable[idx];
    }

    /*该坐标是否在范围内且可通行*/
    public boolean isWalkableAt(int x, int y) {
        return isWalkableAt(indexOf(x, y));
    }

    /*设置某格的通行性；坐标越界时静默忽略*/
    public void setWalkableAt(int x, int y, boolean value) {
        final int idx = indexOf(x, y);
        if (idx >= 0) walkable[idx] = value;
    }

/*
支持同类型的boolen矩阵导入,要求必须长度一致
 */
    public void fillFromMask(boolean[] mask) {
        requireSameLength(mask.length, "mask");
        System.arraycopy(mask, 0, walkable, 0, walkable.length);
    }

    // ------------------------------------------------------------------ 代价

    /* 该格的代价倍率；索引越界返回1.0*/
    public float costAt(int idx) {
        return (idx >= 0 && idx < cost.length) ? cost[idx] : 1.0f;
    }

/*
设置单个点的代价
 */
    public float setCostAt(int idx, float value) {
        if (idx < 0 || idx >= cost.length) return MIN_COST;
        final float clamped = Float.isNaN(value) ? MIN_COST : Math.max(value, MIN_COST);
        cost[idx] = clamped;
        return clamped;
    }

    /*
     从数组整体载入代价（给将来的危险区用）
     @throws IllegalArgumentException 长度不匹配
     */
    public void fillCosts(float[] costs) {
        requireSameLength(costs.length, "costs");
        for (int i = 0; i < cost.length; i++) {
            setCostAt(i, costs[i]);
        }
    }


    /*向某方向的移动代价倍率（正交 1.0，斜向 √2）；方向非法时返回 1.0*/
    public static float stepCost(int dir) {
        return (dir >= 0 && dir < DIRECTION_COUNT) ? STEP_COST[dir] : 1.0f;
    }

/*
判断邻居是否可达，防止出现穿墙现象或出现越界现象
 */
    public int neighbor(int idx, int dir) {
        if (dir < 0 || dir >= DIRECTION_COUNT) return -1;

        final int x = idx % width;
        final int y = idx / width;
        final int nx = x + DX[dir];
        final int ny = y + DY[dir];

        if (nx < 0 || nx >= width || ny < 0 || ny >= height) return -1;

        final int nIdx = ny * width + nx;
        if (!walkable[nIdx]) return -1;

        // 斜向：两侧正交邻居都必须是通的，挡住「从墙角缝隙钻过去」
        if (DX[dir] != 0 && DY[dir] != 0) {
            if (!walkable[y * width + nx] || !walkable[ny * width + x]) return -1;
        }
        return nIdx;
    }


    private void requireSameLength(int actual, String name) {
        if (actual != walkable.length) {
            throw new IllegalArgumentException(
                    name + " 长度必须为 " + walkable.length + "，收到 " + actual);
        }
    }
}
