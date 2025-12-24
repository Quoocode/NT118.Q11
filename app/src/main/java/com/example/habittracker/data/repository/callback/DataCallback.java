package com.example.habittracker.data.repository.callback;

/**
 * Interface chung để trả về dữ liệu từ Firebase.
 * @param <T> Kiểu dữ liệu trả về (String, List<Habit>, Habit...)
 */
public interface DataCallback<T> {
    void onSuccess(T data);
    void onFailure(Exception e);
}