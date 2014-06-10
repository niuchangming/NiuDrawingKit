package com.cmniu.pressure;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;

public class PressureCooker {
    private static final String PREFS_NAME = "Markers";
    
    private static final String PREF_PRESSURE_MIN = "pressure_min";
    private static final String PREF_PRESSURE_MAX = "pressure_max";
    private static final String PREF_FIRST_RUN = "first_run";

    private static final float DEF_PRESSURE_MIN = 0.2f;
    private static final float DEF_PRESSURE_MAX = 0.9f;

    private float mPressureMin = 0;
    private float mPressureMax = 1;

    public static final float PRESSURE_UPDATE_DECAY = 0.1f;
    public static final int PRESSURE_UPDATE_STEPS_FIRSTBOOT = 100; // points, a quick-training regimen
    public static final int PRESSURE_UPDATE_STEPS_NORMAL = 1000; // points, in normal use

    private static final boolean PARTNER_HACK = false;
    
    private int mPressureCountdownStart = PRESSURE_UPDATE_STEPS_NORMAL;
    private int mPressureUpdateCountdown = mPressureCountdownStart;
    private float mPressureRecentMin = 1;
    private float mPressureRecentMax = 0;
    
    private Context mContext;
    
    public PressureCooker(Context context) {
        mContext = context;
        loadStats();
    }
    
    public void loadStats() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, 0);

        mPressureMin = prefs.getFloat(PREF_PRESSURE_MIN, DEF_PRESSURE_MIN);
        mPressureMax = prefs.getFloat(PREF_PRESSURE_MAX, DEF_PRESSURE_MAX);
        
        final boolean firstRun = prefs.getBoolean(PREF_FIRST_RUN, true);
        setFirstRun(firstRun);
    }

    public void saveStats() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor prefsE = prefs.edit();
        prefsE.putBoolean(PREF_FIRST_RUN, false);
    
        prefsE.putFloat(PREF_PRESSURE_MIN, mPressureMin);
        prefsE.putFloat(PREF_PRESSURE_MAX, mPressureMax);
    
        prefsE.commit();
    }
    
    // Adjusts pressure values on the fly based on historical maxima/minima.
    public float getAdjustedPressure(float pressure) {
        if (PARTNER_HACK) {
            return pressure; 
        }
        
        if (pressure < mPressureRecentMin) mPressureRecentMin = pressure;
        if (pressure > mPressureRecentMax) mPressureRecentMax = pressure;
        
        if (--mPressureUpdateCountdown == 0) {
            final float decay = PRESSURE_UPDATE_DECAY;
            mPressureMin = (1-decay) * mPressureMin + decay * mPressureRecentMin;
            mPressureMax = (1-decay) * mPressureMax + decay * mPressureRecentMax;

            // upside-down values, will be overwritten on the next point
            mPressureRecentMin = 1;
            mPressureRecentMax = 0;

            // walk the countdown up to the maximum value
            if (mPressureCountdownStart < PRESSURE_UPDATE_STEPS_NORMAL) {
                mPressureCountdownStart = (int) (mPressureCountdownStart * 1.5f);
                if (mPressureCountdownStart > PRESSURE_UPDATE_STEPS_NORMAL)
                    mPressureCountdownStart = PRESSURE_UPDATE_STEPS_NORMAL;
            }
            mPressureUpdateCountdown = mPressureCountdownStart;
            
            saveStats();
        }

        final float pressureNorm = (pressure - mPressureMin)
            / (mPressureMax - mPressureMin);

        return pressureNorm;
    }
    
    public void setFirstRun(boolean firstRun) {
        if (firstRun) {
            // "Why do my eyes hurt?"
            // "You've never used them before."
            mPressureUpdateCountdown = mPressureCountdownStart = PRESSURE_UPDATE_STEPS_FIRSTBOOT;
        }
    }

    public void setPressureRange(float min, float max) {
        mPressureMin = min;
        mPressureMax = max;
    }
    
    public float[] getPressureRange(float[] r) {
        r[0] = mPressureMin;
        r[1] = mPressureMax;
        return r;
    }
}
