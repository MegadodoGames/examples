package org.tensorflow.lite.examples.classification.tflite;

import android.app.Activity;

import java.io.IOException;

public class RegressorQuantizedFaceFlow extends Classifier {

    private static final float IMAGE_MAX = 255f;

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
     * of the super class, because we need a primitive array here.
     */
    private float[][] labelProbArray = null;

    public RegressorQuantizedFaceFlow(Activity activity, Device device, int numThreads)
            throws IOException {
        super(activity, device, numThreads);
        labelProbArray = new float[1][getNumLabels()];
    }

    @Override
    public int getImageSizeX() {
        return 160;
    }

    @Override
    public int getImageSizeY() {
        return 60;
    }

    @Override
    protected String getModelPath() {
        // you can download this file from
        // see build.gradle for where to obtain this file. It should be auto
        // downloaded into assets.
        return "model_twincam_quant.tflite";
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
        // see the formula for gray-scale at https://stackoverflow.com/a/19181932/2593810(
//        int alpha = pixelValue >> 24 & 0xFF;
        int red = pixelValue >> 16 & 0xFF; // from 0 to 255
        int green = pixelValue >> 8 & 0xFF; // from 0 to 255
        int blue = pixelValue & 0xFF; // from 0 to 255
        float gray = 0.299f * red + 0.587f * green + 0.114f * blue; // approx average from 0 to 255
//        Log.v("RegressorFloatFaceFlow", String.format("ARGB gray: %d %d %d %d %f", alpha, red, green, blue, gray));
        imgData.putFloat(gray / IMAGE_MAX);
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
