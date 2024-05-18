package io.github.kildot.backgroundRemover;

import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

public final class Common {

    public static final String HELP_URL = "https://kilianna.github.io/points-detector/help/";
    public static final String NEW_PARAMS = "[ New preset ]";
    public static final String MRU_PARAMS = "[ Recently used ]";
    public static final int PLOT_UPDATE_DELAY = 1000;
    public static final int MAX_PROFILE_PLOTS = 25;

    //-------------------------------------------------------------------------

    private static final Timer timer = new Timer();

    public static TimerTask invokeLater(Runnable doRun, int ms) {
        return invokeLater(doRun, ms, -1);
    }

    public static TimerTask invokeLater(Runnable doRun, int ms, int period) {
            TimerTask tt = new TimerTask() {
                @Override
                public void run() {
                        SwingUtilities.invokeLater(doRun);
                }
            };
            if (period < 0) {
                    timer.schedule(tt, ms);
            } else {
                    timer.schedule(tt, ms, period);
            }
            return tt;
    }

}
