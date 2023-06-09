import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.*;

import javax.swing.SwingUtilities;

public class Points_Detector implements PlugIn, RoiListener, DialogListener {

	static final int MAX_PROFILE_PLOTS = 25;

	static final int PIXEL_OUTPUT_WHITE = 0;
	static final int PIXEL_OUTPUT_BLACK = 1;
	static final int PIXEL_OUTPUT_ORIGINAL = 2;
	static final int PIXEL_OUTPUT_RESULT = 3;
	static final int PIXEL_OUTPUT_NET = 4;
	static final int PIXEL_OUTPUT_NET_SCALED = 5;
	static final int PIXEL_OUTPUT_NET_MODE = 6;
	static final int PIXEL_OUTPUT_NET_SCALED_MODE = 7;
	static final int PIXEL_OUTPUT_NET_MEDIAN = 8;
	static final int PIXEL_OUTPUT_NET_SCALED_MEDIAN = 9;

	static final int NET_TYPE_AVERAGE = 0;
	static final int NET_TYPE_MODE = 1;
	static final int NET_TYPE_MEDIAN = 2;

	// GUI
	private String[] params;
	private static String lastParams;
	private ImagePlus sourceImage;
	private ImagePlus previewImage;
	private ImagePlus pointsImage;
	private ImagePlus noiseImage;
	private NonBlockingGenericDialog dialog;
	private ImageProcessor previewProcessor;
	private Timer timer;
	private TimerTask updateNoiseTask;
	private TimerTask updatePointsTask;

	// Plot
	private Plot plot;
	private Roi ignoreRoi;
	private double[] oldPointsX;
	private double[] oldPointsY;

	// Profile plot
	private Plot profilePlot;
	private PlotWindow profilePlotWindow;

	// Parameters
	private int windowRadius;
	private int pointRadius;
	private int backgroundRadius;
	private float limitLineA;
	private float limitLineB;

	// Image
	private int width;
	private int height;
	private float[] pixels;

