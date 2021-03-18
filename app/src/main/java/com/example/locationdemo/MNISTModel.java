package com.example.locationdemo;

import android.util.Log;

import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.IOException;
import java.util.List;

public class MNISTModel implements FederatedModel {

    private static final String TAG = MNISTModel.class.getSimpleName();
    private static final int BATCH_SIZE = 64;
    private static final int OUTPUT_NUM = 10;
    private static final int N_EPOCHS = 1;
    private static final int rngSeed = 123;

    private TrainingListener mIterationListener;
    private MultiLayerNetwork model;

    private DataSetIterator mnistTrain = new MnistDataSetIteratorForAndroid(BATCH_SIZE, true, rngSeed, 6464, false);
//    private DataSetIterator mnistTrainNONIID = new MnistDataSetIteratorForAndroid(BATCH_SIZE, true, rngSeed, 6464, true);
//    private DataSetIterator mnistTest = new MnistDataSetIteratorForAndroid(BATCH_SIZE, false, rngSeed);


    public MNISTModel(TrainingListener trainingListener) throws IOException {

        mIterationListener = trainingListener;
    }

    @Override
    public void buildModel() {
        //number of rows and columns in the input pictures
        final int numRows = 28;
        final int numColumns = 28;
        int rngSeed = 123; // random number seed for reproducibility
        double learningRate = 0.006; // learning rate

        Log.d(TAG, "Build local model....");

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(rngSeed) //include a random seed for reproducibility
                // use stochastic gradient descent as an optimization algorithm
                .updater(new Nesterovs(learningRate, 0.9))
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder() //create the first, input layer with xavier initialization
                        .nIn(numRows * numColumns)
                        .nOut(1000)
                        .activation(Activation.RELU)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) //create hidden layer
                        .nIn(1000)
                        .nOut(OUTPUT_NUM)
                        .activation(Activation.SOFTMAX)
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .build();

        model = new MultiLayerNetwork(conf);
        model.init();
        model.setListeners(mIterationListener);  //print the score with every iteration
    }

    @Override
    public void buildModelFromInitModel(JSONObject clientInitObj) throws Exception {
        //number of rows and columns in the input pictures
        final int numRows = 28;
        final int numColumns = 28;
        int rngSeed = 123; // random number seed for reproducibility
        double learningRate = 0.006; // learning rate

        Log.d(TAG, "Build local model....");

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(rngSeed) //include a random seed for reproducibility
                // use stochastic gradient descent as an optimization algorithm
                .updater(new Nesterovs(learningRate, 0.9))
                .l2(1e-4)
                .list()
                .layer(new DenseLayer.Builder() //create the first, input layer with xavier initialization
                        .nIn(numRows * numColumns)
                        .nOut(100)
                        .activation(Activation.RELU)
//                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD) //create hidden layer
                        .nIn(100)
                        .nOut(OUTPUT_NUM)
                        .activation(Activation.SOFTMAX)
//                        .weightInit(WeightInit.XAVIER)
                        .build())
                .build();

        model = new MultiLayerNetwork(conf);
        model.init();

        model = ModelUtils.updateModel(clientInitObj, model, 2);

        Log.d(TAG, "buildModelFromInitModel 0_W first element: " + model.getParam("0_W").getRow(0).getFloat(0));

        model.setListeners(mIterationListener);  //print the score with every iteration

    }

    @Override
    public String evaluate(TrainerDataSource trainerDataSource) {
        DataSet testData = trainerDataSource.getTrainingData(BATCH_SIZE);
        List<DataSet> listDs = testData.asList();
        DataSetIterator iterator = new ListDataSetIterator(listDs, BATCH_SIZE);

        Evaluation eval = new Evaluation(OUTPUT_NUM); //create an evaluation object with 10 possible classes
        while (iterator.hasNext()) {
            DataSet next = iterator.next();
            INDArray output = model.output(next.getFeatures()); //get the networks prediction
            eval.eval(next.getLabels(), output); //check the prediction against the true class
        }

        return eval.stats();
    }

    @Override
    public void train(int numEpochs) {
        model.fit(mnistTrain, numEpochs);
    }

    @Override
    public void updateWeights(JSONObject requestUpdateObj) {
        try {
            model = ModelUtils.updateModel(requestUpdateObj, model, 2);
            int round = requestUpdateObj.getInt("currentRound");
            Log.d(TAG, "updateWeights: update local weight success at round: " + round);
        }
        catch (Exception e) {
            Log.d(TAG, "updateWeights: " + e.getMessage());
        }
    }

    @Override
    public JSONObject modelToJson() {
        JSONObject localModelObj = new JSONObject();
        try {
            localModelObj = ModelUtils.modelToJson(model);
            return localModelObj;
        }
        catch (Exception e) {
            Log.e(TAG, "modelToJson: " + e.getMessage());
        }
        return localModelObj;
    }

    @Override
    public INDArray getGradientAsArray() {
        return null;
    }

    public JSONArray get0W() throws JSONException {
        return ModelUtils.model0WToJsonArray(model);
    }
    public JSONArray get1W() throws JSONException {
        return ModelUtils.model1WToJsonArray(model);
    }
    public JSONArray get0B() throws JSONException {
        return ModelUtils.model0BToJsonArray(model);
    }
    public JSONArray get1B() throws JSONException {
        return ModelUtils.model1BToJsonArray(model);
    }
    @Override
    public void updateWeights(INDArray remoteGradient) {

    }

    @Override
    public Gradient getGradient() {
        return model.gradient();
    }
}
