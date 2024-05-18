package io.github.kildot.backgroundRemover;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JOptionPane;


public class Points_Detector implements PlugIn, RoiListener, Params.Listener {

    static final int NET_TYPE_AVERAGE = 0;
    static final int NET_TYPE_MODE = 1;
    static final int NET_TYPE_MEDIAN = 2;

    // GUI
    private Window guiWindow;
    private Params globalParams;
    private ImagePlus sourceImage;
    private ImagePlus previewImage;
    private ImageProcessor previewProcessor;
    private TimerTask updateNoiseTask;
    private TimerTask updatePointsTask;
    private Roi pointsRoi;
    private Roi noiseRoi;

    // Plot
    private Plot plot;
    private Roi ignoreRoi;
    private double[] oldPointsX;
    private double[] oldPointsY;
    private double[] oldNoiseX;
    private double[] oldNoiseY;

    // Profile plot
    private Plot profilePlot;
    private PlotWindow profilePlotWindow;

    // Image
    private int width;
    private int height;
    private float[] pixels;

    // Window
    private int windowRadius;
    private int windowSize;
    private int[] indexUp;
    private int[] indexDown;
    private float[] weightUp;
    private float[] weightDown;

    // Histograms
    private int histSize;
    private float[] hist;

    @Override
    public void run(String arg) {
        logMethod();
        sourceImage = IJ.getImage();
        if (sourceImage == null) {
            IJ.error("Select image");
        }
        globalParams = new Params();
        globalParams.addListener(this);
        guiWindow = new Window(globalParams);
        guiWindow.showBlocking();
        if (!guiWindow.wasCanceled()) {
            closeInteractiveTools();
            imageProcess();
        }
        closed();
    }

    private void imageProcess() {
        logMethod();
        ImagePlus outputImage;
        makeWindow();
        boolean allSlices = globalParams.allSlices;
        int pointPixelOutput = globalParams.pointOutput;
        boolean pointScaled = globalParams.pointScaled;
        int backgroundPixelOutput = globalParams.bgOutput;
        boolean keepOriginalSlices = globalParams.addInputSlices;
        ImageStack stack = sourceImage.getStack();
        if (allSlices && stack != null && stack.size() > 1) {
            ImageStack is = new ImageStack(sourceImage.getWidth(), sourceImage.getHeight());
            for (int i = 1; i <= stack.size(); i++) {
                ImageProcessor ip = stack.getProcessor(i);
                ProcessingResults r = processSingleImage(ip, pointPixelOutput, pointScaled, backgroundPixelOutput,
                        keepOriginalSlices, i - 1, stack.size());
                is.addSlice(r.result);
                if (keepOriginalSlices)
                    is.addSlice(r.original);
            }
            outputImage = new ImagePlus("Output", is);
        } else if (keepOriginalSlices) {
            ImageStack is = new ImageStack(sourceImage.getWidth(), sourceImage.getHeight());
            ProcessingResults r = processSingleImage(sourceImage.getProcessor(), pointPixelOutput, pointScaled, backgroundPixelOutput,
                    keepOriginalSlices, 0, 1);
            is.addSlice(r.result);
            is.addSlice(r.original);
            outputImage = new ImagePlus("Output", is);
        } else {
            ImageProcessor r = processSingleImage(sourceImage.getProcessor(), pointPixelOutput, pointScaled, backgroundPixelOutput,
                    keepOriginalSlices, 0, 1).result;
            outputImage = new ImagePlus("Output", r);
        }
        Utils.addProcessingInfo(sourceImage, outputImage, "Points Detector: " + globalParams.toString());
        outputImage.show();
    }

    private class ProcessingResults {
        public ImageProcessor result;
        public ImageProcessor original;
        public ProcessingResults(ImageProcessor r, ImageProcessor o) {
            result = r;
            original = o;
        }
    }

    private ProcessingResults processSingleImage(ImageProcessor ip, int pointPixelOutput, boolean pointScaled, int backgroundPixelOutput,
            boolean keepOriginalSlices, int index, int total) {
        ImageProcessor src = ip.convertToFloat();
        ImageProcessor dst = ip.duplicate();
        ImageProcessor original = null;
        if (keepOriginalSlices)
            original = dst.duplicate();
        pixels = (float[]) src.getPixels();
        width = src.getWidth();
        height = src.getHeight();
        makeHist();
        IJ.showProgress(index, total);
        outputToProcessor(dst, ip, pointPixelOutput, pointScaled, backgroundPixelOutput);
        return new ProcessingResults(dst, original);
    }

