package io.github.kildot.backgroundRemover;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import ij.IJ;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JOptionPane;

public class Params {

    static final int POINT_OUTPUT_WHITE = 0;
    static final int POINT_OUTPUT_BLACK = 1;
    static final int POINT_OUTPUT_ORIGINAL = 2;
    static final int POINT_OUTPUT_RESULT = 3;
    static final int POINT_OUTPUT_NET = 4;
    static final int POINT_OUTPUT_NET_SCALED = 5;
    static final int POINT_OUTPUT_NET_MODE = 6;
    static final int POINT_OUTPUT_NET_SCALED_MODE = 7;
    static final int POINT_OUTPUT_NET_MEDIAN = 8;
    static final int POINT_OUTPUT_NET_SCALED_MEDIAN = 9;

    static final String[] POINT_OUTPUTS = new String[] {
        "White",
        "Black",
        "Original",
        "Degree of matching",
        "Net signal (average)",
        "Net signal scaled (average)",
        "Net signal (mode)",
        "Net signal scaled (mode)",
        "Net signal (median)",
        "Net signal scaled (median)"
    };

    static final int BG_OUTPUT_WHITE = 0;
    static final int BG_OUTPUT_BLACK = 1;
    static final int BG_OUTPUT_ORIGINAL = 2;
    static final int BG_OUTPUT_RESULT = 3;

    static final String[] BG_OUTPUTS = new String[] {
        "White",
        "Black",
        "Original",
        "Degree of matching"
    };

    // Preliminary parameters
    public int windowRadius;
    public int pointRadius;
    public int backgroundStartRadius;
    public boolean resetDisplayRange;

    // Discrimination line parameters
    public double slope;
    public double yIntercept;

    // Output parameters
    public int pointOutput;
    public int bgOutput;
    public int skipPixels;
    public int takePixels;
    public boolean allSlices;
    public boolean addInputSlices;

    // Additional
    public boolean selectNoise;
    public boolean interactive;
    public boolean profileWindow;

    public static final long WINDOW_RADIUS = 0x0001;
    public static final long POINT_RADIUS = 0x0002;
    public static final long BACKGROUND_START_RADIUS = 0x0004;
    public static final long RESET_DISPLAY_RANGE = 0x0008;

    public static final long SLOPE = 0x0010;
    public static final long Y_INTERCEPT = 0x0020;

    public static final long POINT_OUTPUT = 0x0040;
    public static final long BG_OUTPUT = 0x0080;
    public static final long SKIP_PIXELS = 0x0100;
    public static final long TAKE_PIXELS = 0x0200;
    public static final long ALL_SLICES = 0x0400;
    public static final long ADD_INPUT_SLICES = 0x0800;

    public static final long PERSISTENT_PARAMETERS_MASK = 0x0FFF;

    public static final long SELECT_NOISE = 0x1000;
    public static final long INTERACTIVE = 0x2000;
    public static final long PROFILE_WINDOW = 0x4000;

    public static final long EVENT_AUTO_FIT = 0x0001;

    public final void loadDefaults() {
        windowRadius = 20;
        pointRadius = 5;
        backgroundStartRadius = 6;
        resetDisplayRange = false;

        slope = 1.0;
        yIntercept = 0.0;

        pointOutput = POINT_OUTPUT_WHITE;
        bgOutput = POINT_OUTPUT_BLACK;
        skipPixels = 2;
        takePixels = 3;
        allSlices = true;
        addInputSlices = false;

        selectNoise = true;
        interactive = false;
        profileWindow = false;
    }

    private void setDirect(Params src, boolean persistentOnly) {
        windowRadius = src.windowRadius;
        pointRadius = src.pointRadius;
        backgroundStartRadius = src.backgroundStartRadius;
        resetDisplayRange = src.resetDisplayRange;

        slope = src.slope;
        yIntercept = src.yIntercept;

        pointOutput = src.pointOutput;
        bgOutput = src.bgOutput;
        skipPixels = src.skipPixels;
        takePixels = src.takePixels;
        allSlices = src.allSlices;
        addInputSlices = src.addInputSlices;

        if (persistentOnly) return;

        selectNoise = src.selectNoise;
        interactive = src.interactive;
        profileWindow = src.profileWindow;
    }

