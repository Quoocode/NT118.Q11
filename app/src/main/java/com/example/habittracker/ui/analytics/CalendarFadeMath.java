package com.example.habittracker.ui.analytics;

/**
 * Pure math helper for mapping BottomSheet top position to calendar fade progress.
 *
 * Contract:
 * - sheetTopPx: current top position of the sheet in the SAME coordinate space as the bounds.
 * - fadeStartTopPx: the sheetTop when calendar is fully visible (collapsed).
 * - fadeEndTopPx: the sheetTop when calendar is fully faded (expanded).
 *
 * Returns t in [0..1]. Callers typically use alpha = 1 - t.
 */
public final class CalendarFadeMath {

    private CalendarFadeMath() {
    }

    // tính toán tiến trình mờ dần của lịch dựa trên vị trí hiện tại của bảng dưới cùng và các điểm bắt đầu/kết thúc mờ dần
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

