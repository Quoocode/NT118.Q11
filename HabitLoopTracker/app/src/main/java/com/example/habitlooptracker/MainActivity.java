package com.example.habitlooptracker;

import android.os.Bundle;
import android.util.Log; // Make sure this is imported

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.habitlooptracker.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// --- ADD ALL THESE FIREBASE/UTIL IMPORTS ---
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot; // <-- For Test 2
import com.google.firebase.firestore.QuerySnapshot; // <-- For Test 2
import com.google.firebase.firestore.DocumentReference; // <-- For Test 3
import java.util.Date; // <-- For Test 3
import java.util.HashMap; // <-- For Test 3
import java.util.Map; // <-- For Test 3
// ---

public class MainActivity extends AppCompatActivity {

    // 1. Define a "TAG" for logging
    // We'll use "TESTING" to match the new tests
    private static final String TAG = "TESTING";

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- YOUR ORIGINAL UI CODE ---
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // =================================================================
        // --- START OF ALL TEST CODE (JAVA) ---
        // =================================================================

        // 2. Get an instance of the database
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 3. !! IMPORTANT !!
        // This is the User UID you copied from Authentication
        // (e.g., "8m3kL2rXq9abcXYZ...")
        final String TEST_USER_ID = "KJKMPNgj6yaBOEY1G513ta45mcl1"; // <-- PASTE YOUR REAL USER UID HERE

        // --- Test 1: Reading the main user document (Your original test) ---
        Log.d(TAG, "Test 1: Attempting to fetch user document...");
        db.collection("users").document(TEST_USER_ID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Log.d(TAG, "Test 1 ✅ SUCCESS! Data: " + documentSnapshot.getData());
                        } else {
                            Log.w(TAG, "Test 1 ⚠️ Connection OK, but doc not found. (ID: " + TEST_USER_ID + ")");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Test 1 ❌ FAILURE! Error connecting", e);
                    }
                });


        // --- Test 2: Reading this user's habits (Java version of fetchTestUserHabits) ---
        Log.d(TAG, "Test 2: Attempting to fetch habits sub-collection...");
        db.collection("users").document(TEST_USER_ID).collection("habits")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot habitDocuments) {
                        Log.d(TAG, "Test 2 ✅ SUCCESS! Fetched " + habitDocuments.size() + " habits:");
                        for (QueryDocumentSnapshot doc : habitDocuments) {
                            // You'll see "Đọc sách" printed in your Logcat
                            Log.d(TAG, "   -> Habit: " + doc.getData().get("name") + " (ID: " + doc.getId() + ")");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Test 2 ❌ FAILURE! Error fetching habits", e);
                    }
                });


        // --- Test 3: Adding a completion (Java version of addTestCompletion) ---
        Log.d(TAG, "Test 3: Attempting to add a test completion...");

        // !! IMPORTANT !!
        // Go to the console, find your "Đọc sách" habit, and copy its ID
        final String TEST_HABIT_ID = "Wtl6LaaLS2LLYHQ7GhWX"; // <-- PASTE HABIT ID HERE

        // Create the data for the new completion document
        Map<String, Object> newCompletion = new HashMap<>();
        newCompletion.put("userId", TEST_USER_ID);
        newCompletion.put("habitId", TEST_HABIT_ID);
        newCompletion.put("date", new Date()); // This adds the current timestamp
        newCompletion.put("completedValue", 30);

        db.collection("completions")
                .add(newCompletion)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Test 3 ✅ SUCCESS! Completion added with ID: " + documentReference.getId());
                        // Now, go check your Firebase console. A "completions"
                        // collection will have appeared with this new document in it.
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Test 3 ❌ FAILURE! Error adding completion", e);
                    }
                });

        // =================================================================
        // --- END OF ALL TEST CODE (JAVA) ---
        // =================================================================
    }
}