package io.github.kildot.backgroundRemover;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.util.*;


public class Slices_Correction implements PlugIn {

    static double[] prevNumbers = new double[]{0.0, 1.0, 0.0, 30.0};

    @Override
    public void run(String arg) {
        ImagePlus sourceImage = IJ.getImage();
        ImageStack stack = sourceImage.getStack();
        int count = stack != null ? stack.size() : 1;
        double[] numbers = showDialog();
        double maxValue = Double.NEGATIVE_INFINITY;
        double rangeMax = 65535;
        double rangeMin = 0;
        if (numbers == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            ImageProcessor ip = stack != null ? stack.getProcessor(i + 1) : sourceImage.getProcessor();
            rangeMax = ip.getMax();
            rangeMin = ip.getMin();
            double maxForImage = divImage(ip, numbers, i);
            maxValue = Math.max(maxValue, maxForImage);
            maxValue = Math.max(maxValue, rangeMax);
        }
        if (maxValue > 65535) {
            IJ.log("Values out of range. Max. value " + maxValue + " > 65535");
            maxValue = 65535;
        }
        IJ.setMinAndMax(sourceImage, rangeMin, (int)(maxValue + 0.5));
        if (maxValue > rangeMax) {
            IJ.log("Display range changed: " + rangeMin + " ÷ " + rangeMax + "  ->  " + rangeMin + " ÷ " + (int)(maxValue + 0.5));
        }
        Utils.addProcessingInfo(sourceImage, sourceImage, "Slices Correction: " + numbers[0] + "; " + numbers[1] + "; " + numbers[2] + "; " + numbers[3]);
        sourceImage.updateAndDraw();
    }

    private double[] showDialog() {
        GenericDialog dialog = new GenericDialog("Parameters");
        dialog.addStringField("First depth:", Double.toString(prevNumbers[0]), 20);
        dialog.addStringField("Slice thick:", Double.toString(prevNumbers[1]), 20);
        dialog.addStringField("Time 0", Double.toString(prevNumbers[2]), 20);
        dialog.addStringField("Time:", Double.toString(prevNumbers[3]), 20);
        dialog.showDialog();
        if (dialog.wasCanceled()) {
            return null;
        }
        Vector<TextField> vect = dialog.getStringFields();
        double[] results = prevNumbers;
        results[0] = Double.parseDouble(vect.get(0).getText().trim().replace(',', '.'));
        results[1] = Double.parseDouble(vect.get(1).getText().trim().replace(',', '.'));
        results[2] = Double.parseDouble(vect.get(2).getText().trim().replace(',', '.'));
        results[3] = Double.parseDouble(vect.get(3).getText().trim().replace(',', '.'));
        return results;
    }

    private double divImage(ImageProcessor ip, double[] numbers, int index) {

        double div = 1.0;

        double gl0 = numbers[0];
        double step_gl0 = numbers[1];
        double gl = gl0 + (double)index * step_gl0;
        div *= Math.exp(-0.015 * gl);

        double t0 = numbers[2];
        double step_t0 = numbers[3];
        double t = t0 + (double)index * step_t0;
        div *= 0.765 * Math.exp(-t / 443.85) + 0.235;

        double black = ip.getMin();
        int width = ip.getWidth();
        int height = ip.getHeight();
        double maxValue = Double.NEGATIVE_INFINITY;
        if (ip instanceof ShortProcessor) {
            ShortProcessor processor = (ShortProcessor)ip;
            short[] px = (short[])processor.getPixels();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double v = (double)((int)px[x + width * y] & 0xFFFF);
                    v = (v - black) / div + black;
                    v = Math.round(v);
                    px[x + width * y] = (short)(int)(0.5 + Math.max(black, Math.min(65535, v)));
                    maxValue = Math.max(maxValue, v);
                }
            }
        }
        return maxValue;
    }

}