    private long getFlags(Params src) {
        long flags = 0;

        if (windowRadius != src.windowRadius) flags |= WINDOW_RADIUS;
        if (pointRadius != src.pointRadius) flags |= POINT_RADIUS;
        if (backgroundStartRadius != src.backgroundStartRadius) flags |= BACKGROUND_START_RADIUS;
        if (resetDisplayRange != src.resetDisplayRange) flags |= RESET_DISPLAY_RANGE;

        if (slope != src.slope) flags |= SLOPE;
        if (yIntercept != src.yIntercept) flags |= Y_INTERCEPT;

        if (pointOutput != src.pointOutput) flags |= POINT_OUTPUT;
        if (bgOutput != src.bgOutput) flags |= BG_OUTPUT;
        if (skipPixels != src.skipPixels) flags |= SKIP_PIXELS;
        if (takePixels != src.takePixels) flags |= TAKE_PIXELS;
        if (allSlices != src.allSlices) flags |= ALL_SLICES;
        if (addInputSlices != src.addInputSlices) flags |= ADD_INPUT_SLICES;

        if (selectNoise != src.selectNoise) flags |= SELECT_NOISE;
        if (interactive != src.interactive) flags |= INTERACTIVE;
        if (profileWindow != src.profileWindow) flags |= PROFILE_WINDOW;

        return flags;
    }

    private void toProperties(Properties props, String prefix, String name) {
        props.setProperty(prefix + "windowRadius", Integer.toString(windowRadius));
        props.setProperty(prefix + "pointRadius", Integer.toString(pointRadius));
        props.setProperty(prefix + "backgroundStartRadius", Integer.toString(backgroundStartRadius));
        props.setProperty(prefix + "resetDisplayRange", Boolean.toString(resetDisplayRange));

        props.setProperty(prefix + "slope", Double.toString(slope));
        props.setProperty(prefix + "yIntercept", Double.toString(yIntercept));

        props.setProperty(prefix + "pointOutput", Integer.toString(pointOutput));
        props.setProperty(prefix + "bgOutput", Integer.toString(bgOutput));
        props.setProperty(prefix + "skipPixels", Integer.toString(skipPixels));
        props.setProperty(prefix + "takePixels", Integer.toString(takePixels));
        props.setProperty(prefix + "allSlices", Boolean.toString(allSlices));
        props.setProperty(prefix + "addInputSlices", Boolean.toString(addInputSlices));

        // skip selectNoise
        // skip interactive
        // skip profileWindow

        props.setProperty(prefix + "name", name);
    }

    private void setParamUnchecked(String paramName, String value) {
        switch (paramName) {
            case "windowRadius":
                windowRadius = Integer.parseInt(value);
                break;
            case "pointRadius":
                pointRadius = Integer.parseInt(value);
                break;
            case "backgroundStartRadius":
                backgroundStartRadius = Integer.parseInt(value);
                break;
            case "resetDisplayRange":
                resetDisplayRange = Boolean.parseBoolean(value);
                break;
            case "slope":
                slope = Double.parseDouble(value);
                break;
            case "yIntercept":
                yIntercept = Double.parseDouble(value);
                break;
            case "pointOutput": {
                pointOutput = parseEnum(value, POINT_OUTPUTS, POINT_OUTPUT_WHITE);
                break;
            }
            case "bgOutput":
                bgOutput = parseEnum(value, BG_OUTPUTS, POINT_OUTPUT_BLACK);
                break;
            case "skipPixels":
                skipPixels = Integer.parseInt(value);
                break;
            case "takePixels":
                takePixels = Integer.parseInt(value);
                break;
            case "allSlices":
                allSlices = Boolean.parseBoolean(value);
                break;
            case "addInputSlices":
                addInputSlices = Boolean.parseBoolean(value);
                break;
            case "selectNoise":
                selectNoise = Boolean.parseBoolean(value);
                break;
            case "interactive":
                interactive = Boolean.parseBoolean(value);
                break;
            case "profileWindow":
                profileWindow = Boolean.parseBoolean(value);
                break;
            default:
                break;
        }
    }

