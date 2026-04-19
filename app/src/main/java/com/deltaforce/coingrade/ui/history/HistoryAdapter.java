package com.deltaforce.coingrade.ui.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.deltaforce.coingrade.data.HistoryRepository;
import com.deltaforce.coingrade.databinding.ItemHistoryBinding;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    private final List<HistoryRepository.Entry> items = new ArrayList<>();
    private final DateFormat dateFormat =
            DateFormat.getDateInstance(DateFormat.MEDIUM, new Locale("es", "CO"));

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding b = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        HistoryRepository.Entry e = items.get(position);
        holder.binding.itemGrade.setText(e.gradeCode);
        holder.binding.itemName.setText(e.coinName);
        holder.binding.itemVp.setText(e.vp + "%");
        holder.binding.itemDate.setText(dateFormat.format(new Date(e.dateMs)));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submit(List<HistoryRepository.Entry> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemHistoryBinding binding;

        VH(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
