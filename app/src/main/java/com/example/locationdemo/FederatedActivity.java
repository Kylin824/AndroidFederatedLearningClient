package com.example.locationdemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class FederatedActivity extends AppCompatActivity {

    private LineChart mChart;
    private TextView stepText;
    private TextView logArea;
    private int step = 0;
    private static final String TAG = "FederatedActivity";
    private String mUsername;
    private Boolean isConnected = true;
    Handler handler = null;
    private String clientID;

    private Socket mSocket;

    {
        try {
            mSocket = IO.socket("http://10.0.2.2:9092");  // 模拟器要连接里面的wifi！用10.0.2.2对应server的localhost
        } catch (URISyntaxException e) {
            Log.e(TAG, "onCreate: " + e.toString());
        }
    }

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

        // connect to server
        preProcessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUsername = "android_" + Math.floor((Math.random() * 1000) + 1);
                mSocket.on(Socket.EVENT_CONNECT, onConnect);
                mSocket.on(Socket.EVENT_DISCONNECT, onDisConnect);
                mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
                mSocket.on("chatevent", onNewMessage);
                mSocket.on("init", onInit);
                mSocket.on("request_update", onRequestUpdate);
                mSocket.on("stop_and_eval", onStopAndEval);
                mSocket.connect();
            }
        });

        startManualBtn.setOnClickListener(v -> {
//            step++;
//            Log.d(TAG, "onClick: step: " + step);
//            addEntry(step);
            mSocket.emit("client_wake_up");
            logArea.append("send wake up \n");
        });

        startAutoBtn.setOnClickListener(v -> {
            JSONObject data = new JSONObject();
            try {
                data.put("userName", "android_0");
                data.put("message", "this is a msg from android");
            } catch (JSONException e) {
                Log.e(TAG, "onCreate: " + e.toString());
            }
            mSocket.emit("chatevent", data);
            Log.d(TAG, "onCreate: android emit!!");
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

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("data", "connect to server");
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Exception err = (Exception) args[0];
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("data", "connect error");
            msg.setData(bundle);
            handler.sendMessage(msg);
            Log.d(TAG, "onCreate: connect error" + err.getMessage());
            // err.printStackTrace();
        }
    };

    private Emitter.Listener onDisConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Message msg = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("data", "disconnect to server");
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    };

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            try {
                String username = data.getString("userName");
                String message = data.getString("message");

                String msgFromServer = "user_" + username + " : " + message;
                Log.d(TAG, "onCreate: received from server: " + msgFromServer);
                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("data", msgFromServer);
                msg.setData(bundle);
                handler.sendMessage(msg);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

    private Emitter.Listener onInit = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            try {
                String initWeights = data.getString("initWeights");
                Integer epoch = data.getInt("epoch");
                Integer batchSize = data.getInt("batchSize");
                Integer clientIndex = data.getInt("clientIndex");

                // init local model by init weights

                String msgFromServer = "init_weight: " + initWeights + " \n epoch: " + epoch;

                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("data", msgFromServer);
                msg.setData(bundle);
                handler.sendMessage(msg);

                Integer trainSize = 60000;

                JSONObject resp = new JSONObject();
                try {
                    resp.put("trainSize", trainSize);
                } catch (JSONException e) {
                    Log.e(TAG, "onInit: " + e.getMessage());
                }
                mSocket.emit("client_ready", resp);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    };


    private Emitter.Listener onRequestUpdate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            try {
                String modelId = data.getString("modelId");
                String weights = data.getString("weights");
                Integer currentRound = data.getInt("currentRound");

                // set global weight to local model weight

                // train one round to get new weights

                String newWeights = "weight_client_round_" + currentRound;

                JSONObject resp = new JSONObject();
                try {
                    resp.put("roundNumber", currentRound);
                    resp.put("weights", newWeights);
                } catch (JSONException e) {
                    Log.e(TAG, "onRequestUpdate: " + e.getMessage());
                }
                mSocket.emit("client_update", resp);
            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

    private Emitter.Listener onStopAndEval = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject req = (JSONObject) args[0];
            try {
                String finalGlobalAcc = req.getString("finalGlobalTestAcc");

                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("data", "final global acc: " + finalGlobalAcc);
                msg.setData(bundle);
                handler.sendMessage(msg);

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.off("chatevent", onNewMessage);
        mSocket.disconnect();
    }


}