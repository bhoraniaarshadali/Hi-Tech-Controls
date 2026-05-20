package com.example.hi_tech_controls.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.model.ClientModel;
import com.example.hi_tech_controls.ui.activity.ClientDetailsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 🔹 Final ClientAdapter
 * Fully compatible with old code (update() retained)
 * Added shimmer loader + pagination
 */
public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.VH> {

    private final List<ClientModel> list = new ArrayList<>();
    private boolean isLoading = false;

    public boolean isLoading() {
        return isLoading;
    }

    // 🔸 Completely safe update methods
    public void showShimmer() {
        if (isLoading) return;

        isLoading = true;
        int oldSize = list.size();
        list.clear();
        if (oldSize > 0) {
            notifyItemRangeRemoved(0, oldSize);
        }
        notifyItemRangeInserted(0, 5); // Show 5 shimmer items
    }

    public void hideShimmer(List<ClientModel> newData) {
        if (!isLoading) return;

        isLoading = false;
        list.clear();
        list.addAll(newData);
        notifyDataSetChanged(); // Safe when switching from shimmer to real data
    }

    public void update(List<ClientModel> data) {
        isLoading = false;
        list.clear();
        list.addAll(data);
        notifyDataSetChanged();
    }

    public void addMore(List<ClientModel> more) {
        if (more.isEmpty() || isLoading) return;

        int oldSize = list.size();
        list.addAll(more);
        notifyItemRangeInserted(oldSize, more.size());
    }

    public void clearAll() {
        isLoading = false;
        int size = list.size();
        list.clear();
        if (size > 0) {
            notifyItemRangeRemoved(0, size);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isLoading ? 0 : 1;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View shimmerView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_client_shimmer, parent, false);
            return new VH(shimmerView, true);
        } else {
            View realView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_client_card, parent, false);
            return new VH(realView, false);
        }
    }

//    @Override
//    public void onBindViewHolder(@NonNull VH h, int position) {
//        if (isLoading || h.isShimmer) return;
//
//        if (position < list.size()) {
//            ClientModel c = list.get(position);
//            h.name.setText(c.name);
//            h.id.setText("ID: " + c.clientId);
//            h.gpDate.setText(c.gpDate);
//            h.makeName.setText(c.makeName);
//
//            h.btn.setOnClickListener(v -> {
//                Intent intent = new Intent(v.getContext(), ClientDetailsActivity.class);
//                intent.putExtra("clientId", c.clientId);
//                v.getContext().startActivity(intent);
//            });
//
//        }
//    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        if (isLoading || h.isShimmer) return;

        if (position < list.size()) {
            ClientModel c = list.get(position);
            h.name.setText(c.name);
            h.id.setText("ID: " + c.clientId);
            h.gpDate.setText(c.gpDate);
            h.makeName.setText(c.makeName);

            // Reset standard states on bind
            h.itemView.setEnabled(true);
            h.btn.setEnabled(true);
            h.btn.setVisibility(View.VISIBLE);
            if (h.progress != null) {
                h.progress.setVisibility(View.GONE);
            }

            // 🔹 OPTION 1: पूरे item पर click listener with loader & de-bounce
            h.itemView.setOnClickListener(v -> {
                if (!v.isEnabled()) return;
                v.setEnabled(false);
                h.btn.setEnabled(false);
                if (h.progress != null) {
                    h.btn.setVisibility(View.GONE);
                    h.progress.setVisibility(View.VISIBLE);
                }

                v.postDelayed(() -> {
                    Intent intent = new Intent(v.getContext(), ClientDetailsActivity.class);
                    intent.putExtra("clientId", c.clientId);
                    v.getContext().startActivity(intent);
                }, 150);
            });

            // 🔹 OPTION 2: Arrow icon पर click listener with loader & de-bounce
            h.btn.setOnClickListener(v -> {
                if (!v.isEnabled()) return;
                v.setEnabled(false);
                h.itemView.setEnabled(false);
                if (h.progress != null) {
                    h.btn.setVisibility(View.GONE);
                    h.progress.setVisibility(View.VISIBLE);
                }

                v.postDelayed(() -> {
                    Intent intent = new Intent(v.getContext(), ClientDetailsActivity.class);
                    intent.putExtra("clientId", c.clientId);
                    v.getContext().startActivity(intent);
                }, 150);
            });
        }
    }

    @Override
    public int getItemCount() {
        return isLoading ? 5 : list.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, id, gpDate, makeName;
        ImageView btn;
        ProgressBar progress;
        boolean isShimmer;

        VH(View v, boolean isShimmer) {
            super(v);
            this.isShimmer = isShimmer;

            if (!isShimmer) {
                name = v.findViewById(R.id.clientName);
                id = v.findViewById(R.id.clientId);
                gpDate = v.findViewById(R.id.gpDate);
                makeName = v.findViewById(R.id.makeName);
                btn = v.findViewById(R.id.arrowIcon);
                progress = v.findViewById(R.id.cardProgress);
            }
        }
    }
}