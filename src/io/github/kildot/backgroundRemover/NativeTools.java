package io.github.kildot.backgroundRemover;

import java.io.File;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import ij.IJ;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/*
Use following command to update C header file after native function signature change:
        javah NativeTools

Use "build_native.sh" script to compile native library.

*/

public class NativeTools {

    static {
        try {
            (new File(getLogPath())).delete();
        } catch (Throwable ex) { }
        try {
            String libName = getLibName();
            String libPath = getLibPath();
            InputStream inputStream = NativeTools.class.getResourceAsStream("/" + libName);
            File targetFile = new File(libPath);
            java.nio.file.Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            inputStream.close();
            try {
                java.nio.file.Files.setPosixFilePermissions(targetFile.toPath(),
                        PosixFilePermissions.fromString("rwxr-xr-x"));
            } catch (Exception ex) {
                if (!libName.endsWith(".dll")) {
                    logException(ex);
                }
            }
            System.load(libPath);
        } catch (Exception ex) {
            logException(ex);
        }
    }
    
    public static void logException(Throwable ex) {
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(getLogPath(), true)));
            pw.print("----------------------------------------------------------------\n");
            ex.printStackTrace(pw);
            pw.close();
        } catch (IOException ex2) {
            IJ.handleException(ex2);
        }
    }
    
    private static String getLibName() {
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
        return libName;
    }
    
    private static String getLibPath() {
        String libName = getLibName();
        return IJ.getDirectory("preferences") + "/" + libName;
    }
    
    public static String getLogPath() {
        return getLibPath() + ".log.txt";
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
