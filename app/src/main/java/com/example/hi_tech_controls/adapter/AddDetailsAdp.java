package com.example.hi_tech_controls.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.model.DetailsModel;
import com.example.hi_tech_controls.ui.activity.AddDetailsActivity;

import java.util.ArrayList;
import java.util.List;

public class AddDetailsAdp extends RecyclerView.Adapter<AddDetailsAdp.ViewHolder> {

    private final Context context;
    private final ArrayList<DetailsModel> items = new ArrayList<>();

    // ------------------------------------------------------------
    // CONSTRUCTOR
    // ------------------------------------------------------------
    public AddDetailsAdp(Context context, ArrayList<DetailsModel> initial) {
        this.context = context;
        if (initial != null) items.addAll(initial);
        setHasStableIds(true);
    }

    // ------------------------------------------------------------
    // STABLE IDs (Better Performance)
    // ------------------------------------------------------------
    @Override
    public long getItemId(int position) {
        return items.get(position).getUId();
    }

    public List<DetailsModel> getCurrentItems() {
        return new ArrayList<>(items);
    }

    // ------------------------------------------------------------
    // DIFFUTIL FOR SMOOTH UPDATES
    // ------------------------------------------------------------
    public void submitList(List<DetailsModel> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new Diff(items, newList));
        items.clear();
        if (newList != null) items.addAll(newList);
        diff.dispatchUpdatesTo(this);
    }

    // ------------------------------------------------------------
    // VIEW HOLDER CREATION
    // ------------------------------------------------------------
    @NonNull
    @Override
    public AddDetailsAdp.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.add_cardview, parent, false);
        return new ViewHolder(view);
    }

    // ------------------------------------------------------------
    // BINDING UI WITH DATA
    // ------------------------------------------------------------
    @Override
    public void onBindViewHolder(@NonNull AddDetailsAdp.ViewHolder holder, int position) {
        DetailsModel item = items.get(position);
        bindBasicInfo(holder, item);
        bindStatus(holder, item);
        bindClickListener(holder, item);
    }

    private void bindBasicInfo(ViewHolder holder, DetailsModel item) {
        holder.userName1.setText(item.getuName());
        holder.userId1.setText(String.valueOf(item.getUId()));
        holder.progress1.setProgress(item.getProgress());
    }

    private void bindStatus(ViewHolder holder, DetailsModel item) {
        int progress = item.getProgress();

        if (progress >= 100) {
            holder.textStatus1.setText("Completed");
        } else if (progress >= 50) {
            holder.textStatus1.setText("Halfway There");
        } else {
            holder.textStatus1.setText("In Progress");
        }
    }

    private void bindClickListener(ViewHolder holder, DetailsModel item) {
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddDetailsActivity.class);
            intent.putExtra("clientId", String.valueOf(item.getUId()));
            context.startActivity(intent);
        });
    }

    // ------------------------------------------------------------
    // ITEM COUNT
    // ------------------------------------------------------------
    @Override
    public int getItemCount() {
        return items.size();
    }

    // ------------------------------------------------------------
    // VIEW HOLDER
    // ------------------------------------------------------------
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userId1, userName1, textStatus1;
        ProgressBar progress1;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            userId1 = itemView.findViewById(R.id.showId);
            userName1 = itemView.findViewById(R.id.showUsername);
            textStatus1 = itemView.findViewById(R.id.textStatus);
            progress1 = itemView.findViewById(R.id.progressStatusBar);
        }
    }

    // ------------------------------------------------------------
    // DIFFUTIL IMPLEMENTATION
    // ------------------------------------------------------------
    static class Diff extends DiffUtil.Callback {

        private final List<DetailsModel> oldL, newL;

        Diff(List<DetailsModel> oldList, List<DetailsModel> newList) {
            this.oldL = oldList == null ? new ArrayList<>() : oldList;
            this.newL = newList == null ? new ArrayList<>() : newList;
        }

        @Override
        public int getOldListSize() {
            return oldL.size();
        }

        @Override
        public int getNewListSize() {
            return newL.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldL.get(oldPos).getUId() == newL.get(newPos).getUId();
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            DetailsModel a = oldL.get(oldPos);
            DetailsModel b = newL.get(newPos);

            return a.getUId() == b.getUId()
                    && a.getProgress() == b.getProgress()
                    && safe(a.getuName()).equals(safe(b.getuName()));
        }

        private String safe(String s) {
            return s == null ? "" : s;
        }
    }
}
