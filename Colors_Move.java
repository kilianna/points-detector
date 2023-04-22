import ij.*;
import ij.plugin.*;
import ij.process.*;


public class Colors_Move implements PlugIn {

	@Override
	public void run(String arg) {
		ImagePlus sourceImage = IJ.getImage();
		double min = sourceImage.getDisplayRangeMin();
		double max = sourceImage.getDisplayRangeMax();
		String info = min + " ÷ " + max + "  ->  0 ÷ " + (max - min);
		IJ.log(info);
		IJ.setMinAndMax(sourceImage, 0, max - min);
		ImageStack stack = sourceImage.getStack();
		int count = stack != null ? stack.size() : 1;
		for (int i = 0; i < count; i++) {
			ImageProcessor ip = stack != null ? stack.getProcessor(i + 1) : sourceImage.getProcessor();
			modImage(ip, (int)(min + 0.5));
		}
		Utils.addProcessingInfo(sourceImage, sourceImage, "Reset colors balance: " + info);
		sourceImage.updateAndDraw();
	}

	private void modImage(ImageProcessor ip, int colorOffset) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		if (ip instanceof ShortProcessor) {
			ShortProcessor processor = (ShortProcessor)ip;
			short[] px = (short[])processor.getPixels();
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int v = (int)px[x + width * y] & 0xFFFF;
					px[x + width * y] = (short)Math.max(0, v - colorOffset);
				}
			}
		} else {
			IJ.log("Image format not supported!");
		}
	}
}
