package com.example.locationdemo;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.json.JSONObject;
import org.nd4j.linalg.api.ndarray.INDArray;

public class ModelUtils {

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

    public static MultiLayerNetwork jsonToModel(JSONObject jsonObject, MultiLayerNetwork model) throws Exception{
        INDArray param;
        String key;
        int layerNum = jsonObject.getInt("layerNum");
        for (int i = 0; i < layerNum; i++) {
            key = i + "_W";
            param = (INDArray)jsonObject.get(key);
            model.setParam(key, param);
            key = i + "_b";
            param = (INDArray)jsonObject.get(key);
            model.setParam(key, param);
        }
        return model;
    }
}