    public boolean setParamString(String paramName, String value) {
        try {
            boolean ok;
            switch (paramName) {
                case "windowRadius":
                    ok = verifyWindowRadius(Integer.parseInt(value));
                    break;
                case "pointRadius":
                    ok = verifyPointRadius(Integer.parseInt(value));
                    break;
                case "backgroundStartRadius":
                    ok = verifyBackgroundStartRadius(Integer.parseInt(value));
                    break;
                case "slope":
                    ok = verifySlope(Double.parseDouble(value));
                    break;
                case "yIntercept":
                    ok = verifyYIntercept(Double.parseDouble(value));
                    break;
                case "pointOutput":
                case "bgOutput":
                    ok = true;
                    break;
                case "skipPixels":
                    ok = verifySkipPixels(Integer.parseInt(value));
                    break;
                case "takePixels":
                    ok = verifyTakePixels(Integer.parseInt(value));
                    break;
                default:
                    return true;
            }
            if (!ok) {
                return false;
            }
            setParamUnchecked(paramName, value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean setParamBoolean(String paramName, boolean value) {
        switch (paramName) {
            case "resetDisplayRange":
                resetDisplayRange = value;
                return true;
            case "allSlices":
                allSlices = value;
                return true;
            case "addInputSlices":
                addInputSlices = value;
                return true;
            case "selectNoise":
                selectNoise = value;
                return true;
            case "interactive":
                interactive = value;
                return true;
            case "profileWindow":
                profileWindow = value;
                return true;
            default:
                return true;
        }
    }

    private boolean verifyWindowRadius(int value) {
        return pointRadius < value && backgroundStartRadius < value && 3 <= value && value < 100;
    }

    private boolean verifyPointRadius(int value) {
        return 2 <= value && value < windowRadius;
    }

    private boolean verifyBackgroundStartRadius(int value) {
        return 2 <= value && value < windowRadius;
    }

    private boolean verifySlope(double value) {
        return -1000000.0 <= value && value <= 1000000.0;
    }

    private boolean verifyYIntercept(double value) {
        return -1000000000.0 <= value && value <= 1000000000.0;
    }

    private boolean verifySkipPixels(int value) {
        return 0 <= value && value <= 16;
    }

    private boolean verifyTakePixels(int value) {
        return 0 <= value && value <= 32;
    }

    public boolean verifyAll() {
        return true
                && verifyWindowRadius(windowRadius)
                && verifyPointRadius(pointRadius)
                && verifyBackgroundStartRadius(backgroundStartRadius)
                && verifySlope(slope)
                && verifyYIntercept(yIntercept)
                && verifySkipPixels(skipPixels)
                && verifyTakePixels(takePixels)
                ;
    }

    public void fixAll() {
        windowRadius = range(windowRadius, 3, 100);
        pointRadius = range(pointRadius, 2, windowRadius - 1);
        backgroundStartRadius = range(backgroundStartRadius, 2, windowRadius - 1);
        slope = range(slope, -1000000.0, 1000000.0);
        yIntercept = range(yIntercept, -1000000000.0, 1000000000.0);
        skipPixels = range(skipPixels, 0, 16);
        takePixels = range(takePixels, 0, 32);
    }

    public static boolean isSkipTakePixelsNeeded(int pixelOutput) {
        return true
                && pixelOutput != Params.POINT_OUTPUT_BLACK
                && pixelOutput != Params.POINT_OUTPUT_WHITE
                && pixelOutput != Params.POINT_OUTPUT_ORIGINAL
                && pixelOutput != Params.POINT_OUTPUT_RESULT
                ;
    }

    //--------------------------------------------------------------------------
    
    public static class EventData {
        public long event;
    }

    public static class EventAutoFit extends EventData {
        public static final int ABOVE_NOISE = 0;
        public static final int BELOW_POINTS = 1;
        public static final int MIDDLE = 2;
        public int position;
        public EventAutoFit(int position) {
            event = EVENT_AUTO_FIT;
            this.position = position;
        }
    }

    public static interface Listener {
        void parametersChanged(long fields, boolean self);
        void eventTriggered(EventData event);
    }

    private Set<Listener> listeners = new HashSet<>();

    public Params() {
        loadDefaults();
    }

    public void set(Params src, boolean persistentOnly, Listener sender) {
        long flags = getFlags(src);
        if (persistentOnly) {
            flags &= PERSISTENT_PARAMETERS_MASK;
        }
        setDirect(src, persistentOnly);
        notifyListeners(flags, sender);
    }

    public Params copy() {
        Params p = new Params();
        p.set(this, false, null);
        return p;
    }

    public boolean equal(Params other, boolean persistentOnly) {
        long mask = persistentOnly ? PERSISTENT_PARAMETERS_MASK : -1L;
        return (getFlags(other) & mask) == 0;
    }

    @Override
    public String toString() {
        Properties props = new Properties();
        toProperties(props, "", "");
        props.remove("name");
        String res;
        try {
            Writer writer = new StringWriter();
            props.store(writer, null);
            res = writer.toString().trim().replaceAll("(\\s*\\r?\\n)+\\s*", ", ");
        } catch (IOException ex) {
            res = "";
        }
        return res;
    }

    public void addListener(Listener listener) {
        if (listeners == null) listeners = new HashSet<>();
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        if (listeners == null) return;
        listeners.remove(listener);
    }

    private void notifyListeners(long flags, Listener sender) {
        if (flags == 0 || listeners == null) return;
        Listener[] arr = new Listener[listeners.size()];
        listeners.toArray(arr);
        for (Listener listener : arr) {
            listener.parametersChanged(flags, listener == sender);
        }
    }

    public void triggerEvent(EventData event) {
        if (listeners == null) return;
        Listener[] arr = new Listener[listeners.size()];
        listeners.toArray(arr);
        for (Listener listener : arr) {
            listener.eventTriggered(event);
        }
    }

    private static Properties properties = new Properties();;
    private static final String PRESETS_PATH = IJ.getDirectory("preferences") + "/" + Params.class.getName() + ".txt";
    private static boolean propertiesBroken = false;

    private static void readProperties() {
        properties.clear();
        if (propertiesBroken) return;
        try {
            try {
                properties.load(new FileInputStream(PRESETS_PATH));
            } catch (FileNotFoundException ex) {
                Params p = new Params();
                p.toProperties(properties, "0-", Common.MRU_PARAMS);
                writeProperties();
                properties.load(new FileInputStream(PRESETS_PATH));
            }
        } catch (IOException ex) {
            propertiesBroken = true;
            error("Error reading presets. " + ex.getMessage());
        }
    }

    private static void writeProperties() {
        if (propertiesBroken) return;
        try {
            properties.store(new FileOutputStream(PRESETS_PATH), "Presets for " + Params.class.getName());
        } catch (IOException ex) {
            propertiesBroken = true;
            error("Cannot write preset. " + ex.getMessage());
        }
    }

    private static String getPrefix(String name) {
        readProperties();
        for (String key : propertiesKeys()) {
            if (key.endsWith("-name") && properties.getProperty(key).equals(name)) {
                return key.substring(0, key.length() - 4);
            }
        }
        return null;
    }

    public static String[] listPresets() {
        readProperties();
        ArrayList<String> list = new ArrayList<>();
        for (String key : propertiesKeys()) {
            if (key.endsWith("-name")) {
                list.add(properties.getProperty(key));
            }
        }
        String[] arr = new String[list.size()];
        list.toArray(arr);
        Arrays.sort(arr);
        return arr;
    }

    private boolean loadUnchecked(String name) {
        String prefix = getPrefix(name);
        if (prefix == null) {
            return false;
        }
        for (String key : propertiesKeys()) {
            if (key.startsWith(prefix)) {
                try {
                    setParamUnchecked(key.substring(prefix.length()), (String)properties.get(key));
                } catch (Exception ex) {
                    return false;
                }
            }
        }
        fixAll();
        return true;
    }

    public static Params loadPreset(String name) {
        Params p = new Params();
        if (p.loadUnchecked(name) && p.verifyAll()) {
            return p;
        } else {
            error("Cannot load '" + name + "' preset.");
            return new Params();
        }
    }

    public void storePreset(String name) {
        String prefix = getPrefix(name);
        String[] keys = propertiesKeys();
        if (prefix != null) {
            for (String key : keys) {
                if (key.startsWith(prefix)) {
                    properties.remove(key);
                }
            }
        } else {
            int prefixInt = 1;
            outerloop:
            while (true) {
                prefix = (prefixInt++) + "-";
                for (String key : keys)
                    if (key.startsWith(prefix))
                        continue outerloop;
                break;
            }
        }
        toProperties(properties, prefix, name);
        writeProperties();
    }

    private static String[] propertiesKeys() {
        Set<String> set = properties.stringPropertyNames();
        String[] arr = new String[set.size()];
        set.toArray(arr);
        return arr;
    }

    public static void remove(String name) {
        String prefix = getPrefix(name);
        if (prefix != null) {
            for (String key : propertiesKeys()) {
                if (key.startsWith(prefix)) {
                    properties.remove(key);
                }
            }
            writeProperties();
        }
    }

    private static void error(String text) {
        JOptionPane.showMessageDialog(null, text, "Preset error", JOptionPane.ERROR_MESSAGE);    
    }

    private int parseEnum(String value, String[] arr, int def) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) {
                return i;
            }
        }
        try {
            int v = Integer.parseInt(value);
            if (0 <= v && v < arr.length) return v;
        } catch (NumberFormatException ex) {}
        return def;
    }

    private static int range(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double range(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

}
