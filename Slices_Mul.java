import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.util.*;


public class Slices_Mul implements PlugIn {

	@Override
	public void run(String arg) {
		ImagePlus sourceImage = IJ.getImage();
		ImageStack stack = sourceImage.getStack();
		int count = stack != null ? stack.size() : 1;
		String text = showDialog(count);
		if (text == null) {
			return;
		}
		String[] strings = splitParams(text);
		double[] numbers = new double[strings.length];
		if (numbers.length != count) {
			IJ.log("Invalid number of values " + numbers.length + ", expected " + count);
			return;
		}
		for (int i = 0; i < strings.length; i++) {
			try {
				numbers[i] = Double.parseDouble(strings[i]);
			} catch (Exception ex) {
				IJ.log("Invalid value at position " + (i + 1));
				return;
			}
		}
		double whitest = 0;
		double black = 0;
		for (int i = 0; i < count; i++) {
			ImageProcessor ip = stack != null ? stack.getProcessor(i + 1) : sourceImage.getProcessor();
			double r = mulImage(ip, numbers[i]);
			whitest = Math.max(r, whitest);
			black = ip.getMin();
		}
		if ((int)(whitest + 0.5) > 65535) {
			IJ.log("Display range too high: " + whitest + ", maximum is: 65535");
			whitest = 65535;
		}
		IJ.setMinAndMax(sourceImage, black, (int)(whitest + 0.5));
		Utils.addProcessingInfo(sourceImage, sourceImage, "Slices Multiply: " + text);
		sourceImage.updateAndDraw();
	}

	private String showDialog(int slicesCount) {
		GenericDialog dialog = new GenericDialog("Parameters");
		dialog.addStringField("List of " + slicesCount + " values:", "", 30);
		dialog.showDialog();
		if (dialog.wasCanceled()) {
			return null;
		}
		Vector<TextField> vect = dialog.getStringFields();
		return vect.get(0).getText();
	}

	private double mulImage(ImageProcessor ip, double number) {
		double black = ip.getMin();
		double white = ip.getMax();
		int width = ip.getWidth();
		int height = ip.getHeight();
		if (ip instanceof ShortProcessor) {
			ShortProcessor processor = (ShortProcessor)ip;
			short[] px = (short[])processor.getPixels();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double v = (double)((int)px[x + width * y] & 0xFFFF);
					v = (v - black) * number + black;
					px[x + width * y] = (short)(int)(0.5 + Math.max(black, Math.min(65535.0, Math.round(v))));
				}
			}
		}
		return (white - black) * number + black;
	}

	private String[] splitParams(String p) {
		String[] arr = p.split(";");
		if ((arr.length == 1) && (p.split(",").length > 0)) {
			arr = p.split(",");
		}
		for (int i = 0; i < arr.length; i++) {
			arr[i] = arr[i].replace(",", ".").trim();
		}
		return arr;
	}

}
