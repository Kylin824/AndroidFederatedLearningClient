package com.example.locationdemo;

import org.deeplearning4j.nn.gradient.Gradient;
import org.json.JSONObject;
import org.nd4j.linalg.api.ndarray.INDArray;

public interface FederatedModel {

    void updateWeights(INDArray remoteGradient);

    INDArray getGradientAsArray();

    void updateWeights(JSONObject jsonObject);

    Gradient getGradient();

    void buildModel();

    JSONObject modelToJson();

    void buildModelFromInitModel(JSONObject clientInitObj) throws Exception;

    void train(TrainerDataSource trainerDataSource);

    String evaluate(TrainerDataSource trainerDataSource);

}
