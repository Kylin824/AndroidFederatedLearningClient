package com.example.locationdemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorActivity extends AppCompatActivity implements TencentLocationListener {

    private TextView gpsText;
    private TextView memoryText;
    private TextView wifiText;
    private TextView cpuText;
    private TextView gmsText;
    private static TextView speedText;
    private TencentLocationManager mLocationManager;
    private static final String TAG = "MonitorActivity";
    private static final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private static long lastTotalRxBytes = 0;
    private static long lastTimeStamp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);

        gpsText = findViewById(R.id.gps_text);
        memoryText = findViewById(R.id.memory_text);
        wifiText = findViewById(R.id.wifi_text);
        cpuText = findViewById(R.id.cpu_text);
        gmsText = findViewById(R.id.signal_text);
        speedText = findViewById(R.id.speed_text);

        // 获取gps定位信息
        mLocationManager = TencentLocationManager.getInstance(this);
        // 连续定位
//        TencentLocationRequest locationRequest = TencentLocationRequest.create()
//                .setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_POI)
//                .setInterval(10000) // 定位周期(位置监听器回调周期), 单位为ms(毫秒)
//                .setAllowGPS(true); //允许使用GPS
//        mLocationManager.requestLocationUpdates(locationRequest, this);

        // 单次定位
        mLocationManager.requestSingleFreshLocation(null, this, Looper.getMainLooper());

        String wifiInfo = getWifiInfo();
        wifiText.setText(wifiInfo);

        String memoryInfo = getMemoryInfo();
        memoryText.setText(memoryInfo);

        getCpuInfo();
        getSignalInfo();

        scheduledExecutorService.scheduleAtFixedRate(this::getDownloadRate, 0, 2, TimeUnit.SECONDS);
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String currentSpeed = "实时网速: " + msg.arg1 + "Kb/s";
            // 调用setText()方法时如果传入int型是不会被当成内容而是resourceID来使用
            speedText.setText(currentSpeed);
            return false;
        }
    });

    // 用于接收定位结果
    @Override
    public void onLocationChanged(TencentLocation tencentLocation, int i, String s) {
        String msg = null;
        if (TencentLocation.ERROR_OK == 0) {
            Log.d(TAG, "onLocationChanged: 定位成功");
            // 定位成功
            if (tencentLocation != null) {
                StringBuilder sb = new StringBuilder();

                sb.append("来源(gps/network): ").append(tencentLocation.getProvider())
                        .append("\n纬度: ").append(tencentLocation.getLatitude())
                        .append("\n经度: ").append(tencentLocation.getLongitude())
//                        .append(",海拔: ").append(tencentLocation.getAltitude())
                        .append("\n精度: ").append(tencentLocation.getAccuracy());
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
        scheduledExecutorService.shutdown();
        Log.d(TAG, "onDestroy: finish monitoring");
    }

    // 获取wifi信息
    private String getWifiInfo() {
        // Wifi的连接速度及信号强度
        // 无线网络服务WIFI_SERVICE必须由Application的上下文去获取，否则的话，在7.0以下的设备中会发生内存泄漏。
        // 所以要在getSystemService方法之前加上getApplicationContext()
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        StringBuilder sb = new StringBuilder();
        if (info.getBSSID() != null) {
            // 链接信号强度
            int strength = WifiManager.calculateSignalLevel(info.getRssi(), 5);
            // 链接速度
            int speed = info.getLinkSpeed();
            // 链接速度单位
            String units = WifiInfo.LINK_SPEED_UNITS;
            // Wifi源名称
            String ssid = info.getSSID();
            String ip = intToIp(info.getIpAddress());
            sb.append("wifi名称: " + ssid)
                    .append("\n信号强度: " + strength)
                    .append("\n连接速度: " + speed).append(units)
                    .append("\nip: " + ip);
            Log.d(TAG, "obtainWifiInfo: " + sb.toString());
        }
        return sb.toString();
    }

    // IpAddress转为字符串
    private String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + (i >> 24 & 0xFF);
    }

    // 获取信号强度
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void getSignalInfo() {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<CellInfo> cellInfoList = tm.getAllCellInfo();
        Log.d(TAG, "getSignalInfo: cellInfoList" + cellInfoList.toString());
        StringBuilder sb = new StringBuilder();
        for (CellInfo cellInfo : cellInfoList)
        {
            if (cellInfo instanceof CellInfoLte)
            {
                CellSignalStrengthLte cellSignalStrengthLte = ((CellInfoLte)cellInfo).getCellSignalStrength();
                Log.d(TAG, "getSignalInfo: Asu Level: " + cellSignalStrengthLte.getAsuLevel());
                Log.d(TAG, "getSignalInfo: Cqi: " + cellSignalStrengthLte.getCqi());
                Log.d(TAG, "getSignalInfo: Dbm: " + cellSignalStrengthLte.getDbm());
                Log.d(TAG, "getSignalInfo: Level: " + cellSignalStrengthLte.getLevel());
                Log.d(TAG, "getSignalInfo: Rsrp: " + cellSignalStrengthLte.getRsrp());
                Log.d(TAG, "getSignalInfo: Rsrq: " + cellSignalStrengthLte.getRsrq());
                Log.d(TAG, "getSignalInfo: Rssi: " + cellSignalStrengthLte.getRssi());
                Log.d(TAG, "getSignalInfo: Rssnr: " + cellSignalStrengthLte.getRssnr());
                Log.d(TAG, "getSignalInfo: TimingAdvance: " + cellSignalStrengthLte.getTimingAdvance());
                sb.append("Asu Level: ").append(cellSignalStrengthLte.getAsuLevel())
                        .append("\nCqi: ").append(cellSignalStrengthLte.getCqi())
                        .append("\nDbm: ").append(cellSignalStrengthLte.getDbm())
                        .append("\nLevel: ").append(cellSignalStrengthLte.getLevel())
                        .append("\nRsrp: ").append(cellSignalStrengthLte.getRsrp())
                        .append("\nRsrq: ").append(cellSignalStrengthLte.getRsrq())
                        .append("\nRssi: ").append(cellSignalStrengthLte.getRssi())
                        .append("\nRssnr: ").append(cellSignalStrengthLte.getRssnr());
                gmsText.setText(sb.toString());
                break;
            }
        }
    }

    /*
    示例:
    # cat /proc/meminfo
    MemTotal:         5765020 kB     总内存
    MemFree:          108664 kB     空闲内存
    MemAvailable:     2311924 kB     可用内存
    ...
    */
    private String getMemoryInfo() {
        String memInfoFile = "/proc/meminfo"; // 系统内存信息文件
        String memLine; // 第一行为总内存大小，第二行为空闲内存，第三行为可用内存
        String[] arrs; // 存放split后的字符串，第[1]个元素即为内存容量
        StringBuilder memoryInfo = new StringBuilder();
        long totalMemory = 0;
        long availMemory = 0;
        try {
            FileReader localFileReader = new FileReader(memInfoFile);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);

            memLine = localBufferedReader.readLine();// 读取meminfo第一行，即系统总内存大小
            arrs = memLine.split("\\s+"); //  \\s表示空格,回车,换行等空白符,+号表示一个或多个
            totalMemory = Long.parseLong(arrs[1]) * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
            Log.d(TAG, "getTotalMemory: MemTotal = " + Formatter.formatFileSize(getBaseContext(), totalMemory));

            memLine = localBufferedReader.readLine();// 读取meminfo第二行
//            arrs = memLine.split("\\s+"); //  \\s表示空格,回车,换行等空白符,+号表示一个或多个
//            Log.d(TAG, "getTotalMemory: MemFree (KB) = " + Formatter.formatFileSize(getBaseContext(), Long.parseLong(arrs[1]) * 1024));

            memLine = localBufferedReader.readLine();// 读取meminfo第三行
            arrs = memLine.split("\\s+"); //  \\s表示空格,回车,换行等空白符,+号表示一个或多个
            availMemory = Long.parseLong(arrs[1]) * 1024;
            Log.d(TAG, "getTotalMemory: MemAvailable = " + Formatter.formatFileSize(getBaseContext(), availMemory));

            localBufferedReader.close();
            memoryInfo.append("可用内存/总内存: ").append(Formatter.formatFileSize(getBaseContext(), availMemory)).append("/").append(Formatter.formatFileSize(getBaseContext(), totalMemory));
            int batteryLevel = getBatteryLevel();
            memoryInfo.append("\n 当前电量: ").append(batteryLevel).append("%");
            return memoryInfo.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 将Byte自动转换为KB或者MB或者GB的格式
        return Formatter.formatFileSize(getBaseContext(), totalMemory);
    }

    /*
    处理器型号
    核心数
    最大频率
    load average
    availablity
   */
    private void getCpuInfo() {
        String cpuName = getCpuName();
        int coreNum = getCpuCoreNum();
        int maxFreq = getCpuMaxFreq();
        String loadAvg = getCpuLoadAverage();
//        float cpuUsage = getCpuUsage();

        Log.d(TAG, "getCpuInfo: cpuName: " + cpuName);
        Log.d(TAG, "getCpuInfo: core: " + coreNum);
        Log.d(TAG, "getCpuInfo: maxFreq: " + maxFreq + "MHz");
        Log.d(TAG, "getCpuInfo: loadAvg: " + loadAvg);
//        Log.d(TAG, "getCpuInfo: cpuUsage: " +cpuUsage);

        StringBuilder sb = new StringBuilder();
        sb.append("CPU 型号: ").append(cpuName)
                .append("\nCPU 核心数: ").append(coreNum)
                .append("\n最大频率: ").append(maxFreq).append("MHz")
                .append("\nLoad Average: ").append(loadAvg);
//                .append("\nCPU Usage: ").append(cpuUsage);

        cpuText.setText(sb.toString());
    }

    private static int getCpuCoreNum() {
        int cores;
        try {
            // Android 的 CPU 设备文件位于 /sys/devices/system/cpu/ 目录，文件名的的格式为 cpu\d+。
            // 统计一下文件个数获得 CPU 核数
            cores = Objects.requireNonNull(new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER)).length;
        } catch (SecurityException e) {
            cores = -1;
        } catch (NullPointerException e) {
            cores = -1;
        }
        return cores;
    }

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (path.charAt(i) < '0' || path.charAt(i) > '9') {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };

    private String getCpuName() {
        String line;
        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                if (line.contains("Hardware")) {
                    String[] strs = line.split("\\s+");
                    StringBuilder cpuName = new StringBuilder();
                    for (int i = 2; i < strs.length; i++) {
                        cpuName.append(strs[i]);
                    }
                    return cpuName.toString();
                }
            }
            return "Unknown hardware";
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int getCpuMaxFreq() {
        int maxFreq = -1;
        try {
            for (int i = 0; i < getCpuCoreNum(); i++) {
                String filename = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
                File cpuInfoMaxFreqFile = new File(filename);
                if (cpuInfoMaxFreqFile.exists()) {
                    byte[] buffer = new byte[128];
                    FileInputStream stream = new FileInputStream(cpuInfoMaxFreqFile);
                    try {
                        stream.read(buffer);
                        int endIndex = 0;
                        //Trim the first number out of the byte buffer.
                        while (buffer[endIndex] >= '0' && buffer[endIndex] <= '9')
                            endIndex++;
                        String str = new String(buffer, 0, endIndex);
                        int freqBound = Integer.parseInt(str) / 1000;
//                        Log.d(TAG, "getCPUMaxFreqKHz: cpu" + i + " freq: " + freqBound);
                        if (freqBound > maxFreq)
                            maxFreq = freqBound;
                    } catch (NumberFormatException e) {
                        //Fall through and use /proc/cpuinfo.
                    } finally {
                        stream.close();
                    }
                }
            }
            Log.d(TAG, "getCPUMaxFreqKHz: max freq: " + maxFreq);
        } catch (IOException e) {
            maxFreq = -1; //Fall through and return unknown.
        }
        return maxFreq;
    }

    private static String getCpuLoadAverage() {
        // 通过uptime命令获得
        StringBuilder loadAvgLine = new StringBuilder();
        String loadAvgStrs = "";
        ProcessBuilder cmd;
        try {
            String args = "uptime"; // 执行uptime命令，获得的字符串就包含loadAvg
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            int ch;
            while ((ch = in.read()) != -1) {
                loadAvgLine.append((char)ch);
            }
            in.close();
            Log.d(TAG, "getCpuLoadAverage: loadavg: " + loadAvgLine);
            loadAvgStrs = loadAvgLine.substring(loadAvgLine.lastIndexOf(":") + 1);
            Log.d(TAG, "getCpuLoadAverage: loadavg: " + loadAvgStrs.trim());
            return loadAvgStrs.trim();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loadAvgStrs;
    }


    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }


    // 在Android 4.0以上，网络连接不能放在主线程上
    private void getDownloadRate() {
        String netSpeed;
        long nowTotalRxBytes = TrafficStats.getTotalRxBytes() / 1024; // 转为KB;
        long nowTimeStamp = System.currentTimeMillis();
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));// 毫秒转换

        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        netSpeed  = speed + " kb/s";
        Log.d(TAG, "getDownloadRate: speed: " + netSpeed);
        Message msg = new Message();
        msg.arg1 = (int) speed;
        handler.sendMessage(msg);
    }
}