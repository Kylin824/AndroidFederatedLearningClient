package com.example.locationdemo;

import android.util.Log;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nd4j.linalg.api.ndarray.INDArray;

public class ModelUtils {

    private static final String TAG = "ModelUtils";

    public static JSONArray model0WToJsonArray(MultiLayerNetwork model) throws JSONException {

        JSONArray data = new JSONArray();
        INDArray param;
        param = model.getParam("0_W");
        System.out.println("0_W param : row " +param.rows()  +" col: " +param.columns());
        for (int j = 0; j < param.rows(); j++) {
            for (int k = 0; k < param.columns(); k++) {
                data.put(param.getRow(j).getFloat(k));
            }
        }
        return data;
    }

    public static JSONArray model1WToJsonArray(MultiLayerNetwork model) throws JSONException {

        JSONArray data = new JSONArray();
        INDArray param;
        param = model.getParam("1_W");
        System.out.println("1_W param : row " +param.rows()  +" col: " +param.columns());
        for (int j = 0; j < param.rows(); j++) {
            for (int k = 0; k < param.columns(); k++) {
                data.put(param.getRow(j).getFloat(k));
            }
        }
        return data;
    }

    public static JSONArray model0BToJsonArray(MultiLayerNetwork model) throws JSONException {
        JSONArray data = new JSONArray();
        INDArray param;
        param = model.getParam("0_b"); // 只有1行
        for (int k = 0; k < param.columns(); k++) {
            data.put(param.getRow(0).getFloat(k));
        }
        return data;
    }

    public static JSONArray model1BToJsonArray(MultiLayerNetwork model) throws JSONException {
        JSONArray data = new JSONArray();
        INDArray param;
        param = model.getParam("1_b"); // 只有1行
        for (int k = 0; k < param.columns(); k++) {
            data.put(param.getRow(0).getFloat(k));
        }
        return data;
    }

    public static MultiLayerNetwork updateModel(JSONObject jsonObject, MultiLayerNetwork model, int layerNum) throws JSONException {

        JSONArray arrW0 = jsonObject.getJSONArray("arrW0");
        JSONArray arrW1 = jsonObject.getJSONArray("arrW1");
        JSONArray arrB0 = jsonObject.getJSONArray("arrB0");
        JSONArray arrB1 = jsonObject.getJSONArray("arrB1");


        String key;
        INDArray param;

        key = "0_W";
        param = model.getParam(key);
        for (int j = 0; j < param.rows(); j++) {
            for (int k = 0; k < param.columns(); k++) {
                param.putScalar(j, k, (double)arrW0.get(j * param.columns() + k));
            }
        }

        Log.d(TAG, "updateModel: 0_W");

        key = "1_W";
        param = model.getParam(key);
//        Log.d(TAG, "updateModel: row " +param.rows() + " col : " + param.columns());
        for (int j = 0; j < param.rows(); j++) {
            for (int k = 0; k < param.columns(); k++) {
//                Log.d(TAG, "updateModel: idx " + (j * param.columns() + k));
                param.putScalar(j, k, (double)arrW1.get(j * param.columns() + k));
            }
        }

        key = "0_b";
        param = model.getParam(key);
        for (int k = 0; k < param.columns(); k++) {
            param.putScalar(k, (double)arrB0.get(k));
        }

        key = "1_b";
        param = model.getParam(key);
        for (int k = 0; k < param.columns(); k++) {
            param.putScalar(k, (double)arrB1.get(k));
        }

        return model;
    }

    public static JSONObject modelToJson(MultiLayerNetwork model) throws Exception{

        JSONObject modelInJson = new JSONObject();

        int layerNum = model.getLayers().length;
        modelInJson.put("layerNum", layerNum);
        String key;
        for (int i = 0; i < layerNum; i++) {
            key = i + "_W";
            modelInJson.put(key, model.getParam(key));
            key = i + "_b";
            modelInJson.put(key, model.getParam(key));
        }
        return modelInJson;
    }

}
