package io.github.kildot.backgroundRemover;

import ij.*;
import ij.plugin.*;
import java.awt.Desktop;
import java.net.URI;


public class Help_Link implements PlugIn {
    
    static final String HELP_URL = "https://kilianna.github.io/ifj-tools/help/";

    @Override
    public void run(String arg) {      
        try {
            Desktop.getDesktop().browse(new URI(HELP_URL));
        } catch (Exception ex) {
            IJ.log("Cannot open browser with help.");
            IJ.log("Go to: " + HELP_URL);
        }

    }

}