	// Window
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
		if (lastParams == null) {
			lastParams = "20; 5; 6; 1; 0; 2; 3";
		}
		showDialog(false);
		if (getCheckbox(MANUAL_MODE_CHECK_BOX) && !dialog.wasCanceled()) {
			manualProcess();
		}
		if (!dialog.wasCanceled()) {
			imageProcess();
			lastParams = params[0];
		}
		closed();
	}

	private void imageProcess() {
		logMethod();
		ImagePlus outputImage;
		double[] p = parseParams();
		windowRadius = (int) (p[0] + 0.5);
		pointRadius = (int) (p[1] + 0.5);
		backgroundRadius = (int) (p[2] + 0.5);
		limitLineA = (float) p[3];
		limitLineB = (float) p[4];
		makeWindow();
		boolean allSlices = getCheckbox(ALL_SLICES_CHECK_BOX);
		int pointPixelOutput = getChoice(POINT_COLOR_CHOICE);
		int backgroundPixelOutput = getChoice(BACKGROUND_COLOR_CHOICE);
		boolean keepOriginalSlices = getCheckbox(KEEP_ORIGINAL_SLICES);
		ImageStack stack = sourceImage.getStack();
		if (allSlices && stack != null && stack.size() > 1) {
			ImageStack is = new ImageStack(sourceImage.getWidth(), sourceImage.getHeight());
			for (int i = 1; i <= stack.size(); i++) {
				ImageProcessor ip = stack.getProcessor(i);
				ProcessingResults r = processSingleImage(ip, pointPixelOutput, backgroundPixelOutput,
						keepOriginalSlices, i - 1, stack.size());
				is.addSlice(r.result);
				if (keepOriginalSlices)
					is.addSlice(r.original);
			}
			outputImage = new ImagePlus("Output", is);
		} else if (keepOriginalSlices) {
			ImageStack is = new ImageStack(sourceImage.getWidth(), sourceImage.getHeight());
			ProcessingResults r = processSingleImage(sourceImage.getProcessor(), pointPixelOutput, backgroundPixelOutput,
					keepOriginalSlices, 0, 1);
			is.addSlice(r.result);
			is.addSlice(r.original);
			outputImage = new ImagePlus("Output", is);
		} else {
			ImageProcessor r = processSingleImage(sourceImage.getProcessor(), pointPixelOutput, backgroundPixelOutput,
					keepOriginalSlices, 0, 1).result;
			outputImage = new ImagePlus("Output", r);
		}
		Utils.addProcessingInfo(sourceImage, outputImage, "Points Detector: " + params[0]);
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

	private ProcessingResults processSingleImage(ImageProcessor ip, int pointPixelOutput, int backgroundPixelOutput,
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
		outputToProcessor(dst, ip, pointPixelOutput, backgroundPixelOutput);
		return new ProcessingResults(dst, original);
	}

	private void outputToProcessor(ImageProcessor dst, ImageProcessor src, int pointPixelOutput, int backgroundPixelOutput) {
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
			outputToShortProcessor((ShortProcessor)dst, (ShortProcessor)src, results, pointPixelOutput, backgroundPixelOutput, minResult, maxResult);
		} else if (dst instanceof ByteProcessor) {
			outputToByteProcessor((ByteProcessor)dst, (ByteProcessor)src, results, pointPixelOutput, backgroundPixelOutput, minResult, maxResult);
		} else {
			IJ.error("Only 16-bit and 8-bit image format is supported.");
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
		int skipPixels = 2;
		int takePixels = 3;
		double[] p = parseParams();
		if (p != null) {
			skipPixels = Math.min(16, Math.max(0, (int)(p[5] + 0.5)));
			takePixels = Math.min(32, Math.max(0, (int)(p[6] + 0.5)));
		}
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

	private void outputToShortProcessor(ShortProcessor dst, ShortProcessor src, float[] results, int pointPixelOutput, int backgroundPixelOutput,
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
		if ((pointPixelOutput == PIXEL_OUTPUT_NET) || (pointPixelOutput == PIXEL_OUTPUT_NET_SCALED)) {
			diff = calculateNet(results, inputPixels, outputPixels, NET_TYPE_AVERAGE);
			diffMax = Math.max(1.0f, diff[width * height]);
		} else if ((pointPixelOutput == PIXEL_OUTPUT_NET_MODE) || (pointPixelOutput == PIXEL_OUTPUT_NET_SCALED_MODE)) {
			diff = calculateNet(results, inputPixels, outputPixels, NET_TYPE_MODE);
			diffMax = Math.max(1.0f, diff[width * height]);
		} else if ((pointPixelOutput == PIXEL_OUTPUT_NET_MEDIAN) || (pointPixelOutput == PIXEL_OUTPUT_NET_SCALED_MEDIAN)) {
			diff = calculateNet(results, inputPixels, outputPixels, NET_TYPE_MEDIAN);
			diffMax = Math.max(1.0f, diff[width * height]);
		}
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (results[x + y * width] < 0) { // background
					if (backgroundPixelOutput == PIXEL_OUTPUT_WHITE) {
						outputPixels[x + y * width] = white;
					} else if (backgroundPixelOutput == PIXEL_OUTPUT_BLACK) {
						outputPixels[x + y * width] = black;
					} else if (backgroundPixelOutput == PIXEL_OUTPUT_ORIGINAL) {
						outputPixels[x + y * width] = inputPixels[x + y * width];
					} else if (pointPixelOutput == PIXEL_OUTPUT_RESULT) {
						outputPixels[x + y * width] = (short) (blackF + rangeF * (results[x + y * width] - minResult) / (maxResult - minResult));
					} else {
						outputPixels[x + y * width] = (short) (blackF + rangeF * (1.0f - results[x + y * width] / minResult));
					}
				} else { // point
					if (pointPixelOutput == PIXEL_OUTPUT_WHITE) {
						outputPixels[x + y * width] = white;
					} else if (pointPixelOutput == PIXEL_OUTPUT_BLACK) {
						outputPixels[x + y * width] = black;
					} else if (pointPixelOutput == PIXEL_OUTPUT_ORIGINAL) {
						outputPixels[x + y * width] = inputPixels[x + y * width];
					} else if ((pointPixelOutput == PIXEL_OUTPUT_NET) || (pointPixelOutput == PIXEL_OUTPUT_NET_MODE) || (pointPixelOutput == PIXEL_OUTPUT_NET_MEDIAN)) {
						outputPixels[x + y * width] = (short)(int)(black + diff[x + y * width]);
					} else if ((pointPixelOutput == PIXEL_OUTPUT_NET_SCALED) || (pointPixelOutput == PIXEL_OUTPUT_NET_SCALED_MODE) || (pointPixelOutput == PIXEL_OUTPUT_NET_SCALED_MEDIAN)) {
						outputPixels[x + y * width] = (short)(int)(black + diff[x + y * width] / diffMax * rangeF + 0.5);
					} else if (backgroundPixelOutput == PIXEL_OUTPUT_RESULT) {
						outputPixels[x + y * width] = (short) (blackF + rangeF * (results[x + y * width] - minResult) / (maxResult - minResult));
					} else {
						outputPixels[x + y * width] = (short) (blackF + rangeF * results[x + y * width] / maxResult);
					}
				}
			}
		}
	}

	private void outputToByteProcessor(ByteProcessor dst, ByteProcessor src, float[] results, int pointPixelOutput, int backgroundPixelOutput,
			float minResult, float maxResult) {
		byte[] outputPixels = (byte[])dst.getPixels();
		byte[] inputPixels = (byte[])src.getPixels();
		byte black = (byte)(int)(dst.getMin() + 0.5);
		byte white = (byte)(int)(dst.getMax() + 0.5);
		float blackF = (float)dst.getMin();
		float whiteF = (float)dst.getMax();
		float rangeF = whiteF - blackF;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (results[x + y * width] < 0) { // background
					if (backgroundPixelOutput == PIXEL_OUTPUT_WHITE) {
						outputPixels[x + y * width] = white;
					} else if (backgroundPixelOutput == PIXEL_OUTPUT_BLACK) {
						outputPixels[x + y * width] = black;
					} else if (backgroundPixelOutput == PIXEL_OUTPUT_ORIGINAL) {
						outputPixels[x + y * width] = inputPixels[x + y * width];
					} else if (pointPixelOutput == PIXEL_OUTPUT_RESULT) {
						outputPixels[x + y * width] = (byte) (blackF + rangeF * (results[x + y * width] - minResult) / (maxResult - minResult));
					} else {
						outputPixels[x + y * width] = (byte) (blackF + rangeF * (1.0f - results[x + y * width] / minResult));
					}
				} else { // point
					if (pointPixelOutput == PIXEL_OUTPUT_WHITE) {
						outputPixels[x + y * width] = white;
					} else if (pointPixelOutput == PIXEL_OUTPUT_BLACK) {
						outputPixels[x + y * width] = black;
					} else if (pointPixelOutput == PIXEL_OUTPUT_ORIGINAL) {
						outputPixels[x + y * width] = inputPixels[x + y * width];
					} else if (backgroundPixelOutput == PIXEL_OUTPUT_RESULT) {
						outputPixels[x + y * width] = (byte) (blackF + rangeF * (results[x + y * width] - minResult) / (maxResult - minResult));
					} else {
						outputPixels[x + y * width] = (byte) (blackF + rangeF * results[x + y * width] / maxResult);
					}

				}
			}
		}
	}

	private void manualProcess() {
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

		// Create helper image windows
		pointsImage = new ImagePlus("Points selection", ip.duplicate());
		pointsImage.show();
		noiseImage = new ImagePlus("Noise selection", ip.duplicate());
		noiseImage.show();

		// Create preview image
		ImageStack is = new ImageStack(width, height);
		previewProcessor = ip.duplicate();
		is.addSlice(previewProcessor);
		is.addSlice(previewProcessor.duplicate());
		previewImage = new ImagePlus("Preview", is);
		previewImage.show();

		// Create plot
		plot = new Plot("Plot", "Neighbourhood", "Point");
		plot.setColor(Color.BLUE);
		plot.add("circle", new double[0], new double[0]);
		plot.setColor(Color.RED);
		plot.add("circle", new double[0], new double[0]);
		plot.show();

		// Create Profile plot
		profilePlot = new Plot("Profile", "Distance from pixel", "Average value");
		profilePlot.add("line", new double[0], new double[0]);
		profilePlot.add("line", new double[0], new double[0]);
		profilePlot.add("line", new double[0], new double[0]);
		for (int i = 0; i < 2 * MAX_PROFILE_PLOTS; i++) {
			profilePlot.add("connected circle", new double[0], new double[0]);
		}
		if (getCheckbox(PROFILE_WINDOW_CHECK_BOX)) {
			profilePlotWindow = profilePlot.show();
		}

		// Listen for ROI changes
		Roi.addRoiListener(this);

		do {
			double[] p = parseParams();
			windowRadius = (int) (p[0] + 0.5);
			pointRadius = (int) (p[1] + 0.5);
			backgroundRadius = (int) (p[2] + 0.5);
			limitLineA = (float) p[3];
			limitLineB = (float) p[4];
			makeWindow();
			makeHist();
			updatePoints(true);
			updateNoise();
			updatePreview();
			showDialog(true);
		} while (getCheckbox(MANUAL_MODE_CHECK_BOX) && !dialog.wasCanceled());
	}

	static final int ALL_SLICES_CHECK_BOX = 0;
	static final int KEEP_ORIGINAL_SLICES = 1;
	static final int MANUAL_MODE_CHECK_BOX = 2;
	static final int PROFILE_WINDOW_CHECK_BOX = 3;
	static final int POINT_COLOR_CHOICE = 0;
	static final int BACKGROUND_COLOR_CHOICE = 1;

	private boolean getCheckbox(int id) {
		Vector all = dialog.getCheckboxes();
		if (id >= all.size()) {
			return false;
		}
		return ((Checkbox)all.get(id)).getState();
	}

	private int getChoice(int id) {
		return ((Choice) dialog.getChoices().get(id)).getSelectedIndex();
	}

	private void showDialog(boolean manual) {
		logMethod();
		boolean[] initCheckBox = new boolean[] { false, false, false, false };
		int[] initChoice = new int[] { PIXEL_OUTPUT_WHITE, PIXEL_OUTPUT_BLACK };
		if (dialog != null) {
			initCheckBox[0] = getCheckbox(0);
			initCheckBox[1] = getCheckbox(1);
			initCheckBox[2] = getCheckbox(2);
			initCheckBox[3] = getCheckbox(3);
			initChoice[0] = ((Choice) dialog.getChoices().get(0)).getSelectedIndex();
			initChoice[1] = ((Choice) dialog.getChoices().get(1)).getSelectedIndex();
			dialog.dispose();
		}
		dialog = new NonBlockingGenericDialog("Parameters");
		String initialValue;
		if (params == null) {
			params = new String[] { "[!]", "", "", "", "", "", "", "" };
			initialValue = lastParams;
		} else {
			initialValue = params[0];
		}
		String[] choiceTexts = new String[10];
		choiceTexts[PIXEL_OUTPUT_WHITE] = "White";
		choiceTexts[PIXEL_OUTPUT_BLACK] = "Black";
		choiceTexts[PIXEL_OUTPUT_ORIGINAL] = "Original";
		choiceTexts[PIXEL_OUTPUT_RESULT] = "Degree of matching";
		choiceTexts[PIXEL_OUTPUT_NET] = "Net signal (average)";
		choiceTexts[PIXEL_OUTPUT_NET_SCALED] = "Net signal scaled (average)";
		choiceTexts[PIXEL_OUTPUT_NET_MODE] = "Net signal (mode)";
		choiceTexts[PIXEL_OUTPUT_NET_SCALED_MODE] = "Net signal scaled (mode)";
		choiceTexts[PIXEL_OUTPUT_NET_MEDIAN] = "Net signal (median)";
		choiceTexts[PIXEL_OUTPUT_NET_SCALED_MEDIAN] = "Net signal scaled (median)";
		dialog.addStringField("Parameters (W; R; S; A; B; SP; TP)", initialValue, 30);
		dialog.addStringField("Scanning window radius [pixels] (W)" + (manual ? " *" : ""), params[1], 10);
		dialog.addStringField("Point radius [pixels] (R)", params[2], 10);
		dialog.addStringField("Background start radius [pixels] (S)", params[3], 10);
		dialog.addStringField("Slope (A)", params[4], 10);
		dialog.addStringField("Y-intercept (B)", params[5], 10);
		dialog.addStringField("Skip pixels (SP)", params[6], 10);
		dialog.addStringField("Take pixels (TP)", params[7], 10);
		dialog.addChoice("Points color", choiceTexts, choiceTexts[initChoice[0]]);
		dialog.addChoice("Background color", Arrays.copyOf(choiceTexts, 4), choiceTexts[initChoice[1]]);
		dialog.addCheckbox("All slices", initCheckBox[0]);
		dialog.addCheckbox("Keep original slices", initCheckBox[1]);
		if (manual) {
			dialog.addCheckbox("Manual mode - uncheck to exit manual mode", initCheckBox[2]);
			dialog.addCheckbox("Show profile plot", initCheckBox[3]);
		} else {
			dialog.addCheckbox("Manual mode", initCheckBox[2]);
		}
		if (manual) {
			dialog.addMessage("* - changing window radius requires pressing 'OK'");
		}
		dialog.addHelp(helpText);
		updateDialog();
		dialog.addDialogListener(this);
		dialog.showDialog();
		Vector<TextField> vect = dialog.getStringFields();
		params[0] = vect.get(0).getText();
		params[1] = vect.get(1).getText();
		params[2] = vect.get(2).getText();
		params[3] = vect.get(3).getText();
		params[4] = vect.get(4).getText();
		params[5] = vect.get(5).getText();
		params[6] = vect.get(6).getText();
		params[7] = vect.get(7).getText();
	}

	private void closed() {
		logMethod();
		Roi.removeRoiListener(this);
		if (previewImage != null && previewImage.getWindow() != null)
			previewImage.getWindow().dispose();
		if (pointsImage != null && pointsImage.getWindow() != null)
			pointsImage.getWindow().dispose();
		if (noiseImage != null && noiseImage.getWindow() != null)
			noiseImage.getWindow().dispose();
		if (plot != null && plot.getImagePlus() != null && plot.getImagePlus().getWindow() != null)
			plot.getImagePlus().getWindow().dispose();
		if (profilePlot != null && profilePlot.getImagePlus() != null && profilePlot.getImagePlus().getWindow() != null)
			profilePlot.getImagePlus().getWindow().dispose();
		if (dialog != null)
			dialog.dispose();
		if (timer != null)
			timer.cancel();
		previewImage = null;
		pointsImage = null;
		noiseImage = null;
		plot = null;
		profilePlot = null;
		dialog = null;
		timer = null;
		pixels = null;
		indexUp = null;
		indexDown = null;
		weightUp = null;
		weightDown = null;
		hist = null;
		params = null;
		System.gc();
	}

	private void makeHist() {
		logMethod();
		histSize = windowRadius + 1;
		if (hist == null || hist.length != width * height * histSize) {
			hist = new float[width * height * histSize];
		}
		int threadCount = Runtime.getRuntime().availableProcessors();
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
				useNativeHist = false;
			}
		}

		if (!useNativeHist) {
			makeHistVM(beginY, endY);
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

	private void makeWindow() {
		logMethod();
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

	@Override
	public void roiModified(ImagePlus imp, int id) {
		if (imp == null)
			return;
		logMethod();
		if (imp == pointsImage) {
			if (updatePointsTask != null)
				updatePointsTask.cancel();
			updatePointsTask = invokeLater(new Runnable() {
				public void run() {
					updatePoints(false);
				}
			}, 500, 500);
			updatePoints(true);
		} else if (imp == noiseImage) {
			if (updateNoiseTask != null)
				updateNoiseTask.cancel();
			updateNoiseTask = invokeLater(new Runnable() {
				public void run() {
					updateNoise();
				}
			}, 100, -1);
		} else {
			ImagePlus plotImage = plot.getImagePlus();
			if (imp == plotImage && id != RoiListener.DELETED) {
				updateLimit();
			}
		}
	}

	private TimerTask invokeLater(Runnable doRun, int ms, int period) {
		TimerTask tt = new TimerTask() {
			public void run() {
				SwingUtilities.invokeLater(doRun);
			}
		};
		if (timer == null)
			timer = new Timer();
		if (period < 0) {
			timer.schedule(tt, ms);
		} else {
			timer.schedule(tt, ms, period);
		}
		return tt;
	}

	private void updateLimit() {
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
		limitLineA = (ya - yb) / (xa - xb);
		limitLineB = ya - (ya - yb) / (xa - xb) * xa;
		Vector<TextField> vect = dialog.getStringFields();
		params[4] = toShortNumber(limitLineA);
		params[5] = toShortNumber(limitLineB);
		params[0] = joinParams();
		vect.get(0).setText(params[0]);
		vect.get(4).setText(params[4]);
		vect.get(5).setText(params[5]);
		updatePreview();
	}

	private void updateLimitLine() {
		if (plot == null)
			return;
		logMethod();
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

	private static String toShortNumber(double number) {
		int num = (int) Math.ceil(Math.log10(Math.abs(number * 1.000001)));
		if (num > 5)
			num = 5;
		if (num < -10)
			return String.format("%e", number);
		num = 5 - num;
		String res = String.format("%.0" + num + "f", number);
		if (res.contains(".")) {
			while (res.endsWith("0"))
				res = res.substring(0, res.length() - 1);
			if (res.endsWith("."))
				res = res.substring(0, res.length() - 1);
		}
		return res;
	}

	private void updatePreview() {
		logMethod();
		int pointPixelOutput = getChoice(POINT_COLOR_CHOICE);
		int backgroundPixelOutput = getChoice(BACKGROUND_COLOR_CHOICE);
		this.outputToProcessor(previewProcessor, sourceImage.getProcessor(), pointPixelOutput, backgroundPixelOutput);
		previewImage.updateAndDraw();
	}

	private void updateNoise() {
		Roi roi = noiseImage.getRoi();
		if (roi == null) {
			return;
		}
		logMethod();
		Point[] points = roi.getContainedPoints();
		double[] xx = new double[points.length];
		double[] yy = new double[points.length];
		double[][] profileY = new double[MAX_PROFILE_PLOTS][histSize];
		for (int i = 0; i < points.length; i++) {
			int histOffset = histSize * (points[i].x + points[i].y * width);
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
			yy[i] = firstValue;
			xx[i] = lastValue;
			if (i < MAX_PROFILE_PLOTS) {
				for (int k = 0; k < histSize; k++) {
					profileY[i][k] = hist[histOffset + k];
				}
			}
		}
		plot.setColor(Color.BLUE);
		plot.replace(0, "circle", xx, yy);
		plot.setLimitsToFit(true);
		updateLimitLine();
		updateProfilePlot(profileY, points.length, false);
	}

	private void updatePoints(boolean force) {
		Roi roi = pointsImage.getRoi();
		if (roi == null) {
			return;
		}
		logMethod();
		Point[] points = roi.getContainedPoints();
		double[] xx = new double[points.length];
		double[] yy = new double[points.length];
		double[][] profileY = new double[MAX_PROFILE_PLOTS][histSize];
		for (int i = 0; i < points.length; i++) {
			int histOffset = histSize * (points[i].x + points[i].y * width);
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
			yy[i] = firstValue;
			xx[i] = lastValue;
			if (i < MAX_PROFILE_PLOTS) {
				for (int k = 0; k < histSize; k++) {
					profileY[i][k] = hist[histOffset + k];
				}
			}
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
			plot.setColor(Color.RED);
			plot.replace(1, "circle", xx, yy);
			plot.setLimitsToFit(true);
			updateLimitLine();
			updateProfilePlot(profileY, points.length, true);
		}
	}

	private void updateProfilePlot(double[][] profileY, int usedCount, boolean isPoint) {
		if (profilePlotWindow == null) {
			return;
		}
		usedCount = Math.min(MAX_PROFILE_PLOTS, usedCount);
		double[] x = new double[histSize];
		for (int i = 0; i < histSize; i++) {
			x[i] = i;
		}
		int indexOffset = isPoint ? 3 + MAX_PROFILE_PLOTS : 3;
		Color color = isPoint ? Color.RED : Color.BLUE;
		profilePlot.setColor(color, color);
		for (int i = 0; i < usedCount; i++) {
			profilePlot.replace(indexOffset + i, "connected circle", x, profileY[i]);
		}
		for (int i = usedCount; i < MAX_PROFILE_PLOTS; i++) {
			profilePlot.replace(indexOffset + i, "connected circle", new double[0], new double[0]);
		}
		profilePlot.replace(1, "line", new double[0], new double[0]);
		profilePlot.setLimitsToFit(true);
		double[] limits = profilePlot.getLimits();
		double margin = (limits[3] - limits[2]) * 0.05;
		profilePlot.setColor(Color.ORANGE);
		profilePlot.replace(1, "line", new double[] { backgroundRadius - 0.5, backgroundRadius - 0.5 }, new double[] { limits[2] + margin, limits[3] - margin });
		profilePlot.setColor(Color.CYAN);
		profilePlot.replace(2, "line", new double[] { pointRadius + 0.5, pointRadius + 0.5, backgroundRadius + 0.5, backgroundRadius + 0.5 }, new double[] { limits[2] + margin, limits[3] - margin });
	}

	@Override
	public boolean dialogItemChanged(GenericDialog arg0, AWTEvent arg1) {
		logMethod();
		return updateDialog();
	}

	private boolean updateDialog() {
		logMethod();
		Vector<TextField> vect = dialog.getStringFields();
		String p = vect.get(0).getText();
		String w = vect.get(1).getText();
		String r = vect.get(2).getText();
		String s = vect.get(3).getText();
		String a = vect.get(4).getText();
		String b = vect.get(5).getText();
		String sp = vect.get(6).getText();
		String tp = vect.get(7).getText();
		boolean updatePointRadius = false;
		boolean updatePlotRoi = false;
		if (!p.equals(params[0])) {
			params[0] = p;
			String[] parts = splitParams(p);
			if (parts == null) {
				return false;
			}
			updatePointRadius = !params[2].equals(parts[1]) || !params[3].equals(parts[2]);
			updatePlotRoi = !params[4].equals(parts[3]) || !params[5].equals(parts[4]);
			params[1] = parts[0];
			params[2] = parts[1];
			params[3] = parts[2];
			params[4] = parts[3];
			params[5] = parts[4];
			params[6] = parts[5];
			params[7] = parts[6];
			vect.get(1).setText(params[1]);
			vect.get(2).setText(params[2]);
			vect.get(3).setText(params[3]);
			vect.get(4).setText(params[4]);
			vect.get(5).setText(params[5]);
			vect.get(6).setText(params[6]);
			vect.get(7).setText(params[7]);
		} else if (!(w.equals(params[1]) && r.equals(params[2]) && s.equals(params[3])
				&& a.equals(params[4]) && b.equals(params[5]) && sp.equals(params[6])
				&& tp.equals(params[7]))) {
			updatePointRadius = !params[2].equals(r) || !params[3].equals(s);
			updatePlotRoi = !params[4].equals(a) || !params[5].equals(b);
			params[1] = w;
			params[2] = r;
			params[3] = s;
			params[4] = a;
			params[5] = b;
			params[6] = sp;
			params[7] = tp;
			params[0] = joinParams();
			vect.get(0).setText(params[0]);
		}
		if (dialog != null && plot != null) {
			boolean profileWindowVisible = getCheckbox(PROFILE_WINDOW_CHECK_BOX);
			if (profileWindowVisible && profilePlotWindow == null) {
				profilePlotWindow = profilePlot.show();
				updatePoints(true);
				updateNoise();
			} else if (!profileWindowVisible && profilePlotWindow != null) {
				profilePlotWindow.setVisible(false);
				profilePlotWindow = null;
			}
		}
		double[] parsed = parseParams();
		if (parsed != null && plot != null) {
			if (updatePlotRoi) {
				limitLineA = (float) parsed[3];
				limitLineB = (float) parsed[4];
				updateLimitLine();
			}
			if (updatePointRadius) {
				pointRadius = (int) (parsed[1] + 0.5);
				backgroundRadius = (int) (parsed[2] + 0.5);
				updatePoints(true);
				updateNoise();
			}
			updatePreview();
		}
		return parsed != null;
	}

	private String joinParams() {
		return params[1] + "; " + params[2] + "; " + params[3] + "; " + params[4] + "; " + params[5] + "; " + params[6] + "; " + params[7];
	}

	private double[] parseParams() {
		try {
			double[] res = new double[7];
			res[0] = Integer.parseInt(params[1].replace(",", ".").trim());
			res[1] = Integer.parseInt(params[2].replace(",", ".").trim());
			res[2] = Integer.parseInt(params[3].replace(",", ".").trim());
			res[3] = Double.parseDouble(params[4].replace(",", ".").trim());
			res[4] = Double.parseDouble(params[5].replace(",", ".").trim());
			res[5] = Integer.parseInt(params[6].replace(",", ".").trim());
			res[6] = Integer.parseInt(params[7].replace(",", ".").trim());
			if (res[0] < 4.5 || res[0] > 150 || res[1] < 1.5 || res[1] > res[0] * 0.75
					|| res[2] < 1.5 || res[2] > res[0] - 0.5) {
				return null;
			}
			return res;
		} catch (Exception ex) {
			return null;
		}
	}

	private String[] splitParams(String p) {
		String[] arr = p.split(";");
		if (arr.length == 1) {
			arr = p.split(",");
		}
		if (arr.length != 7) {
			return null;
		}
		for (int i = 0; i < arr.length; i++) {
			arr[i] = arr[i].replace(",", ".").trim();
		}
		return arr;
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

	static private String helpText = "https://kildot.github.io/points-detector/help/";
}
