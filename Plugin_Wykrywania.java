import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.util.*;

public class Plugin_Wykrywania implements PlugIn, RoiListener {

	// GUI
	ImagePlus previewImage;
	ImagePlus pointsImage;
	ImagePlus noiseImage;
	NonBlockingGenericDialog dialog;
	ByteProcessor previewProcessor;

	// Plot
	Plot plot;
	ArrayList<Double> plotValidX;
	ArrayList<Double> plotValidY;

	// Parameters
	int windowRadius;
	int pointRadius;
	float limitLineA;
	float limitLineB;

	// Image
	int width;
	int height;
	float[] pixels;

	// Window
	int windowSize;
	int[] indexUp;
	int[] indexDown;
	float[] weightUp;
	float[] weightDown;

	// Histograms
	int histSize;
	float[] hist;

	@Override
	public void run(String arg) {
		windowRadius = 20;
		pointRadius = 5;
		limitLineA = 1;
		limitLineB = 0;
	
		// Read image
		ImagePlus imp = IJ.getImage();
		if (imp == null) {
			IJ.error("Otwórz obrazek");
		}
		ImageProcessor ip = imp.getProcessor();
		ImageProcessor fp = ip.convertToFloat();
		if (ip == fp) {
			fp = fp.duplicate();
		}
		pixels = (float[]) fp.getPixels();
		width = fp.getWidth();
		height = fp.getHeight();

		// Create helper image windows
		pointsImage = new ImagePlus("Punkty", fp.duplicate());
		pointsImage.show();
		noiseImage = new ImagePlus("Szum", fp.duplicate());
		noiseImage.show();

		// Create preview image
		ImageStack is = new ImageStack(width, height);
		previewProcessor = (ByteProcessor)fp.convertToByte(true);
		is.addSlice(previewProcessor);
		is.addSlice(previewProcessor.duplicate());
		previewImage = new ImagePlus("Podglad", is);
		previewImage.show();

		// Create plot
		plot = new Plot("Wykres", "Otoczenie", "Srodek");
		plot.setColor(Color.RED);
		plot.add("circle", new double[0], new double[0]);
		plot.setColor(Color.BLUE);
		plot.add("circle", new double[0], new double[0]);
		plot.show();

		// Create main dialog window
		dialog = new NonBlockingGenericDialog("Parametry");
		dialog.addNumericField("R", 5, 0);
		dialog.addNumericField("A", 1);
		dialog.addNumericField("B", 0);
		// dialog.addButton("Podglad", new ActionListener() {
		// @Override
		// public void actionPerformed(ActionEvent evt) {
		// previewButtonPressed();
		// }
		// });*/

		makeWindow();
		makeHist();

		Roi.addRoiListener(this);

		dialog.showDialog();

		closed();
	}

	public void previewButtonPressed() {

	}

	public void closed() {
		Roi.removeRoiListener(this);
		previewImage.getWindow().dispose();
		pointsImage.getWindow().dispose();
		noiseImage.getWindow().dispose();
		plot.getImagePlus().getWindow().dispose();
		dialog.dispose();
		previewImage = null;
		pointsImage = null;
		noiseImage = null;
		plot = null;
		dialog = null;
		pixels = null;
		indexUp = null;
		indexDown = null;
		weightUp = null;
		weightDown = null;
		hist = null;
		System.gc();
	}

	private void makeHist() {
		histSize = windowRadius + 1;
		if (hist == null) {
			hist = new float[width * height * histSize];
		}
		for (int centerY = 0; centerY < height; centerY++) {
			if (centerY % 10 == 9) {
				IJ.showProgress(centerY, height);
				IJ.log("Image line " + (1 + centerY) + " of " + height);
			}
			for (int centerX = 0; centerX < width; centerX++) {
				int startX = centerX - windowRadius;
				int startY = centerY - windowRadius;
				int offset = (centerX + centerY * width) * histSize;
				Arrays.fill(hist, offset, offset + histSize, 0.0f);
				if (startX < 0 || startY < 0 || startX + windowSize > width || startY + windowSize > height) {
					continue;
				}
				for (int x = 0; x < windowSize; x++) {
					for (int y = 0; y < windowSize; y++) {
						hist[offset + indexUp[x + y * windowSize]] += weightUp[x + y * windowSize]
								* pixels[startX + x + (startY + y) * width];
						hist[offset + indexDown[x + y * windowSize]] += weightDown[x + y * windowSize]
								* pixels[startX + x + (startY + y) * width];
					}
				}
			}
		}
		IJ.log("All image histograms done.");
	}

