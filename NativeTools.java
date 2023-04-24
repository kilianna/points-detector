import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import ij.IJ;

/*
Use following command to update C header file after native function signature change:
        javah NativeTools

Use "build_native.sh" script to compile native library.

*/

public class NativeTools {

    static {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String libName;
            if (os.contains("windows")) {
                if (System.getProperty("os.arch").contains("64")) {
                    libName = "native_tools64.dll";
                } else {
                    libName = "native_tools32.dll";
                }
            } else if (os.contains("mac") || os.contains("darwin")) {
                libName = "native_tools.dylib";
            } else {
                libName = "native_tools.so";
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
