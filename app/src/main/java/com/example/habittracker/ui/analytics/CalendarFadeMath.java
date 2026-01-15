package com.example.habittracker.ui.analytics;


public final class CalendarFadeMath {

    private CalendarFadeMath() {
    }

    public static float computeProgress(int sheetTopPx, int fadeStartTopPx, int fadeEndTopPx) {
        // Guard against bad/unmeasured bounds to avoid divide-by-zero or inverted ranges.
        if (fadeEndTopPx <= 0 || fadeStartTopPx <= 0 || fadeEndTopPx >= fadeStartTopPx) {
            return 0f;
        }

        float t = (fadeStartTopPx - sheetTopPx) / (float) (fadeStartTopPx - fadeEndTopPx);
        if (t < 0f) return 0f;
        if (t > 1f) return 1f;
        return t;
    }
}

