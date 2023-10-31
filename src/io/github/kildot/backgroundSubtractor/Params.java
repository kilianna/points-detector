package io.github.kildot.backgroundSubtractor;

import ij.IJ;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Properties;
import java.util.Set;
import javax.swing.JOptionPane;

public class Params {
    
    // Preliminary parameters
    public int windowRadius;
    public int pointRadius;
    public int backgroungStartRadius;
    public boolean resetDisplayRange;

    public final void loadDefaults() {
        windowRadius = 20;
        pointRadius = 5;
        backgroungStartRadius = 6;
        resetDisplayRange = false;
    }

    public final void copyTo(Params dest) {
        dest.windowRadius = windowRadius;
        dest.pointRadius = pointRadius;
        dest.backgroungStartRadius = backgroungStartRadius;
        dest.resetDisplayRange = resetDisplayRange;
    }
    
    private void setParam(String paramName, String value) {
        if (paramName.equals("windowRadius")) windowRadius = Integer.parseInt(value);
        if (paramName.equals("pointRadius")) pointRadius = Integer.parseInt(value);
        if (paramName.equals("backgroungStartRadius")) backgroungStartRadius = Integer.parseInt(value);
        if (paramName.equals("resetDisplayRange")) resetDisplayRange = Boolean.parseBoolean(value);
    }

    private void toProperties(String prefix, String name) {
        properties.setProperty(prefix + "windowRadius", Integer.toString(windowRadius));
        properties.setProperty(prefix + "pointRadius", Integer.toString(pointRadius));
        properties.setProperty(prefix + "backgroungStartRadius", Integer.toString(backgroungStartRadius));
        properties.setProperty(prefix + "resetDisplayRange", Boolean.toString(resetDisplayRange));

        properties.setProperty(prefix + "name", name);
    }

    public Params() {
        loadDefaults();
    }
    
    private Properties properties = new Properties();
    
    private String _propPath = IJ.getDirectory("preferences") + "/" + Params.class.getName() + ".txt";

    private void readProperties() {
        properties.clear();
        try {
            properties.load(new FileInputStream(_propPath));
        } catch (Exception ex) {
            loadDefaults();
            toProperties("0-", Common.MRU_PARAMS);
            writeProperties();
        }
    }
    
    private void writeProperties() {
        try {
            properties.store(new FileOutputStream(_propPath), "Presets for " + Params.class.getName());
        } catch (IOException ex) {
            error("Cannot write preset. " + ex.getMessage());
        }
    }

    private String getPrefix(String name) {
        readProperties();
        for (String key : properties.stringPropertyNames()) {
            if (key.endsWith("-name") && properties.getProperty(key).equals(name)) {
                return key.substring(0, key.length() - 4);
            }
        }
        return null;
    }
    
    private boolean loadUnchecked(String name) {
        String prefix = getPrefix(name);
        if (prefix == null) {
            return false;
        }
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                setParam(key.substring(prefix.length()), (String)properties.get(key));
            }
        }
        return true;
    }
    
    public String[] list() {
        ArrayList<String> list = new ArrayList<>();
        readProperties();
        for (String key : properties.stringPropertyNames()) {
            if (key.endsWith("-name")) {
                list.add(properties.getProperty(key));
            }
        }
        String[] arr = new String[list.size()];
        list.toArray(arr);
        Arrays.sort(arr);
        return arr;
    }
    
    public void load(String name) {
        Params p = new Params();
        if (p.loadUnchecked(name) && p.verifyAll()) {
            p.copyTo(this);
        } else {
            error("Cannot load '" + name + "' preset.");
        }
    }

    public void store(String name) {
        String prefix = getPrefix(name);
        Set<String> set = properties.stringPropertyNames();
        String[] arr = new String[set.size()];
        set.toArray(arr);
        if (prefix != null) {
            for (String key : arr) {
                if (key.startsWith(prefix)) {
                    properties.remove(key);
                }
            }
        } else {
            int prefixInt = 1;
            outerloop:
            while (true) {
                prefix = (prefixInt++) + "-";
                for (String key : arr)
                    if (key.startsWith(prefix))
                        continue outerloop;
                break;
            }
        }
        toProperties(prefix, name);
        writeProperties();
    }

    public void remove(String name) {
        String prefix = getPrefix(name);
        Set<String> set = properties.stringPropertyNames();
        String[] arr = new String[set.size()];
        set.toArray(arr);
        if (prefix != null) {
            for (String key : arr) {
                if (key.startsWith(prefix)) {
                    properties.remove(key);
                }
            }
            writeProperties();
        }
    }

    public boolean verifyAll() {
        return true
                && windowRadiusVerifier.verify(windowRadius)
                && pointRadiusVerifier.verify(pointRadius)
                && backgroungStartRadiusVerifier.verify(backgroungStartRadius)
                ;
    }
    
    public Verifiers.IntegerInputVerifier windowRadiusVerifier = new Verifiers.IntegerInputVerifier() {
        @Override
        public boolean verify(int value) {
            return pointRadius < value && backgroungStartRadius < value && 3 <= value && value < 100;
        }
    };

    public Verifiers.IntegerInputVerifier pointRadiusVerifier = new Verifiers.IntegerInputVerifier() {
        @Override
        public boolean verify(int value) { return 2 <= value && value < windowRadius; }
    };

    public Verifiers.IntegerInputVerifier backgroungStartRadiusVerifier = new Verifiers.IntegerInputVerifier() {
        @Override
        public boolean verify(int value) { return 2 <= value && value < windowRadius; }
    };

    private void error(String text) {
        JOptionPane.showMessageDialog(null, text, "Preset error", JOptionPane.ERROR_MESSAGE);    
    }

}
