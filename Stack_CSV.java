import ij.*;
import ij.plugin.*;
import ij.process.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;


public class Stack_CSV implements PlugIn {

	static String filename = "";

	@Override
	public void run(String arg) {
		ImagePlus sourceImage = IJ.getImage();
		ImageStack stack = sourceImage.getStack();
		int count = stack != null ? stack.size() : 1;
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setCurrentDirectory((new File(filename)).getParentFile());
		int result = fileChooser.showSaveDialog(sourceImage.getWindow());
		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}
		filename = fileChooser.getSelectedFile().getAbsolutePath();
		String[] parts = filename.split("\\.(?=[^\\.]+$)");
		String ext = parts.length > 1 ? "." + parts[1] : ".csv";
		for (int i = 0; i < count; i++) {
			try {
				String thisFileName = parts[0] + "-" + (i + 1) + ext;
				BufferedWriter writer = new BufferedWriter(new FileWriter(thisFileName));
				ImageProcessor ip = stack != null ? stack.getProcessor(i + 1) : sourceImage.getProcessor();
				int width = ip.getWidth();
				int height = ip.getHeight();
				if (ip instanceof ShortProcessor) {
					ShortProcessor shortProcessor = (ShortProcessor)ip;
					short[] px = (short[])shortProcessor.getPixels();
					for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
							writer.write((x == 0 ? "" : ",") + px[x + y * width]);
						}
						writer.write("\r\n");
					}
				}
				writer.close();
			} catch (IOException e) {
				IJ.log(e.toString());
			}
		}
	}
}
