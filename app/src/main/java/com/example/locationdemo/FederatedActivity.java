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

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class FederatedActivity extends AppCompatActivity {

    private ExecutorService executor;
    private LineChart mChart;
    private TextView stepText;
    private TextView logArea;
    private static final String TAG = "FederatedActivity";
    private String mUsername;
    Handler handler = null;
    private String clientID;
    private MNISTModel localModel;
    private Socket mSocket;
    private int currentRound = 0;

    {
        try {
//            mSocket = IO.socket("http://10.0.2.2:9092");  // 模拟器要连接里面的wifi！用10.0.2.2对应server的localhost
            mSocket = IO.socket("http://192.168.1.46:9092");  // 模拟器要连接里面的wifi！用10.0.2.2对应server的localhost
        } catch (URISyntaxException e) {
            Log.e(TAG, "onCreate: " + e.toString());
        }
    }

    List<Entry> entryList = new ArrayList<>();          //实例化一个List用来保存你的数据


    private TrainingListener trainingListener = new TrainingListener() {

//        int iterCount;

        @Override
        public void iterationDone(Model model, int iteration, int epoch) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    double result = model.score();
//                    String message = "\nScore at iteration " + iterCount + " is " + result;
//                    Log.d(TAG, message);
////                    logArea.append(message);
//                    iterCount++;
//                }
//            });
        }

        @Override
        public void onEpochStart(Model model) {
            Log.d(TAG, "onEpochStart: start");
        }

        @Override
        public void onEpochEnd(Model model) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double result = model.score();
                    logArea.append("round " + currentRound + " train end \n");
                    logArea.append("local loss: " + result + " \n");
                    Log.d(TAG, "onEpochEnd: end");
                    addEntry(currentRound, result);
                }
            });
        }

        @Override
        public void onForwardPass(Model model, List<INDArray> activations) {

        }

        @Override
        public void onForwardPass(Model model, Map<String, INDArray> activations) {

        }

        @Override
        public void onGradientCalculation(Model model) {
//            Log.d(TAG, "onGradientCalculation: gradient");
        }

        @Override
        public void onBackwardPass(Model model) {

        }
    };
    private TrainerDataSource trainerDataSource = new MNISTDataSource(123);;

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

        executor = Executors.newSingleThreadExecutor();

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

    private void addEntry(int currentRound, double score) {
        LineData lineData = mChart.getData();
        lineData.addEntry(new Entry(currentRound, (float)score), 0);
        mChart.setVisibleXRangeMaximum(currentRound + 5);
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
            JSONObject clientInitObject = (JSONObject) args[0];

            try {

                Log.d(TAG, "init epoch: " + clientInitObject.getInt("epoch"));
                Log.d(TAG, "init batchSize: " + clientInitObject.getInt("batchSize"));
                Log.d(TAG, "init clientIndex: " + clientInitObject.getInt("clientIndex"));
                Log.d(TAG, "init laynum: " + clientInitObject.getInt("layerNum"));

//                Log.d(TAG, "init: jsonArray" + clientInitObject.getJSONObject("initWeights").getJSONArray("0_W").get(0));
                Log.d(TAG, "init arrW0: " + clientInitObject.getJSONArray("arrW0").get(0));
                Log.d(TAG, "init arrB0: " + clientInitObject.getJSONArray("arrB0").get(0));
                Log.d(TAG, "init arrW1: " + clientInitObject.getJSONArray("arrW1").get(0));
                Log.d(TAG, "init arrB1: " + clientInitObject.getJSONArray("arrB1").get(0));


                localModel = new MNISTModel(trainingListener);

                localModel.buildModelFromInitModel(clientInitObject);

                String msgFromServer = "client init model";

                Message msg = new Message();
                Bundle bundle = new Bundle();
                bundle.putString("data", msgFromServer);
                msg.setData(bundle);
                handler.sendMessage(msg);

                // client有多少数据训练
                Integer trainSize = 60000;

                JSONObject resp = new JSONObject();
                try {
                    resp.put("trainSize", trainSize);
                } catch (JSONException e) {
                    Log.e(TAG, "onInit: " + e.getMessage());
                }

                Log.d(TAG, "call: init rest");
                mSocket.emit("client_ready", resp);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    };


    private Emitter.Listener onRequestUpdate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject requestUpdateObj = (JSONObject) args[0];
            try {
                localModel.updateWeights(requestUpdateObj);
                currentRound = requestUpdateObj.getInt("currentRound");

                double testLoss = requestUpdateObj.getDouble("testLoss");
                double testAcc = requestUpdateObj.getDouble("testAcc");

                Log.d(TAG, "Starting training, round " + currentRound);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String msg = "global acc: " + testAcc + "\n";
                        logArea.append(msg);
                        stepText.setText(getString(R.string.current_round, currentRound));
//                        addEntry(currentRound, testAcc);
                    }
                });

                trainOneRound(currentRound);

            } catch (JSONException e) {
                Log.e(TAG, e.toString());
            }
        }
    };

    private void trainOneRound(int currentRound) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "execute: train start!");
                localModel.train(1);
                Log.d(TAG, "run: train finish!");

                JSONObject resp = new JSONObject();

                try {
                    JSONArray arrW0 = localModel.get0W();
                    JSONArray arrW1 = localModel.get1W();
                    JSONArray arrB0 = localModel.get0B();
                    JSONArray arrB1 = localModel.get1B();

                    resp.put("currentRound", currentRound);
                    resp.put("arrW0", arrW0);
                    resp.put("arrW1", arrW1);
                    resp.put("arrB0", arrB0);
                    resp.put("arrB1", arrB1);
                } catch (JSONException e) {
                    Log.e(TAG, "onRequestUpdate: " + e.getMessage());
                }
                //
                Log.d(TAG, "run: have a rest");
                 mSocket.emit("client_update", resp);
            }
        });
    }

    private Emitter.Listener onStopAndEval = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject req = (JSONObject) args[0];
            try {
                double finalGlobalAcc = req.getDouble("testAcc");
                double finalGlobalLoss = req.getDouble("testLoss");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String msg = "final loss: " + finalGlobalLoss + " \n acc: " + finalGlobalAcc + " \n";
                        logArea.append(msg);
                        logArea.append("train task finish !!! \n");
                    }
                });
                // 收尾工作

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