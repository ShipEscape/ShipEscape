package com.example.bluetoothpositioningapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ShipMapView extends View {

    // 定义变量来存储位置信息
    private float userX = 0;
    private float userY = 0;
    private float userRadius = 20;

    // 船坞平面图位图对象
    private Bitmap floorMapBitmap;
    // 图片缩放适配矩阵
    private Matrix mapScaleMatrix;
    // 地图实际物理尺寸 单位米，和你之前信标坐标对应
    private final float MAP_WIDTH_METER = 10f;
    private final float MAP_HEIGHT_METER = 10f;
    // 屏幕像素和实际米的换算比例
    private float pixelPerMeter = 1f;

    // 【关键修复 1】这是 Java 代码直接 new 对象时用的构造函数
    public ShipMapView(Context context) {
        super(context);
        initMap();
    }

    // 【关键修复 2】这是 XML 布局文件加载时必须要用的构造函数！
    // 缺少这个就会导致你在截图里遇到的崩溃
    public ShipMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initMap();
    }

    // 统一初始化方法，加载平面图资源
    private void initMap(){
        // 加载你项目里的ship_floor_plan.png
        floorMapBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ship_floor_plan);
        mapScaleMatrix = new Matrix();
    }

    // 【关键方法】供 MainActivity 调用，用来更新红点位置，输入是米单位的物理坐标
    public void updateUserPosition(float x, float y, float radius) {
        // 把米单位的物理坐标转换成屏幕像素坐标
        this.userX = x * pixelPerMeter;
        this.userY = y * pixelPerMeter;
        this.userRadius = radius;

        // 告诉系统数据变了，需要重绘界面
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(floorMapBitmap == null) return;

        // 控件尺寸确定后，自动把平面图缩放到铺满整个控件，保持比例不变形
        float scaleX = (float) w / floorMapBitmap.getWidth();
        float scaleY = (float) h / floorMapBitmap.getHeight();
        float finalScale = Math.min(scaleX, scaleY);

        mapScaleMatrix.setScale(finalScale, finalScale);
        // 计算米到像素的换算比例
        pixelPerMeter = (float)w / MAP_WIDTH_METER;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 先绘制船坞平面图背景
        if(floorMapBitmap != null && !floorMapBitmap.isRecycled()){
            canvas.drawBitmap(floorMapBitmap, mapScaleMatrix, null);
        }

        // 1. 画背景网格 (可选，为了好看)
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1);

        // 防止 getWidth() 为 0 时报错
        if (getWidth() > 0 && getHeight() > 0) {
            for (int i = 0; i < getWidth(); i += 50) {
                canvas.drawLine(i, 0, i, getHeight(), gridPaint);
            }
            for (int i = 0; i < getHeight(); i += 50) {
                canvas.drawLine(0, i, getWidth(), i, gridPaint);
            }
        }

        // 2. 画用户位置 (红点)
        Paint userPaint = new Paint();
        userPaint.setColor(Color.RED);
        userPaint.setAntiAlias(true); // 抗锯齿，让圆更圆润

        // 画实心圆点
        canvas.drawCircle(userX, userY, userRadius, userPaint);

        // 画个空心圈表示范围
        userPaint.setStyle(Paint.Style.STROKE);
        userPaint.setStrokeWidth(2);
        canvas.drawCircle(userX, userY, userRadius + 10, userPaint);
    }
}
