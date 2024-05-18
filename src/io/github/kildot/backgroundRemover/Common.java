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
    public static final int SIGNIFICANT_DIGITS = 5;

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
    
    public static String numberToString(double x) {
        return numberToString(x, SIGNIFICANT_DIGITS);
    }

    public static String numberToString(double x, int significantDigits) {
        String minus = x < 0.0 ? "-" : "";
        x = Math.abs(x);
        if (x < 1e-50) {
            return "0";
        }
        int frac = 0;
        long r = Math.round(x);
        String longStr = Long.toString(r);
        while (longStr.length() < significantDigits) {
            frac++;
            r = Math.round(x * Math.pow(10.0, (double)frac));
            longStr = Long.toString(r);
        }
        while (longStr.charAt(longStr.length() - 1) == '0' && frac > 0) {
            frac--;
            r = Math.round(x * Math.pow(10.0, (double)frac));
            longStr = Long.toString(r);
        }
        if (frac == 0) {
            return minus + longStr;
        } else if (frac < significantDigits) {
            return minus
                    + longStr.substring(0, longStr.length() - frac)
                    + "."
                    + longStr.substring(longStr.length() - frac);
        } else {
            return minus
                    + "0."
                    + (new String(new char[frac - significantDigits]).replace("\0", "0"))
                    + longStr;
        }
    }

}
