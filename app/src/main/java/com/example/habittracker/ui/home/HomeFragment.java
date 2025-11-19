package com.example.habittracker.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentHomeBinding; // Tạo từ fragment_home.xml

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private NavController navController;
    // private HabitAdapter habitAdapter; // Sẽ cần cho RecyclerView
    // private HomeViewModel homeViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);
        // homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Nút "Profile" ở góc trên bên phải (ID: btn_profile)
        binding.btnAddHabitToday.setOnClickListener(v -> {
            // Điều hướng đến Settings
            navController.navigate(R.id.action_homeFragment_to_addEditHabitFragment);
        });

        // Nút "Show more" (ID: tv_toggle_today_habits, nằm trong include)
        binding.includeTodayHabits.tvToggleTodayHabits.setOnClickListener(v -> {
            // TODO: Viết logic ẩn/hiện RecyclerView
        });
        binding.testHabitItem.tvTitle.setText("Habit Test (Fix cứng)");
        binding.testHabitItem.tvTime.setText("Click để xem chi tiết");


        binding.testHabitItem.getRoot().setOnClickListener(v -> {
            navController.navigate(R.id.action_homeFragment_to_habitDetailsFragment);
        });

    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}