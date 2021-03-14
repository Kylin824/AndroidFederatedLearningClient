package com.example.locationdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FederatedActivity extends AppCompatActivity {

    private LineChart mChart;
    private TextView stepText;
    private TextView logArea;
    private int step = 0;
    private static final String TAG = "FederatedActivity";

    //IP地址和端口号
//    public static String IP_ADDRESS = "127.0.0.1";
//    public static String IP_ADDRESS = "localhost";
    public static String IP_ADDRESS = "10.0.2.2"; //模拟器上可以用10.0.2.2代替127.0.0.1和localhost
    public static int PORT = 8088;
    Socket socket = null;
    DataOutputStream outputStream = null;
    DataInputStream inputStream = null;
    String messageReceived = null;
    Handler handler = null;
    List<Entry> entryList = new ArrayList<>();          //实例化一个List用来保存你的数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_federated);
        mChart = findViewById(R.id.chart);
        stepText = findViewById(R.id.step);
        logArea = findViewById(R.id.log_area);
        // 每点击一次按钮，增加一个点
        Button startManualBtn = findViewById(R.id.btn_start_manual);
        Button startAutoBtn = findViewById(R.id.btn_start_auto);
        Button preProcessBtn = findViewById(R.id.btn_preprocess);



        // X轴所在位置改为下面 默认为上面
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        // 隐藏右边的Y轴
        mChart.getAxisRight().setEnabled(false);
        mChart.getDescription().setEnabled(false);

        entryList.add(new Entry(0, 0)); // 其中两个数字对应的分别是 X轴 Y轴

        LineDataSet lineDataSet = new LineDataSet(entryList, "loss");
        LineData lineData = new LineData(lineDataSet);
        mChart.setData(lineData);


        startManualBtn.setOnClickListener(v -> {
            step++;
            Log.d(TAG, "onClick: step: " + step);
            addEntry(step);
        });

        startAutoBtn.setOnClickListener(v -> {
            Log.d(TAG, "onCreate: start connection thread");
            new ConnectionThread("这是一条来自安卓客户端的消息").start();
        });

        handler = new Handler(msg -> {
            Bundle bundle = msg.getData();  //获取消息中的Bundle对象
            String str = bundle.getString("data");  //获取键为data的字符串的值
            logArea.append(str + "\n");
            return false;
        });
    }

    private void addEntry(int step) {
        LineData lineData = mChart.getData();
        float ydata = (float) (Math.random() * 20);
        lineData.addEntry(new Entry(step, ydata), 0);

        mChart.setVisibleXRangeMaximum(step + 5);

        mChart.notifyDataSetChanged();
        mChart.invalidate();

        mChart.animateX(500);
    }



    //新建一个子线程，实现socket通信
    class ConnectionThread extends Thread {

        String message;

        public ConnectionThread(String msg) {
            message = msg;
        }

        @Override
        public void run() {
            if (socket == null) {
                try {
                    socket = new Socket(IP_ADDRESS, PORT);
                    //获取socket的输入输出流
                    inputStream = new DataInputStream(socket.getInputStream());
                    outputStream = new DataOutputStream(socket.getOutputStream());
                } catch (IOException e) {
                    Log.d(TAG, "run: " + e.toString());
                    e.printStackTrace();
                }
            }
            try {
                // 发送
                PrintWriter printWriter = new PrintWriter(outputStream);
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                printWriter.write(message);
//                printWriter.flush();

                // 接收
                String info;
                while ((info = br.readLine()) != null) {
                    messageReceived = info;
                    Log.d(TAG, "run: 我是客户端，服务器返回信息：" + messageReceived);;
                }
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("data", messageReceived);
                msg.setData(bundle);
                handler.sendMessage(msg);
                br.close();
                printWriter.close();
            } catch (IOException e) {
                Log.d(TAG, "run: " + e.toString());
                e.printStackTrace();
            }
        }
    }

}