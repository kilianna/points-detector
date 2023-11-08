package io.github.kildot.backgroundSubtractor;

import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.util.*;


public class Points_Generator implements PlugIn {
    
        private double[] numbers;
        private double radius;

	@Override
	public void run(String arg) {
		ImagePlus sourceImage = IJ.getImage();
		ImageStack stack = sourceImage.getStack();
		int count = stack != null ? stack.size() : 1;
		boolean diaglogOk = showDialog();
		if (!diaglogOk) {
			return;
		}
		for (int i = 0; i < count; i++) {
			ImageProcessor ip = stack != null ? stack.getProcessor(i + 1) : sourceImage.getProcessor();
			processImage(ip);
		}
		sourceImage.updateAndDraw();
	}

	private boolean showDialog() {
		GenericDialog dialog = new GenericDialog("Parameters");
		dialog.addStringField("List of point values:", "", 60);
		dialog.addStringField("Radius:", "", 30);
		dialog.showDialog();
		if (dialog.wasCanceled()) {
			return false;
		}
		Vector<TextField> vect = dialog.getStringFields();
		String[] strings = splitParams(vect.get(0).getText());
		numbers = new double[strings.length];
		for (int i = 0; i < strings.length; i++) {
			try {
				numbers[i] = Double.parseDouble(strings[i]);
			} catch (Exception ex) {
				IJ.log("Invalid value at position " + (i + 1));
				return false;
			}
		}
                try {
                        radius = Double.parseDouble(vect.get(1).getText());
                        if (radius < 2 || radius > 100) throw new Exception();
                } catch (Exception ex) {
                        IJ.log("Invalid radius");
                        return false;
                }
                return true;
	}

	private void processImage(ImageProcessor ip) {
            Random rnd = new Random();
            int width = ip.getWidth();
            int height = ip.getHeight();
            double startX = Math.max(width / 20, 2 * radius);
            double startY = Math.max(height / 20, 2 * radius);
            double totalX = width - 2 * startX;
            double totalY = height - 2 * startY;
            if (totalX < 2 * radius || totalY < 2 * radius) {
                IJ.log("Invalid radius");
                return;
            }
            if (ip instanceof ShortProcessor) {
                ShortProcessor processor = (ShortProcessor) ip;
                short[] px = (short[]) processor.getPixels();
                for (int k = 0; k < numbers.length; k++) {
                    double xx = startX + totalX * rnd.nextDouble();
                    double yy = startY + totalY * rnd.nextDouble();
                    for (int x = (int)(xx - radius) - 1; x < (int)(xx + radius) + 1; x++) {
                        for (int y = (int)(yy - radius) - 1; y < (int)(yy + radius) + 1; y++) {
                            double dx = xx - (double)x;
                            double dy = yy - (double)y;
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d <= radius + 0.001) {
                                int valueInt = (int)px[x + y * width] & 0xFFFF;
                                double value = (double)valueInt + numbers[k];
                                if (value < 0.0) value = 0;
                                if (value >= 65535.0) value = 65535.0;
                                px[x + y * width] = (short)(int)(value + 0.5);
                            }
                        }                    
                    }
                }
            } else if (ip instanceof ByteProcessor) {
                ByteProcessor processor = (ByteProcessor) ip;
                byte[] px = (byte[]) processor.getPixels();
                for (int k = 0; k < numbers.length; k++) {
                    double xx = startX + totalX * rnd.nextDouble();
                    double yy = startY + totalY * rnd.nextDouble();
                    for (int x = (int)(xx - radius) - 1; x < (int)(xx + radius) + 1; x++) {
                        for (int y = (int)(yy - radius) - 1; y < (int)(yy + radius) + 1; y++) {
                            double dx = xx - (double)x;
                            double dy = yy - (double)y;
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d <= radius + 0.001) {
                                int valueInt = (int)px[x + y * width] & 0xFF;
                                double value = (double)valueInt + numbers[k];
                                if (value < 0.0) value = 0;
                                if (value >= 255.0) value = 255.0;
                                px[x + y * width] = (byte)(int)(value + 0.5);
                            }
                        }                    
                    }
                }
            } else if (ip instanceof FloatProcessor) {
                FloatProcessor processor = (FloatProcessor) ip;
                float[] px = (float[]) processor.getPixels();
                for (int k = 0; k < numbers.length; k++) {
                    double xx = startX + totalX * rnd.nextDouble();
                    double yy = startY + totalY * rnd.nextDouble();
                    for (int x = (int)(xx - radius) - 1; x < (int)(xx + radius) + 1; x++) {
                        for (int y = (int)(yy - radius) - 1; y < (int)(yy + radius) + 1; y++) {
                            double dx = xx - (double)x;
                            double dy = yy - (double)y;
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d <= radius + 0.001) {
                                double value = (double)px[x + y * width] + numbers[k];
                                px[x + y * width] = (float)value;
                            }
                        }                    
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
