package com.example.habittracker.ui.achievements;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.habittracker.databinding.FragmentAchievementsBinding; // Tạo từ fragment_achievements.xml

public class AchievementsFragment extends Fragment {

    private FragmentAchievementsBinding binding;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // LƯU Ý: Bạn chưa cung cấp file 'fragment_achievements.xml'
        // Hãy đảm bảo bạn đã tạo nó.
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // TODO: Thêm sự kiện click cho nút "Back" nếu layout của bạn có
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}