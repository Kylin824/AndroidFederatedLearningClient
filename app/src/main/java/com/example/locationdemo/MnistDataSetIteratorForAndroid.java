package com.example.locationdemo;

import org.deeplearning4j.datasets.fetchers.MnistDataFetcher;
import org.nd4j.linalg.dataset.api.iterator.BaseDatasetIterator;
import org.nd4j.linalg.dataset.api.iterator.fetcher.DataSetFetcher;

import java.io.IOException;

public class MnistDataSetIteratorForAndroid extends BaseDatasetIterator {


    public MnistDataSetIteratorForAndroid(int batch, int numExamples, DataSetFetcher fetcher) {
        super(batch, numExamples, fetcher);
    }

    public MnistDataSetIteratorForAndroid(int batch, int numExamples, boolean binarize) throws IOException {
        this(batch, numExamples, binarize, true, false, 0);
    }

    public MnistDataSetIteratorForAndroid(int batchSize, boolean train, int seed) throws IOException {
        this(batchSize, (train ? MnistDataFetcher.NUM_EXAMPLES : MnistDataFetcher.NUM_EXAMPLES_TEST), false, train,
                true, seed);
    }


    public MnistDataSetIteratorForAndroid(int batch, int numExamples, boolean binarize, boolean train, boolean shuffle,
                                          long rngSeed) throws IOException {
        super(batch, numExamples, new MnistDataFetcherForAndroid(binarize, train, shuffle, rngSeed, numExamples));
    }
}
