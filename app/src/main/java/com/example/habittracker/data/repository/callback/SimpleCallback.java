package com.example.habittracker.data.repository.callback;

/**
 * Interface callback đơn giản cho các hàm (Add, Update, Delete)
 * chỉ cần trả về Thành công (true) hoặc Thất bại (false/Exception).
 */
public interface SimpleCallback {
    void onComplete(boolean success, Exception e);
}