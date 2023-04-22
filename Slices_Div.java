import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.util.*;


public class Slices_Div implements PlugIn {

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
		for (int i = 0; i < count; i++) {
			ImageProcessor ip = stack != null ? stack.getProcessor(i + 1) : sourceImage.getProcessor();
			divImage(ip, numbers[i]);
		}
		Utils.addProcessingInfo(sourceImage, sourceImage, "Slices Div: " + text);
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

	private void divImage(ImageProcessor ip, double number) {
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
					v = (v - black) / number + black;
					px[x + width * y] = (short)(0.5 + Math.max(black, Math.min(white, Math.round(v))));
				}
			}
		}
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