	private void makeWindow() {
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
				if (d >= (float)windowRadius - 0.05f)
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
		/*
		 * previewImage(weightUp, windowSize, windowSize);
		 * previewImage(weightDown, windowSize, windowSize);
		 * float[] sum = new float[windowSize * windowSize];
		 * for (int x = 0; x < windowSize; x++) {
		 * for (int y = 0; y < windowSize; y++) {
		 * sum[x + y * windowSize] = weightUp[x + y * windowSize] + weightDown[x + y *
		 * windowSize];
		 * }
		 * }
		 * previewImage(sum, windowSize, windowSize);
		 */
	}

	private void previewImage(float[] arr, int width, int height) {
		float[] img = new float[arr.length];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < width; y++) {
				img[x + width * y] = arr[x + y * width];
			}
		}
		FloatProcessor fp2 = new FloatProcessor(width, height, img, null);
		ImagePlus image = new ImagePlus("Preview", fp2);
		image.show();
	}

	@Override
	public void roiModified(ImagePlus imp, int id) {
		if (imp == null) return;
		if (imp == pointsImage) {
			updatePoints();
		} else if (imp == noiseImage) {
			updateNoise();
		} else {
			ImagePlus plotImage = plot.getImagePlus();
			if (imp == plotImage) {
				updateLimit(plotImage);
			}
		}
	}

	private void updateLimit(ImagePlus plotImage) {
		Roi roi = plotImage.getRoi();
		if (roi == null || !(roi instanceof Line)) {
			return;
		}
		Line line = (Line)roi;
		Polygon poly = line.getPoints();
		assert poly.npoints == 2;
		float xa = (float)plot.descaleX(poly.xpoints[0]);
		float ya = (float)plot.descaleY(poly.ypoints[0]);
		float xb = (float)plot.descaleX(poly.xpoints[1]);
		float yb = (float)plot.descaleY(poly.ypoints[1]);
		limitLineA = (ya - yb) / (xa - xb);
		limitLineB = ya - (ya - yb) / (xa - xb) * xa;
		IJ.log("x = " + limitLineA + " * x + " + limitLineB);
		updatePreview();
	}

	private void updatePreview() {
		byte[] pixels = (byte[])previewProcessor.getPixels();
		int half = (histSize + 1) / 2;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int histOffset = histSize * (x + y * width);
				float firstValue = 0;
				for (int k = 0; k < pointRadius; k++) {
					firstValue += hist[histOffset + k];
				}
				firstValue /= (float)pointRadius;
				float lastValue = 0;
				for (int k = half; k < histSize; k++) {
					lastValue += hist[histOffset + k];
				}
				lastValue /= (float)(histSize - half);
				float yy = firstValue;
				float xx = lastValue;
				pixels[x + y * width] =  yy < limitLineA * xx + limitLineB ? (byte)255 : (byte)0;
			}
		}
		previewImage.updateAndDraw();
	}

	private void updateNoise() {
		Roi roi = noiseImage.getRoi();
		if (roi == null) {
			return;
		}
		Point[] points = roi.getContainedPoints();
		int half = (histSize + 1) / 2;
		double[] xx = new double[points.length];
		double[] yy = new double[points.length];
		for (int i = 0; i < points.length; i++) {
			int histOffset = histSize * (points[i].x + points[i].y * width);
			float firstValue = 0;
			for (int k = 0; k < pointRadius; k++) {
				firstValue += hist[histOffset + k];
			}
			firstValue /= (float)pointRadius;
			float lastValue = 0;
			for (int k = half; k < histSize; k++) {
				lastValue += hist[histOffset + k];
			}
			lastValue /= (float)(histSize - half);
			yy[i] = firstValue;
			xx[i] = lastValue;
		}
		plot.setColor(Color.RED);
		plot.replace(1, "circle", xx, yy);
		plot.setLimitsToFit(true);
	}

	private void updatePoints() {
		Roi roi = pointsImage.getRoi();
		if (roi == null || !(roi instanceof PointRoi)) {
			return;
		}
		PointRoi pr = (PointRoi)roi;
		Point[] points = pr.getContainedPoints();
		int half = (histSize + 1) / 2;
		double[] xx = new double[points.length];
		double[] yy = new double[points.length];
		for (int i = 0; i < points.length; i++) {
			int histOffset = histSize * (points[i].x + points[i].y * width);
			float firstValue = 0;
			for (int k = 0; k < pointRadius; k++) {
				firstValue += hist[histOffset + k];
			}
			firstValue /= (float)pointRadius;
			float lastValue = 0;
			for (int k = half; k < histSize; k++) {
				lastValue += hist[histOffset + k];
			}
			lastValue /= (float)(histSize - half);
			yy[i] = firstValue;
			xx[i] = lastValue;
		}
		plot.setColor(Color.BLUE);
		plot.replace(0, "circle", xx, yy);
		plot.setLimitsToFit(true);
	}

}
