package com.deltaforce.coingrade.ui.result;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.deltaforce.coingrade.R;
import com.deltaforce.coingrade.databinding.FragmentResultBinding;

public class ResultFragment extends Fragment {

    private FragmentResultBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentResultBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args == null) return;

        String code = args.getString("gradeCode", "-");
        String gName = args.getString("gradeName", "");
        int vp = args.getInt("vp", 0);

        binding.textGradeCode.setText(code);
        binding.textGradeName.setText(gName);
        binding.textVp.setText(getString(R.string.result_vp_fmt, vp));

        binding.btnNew.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_result_to_home));
        binding.btnGoHistory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_result_to_history));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
