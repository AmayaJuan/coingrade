package com.deltaforce.coingrade.ui.verification;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.deltaforce.coingrade.R;
import com.deltaforce.coingrade.data.HistoryRepository;
import com.deltaforce.coingrade.databinding.FragmentVerificationBinding;
import com.deltaforce.coingrade.model.GradeCatalog;
import com.deltaforce.coingrade.model.GradeOption;

public class VerificationFragment extends Fragment {

    private FragmentVerificationBinding binding;
    private String coinName;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            coinName = getArguments().getString("coinName");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVerificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.txtAnalyzingCount.setText(getString(R.string.analyzing_photos_fmt, 6));

        binding.btnAnalyzeIa.setOnClickListener(v -> {
            // Simulate AI Analysis
            binding.btnAnalyzeIa.setEnabled(false);
            binding.btnAnalyzeIa.setText(R.string.analyzing);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (getContext() == null) return;

                GradeOption g = GradeCatalog.randomGrade();
                HistoryRepository.add(requireContext(), coinName, g.code, g.vp);

                Bundle b = new Bundle();
                b.putString("coinName", coinName);
                b.putString("gradeCode", g.code);
                b.putString("gradeName", g.displayName);
                b.putInt("vp", g.vp);

                Navigation.findNavController(binding.getRoot())
                        .navigate(R.id.action_verification_to_result, b);
            }, 2000);
        });

        binding.btnRetake.setOnClickListener(v -> {
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_verification_to_capture);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
