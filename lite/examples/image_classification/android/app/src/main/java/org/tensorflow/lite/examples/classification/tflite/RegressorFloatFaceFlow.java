package org.tensorflow.lite.examples.classification.tflite;

import android.app.Activity;

import java.io.IOException;

public class RegressorFloatFaceFlow extends Classifier {

    private static final float IMAGE_MAX = 255f;

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
     * of the super class, because we need a primitive array here.
     */
    private float[][] labelProbArray = null;

    public RegressorFloatFaceFlow(Activity activity, Device device, int numThreads)
            throws IOException {
        super(activity, device, numThreads);
        labelProbArray = new float[1][getNumLabels()];
    }

    @Override
    public int getImageSizeX() {
        return 80;
    }

    @Override
    public int getImageSizeY() {
        return 80;
    }

    @Override
    protected String getModelPath() {
        // you can download this file from
        // see build.gradle for where to obtain this file. It should be auto
        // downloaded into assets.
        return "model_adapted.tflite";
    }

    @Override
    protected String getLabelPath() {
        return "labels_blendshape.txt";
    }

    @Override
    protected int getNumBytesPerChannel() {
        return 4; // Float.SIZE / Byte.SIZE;
    }

    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.putFloat((pixelValue >> 16 & 0xFF) / IMAGE_MAX);
        imgData.putFloat((pixelValue >> 8 & 0xFF) / IMAGE_MAX);
        imgData.putFloat((pixelValue & 0xFF) / IMAGE_MAX);
    }

    @Override
    protected float getProbability(int labelIndex) {
        return labelProbArray[0][labelIndex];
    }

    @Override
    protected void setProbability(int labelIndex, Number value) {
        labelProbArray[0][labelIndex] = value.floatValue();
    }

    @Override
    protected float getNormalizedProbability(int labelIndex) {
        return labelProbArray[0][labelIndex];
    }

    @Override
    protected void runInference() {
        tflite.run(imgData, labelProbArray);
    }
}
