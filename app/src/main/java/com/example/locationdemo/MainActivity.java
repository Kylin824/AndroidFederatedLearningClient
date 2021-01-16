package com.example.locationdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import cn.speedtest.speedtest_sdk.SpeedtestInterface;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements TencentLocationListener {

    private TextView gpsText;
    private TencentLocationManager mLocationManager;
    private static final String TAG = "MainActivity";
    private Button nouiBtn;
    private Button uiBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gpsText = findViewById(R.id.gpsText);

        requirePermission();

        mLocationManager = TencentLocationManager.getInstance(this);
        // 连续定位
//        TencentLocationRequest locationRequest = TencentLocationRequest.create()
//                .setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_POI)
//                .setInterval(10000) // 定位周期(位置监听器回调周期), 单位为ms(毫秒)
//                .setAllowGPS(true); //允许使用GPS
//        mLocationManager.requestLocationUpdates(locationRequest, this);

        // 单次定位
        mLocationManager.requestSingleFreshLocation(null, this, Looper.getMainLooper());


    }

    @AfterPermissionGranted(1)
    private void requirePermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        String[] permissionsForQ = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION, //target为Q时，动态请求后台定位权限
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        if (Build.VERSION.SDK_INT >= 29 ? EasyPermissions.hasPermissions(this, permissionsForQ) :
                EasyPermissions.hasPermissions(this, permissions)) {
            Toast.makeText(this, "权限OK", Toast.LENGTH_LONG).show();
        } else {
            EasyPermissions.requestPermissions(this, "需要权限",
                    1, Build.VERSION.SDK_INT >= 29 ? permissionsForQ : permissions);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    // 用于接收定位结果
    @Override
    public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
        String msg = null;
        if (TencentLocation.ERROR_OK == 0) {
            Log.d(TAG, "onLocationChanged: 定位成功");
            // 定位成功
            if (tencentLocation != null) {
                StringBuilder sb = new StringBuilder();

                sb.append("来源: ").append(tencentLocation.getProvider())
                        .append("，纬度: ").append(tencentLocation.getLatitude())
                        .append(",经度: ").append(tencentLocation.getLongitude())
//                        .append(",海拔: ").append(tencentLocation.getAltitude())
                        .append(",精度: ").append(tencentLocation.getAccuracy());
//                        .append(",国家: ").append(tencentLocation.getNation())
//                        .append(",省: ").append(tencentLocation.getProvince())
//                        .append(",市: ").append(tencentLocation.getCity())
//                        .append(",区: ").append(tencentLocation.getDistrict())
//                        .append(",镇: ").append(tencentLocation.getTown())
//                        .append(",村=").append(tencentLocation.getVillage())
//                        .append(", 街道").append(tencentLocation.getStreet())
//                        .append(", 门号").append(tencentLocation.getStreetNo())
//                        .append(", POI: ").append(tencentLocation.getPoiList().get(0).getName());
                        // 注意, 根据国家相关法规, wgs84坐标下无法提供地址信息
//                        .append("{84坐标下不提供地址!}");
                msg = sb.toString();
                Log.d(TAG, "onLocationChanged: " + msg);
                gpsText.setText(msg);
            }
        } else {
            // 定位失败
            Log.d(TAG, "onLocationChanged: 定位失败");
        }
    }

    // 用于接收GPS、WiFi、Cell状态码
    @Override
    public void onStatusUpdate(String s, int i, String s1) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationManager.removeUpdates(this);
    }
}