import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;

import ij.IJ;

public class NativeTools {

    static {
        try {
            String libName = "native_tools.so";
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                libName = "native_tools.dll";
            }
            String prefDir = IJ.getDirectory("preferences");
            InputStream inputStream = NativeTools.class.getResourceAsStream(libName);
            File targetFile = new File(prefDir + "/" + libName);
            java.nio.file.Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            try {
                java.nio.file.Files.setPosixFilePermissions(targetFile.toPath(),
                        PosixFilePermissions.fromString("rwxr-xr-x"));
            } catch (Exception ex) {
            }
            System.load(prefDir + "/" + libName);
        } catch (Exception e) {
        }
    }

    public static native void calHist(
            int histSize,
            int width,
            int height,
            int beginY,
            int endY,
            int[] indexDown,
            float[] weightDown,
            int[] indexUp,
            float[] weightUp,
            float[] input,
            float[] output,
            float[] tempBuffer);
}
