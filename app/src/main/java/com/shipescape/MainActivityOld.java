//package com.shipescape;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothDevice;
//import android.bluetooth.BluetoothManager;
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import java.util.ArrayList;
//import java.util.List;
//
////fhniubi
//public class MainActivityOld extends AppCompatActivity {
//
//    private static final int REQUEST_PERMISSION_CODE = 100;
//    private BluetoothAdapter bluetoothAdapter;
//    private Handler mainHandler;
//    private boolean isScanning = false;
//
//    // 替换成你实际的信标MAC地址
//    private final String[] TARGET_MACS = {
//            "52:0a:25:12:03:21",
//            "52:0a:25:12:03:17",
//            "4c:dd:02:00:00:01"
//    };
//
//    // 信标物理坐标 单位米 lell
//    private final double[][] BEACON_POSITIONS = {
//            {0, 0},
//            {10, 0},
//            {0, 10}
//    };
//
//    private final int TX_POWER = -59;
//    private final double[] distances = {-1, -1, -1};
//
//    // 安卓7.0原生兼容BLE扫描回调
//    private final BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
//        @Override
//        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            String address = device.getAddress();
//            if (address == null) return;
//
//            // 扫到任何设备立刻弹短Toast，你手机上直接就能看到
//            runOnUiThread(() -> Toast.makeText(MainActivityOld.this, "扫到:" + address + " RSSI:" + rssi, Toast.LENGTH_SHORT).show());
//
//            int tempIndex = -1;
//            for (int i = 0; i < TARGET_MACS.length; i++) {
//                if (TARGET_MACS[i].equalsIgnoreCase(address)) {
//                    tempIndex = i;
//                    break;
//                }
//            }
//            final int matchIndex = tempIndex;
//
//            if (matchIndex != -1) {
//                double dist = calculateDistance(rssi, TX_POWER);
//                distances[matchIndex] = dist;
//                runOnUiThread(() -> Toast.makeText(MainActivityOld.this, "匹配信标" + matchIndex + " 距:" + String.format("%.1f", dist) + "m", Toast.LENGTH_SHORT).show());
//                calculatePosition();
//            }
//        }
//    };
//
//    @SuppressLint("MissingPermission")
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // 初始化全局Handler，之前漏写这行会空指针
//        mainHandler = new Handler(Looper.getMainLooper());
//
//        // 先初始化蓝牙管理器，绝对不能省这一步
//        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        bluetoothAdapter = bluetoothManager.getAdapter();
//
//        // 空判断，蓝牙适配器为空直接弹提示返回，绝对不闪退
//        if (bluetoothAdapter == null) {
//            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        // 现在再判断蓝牙是否开启，就不会空指针了
//        if (!bluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
//                startActivity(enableBtIntent);
//            }
//            Toast.makeText(this, "3. 请打开蓝牙", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "3. 蓝牙已开启", Toast.LENGTH_SHORT).show();
//        }
//
//        checkPermissionsAndStartScan();
//    }
//
//    private void checkPermissionsAndStartScan() {
//        List<String> permissions = new ArrayList<>();
//
//        // 安卓7.0仅需要精确定位权限
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
//        }
//
//        // 仅安卓12+申请新蓝牙权限
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
//            }
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
//            }
//        }
//
//        if (!permissions.isEmpty()) {
//            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_PERMISSION_CODE);
//        } else {
//            startLoopScan();
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_PERMISSION_CODE) {
//            boolean allGranted = true;
//            for (int result : grantResults) {
//                if (result != PackageManager.PERMISSION_GRANTED) {
//                    allGranted = false;
//                    break;
//                }
//            }
//            if (allGranted) {
//                Toast.makeText(this, "4. 权限全部通过", Toast.LENGTH_SHORT).show();
//                startLoopScan();
//            } else {
//                Toast.makeText(this, "4. 权限被拒绝", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    @SuppressLint("MissingPermission")
//    private void startLoopScan() {
//        if (!isScanning) {
//            // 先判蓝牙适配器不为空，再执行扫描
//            if (bluetoothAdapter != null) {
//                bluetoothAdapter.startLeScan(leScanCallback);
//                isScanning = true;
//            } else {
//                Toast.makeText(this, "蓝牙适配器未就绪", Toast.LENGTH_SHORT).show();
//            }
//        }
//
//        Toast.makeText(this, "5. 蓝牙扫描已启动", Toast.LENGTH_SHORT).show();
//
//        // 每10秒自动重启扫描，安卓7.0后台不会断
//        mainHandler.postDelayed(() -> {
//            bluetoothAdapter.stopLeScan(leScanCallback);
//            isScanning = false;
//            startLoopScan();
//        }, 10000);
//    }
//
//    // 三边定位计算
//    private void calculatePosition() {
//        if (distances[0] < 0 || distances[1] < 0 || distances[2] < 0) {
//            int validCount = 0;
//            for (double d : distances) {
//                if (d >= 0) validCount++;
//            }
//            Toast.makeText(this, "等待3个信标，当前有效:" + validCount, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        double d1 = distances[0];
//        double d2 = distances[1];
//        double d3 = distances[2];
//
//        double x1 = BEACON_POSITIONS[0][0], y1 = BEACON_POSITIONS[0][1];
//        double x2 = BEACON_POSITIONS[1][0], y2 = BEACON_POSITIONS[1][1];
//        double x3 = BEACON_POSITIONS[2][0], y3 = BEACON_POSITIONS[2][1];
//
//        double A = 2 * (x2 - x1);
//        double B = 2 * (y2 - y1);
//        double C = d1 * d1 - d2 * d2 - x1 * x1 + x2 * x2 - y1 * y1 + y2 * y2;
//
//        double D = 2 * (x3 - x2);
//        double E = 2 * (y3 - y2);
//        double F = d2 * d2 - d3 * d3 - x2 * x2 + x3 * x3 - y2 * y2 + y3 * y3;
//
//        double x = (C * E - F * B) / (E * A - B * D);
//        double y = (C * D - A * F) / (B * D - A * E);
//
//        runOnUiThread(() -> Toast.makeText(MainActivityOld.this, "✅ 定位成功 X:" + String.format("%.1f", x) + " Y:" + String.format("%.1f", y), Toast.LENGTH_LONG).show());
//        // 定位成功后更新地图红点
//        ShipMapView mapView = findViewById(R.id.shipMapView);
//        mapView.updateUserPosition((float) x, (float) y, 20);
//
//    }
//
//    // RSSI转距离公式
//    private double calculateDistance(int rssi, int txPower) {
//        if (rssi == 0) return -1;
//        double ratio = (txPower * 1.0 - rssi) / (10 * 2.0);
//        return Math.pow(10, ratio);
//    }
//
//    @SuppressLint("MissingPermission")
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (bluetoothAdapter != null) {
//            bluetoothAdapter.stopLeScan(leScanCallback);
//        }
//        if (mainHandler != null) {
//            mainHandler.removeCallbacksAndMessages(null);
//        }
//    }
//}
