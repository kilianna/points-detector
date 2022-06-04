
import java.lang.Math;
import ij.IJ;
import ij.gui.Roi;
import ij.gui.PointRoi;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.FloatProcessor;
import ij.*;
import ij.gui.*;
import ij.plugin.PlugIn;
import java.awt.*;

public class Filtr_Pukntkow implements PlugInFilter {
	protected ImagePlus image;
	private int width;
	private int height;
	private int radius;
	private int method;
	private float pixelMin;
	private float pixelMax;
	int[][] indexUp;
	int[][] indexDown;
	float[][] weightUp;
	float[][] weightDown;

	@Override
	public int setup(String arg, ImagePlus image) {
		this.image = image;
		return DOES_8G | DOES_16 | DOES_32 | CONVERT_TO_FLOAT;
	}

	@Override
	public void run(ImageProcessor ip) {
		FloatProcessor fp = (FloatProcessor)ip;
		width = fp.getWidth();
		height = fp.getHeight();
		fp.findMinAndMax();
		pixelMin = (float)fp.getMin();
		pixelMax = (float)fp.getMax();
		IJ.log("Min: " + pixelMin);
		IJ.log("Max: " + pixelMax);

		if (showDialog()) {
			makeWindow();
			process( (float[]) fp.getPixels() );
			image.updateAndDraw();
		}
	}

	private boolean showDialog() {
		GenericDialog gd = new GenericDialog("Process pixels");

		gd.addNumericField("radius", 20, 0);
		gd.addNumericField("method", 1, 0);

		gd.showDialog();
		if (gd.wasCanceled())
			return false;

		// get entered values
		radius = (int)gd.getNextNumber();
		method = (int)gd.getNextNumber();

		return true;
	}

	private void preview(float[][] arr) {
		float[] img = new float[arr.length * arr[0].length];
		for (int x = 0; x < arr.length; x++) {
			for (int y = 0; y < arr[x].length; y++) {
				img[x + arr.length * y] = arr[x][y];
			}
		}
		FloatProcessor fp2 = new FloatProcessor(arr.length, arr[0].length, img, null);
		ImagePlus image = new ImagePlus("preview", fp2);
		image.show();
	}

	private void makeWindow() {
		int size = 2 * radius + 1;
		float smallRadius = radius - 0.05f;
		indexUp = new int[size][size];
		indexDown = new int[size][size];
		weightUp = new float[size][size];
		weightDown = new float[size][size];
		float[] radiusWeight = new float[size];
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				int dx = x - radius;
				int dy = y - radius;
				float d = (float)Math.sqrt((float)(dx * dx + dy * dy)) - 1.0f;
				if (d < 0.0f) d = 0.0f;
				if (d >= smallRadius) continue;
				int up = (int)Math.ceil(d);
				int down = (int)d;
				indexUp[x][y] = up;
				indexDown[x][y] = down;
				if (up == down) {
					weightUp[x][y] = 1.0f;
					radiusWeight[up] += 1.0f;
				} else {
					weightUp[x][y] = d - (float)down;
					weightDown[x][y] = (float)up - d;
					radiusWeight[up] += d - (float)down;
					radiusWeight[down] += (float)up - d;
				}
			}
		}
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				weightUp[x][y] /= radiusWeight[indexUp[x][y]];
				weightDown[x][y] /= radiusWeight[indexDown[x][y]];
			}
		}
		/*preview(weightUp);
		preview(weightDown);
		float[][] sum = new float[size][size];
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				sum[x][y] = weightUp[x][y] + weightDown[x][y];
			}
		}
		preview(sum);*/
	}
	
	
	private float[] calcHist(float[] pixels, int centerX, int centerY) {
		int startX = centerX - radius;
		int startY = centerY - radius;
		//IJ.log(startX + ", " + startY);
		float[] hist = new float[radius + 1];
		for (int x = 0; x < 2 * radius + 1; x++) {
			for (int y = 0; y < 2 * radius + 1; y++) {
				hist[indexUp[x][y]] += weightUp[x][y] * pixels[startX + x + (startY + y) * width];
				hist[indexDown[x][y]] += weightDown[x][y] * pixels[startX + x + (startY + y) * width];
			}		
		}
		return hist;
	}
	
	public void process(float[] pixels) {
		if (method == 1) {
			process1(pixels);
		} else {
			process2(pixels);
		}
	}
	
	public void process2(float[] pixels) {
		float x1 = 7767f;
		float y1 = 8050f;
		float x2 = 8679f;
		float y2 = 9009f;
		int quater = (radius + 3) / 4;
		int half = (radius + 1) / 2;
		float[][] output = new float[width][height];
		for (int x = radius + 1; x < width - radius - 1; x++) {
			IJ.log("x = " + x);
			for (int y = radius + 1; y < height - radius - 1; y++) {
				float[] hist = calcHist(pixels, x, y);
				float firstValue = 0;
				for (int k = 0; k < quater; k++) {
					firstValue += hist[k];
				}
				firstValue /= (float)quater;
				float lastValue = 0;
				for (int k = half; k < radius + 1; k++) {
					lastValue += hist[k];
				}
				lastValue /= (float)(radius + 1 - half);
				output[x][y] = (firstValue-y1)*(x2-x1) < (y2-y1)*(lastValue-x1) ? pixelMin : pixelMax;
			}
		}
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				pixels[x + y * width] = output[x][y];
			}
		}
	}

	public void process1(float[] pixels) {
        Plot plot = new Plot("Points","Otoczenie","Środek");
        Plot plot2 = new Plot("Histograms","Promień","Jasność");
		float[] xx = new float[radius + 1];
		for (int i = 0; i < radius + 1; i++) {
			xx[i] = i;
		}
		PointRoi roi = (PointRoi)image.getRoi();
		java.awt.Point[] points = roi.getContainedPoints();
		int quater = (radius + 3) / 4;
		int half = (radius + 1) / 2;
		float px[][] = new float[2][points.length / 2];
		float py[][] = new float[2][points.length / 2];
		for (int i = 0; i < points.length; i++) {
			float[] hist = calcHist(pixels, points[i].x, points[i].y);
			float firstValue = 0;
			for (int k = 0; k < quater; k++) {
				firstValue += hist[k];
			}
			firstValue /= (float)quater;
			float lastValue = 0;
			for (int k = half; k < radius + 1; k++) {
				lastValue += hist[k];
			}
			lastValue /= (float)(radius + 1 - half);
			py[i % 2][i / 2] = firstValue;
			px[i % 2][i / 2] = lastValue;
	        plot2.setColor((i % 2) == 0 ? Color.red : Color.blue);
	        plot2.addPoints(xx,hist,PlotWindow.LINE);
		}
        plot.setColor(Color.red);
        plot.addPoints(px[0], py[0], PlotWindow.CIRCLE);
        plot.setColor(Color.blue);
        plot.addPoints(px[1], py[1], PlotWindow.CIRCLE);
        plot.show();
        plot2.show();
		IJ.log("DONE");
	}
}