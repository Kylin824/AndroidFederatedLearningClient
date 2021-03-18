package com.example.locationdemo;

import org.deeplearning4j.datasets.fetchers.MnistDataFetcher;
import org.nd4j.linalg.dataset.api.iterator.BaseDatasetIterator;

import java.io.IOException;

public class MnistDataSetIteratorForAndroid extends BaseDatasetIterator {

    public MnistDataSetIteratorForAndroid(int batchSize, boolean train, int seed) throws IOException {
        this(batchSize, (train ? MnistDataFetcher.NUM_EXAMPLES : MnistDataFetcher.NUM_EXAMPLES_TEST), false, train,
                true, seed, true);
    }

    public MnistDataSetIteratorForAndroid(int batchSize, boolean train, int seed, int numExample, boolean isIID) throws IOException {
        this(batchSize, (train ? numExample : MnistDataFetcher.NUM_EXAMPLES_TEST), false, train,
                true, seed, isIID);
    }


    public MnistDataSetIteratorForAndroid(int batch, int numExamples, boolean binarize, boolean train, boolean shuffle,
                                          long rngSeed, boolean isIID) throws IOException {
        super(batch, numExamples, (isIID ? new MnistDataFetcherIID(binarize, train, shuffle, rngSeed) : new MnistDataFetcherNonIID(binarize, train, shuffle, rngSeed, numExamples)));
    }

}
