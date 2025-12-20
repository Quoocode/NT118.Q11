package com.example.habittracker.DatabaseStructure;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.habittracker.DatabaseStructure.Habit;
import com.example.habittracker.DatabaseStructure.History;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HabitRepository {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUid;

    public HabitRepository() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    // Interface callbacks because Firestore is asynchronous
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }

    // 1. ADD HABIT (To 'habits' sub-collection)
    public void addHabit(Habit habit, final DataCallback<Boolean> callback) {
        if (currentUid == null) return;

        db.collection("users").document(currentUid)
                .collection("habits")
                .add(habit)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    // 2. CHECK-IN (Create History - Flattened Structure)
    public void checkInHabit(String habitId, int value, String note, final DataCallback<Boolean> callback) {
        if (currentUid == null) return;

        // Create ID format: habitId_YYYYMMDD
        String todayStr = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        String historyId = habitId + "_" + todayStr;

        History history = new History(historyId, habitId, currentUid, value, note);

        // Save to 'history' collection (Same level as habits)
        db.collection("users").document(currentUid)
                .collection("history")
                .document(historyId) // Manually set ID to prevent duplicate check-ins per day
                .set(history)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        callback.onSuccess(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callback.onError(e);
                    }
                });
    }

    // 3. GET ACTIVE HABITS
    public void getActiveHabits(final DataCallback<List<Habit>> callback) {
        if (currentUid == null) return;

        db.collection("users").document(currentUid)
                .collection("habits")
                .whereEqualTo("archived", false) // Note: Check exact field name in Firestore
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Habit> habitList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // MAGIC: Convert JSON to Java Object
                        Habit habit = document.toObject(Habit.class);
                        habitList.add(habit);
                    }
                    callback.onSuccess(habitList);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    // 4. GET TODAY'S HISTORY
    public void getTodayHistory(final DataCallback<List<History>> callback) {
        if (currentUid == null) return;

        db.collection("users").document(currentUid)
                .collection("history")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<History> historyList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        History history = document.toObject(History.class);
                        historyList.add(history);
                    }
                    callback.onSuccess(historyList);
                })
                .addOnFailureListener(e -> callback.onError(e));
    }
}