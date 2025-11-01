package com.example.habittracker.data.repository;

import com.example.habittracker.data.remote.FirebaseAuthSource;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {
    private final FirebaseAuthSource authSource;

    public AuthRepository() {
        this.authSource = new FirebaseAuthSource();
    }

    public Task<AuthResult> register(String email, String password) {
        return authSource.register(email, password);
    }

    public Task<AuthResult> login(String email, String password) {
        return authSource.login(email, password);
    }

    public void logout() {
        authSource.logout();
    }

    public FirebaseUser getCurrentUser() {
        return authSource.getCurrentUser();
    }
}