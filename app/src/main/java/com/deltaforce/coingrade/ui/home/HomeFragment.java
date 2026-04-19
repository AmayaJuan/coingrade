package com.deltaforce.coingrade.ui.home;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.deltaforce.coingrade.R;
import com.deltaforce.coingrade.databinding.FragmentHomeBinding;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnStart.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_capture));
        binding.btnHistory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_home_to_history));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}