package io.github.kildot.backgroundRemover;

import ij.ImagePlus;
import ij.io.FileInfo;

public class Utils {

    public static void addProcessingInfo(ImagePlus src, ImagePlus dst, String text) {
        int index = 1;
        if (src != dst) {
            while (src.getProp("Annotation[" + index + "]") != null) {
                addAnnotation(dst, src.getProp("Annotation[" + index + "]"));
                index++;
            }
        }
        String expectedFileName = null;
        FileInfo info = src.getOriginalFileInfo();
        if (info != null && info.getFilePath() != null) {
            expectedFileName = info.getFilePath();
        }
        addAnnotation(dst, text, expectedFileName);
    }

    public static void addAnnotation(ImagePlus dst, String text) {
        addAnnotation(dst, text, null);
    }

    public static void addAnnotation(ImagePlus dst, String text, String expectedFileName) {
        String fileName = "";
        int lastIndex = 0;
        for (int i = 1; i < lastIndex + 20; i++) {
            String value = dst.getProp("Annotation[" + i + "]");
            if (value == null || value.length() == 0) continue;
            lastIndex = i;
            if (value.startsWith("File: ")) fileName = value.substring(6);
        }
        if (expectedFileName != null && !fileName.equals(expectedFileName)) {
            int sep = Math.max(expectedFileName.lastIndexOf('/'), expectedFileName.lastIndexOf('\\'));
            if (expectedFileName.length() > 73 && sep > 0) {
                lastIndex = addProp(dst, lastIndex, "Path: " + expectedFileName.substring(0, sep));
                lastIndex = addProp(dst, lastIndex, "File: " + expectedFileName.substring(sep + 1));
            } else {
                lastIndex = addProp(dst, lastIndex, "File: " + expectedFileName);
            }
        }
        addProp(dst, lastIndex, text);
    }

    private static int addProp(ImagePlus dst, int index, String text) {
        if (text.length() >= 80) {
            index++;
            dst.setProp("Annotation[" + index + "]", text.substring(0, 76) + "...");
            text = text.substring(76);
            while (text.length() > 76) {
                index++;
                dst.setProp("Annotation[" + index + "]", "..." + text.substring(0, 73) + "...");
                text = text.substring(73);
            }
            index++;
            dst.setProp("Annotation[" + index + "]", "..." + text);
        } else {
            index++;
            dst.setProp("Annotation[" + index + "]", text);
        }
        return index;
    }
}
