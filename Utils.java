import ij.ImagePlus;

public class Utils {

    public static void addProcessingInfo(ImagePlus src, ImagePlus dst, String text) {
        int index = 1;
        if (src != dst) {
            while (src.getProp("Annotation[" + index + "]") != null) {
                addAnnotation(dst, src.getProp("Annotation[" + index + "]"));
                index++;
            }
        }
        addAnnotation(dst, text);
    }

    public static void addAnnotation(ImagePlus dst, String text) {
        int index = 1;
        while (dst.getProp("Annotation[" + index + "]") != null && dst.getProp("Annotation[" + index + "]").length() > 0) {
            index++;
        }
        dst.setProp("Annotation[" + index + "]", text);
    }
}
