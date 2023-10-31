package io.github.kildot.backgroundSubtractor;

import java.util.HashSet;
import java.util.Set;


interface DataModelListener {
    void parametersChanged(long fields);
}

public class DataModel {
    
    private Set<DataModelListener> listeners = new HashSet<>();
    
    public static final long WINDOW_RADIUS = 1;
    public static final long POINT_RADIUS = 2;
    public static final long BACKGROUND_START_RADIUS = 4;
    public static final long RESET_DISPLAY_RANGE = 8;
    
    public boolean setWindowRadius(int value, DataModelListener source) {
        return false;
    }
    
    public boolean setPointRadius(int value, DataModelListener source) {
        return false;
    }
    
    public boolean setBackgroungStartRadius(int value, DataModelListener source) {
        return false;
    }
    
    public boolean setResetDisplayRange(boolean value, DataModelListener source) {
        return false;
    }
    
}
