package com.example.locationdemo;

import android.util.Log;

import org.apache.commons.io.FilenameUtils;
import org.deeplearning4j.datasets.mnist.MnistImageFile;
import org.deeplearning4j.datasets.mnist.MnistManager;
import org.nd4j.linalg.api.buffer.DataType;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.fetcher.BaseDataFetcher;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class MnistDataFetcherForAndroid extends BaseDataFetcher {

//    public static final int NUM_EXAMPLES = 60000;
    public static final int NUM_EXAMPLES = 6464;

    public static final int NUM_EXAMPLES_TEST = 10000;

    protected static final long CHECKSUM_TRAIN_FEATURES = 2094436111L;
    protected static final long CHECKSUM_TRAIN_LABELS = 4008842612L;
    protected static final long CHECKSUM_TEST_FEATURES = 2165396896L;
    protected static final long CHECKSUM_TEST_LABELS = 2212998611L;

    protected static final long[] CHECKSUMS_TRAIN = new long[]{CHECKSUM_TRAIN_FEATURES, CHECKSUM_TRAIN_LABELS};
    protected static final long[] CHECKSUMS_TEST = new long[]{CHECKSUM_TEST_FEATURES, CHECKSUM_TEST_LABELS};

    protected transient MnistManager man;
    protected boolean binarize = true;
    protected boolean train;
    protected int[] order;
    protected Random rng;
    protected boolean shuffle;
    protected boolean oneIndexed = false;
    protected boolean fOrder = false; //MNIST is C order, EMNIST is F order

    protected boolean firstShuffle = true;
    protected final int numExamples;

    private float[][] featureData = null;

    private static final String TAG = "MnistDataFetcherForAndroid";

    public MnistDataFetcherForAndroid(boolean binarize, boolean train, boolean shuffle, long rngSeed, int numExamples) throws IOException {

//        if (!mnistExists()) {
//            new MnistFetcher().downloadAndUntar();
//        }

//        String MNIST_ROOT = DL4JResources.getDirectory(ResourceType.DATASET, "MNIST").getAbsolutePath();

//        String MNIST_ROOT = "/data/mnist/";
        String MNIST_ROOT = "/storage/emulated/0/Download/MNIST";

        String images;
        String labels;
//        long[] checksums;
        if (train) {
            images = FilenameUtils.concat(MNIST_ROOT, "train-images-idx3-ubyte");
            labels = FilenameUtils.concat(MNIST_ROOT, "train-labels-idx1-ubyte");
            Log.d(TAG, "image path: " + images);
            Log.d(TAG, "label path: " + labels);
            totalExamples = NUM_EXAMPLES;
//            checksums = CHECKSUMS_TRAIN;
        } else {
            images = FilenameUtils.concat(MNIST_ROOT, "t10k-images-idx3-ubyte");
            labels = FilenameUtils.concat(MNIST_ROOT, "t10k-labels-idx1-ubyte");
            totalExamples = NUM_EXAMPLES_TEST;
//            checksums = CHECKSUMS_TEST;
        }
//        String[] files = new String[]{images, labels};

        try {

//            MnistImageFile file = new MnistImageFile(images, "r");
//            Log.d(TAG, "MnistDataFetcherForAndroid: row " + file.getRows());

            man = new MnistManager(images, labels, train);
            man.readImage();
            Log.d(TAG, "MnistDataFetcherForAndroid: new Mnistmanager");
//            validateFiles(files, checksums);
        } catch (Exception e) {
            Log.d(TAG, "MnistDataFetcherForAndroid: " + e.getMessage());
//            validateFiles(files, checksums);
        }

        numOutcomes = 10;
        this.binarize = binarize;
        cursor = 0;
        inputColumns = man.getImages().getEntryLength();
        this.train = train;
        this.shuffle = shuffle;

        if (train) {
            order = new int[NUM_EXAMPLES];
        } else {
            order = new int[NUM_EXAMPLES_TEST];
        }
        for (int i = 0; i < order.length; i++)
            order[i] = i;
        rng = new Random(rngSeed);
        this.numExamples = numExamples;
        reset(); //Shuffle order
    }

    @Override
    public void fetch(int numExamples) {
        if (!hasMore()) {
            throw new IllegalStateException("Unable to get more; there are no more images");
        }

        INDArray labels = Nd4j.zeros(DataType.FLOAT, numExamples, numOutcomes);

        if(featureData == null || featureData.length < numExamples){
            featureData = new float[numExamples][28*28];
        }

        int actualExamples = 0;
        byte[] working = null;
        for (int i = 0; i < numExamples; i++, cursor++) {
            if (!hasMore())
                break;

            byte[] img = man.readImageUnsafe(order[cursor]);

            if (fOrder) {
                //EMNIST requires F order to C order
                if (working == null) {
                    working = new byte[28 * 28];
                }
                for (int j = 0; j < 28 * 28; j++) {
                    working[j] = img[28 * (j % 28) + j / 28];
                }
                img = working;
            }

            int label = man.readLabel(order[cursor]);
            if (oneIndexed) {
                //For some inexplicable reason, Emnist LETTERS set is indexed 1 to 26 (i.e., 1 to nClasses), while everything else
                // is indexed (0 to nClasses-1) :/
                label--;
            }

            labels.put(actualExamples, label, 1.0f);

            for(int j = 0 ; j < img.length ; j++) {
                featureData[actualExamples][j] = ((int) img[j]) & 0xFF;
            }

            actualExamples++;
        }

        INDArray features;

        if(featureData.length == actualExamples){
            features = Nd4j.create(featureData);
        } else {
            features = Nd4j.create(Arrays.copyOfRange(featureData, 0, actualExamples));
        }

        if (actualExamples < numExamples) {
            labels = labels.get(NDArrayIndex.interval(0, actualExamples), NDArrayIndex.all());
        }

        if(binarize){
            features = features.gt(30.0).castTo(DataType.FLOAT);
        } else {
            features.divi(255.0);
        }

        curr = new DataSet(features, labels);
    }
}