    private void outputToProcessor(ImageProcessor dst, ImageProcessor src, int pointPixelOutput, boolean pointScaled, int backgroundPixelOutput) {
        int pointRadius = globalParams.pointRadius;
        int backgroundRadius = globalParams.backgroundStartRadius;
        float limitLineA = (float)globalParams.slope;
        float limitLineB = (float)globalParams.yIntercept;
        float[] results = new float[Array.getLength(dst.getPixels())];
        float minResult = Float.POSITIVE_INFINITY;
        float maxResult = Float.NEGATIVE_INFINITY;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int histOffset = histSize * (x + y * width);
                float firstValue = 0;
                for (int k = 0; k <= pointRadius; k++) {
                    firstValue += hist[histOffset + k];
                }
                firstValue /= (float) (pointRadius + 1);
                float lastValue = 0;
                for (int k = backgroundRadius; k < histSize; k++) {
                    lastValue += hist[histOffset + k];
                }
                lastValue /= (float) (histSize - backgroundRadius);
                float yy = firstValue;
                float xx = lastValue;
                float res = yy - (limitLineA * xx + limitLineB);
                if (res > maxResult)
                    maxResult = res;
                if (res < minResult)
                    minResult = res;
                results[x + y * width] = res;
            }
        }
        if (dst instanceof ShortProcessor) {
            outputToShortProcessor((ShortProcessor)dst, (ShortProcessor)src, results, pointPixelOutput, pointScaled, backgroundPixelOutput, minResult, maxResult);
        //} else if (dst instanceof ByteProcessor) {
        //    outputToByteProcessor((ByteProcessor)dst, (ByteProcessor)src, results, pointPixelOutput, pointScaled, backgroundPixelOutput, minResult, maxResult);
        } else {
            IJ.error("Only 16-bit image format is supported.");
        }
    }

    private class CalculateNetData {
        public int netType;
        public float[] results;
        public float[] diff;
        public float diffMax;
        public short[] inputPixels;
        public int skipPixels;
        public int maxMark;
        public byte[] map;
        public short[] queueX;
        public short[] queueY;
        public int queueMask;
        public short[] listCurrX;
        public short[] listCurrY;
        public short[] listNextX;
        public short[] listNextY;
        public int[] listValues;
        public HashMap<Integer, Integer> valuesCount;
    }

    private void calculateNetPointMarks(int startX, int startY, CalculateNetData d) {
        d.queueX[0] = (short)startX;
        d.queueY[0] = (short)startY;
        int queueFirst = 0;
        int queueLast = 1;
        while (queueFirst != queueLast) {
            int empty = (queueFirst - queueLast) & d.queueMask;
            int x = d.queueX[queueFirst];
            int y = d.queueY[queueFirst];
            queueFirst = (queueFirst + 1) & d.queueMask;
            byte n = (byte)(d.map[x + y * width] + 1);
            if (d.results[x + y * width] >= 0) {
                d.map[x + y * width] = 0;
                n = 1;
            }
            if (empty < 7 || n > d.maxMark) {
                continue;
            }
            if (x < width - 1 && d.map[(x + 1) + y * width] > n) {
                d.map[(x + 1) + y * width] = n;
                d.queueX[queueLast] = (short)(x + 1);
                d.queueY[queueLast] = (short)y;
                queueLast = (queueLast + 1) & d.queueMask;
            }
            if (x > 0 && d.map[(x - 1) + y * width] > n) {
                d.map[(x - 1) + y * width] = n;
                d.queueX[queueLast] = (short)(x - 1);
                d.queueY[queueLast] = (short)y;
                queueLast = (queueLast + 1) & d.queueMask;
            }
            if (y < height - 1 && d.map[x + (y + 1) * width] > n) {
                d.map[x + (y + 1) * width] = n;
                d.queueX[queueLast] = (short)x;
                d.queueY[queueLast] = (short)(y + 1);
                queueLast = (queueLast + 1) & d.queueMask;
            }
            if (y > 0 && d.map[x + (y - 1) * width] > n) {
                d.map[x + (y - 1) * width] = n;
                d.queueX[queueLast] = (short)x;
                d.queueY[queueLast] = (short)(y - 1);
                queueLast = (queueLast + 1) & d.queueMask;
            }
        }
    }

    private void calculateNetPointValue(int startX, int startY, CalculateNetData d) {
        d.queueX[0] = (short)startX;
        d.queueY[0] = (short)startY;
        int queueSize = 1;
        int queueIndex = 0;
        int listCurrSize = 0;
        int listNextSize = 0;
        while (queueIndex < queueSize) {
            int x = d.queueX[queueIndex];
            int y = d.queueY[queueIndex++];
            if (queueSize + 4 >= d.queueX.length || listNextSize + 4 >= d.listNextX.length) {
                continue;
            }
            if (x < width - 1 && d.map[(x + 1) + y * width] <= 1) {
                if (d.map[(x + 1) + y * width] == 0) {
                    d.map[(x + 1) + y * width] = 126;
                    d.queueX[queueSize] = (short)(x + 1);
                    d.queueY[queueSize++] = (short)y;
                } else {
                    d.listNextX[listNextSize] = (short)(x + 1);
                    d.listNextY[listNextSize++] = (short)y;
                }
            }
            if (x > 0 && d.map[(x - 1) + y * width] <= 1) {
                if (d.map[(x - 1) + y * width] == 0) {
                    d.map[(x - 1) + y * width] = 126;
                    d.queueX[queueSize] = (short)(x - 1);
                    d.queueY[queueSize++] = (short)y;
                } else {
                    d.listNextX[listNextSize] = (short)(x - 1);
                    d.listNextY[listNextSize++] = (short)y;
                }
            }
            if (y < height - 1 && d.map[x + (y + 1) * width] <= 1) {
                if (d.map[x + (y + 1) * width] == 0) {
                    d.map[x + (y + 1) * width] = 126;
                    d.queueX[queueSize] = (short)x;
                    d.queueY[queueSize++] = (short)(y + 1);
                } else {
                    d.listNextX[listNextSize] = (short)x;
                    d.listNextY[listNextSize++] = (short)(y + 1);
                }
            }
            if (y > 0 && d.map[x + (y - 1) * width] <= 1) {
                if (d.map[x + (y - 1) * width] == 0) {
                    d.map[x + (y - 1) * width] = 126;
                    d.queueX[queueSize] = (short)x;
                    d.queueY[queueSize++] = (short)(y - 1);
                } else {
                    d.listNextX[listNextSize] = (short)x;
                    d.listNextY[listNextSize++] = (short)(y - 1);
                }
            }
        }

        long sum = 0;
        int count = 0;
        long sumAlt = 0;
        int countAlt = 0;

        if (d.valuesCount != null) {
            d.valuesCount.clear();
        }

        for (int i = 1; i <= d.maxMark; i++) {
            listCurrSize = listNextSize;
            listNextSize = 0;
            short[] tmp;
            tmp = d.listCurrX;
            d.listCurrX = d.listNextX;
            d.listNextX = tmp;
            tmp = d.listCurrY;
            d.listCurrY = d.listNextY;
            d.listNextY = tmp;
            for (int listCurrIndex = 0; listCurrIndex < listCurrSize; listCurrIndex++) {
                int x = d.listCurrX[listCurrIndex];
                int y = d.listCurrY[listCurrIndex];
                if (i > d.skipPixels) {
                    int value = (int)d.inputPixels[x + y * width] & (int)0xFFFF;
                    if (d.listValues != null && count < d.listValues.length) {
                        d.listValues[count] = value;
                    }
                    if (d.valuesCount != null) {
                        if (count == 0) {
                            d.valuesCount.clear();
                        }
                        d.valuesCount.put(value, d.valuesCount.getOrDefault(value, 0) + 1);
                    }
                    sum += (long)value;
                    count++;
                } else if (count == 0) {
                    int value = (int)d.inputPixels[x + y * width] & (int)0xFFFF;
                    if (d.listValues != null && countAlt < d.listValues.length) {
                        d.listValues[countAlt] = value;
                    }
                    if (d.valuesCount != null) {
                        d.valuesCount.put(value, d.valuesCount.getOrDefault(value, 0) + 1);
                    }
                    sumAlt += (long)value;
                    countAlt++;
                }
                if (listNextSize + 4 >= d.listCurrX.length) {
                    continue;
                }
                if (x < width - 1 && d.map[(x + 1) + y * width] == i + 1) {
                    d.listNextX[listNextSize] = (short)(x + 1);
                    d.listNextY[listNextSize++] = (short)y;
                }
                if (x > 0 && d.map[(x - 1) + y * width] == i + 1) {
                    d.listNextX[listNextSize] = (short)(x - 1);
                    d.listNextY[listNextSize++] = (short)y;
                }
                if (y < height - 1 && d.map[x + (y + 1) * width] == i + 1) {
                    d.listNextX[listNextSize] = (short)x;
                    d.listNextY[listNextSize++] = (short)(y + 1);
                }
                if (y > 0 && d.map[x + (y - 1) * width] == i + 1) {
                    d.listNextX[listNextSize] = (short)x;
                    d.listNextY[listNextSize++] = (short)(y - 1);
                }
            }
        }

        float avg = 0.0f;
        if (count > 0) {
            avg = (float)sum / (float)count;
        } else if (countAlt > 0) {
            count = countAlt;
            avg = (float)sumAlt / (float)countAlt;
        }

        if (d.netType == NET_TYPE_MODE && d.valuesCount.size() > 0) {
            int bestValueCount = -1;
            for (Map.Entry<Integer, Integer> entry : d.valuesCount.entrySet()) {
                int v = entry.getKey();
                int vc = entry.getValue();
                if (vc > bestValueCount) {
                    d.listValues[0] = v;
                    count = 1;
                    bestValueCount = vc;
                } else if (vc == bestValueCount && count < d.listValues.length) {
                    d.listValues[count] = v;
                    count++;
                }
            }
        }

        if ((d.netType == NET_TYPE_MEDIAN || d.netType == NET_TYPE_MODE) && count > 0) {
            count = Math.min(count, d.listValues.length);
            Arrays.sort(d.listValues, 0, count);
            if (count % 2 == 0) {
                avg = (float)(d.listValues[count / 2 - 1] + d.listValues[count / 2]) / 2.0f;
            } else {
                avg = (float)d.listValues[count / 2];
            }
        }

        for (int i = 0; i < queueSize; i++) {
            int x = d.queueX[i];
            int y = d.queueY[i];
            float diff = Math.max(0.0f, (float)((int)d.inputPixels[x + y * width] & 0xFFFF) - avg);
            d.diff[x + y * width] = diff;
            d.diffMax = Math.max(d.diffMax, diff);
        }
    }

    private float[] calculateNet(float[] results, short[] inputPixels, short[] outputPixels, int netType) {
        int skipPixels = globalParams.skipPixels;
        int takePixels = globalParams.takePixels;
        CalculateNetData d = new CalculateNetData();
        d.netType = netType;
        d.results = results;
        d.diff = new float[width * height + 1];
        d.inputPixels = inputPixels;
        d.skipPixels = skipPixels;
        d.maxMark = skipPixels + takePixels;
        d.map = new byte[width * height];
        int size = width * height / 2;
        d.queueMask = 0;
        while (size > 0) {
            size >>= 1;
            d.queueMask = (d.queueMask << 1) | 1;
        }
        d.queueX = new short[d.queueMask + 1];
        d.queueY = new short[d.queueMask + 1];
        d.listCurrX = new short[width * height / 4];
        d.listCurrY = new short[width * height / 4];
        d.listNextX = new short[width * height / 4];
        d.listNextY = new short[width * height / 4];
        if ((netType == NET_TYPE_MEDIAN) || (netType == NET_TYPE_MODE)) {
            d.listValues = new int[width * height / 4];
        }
        if (netType == NET_TYPE_MODE) {
            d.valuesCount = new HashMap<Integer, Integer>();
        }
        Arrays.fill(d.map, (byte)127);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (results[x + y * width] >= 0 && d.map[x + y * width] == 127) {
                    calculateNetPointMarks(x, y, d);
                }
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (results[x + y * width] >= 0 && d.map[x + y * width] == 0) {
                    calculateNetPointValue(x, y, d);
                }
            }
        }
        d.diff[width * height] = d.diffMax;
        return d.diff;
    }

    private void outputToShortProcessor(ShortProcessor dst, ShortProcessor src, float[] results, int pointPixelOutput, boolean pointScaled, int backgroundPixelOutput,
            float minResult, float maxResult) {
        short[] outputPixels = (short[])dst.getPixels();
        short[] inputPixels = (short[])src.getPixels();
        short black = (short)(int)(dst.getMin() + 0.5);
        short white = (short)(int)(dst.getMax() + 0.5);
        float blackF = (float)dst.getMin();
        float whiteF = (float)dst.getMax();
        float rangeF = whiteF - blackF;
        float[] diff = null;
        float diffMax = 1.0f;
        if (pointPixelOutput == Params.POINT_OUTPUT_NET) {
            diff = calculateNet(results, inputPixels, outputPixels, NET_TYPE_AVERAGE);
            diffMax = Math.max(1.0f, diff[width * height]);
        } else if (pointPixelOutput == Params.POINT_OUTPUT_NET_MODE) {
            diff = calculateNet(results, inputPixels, outputPixels, NET_TYPE_MODE);
            diffMax = Math.max(1.0f, diff[width * height]);
        } else if (pointPixelOutput == Params.POINT_OUTPUT_NET_MEDIAN) {
            diff = calculateNet(results, inputPixels, outputPixels, NET_TYPE_MEDIAN);
            diffMax = Math.max(1.0f, diff[width * height]);
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (results[x + y * width] < 0) { // background
                    if (backgroundPixelOutput == Params.BG_OUTPUT_WHITE) {
                        outputPixels[x + y * width] = white;
                    } else if (backgroundPixelOutput == Params.BG_OUTPUT_BLACK) {
                        outputPixels[x + y * width] = black;
                    } else if (backgroundPixelOutput == Params.BG_OUTPUT_ORIGINAL) {
                        outputPixels[x + y * width] = inputPixels[x + y * width];
                    } else if (pointPixelOutput == Params.POINT_OUTPUT_RESULT) {
                        outputPixels[x + y * width] = (short) (blackF + rangeF * (results[x + y * width] - minResult) / (maxResult - minResult));
                    } else {
                        outputPixels[x + y * width] = (short) (blackF + rangeF * (1.0f - results[x + y * width] / minResult));
                    }
                } else { // point
                    if (pointPixelOutput == Params.POINT_OUTPUT_WHITE) {
                        outputPixels[x + y * width] = white;
                    } else if (pointPixelOutput == Params.POINT_OUTPUT_BLACK) {
                        outputPixels[x + y * width] = black;
                    } else if (pointPixelOutput == Params.POINT_OUTPUT_ORIGINAL) {
                        outputPixels[x + y * width] = inputPixels[x + y * width];
                    } else if ((pointPixelOutput == Params.POINT_OUTPUT_NET) || (pointPixelOutput == Params.POINT_OUTPUT_NET_MODE) || (pointPixelOutput == Params.POINT_OUTPUT_NET_MEDIAN)) {
                        if (pointScaled) {
                            outputPixels[x + y * width] = (short)(int)(black + diff[x + y * width] / diffMax * rangeF + 0.5);
                        } else {
                            outputPixels[x + y * width] = (short)(int)(black + diff[x + y * width]);                            
                        }
                    } else if (backgroundPixelOutput == Params.BG_OUTPUT_RESULT) {
                        outputPixels[x + y * width] = (short) (blackF + rangeF * (results[x + y * width] - minResult) / (maxResult - minResult));
                    } else {
                        outputPixels[x + y * width] = (short) (blackF + rangeF * results[x + y * width] / maxResult);
                    }
                }
            }
        }
    }

    private void outputToByteProcessor(ByteProcessor dst, ByteProcessor src, float[] results, int pointPixelOutput, boolean pointScaled, int backgroundPixelOutput,
            float minResult, float maxResult) throws Exception {
        throw new Exception("8-bit images are not implemented");
    }

    private void closeInteractiveTools() {
        logMethod();
        Roi.removeRoiListener(this);
        if (updateNoiseTask != null)
            updateNoiseTask.cancel();
        if (updatePointsTask != null)
            updatePointsTask.cancel();
        if (previewImage != null && previewImage.getWindow() != null)
            previewImage.getWindow().dispose();
        if (plot != null && plot.getImagePlus() != null && plot.getImagePlus().getWindow() != null)
            plot.getImagePlus().getWindow().dispose();
        if (profilePlot != null && profilePlot.getImagePlus() != null && profilePlot.getImagePlus().getWindow() != null)
            profilePlot.getImagePlus().getWindow().dispose();
        if (guiWindow != null)
            guiWindow.dispose();
        updateNoiseTask = null;
        updatePointsTask = null;
        previewImage = null;
        plot = null;
        profilePlot = null;
        guiWindow = null;
    }

    private void closed() {
        logMethod();
        closeInteractiveTools();
        pixels = null;
        indexUp = null;
        indexDown = null;
        weightUp = null;
        weightDown = null;
        hist = null;
        System.gc();
    }

    private int bordersToProcess;
    private int threadsToWait;
    final Lock lock = new ReentrantLock();
    final Condition allThreadsReady  = lock.newCondition(); 

    private void resetBorderToProcess(int threads) {
        lock.lock();
        try {
            bordersToProcess = 0xFF;
            threadsToWait = threads;
        } finally {
          lock.unlock();
        }
    }

    private Rectangle getNextBorderToProcess() {
        int index;
        lock.lock();
        try {
            if (threadsToWait > 0) {
                threadsToWait--;
                while (threadsToWait > 0) {
                    try {
                        allThreadsReady.await();
                    } catch (InterruptedException e) {}
                }
                allThreadsReady.signal();
            }
            if (bordersToProcess == 0) return null;
            index = 0;
            while ((bordersToProcess & (1 << index)) == 0) index++;
            bordersToProcess ^= (1 << index);
        } finally {
          lock.unlock();
        }
        int marginX2 = width - windowRadius;
        int marginY1 = windowRadius;
        int marginY2 = height - windowRadius;
        int midX = width / 2;
        int midY = height / 2;
        int width1 = midX;
        int width2 = width - midX;
        int height1 = midY - windowRadius;
        int height2 = height - midY - windowRadius;
        switch (index) {
            case 0: return new Rectangle(0, 0, width1, windowRadius);
            case 1: return new Rectangle(0, marginY1, windowRadius, height1);
            case 2: return new Rectangle(0, midY, windowRadius, height2);
            case 3: return new Rectangle(0, marginY2, width1, windowRadius);
            case 4: return new Rectangle(midX, 0, width2, windowRadius);
            case 5: return new Rectangle(marginX2, marginY1, windowRadius, height1);
            case 6: return new Rectangle(marginX2, midY, windowRadius, height2);
            case 7: return new Rectangle(midX, marginY2, width2, windowRadius);
            default: return null;
        }
    }

    private void makeHist() {
        logMethod();
        histSize = windowRadius + 1;
        if (hist == null || hist.length != width * height * histSize) {
            hist = new float[width * height * histSize];
        }
        int threadCount = Runtime.getRuntime().availableProcessors();
        while (threadCount > 0 && (height - 2 * windowRadius) / threadCount <= windowRadius) {
            threadCount--;
        }
        threadCount = Math.max(1, threadCount);
        resetBorderToProcess(threadCount);
        Runnable[] runnables = new Runnable[threadCount];
        Thread[] threads = new Thread[threadCount];
        for (int i = 1; i < threadCount; i++) {
            final int beginY = (height * i) / threadCount;
            final int endY = (height * (i + 1)) / threadCount;
            runnables[i] = new Runnable() {
                public void run() {
                    makeHistInner(beginY, endY);
                }
            };
            threads[i] = new Thread(runnables[i]);
            threads[i].start();
        }
        makeHistInner(0, height / threadCount);
        for (int i = 1; i < threadCount; i++) {
            try {
                threads[i].join();
            } catch (Exception ex) {
            }
        }
    }

    private static boolean useNativeHist = true;

    private void makeHistInner(int beginY, int endY) {
        if (useNativeHist) {
            try {
                makeHistNative(beginY, endY);
            } catch (Throwable ex) {
                synchronized (this) {
                    if (useNativeHist == true) {
                        NativeTools.logException(ex);
                        IJ.log("Cannot use a native library to accelerate computations.");
                        IJ.log("You may experience longer calculation times.");
                        IJ.log("Error details in: " + NativeTools.getLogPath());
                        useNativeHist = false;
                    }
                }
            }
        }

        if (!useNativeHist) {
            makeHistVM(beginY, endY);
        }

        Rectangle border = getNextBorderToProcess();
        while (border != null) {
            makeHistBorder(border.x, border.y, border.x + border.width, border.y + border.height);
            border = getNextBorderToProcess();
        }
    }

    private void makeHistNative(int beginY, int endY) {
        NativeTools.calHist(histSize, width, height, beginY, endY, indexDown, weightDown, indexUp, weightUp, pixels,
                hist, new float[width * histSize]);
    }

    private void makeHistVM(int beginY, int endY) {
        for (int centerY = beginY; centerY < endY; centerY++) {
            for (int centerX = 0; centerX < width; centerX++) {
                int startX = centerX - windowRadius;
                int startY = centerY - windowRadius;
                int offset = (centerX + centerY * width) * histSize;
                Arrays.fill(hist, offset, offset + histSize, 0.0f);
                if (startX < 0 || startY < 0 || startX + windowSize > width
                        || startY + windowSize > height) {
                    continue;
                }
                for (int x = 0; x < windowSize; x++) {
                    for (int y = 0; y < windowSize; y++) {
                        hist[offset + indexUp[x + y * windowSize]] += weightUp[x
                                + y * windowSize]
                                * pixels[startX + x + (startY + y) * width];
                        hist[offset + indexDown[x + y * windowSize]] += weightDown[x
                                + y * windowSize]
                                * pixels[startX + x + (startY + y) * width];
                    }
                }
            }
        }
    }

    private void makeHistBorder(int beginX, int beginY, int endX, int endY) {
        float[] weightSum = new float[histSize];
        for (int centerY = beginY; centerY < endY; centerY++) {
            for (int centerX = beginX; centerX < endX; centerX++) {
                int startX = centerX - windowRadius;
                int startY = centerY - windowRadius;
                int offset = (centerX + centerY * width) * histSize;
                Arrays.fill(hist, offset, offset + histSize, 0.0f);
                Arrays.fill(weightSum, 0.0f);
                for (int x = 0; x < windowSize; x++) {
                    for (int y = 0; y < windowSize; y++) {
                        if (startX + x >= 0 && startY + y >= 0 && startX + x < width && startY + y < height) {
                            float weight = weightUp[x + y * windowSize];
                            int index = indexUp[x + y * windowSize];
                            hist[offset + index] += weight * pixels[startX + x + (startY + y) * width];
                            weightSum[index] += weight;
                            weight = weightDown[x + y * windowSize];
                            index = indexDown[x + y * windowSize];
                            hist[offset + index] += weight * pixels[startX + x + (startY + y) * width];
                            weightSum[index] += weight;
                        }
                    }
                }
                for (int i = 0; i < histSize; i++) {
                    hist[offset + i] /= weightSum[i];
                }
            }
        }
    }

    private void makeWindow() {
        logMethod();
        windowRadius = globalParams.windowRadius;
        windowSize = 2 * windowRadius + 1;
        indexUp = new int[windowSize * windowSize];
        indexDown = new int[windowSize * windowSize];
        weightUp = new float[windowSize * windowSize];
        weightDown = new float[windowSize * windowSize];
        float[] radiusWeight = new float[windowSize];

        for (int x = 0; x < windowSize; x++) {
            for (int y = 0; y < windowSize; y++) {
                int dx = x - windowRadius;
                int dy = y - windowRadius;
                float d = (float) Math.sqrt((float) (dx * dx + dy * dy)) - 1.0f;
                if (d < 0.0f)
                    d = 0.0f;
                if (d >= (float) windowRadius - 0.05f)
                    continue;
                int up = (int) Math.ceil(d);
                int down = (int) d;
                indexUp[x + y * windowSize] = up;
                indexDown[x + y * windowSize] = down;
                if (up == down) {
                    weightUp[x + y * windowSize] = 1.0f;
                    radiusWeight[up] += 1.0f;
                } else {
                    weightUp[x + y * windowSize] = d - (float) down;
                    weightDown[x + y * windowSize] = (float) up - d;
                    radiusWeight[up] += d - (float) down;
                    radiusWeight[down] += (float) up - d;
                }
            }
        }
        for (int x = 0; x < windowSize; x++) {
            for (int y = 0; y < windowSize; y++) {
                weightUp[x + y * windowSize] /= radiusWeight[indexUp[x + y * windowSize]];
                weightDown[x + y * windowSize] /= radiusWeight[indexDown[x + y * windowSize]];
            }
        }
    }

    private Roi getPointsRoi() {
        if (!globalParams.selectNoise) {
            pointsRoi = previewImage.getRoi();
        }
        return pointsRoi;
    }

    private Roi getNoiseRoi() {
        if (globalParams.selectNoise) {
            noiseRoi = previewImage.getRoi();
        }
        return noiseRoi;
    }

    @Override
    public void roiModified(ImagePlus imp, int id) {
        if (imp == null)
            return;
        logMethod();
        if (imp == previewImage && globalParams.interactive && id != RoiListener.DELETED) {
            //IJ.log("ROI MODIFIED - " + id);
            if (globalParams.selectNoise) {
                if (updateNoiseTask != null) {
                    updateNoiseTask.cancel();
                }
                updateNoiseTask = Common.invokeLater(new Runnable() {
                    public void run() {
                        updateNoise();
                    }
                }, 100);
            } else {
                if (updatePointsTask != null) {
                    updatePointsTask.cancel();
                }
                updatePointsTask = Common.invokeLater(new Runnable() {
                    public void run() {
                        updatePoints(false);
                    }
                }, 500, 500);
                updatePoints(true);
            }
        } else {
            ImagePlus plotImage = plot.getImagePlus();
            if (imp == plotImage && id != RoiListener.DELETED) {
                updateLimitParams();
            }
        }
    }

    private void updateLimitParams() {
        logMethod();
        ImagePlus plotImage = plot.getImagePlus();
        Roi roi = plotImage.getRoi();
        if (roi == null || !(roi instanceof Line) || roi == ignoreRoi) {
            ignoreRoi = null;
            return;
        }
        ignoreRoi = null;
        Line line = (Line) roi;
        Polygon poly = line.getPoints();
        assert poly.npoints == 2;
        float xa = (float) plot.descaleX(poly.xpoints[0]);
        float ya = (float) plot.descaleY(poly.ypoints[0]);
        float xb = (float) plot.descaleX(poly.xpoints[1]);
        float yb = (float) plot.descaleY(poly.ypoints[1]);
        Params copy = globalParams.copy();
        copy.slope = (ya - yb) / (xa - xb);
        copy.yIntercept = ya - (ya - yb) / (xa - xb) * xa;
        globalParams.set(copy, false, this);
        updatePreview();
    }

    private void updateLimitLine() {
        if (plot == null)
            return;
        logMethod();
        float limitLineA = (float)globalParams.slope;
        float limitLineB = (float)globalParams.yIntercept;
        ImagePlus plotImage = plot.getImagePlus();
        Rectangle rect = plot.getDrawingFrame();
        float rx1 = (float) plot.descaleX(rect.x + 4);
        float ry1 = (float) plot.descaleY(rect.y + 4);
        float rx2 = (float) plot.descaleX(rect.x + rect.width - 3);
        float ry2 = (float) plot.descaleY(rect.y + rect.height - 3);
        float x1 = rx1;
        float y1 = limitLineA * x1 + limitLineB;
        float x2 = rx2;
        float y2 = limitLineA * x2 + limitLineB;
        if (y1 > ry1) {
            y1 = ry1;
            x1 = (y1 - limitLineB) / limitLineA;
        } else if (y1 < ry2) {
            y1 = ry2;
            x1 = (y1 - limitLineB) / limitLineA;
        }
        if (y2 > ry1) {
            y2 = ry1;
            x2 = (y2 - limitLineB) / limitLineA;
        } else if (y2 < ry2) {
            y2 = ry2;
            x2 = (y2 - limitLineB) / limitLineA;
        }
        if (x1 < x2) {
            double px1 = plot.scaleXtoPxl(x1);
            double py1 = plot.scaleYtoPxl(y1);
            double px2 = plot.scaleXtoPxl(x2);
            double py2 = plot.scaleYtoPxl(y2);
            ignoreRoi = Line.create(px1, py1, px2, py2);
        } else {
            ignoreRoi = new Roi(0, 0, 0, 0);
        }
        plotImage.setRoi(ignoreRoi);
    }

    private void updatePreview() {
        logMethod();
        int pointPixelOutput = globalParams.pointOutput;
        boolean pointScaled = globalParams.pointScaled;
        int backgroundPixelOutput = globalParams.bgOutput;
        this.outputToProcessor(previewProcessor, sourceImage.getProcessor(), pointPixelOutput, pointScaled, backgroundPixelOutput);
        previewImage.updateAndDraw();
    }

    private void updateNoise() {
        Roi roi = getNoiseRoi();
        if (roi == null) {
            return;
        }
        logMethod();
        int pointRadius = globalParams.pointRadius;
        int backgroundRadius = globalParams.backgroundStartRadius;
        Point[] points = roi.getContainedPoints();
        double[] xx = new double[points.length];
        double[] yy = new double[points.length];
        double[][] profileY = new double[Common.MAX_PROFILE_PLOTS][histSize];
        int plotIndex = 0;
        int profileIndex = 0;
        int profileStep = Math.max(1, points.length / Common.MAX_PROFILE_PLOTS);
        for (int pointIndex = 0; pointIndex < points.length; pointIndex++) {
            int x = points[pointIndex].x;
            int y = points[pointIndex].y;
            if (x < 0 || y < 0 || x >= width || y >= height) continue;
            int histOffset = histSize * (x + y * width);
            float firstValue = 0;
            for (int k = 0; k <= pointRadius; k++) {
                firstValue += hist[histOffset + k];
            }
            firstValue /= (float) (pointRadius + 1);
            float lastValue = 0;
            for (int k = backgroundRadius; k < histSize; k++) {
                lastValue += hist[histOffset + k];
            }
            lastValue /= (float) (histSize - backgroundRadius);
            yy[plotIndex] = firstValue;
            xx[plotIndex] = lastValue;
            if (plotIndex % profileStep == 0 && profileIndex < Common.MAX_PROFILE_PLOTS) {
                for (int k = 0; k < histSize; k++) {
                    profileY[profileIndex][k] = hist[histOffset + k];
                }
                profileIndex++;
            }
            plotIndex++;
        }
        if (plotIndex < xx.length) {
            xx = Arrays.copyOfRange(xx, 0, plotIndex);
            yy = Arrays.copyOfRange(yy, 0, plotIndex);
        }
        oldNoiseX = xx;
        oldNoiseY = yy;
        plot.setColor(Color.BLUE);
        plot.replace(0, "circle", xx, yy);
        plot.setLimitsToFit(true);
        updateLimitLine();
        updateProfilePlot(profileY, profileIndex, false);
    }

    private void updatePoints(boolean force) {
        Roi roi = getPointsRoi();
        if (roi == null) {
            return;
        }
        logMethod();
        int pointRadius = globalParams.pointRadius;
        int backgroundRadius = globalParams.backgroundStartRadius;
        Point[] points = roi.getContainedPoints();
        double[] xx = new double[points.length];
        double[] yy = new double[points.length];
        double[][] profileY = new double[Common.MAX_PROFILE_PLOTS][histSize];
        int plotIndex = 0;
        int profileIndex = 0;
        int profileStep = Math.max(1, points.length / Common.MAX_PROFILE_PLOTS);
        for (int pointIndex = 0; pointIndex < points.length; pointIndex++) {
            int x = points[pointIndex].x;
            int y = points[pointIndex].y;
            if (x < 0 || y < 0 || x >= width || y >= height) continue;
            int histOffset = histSize * (x + y * width);
            float firstValue = 0;
            for (int k = 0; k <= pointRadius; k++) {
                firstValue += hist[histOffset + k];
            }
            firstValue /= (float) (pointRadius + 1);
            float lastValue = 0;
            for (int k = backgroundRadius; k < histSize; k++) {
                lastValue += hist[histOffset + k];
            }
            lastValue /= (float) (histSize - backgroundRadius);
            yy[plotIndex] = firstValue;
            xx[plotIndex] = lastValue;
            if (plotIndex % profileStep == 0 && profileIndex < Common.MAX_PROFILE_PLOTS) {
                for (int k = 0; k < histSize; k++) {
                    profileY[profileIndex][k] = hist[histOffset + k];
                }
                profileIndex++;
            }
            plotIndex++;
        }
        if (plotIndex < xx.length) {
            xx = Arrays.copyOfRange(xx, 0, plotIndex);
            yy = Arrays.copyOfRange(yy, 0, plotIndex);
        }
        boolean update = true;
        if (oldPointsX != null && !force && oldPointsX.length == xx.length) {
            update = false;
            for (int i = 0; i < xx.length; i++) {
                if (xx[i] != oldPointsX[i] || yy[i] != oldPointsY[i]) {
                    update = true;
                    break;
                }
            }
        }
        oldPointsX = xx;
        oldPointsY = yy;
        if (update) {
            plot.restorePlotObjects();
            if (oldNoiseX != null) {
                plot.setColor(Color.BLUE);
                plot.replace(0, "circle", oldNoiseX, oldNoiseY);
            }
            plot.setColor(Color.RED);
            plot.add("circle", xx, yy);
            plot.setColor(Color.GRAY);
            plot.setFontSize(12);
            plotIndex = 0;
            for (int pointIndex = 0; pointIndex < points.length; pointIndex++) {
                int x = points[pointIndex].x;
                int y = points[pointIndex].y;
                if (x < 0 || y < 0 || x >= width || y >= height) continue;
                plot.addText("" + (pointIndex + 1), xx[plotIndex], yy[plotIndex]);
                plotIndex++;
            }
            plot.setLimitsToFit(true);
            updateLimitLine();
            updateProfilePlot(profileY, profileIndex, true);
        }
    }

    private void updateProfilePlot(double[][] profileY, int usedCount, boolean isPoint) {
        if (profilePlotWindow == null) {
            return;
        }
        usedCount = Math.min(Common.MAX_PROFILE_PLOTS, usedCount);
        double[] x = new double[histSize];
        for (int i = 0; i < histSize; i++) {
            x[i] = i;
        }
        int indexOffset = isPoint ? 3 + Common.MAX_PROFILE_PLOTS : 3;
        Color color = isPoint ? Color.RED : Color.BLUE;
        profilePlot.setColor(color, color);
        for (int i = 0; i < usedCount; i++) {
            profilePlot.replace(indexOffset + i, "connected circle", x, profileY[i]);
        }
        for (int i = usedCount; i < Common.MAX_PROFILE_PLOTS; i++) {
            profilePlot.replace(indexOffset + i, "connected circle", new double[0], new double[0]);
        }
        profilePlot.replace(1, "line", new double[0], new double[0]);
        profilePlot.setLimitsToFit(true);
        double[] limits = profilePlot.getLimits();
        double margin = (limits[3] - limits[2]) * 0.05;
        profilePlot.setColor(Color.ORANGE);
        int backgroundRadius = globalParams.backgroundStartRadius;
        profilePlot.replace(1, "line", new double[] { backgroundRadius - 0.5, backgroundRadius - 0.5 }, new double[] { limits[2] + margin, limits[3] - margin });
        profilePlot.setColor(Color.CYAN);
        int pointRadius = globalParams.pointRadius;
        profilePlot.replace(2, "line", new double[] { pointRadius + 0.5, pointRadius + 0.5, backgroundRadius + 0.5, backgroundRadius + 0.5 }, new double[] { limits[2] + margin, limits[3] - margin });
    }

    static int logMethodCounter = 1;

    private void logMethod() {
        if (true)
            return;
        StackTraceElement[] s = Thread.currentThread().getStackTrace();
        IJ.log(logMethodCounter + " METHOD: " + s[2].getMethodName());
        System.out.println(logMethodCounter + " METHOD: " + s[2].getMethodName());
        logMethodCounter++;
    }

    private void logStack() {
        if (true)
            return;
        StackTraceElement[] s = Thread.currentThread().getStackTrace();
        IJ.log("STACK TRACE:");
        System.out.println("STACK TRACE:");
        for (int i = 0; i < s.length; i++) {
            IJ.log("    " + s[i].toString());
            System.out.println("    " + s[i].toString());
        }
    }

    private static boolean updated(long fields, long mask) {
        return (fields & mask) != 0;
    }

    public static final long MAKE_HIST_MASK = Params.WINDOW_RADIUS;
    public static final long UPDATE_LIMIT_LINE_MASK = Params.WINDOW_RADIUS;

    @Override
    public void parametersChanged(long fields, boolean self) {
        logMethod();

        if ((fields & Params.INTERACTIVE) != 0 && globalParams.interactive && previewImage == null) {
            startInteractive();
            return;
        }

        if (!globalParams.interactive || self) {
            return;
        }

        if (updated(fields,Params.PROFILE_WINDOW)) {
            if (globalParams.profileWindow && profilePlotWindow == null) {
                profilePlotWindow = profilePlot.show();
            } else if (!globalParams.profileWindow && profilePlotWindow != null) {
                profilePlotWindow.setVisible(globalParams.profileWindow);
                profilePlotWindow = null;
            }
        }

        if (updated(fields, Params.WINDOW_RADIUS)) {
            makeWindow();
            makeHist();
        }

        if (updated(fields, Params.WINDOW_RADIUS | Params.POINT_RADIUS | Params.BACKGROUND_START_RADIUS
                | Params.PROFILE_WINDOW)) {
            updatePoints(true);
            updateNoise();
        } else if (updated(fields, Params.SLOPE | Params.Y_INTERCEPT)) {
            updateLimitLine();
        }

        if (updated(fields, Params.WINDOW_RADIUS | Params.POINT_RADIUS | Params.BACKGROUND_START_RADIUS
                | Params.SLOPE | Params.Y_INTERCEPT | Params.POINT_OUTPUT | Params.POINT_SCALED | Params.BG_OUTPUT
                | Params.SKIP_PIXELS | Params.TAKE_PIXELS)) {
            updatePreview();
        }

        // Points/noise selection
        if (updated(fields,Params.SELECT_NOISE)) {
            updateSelectionTool();
        }
    }

    @Override
    public final void eventTriggered(Params.EventData event) {
        if (event instanceof Params.EventAutoFit) {
            Params.EventAutoFit data = (Params.EventAutoFit)event;
            try {
                if (oldNoiseX == null || oldPointsX == null) throw new Input­Mismatch­Exception();
                if (oldNoiseX.length < 3 || oldPointsX.length < 3) throw new Input­Mismatch­Exception();
                LineFinding lf = new LineFinding();
                LineFinding.Point[] points = new LineFinding.Point[oldPointsX.length];
                for (int i = 0; i < points.length; i++) {
                    points[i] = new LineFinding.Point(oldPointsX[i], oldPointsY[i]);
                }
                LineFinding.Point[] noise = new LineFinding.Point[oldNoiseX.length];
                for (int i = 0; i < noise.length; i++) {
                    noise[i] = new LineFinding.Point(oldNoiseX[i], oldNoiseY[i]);
                }
                double weight = 0.0;
                switch (data.position) {
                    case Params.EventAutoFit.BELOW_POINTS:
                        weight = 0.0;
                        break;
                    case Params.EventAutoFit.MIDDLE:
                        weight = 0.5;
                        break;
                    case Params.EventAutoFit.ABOVE_NOISE:
                        weight = 1.0;
                        break;
                }
                double[] r = lf.calc(points, noise, weight);
                if (r == null) throw new InputMismatchException();
                Params copy = globalParams.copy();
                copy.slope = r[0];
                copy.yIntercept = r[1];
                globalParams.set(copy, false, this);
                updateLimitLine();
                updatePreview();
            } catch (InputMismatchException ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Missing data for automatic fitting.\nSelect at least three pixels for noise and points.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateSelectionTool() {
        if (globalParams.selectNoise) {
            IJ.setTool(Toolbar.OVAL);
            pointsRoi = previewImage.getRoi();
            previewImage.killRoi();
            if (noiseRoi != null) {
                previewImage.setRoi(noiseRoi);
            }
        } else {
            IJ.setTool(Toolbar.POINT);
            noiseRoi = previewImage.getRoi();
            previewImage.killRoi();
            if (pointsRoi != null) {
                previewImage.setRoi(pointsRoi);
            }
        }
    }

    private void startInteractive() {
        logMethod();

        // Read image
        ImageProcessor ip = sourceImage.getProcessor();
        ImageProcessor fp = ip.convertToFloat();
        if (ip == fp) {
            fp = fp.duplicate();
        }
        pixels = (float[]) fp.getPixels();
        width = fp.getWidth();
        height = fp.getHeight();

        // Create preview image
        ImageStack is = new ImageStack(width, height);
        previewProcessor = ip.duplicate();
        is.addSlice(previewProcessor);
        is.addSlice(previewProcessor.duplicate());
        previewImage = new ImagePlus("Preview", is);
        previewImage.show();
        ImageWindow win = previewImage.getWindow();
        ToolbarPanel toolbar = new ToolbarPanel(globalParams);
        win.add(toolbar);
        win.getCanvas().fitToWindow();
        win.pack();

        // Create plot
        plot = new Plot("Plot", "Neighbourhood", "Point");
        plot.setColor(Color.BLUE);
        plot.add("circle", new double[0], new double[0]);
        plot.savePlotObjects();
        plot.show();

        // Create Profile plot
        profilePlot = new Plot("Profile", "Distance from pixel", "Average value");
        profilePlot.add("line", new double[0], new double[0]);
        profilePlot.add("line", new double[0], new double[0]);
        profilePlot.add("line", new double[0], new double[0]);
        for (int i = 0; i < 2 * Common.MAX_PROFILE_PLOTS; i++) {
            profilePlot.add("connected circle", new double[0], new double[0]);
        }

        // Listen for ROI changes
        Roi.addRoiListener(this);

        makeWindow();
        makeHist();
        updatePoints(true);
        updateNoise();
        updatePreview();
        updateSelectionTool();
    }
}
