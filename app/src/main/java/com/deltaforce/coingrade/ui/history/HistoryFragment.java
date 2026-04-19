package com.deltaforce.coingrade.ui.history;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.deltaforce.coingrade.data.HistoryRepository;
import com.deltaforce.coingrade.databinding.FragmentHistoryBinding;

import com.deltaforce.coingrade.R;

import java.util.List;

public class HistoryFragment extends Fragment {
    private  FragmentHistoryBinding binding;
    private  HistoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new HistoryAdapter();
        binding.recycler.setAdapter(adapter);
        binding.btnNewFromHistory.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_history_to_capture));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<HistoryRepository.Entry> list = HistoryRepository.getAll(requireContext());
        adapter.submit(list);
        boolean empty = list.isEmpty();
        binding.recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}