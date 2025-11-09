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

import com.example.hi_tech_controls.AddDetailsActivity;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.model.DetailsModel;

import java.util.ArrayList;
import java.util.List;

public class AddDetailsAdp extends RecyclerView.Adapter<AddDetailsAdp.ViewHolder> {

    private final Context context;
    private final ArrayList<DetailsModel> items = new ArrayList<>();

    public AddDetailsAdp(Context context, ArrayList<DetailsModel> initial) {
        this.context = context;
        if (initial != null) items.addAll(initial);
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getUId();
    }

    public List<DetailsModel> getCurrentItems() {
        return new ArrayList<>(items);
    }

    public void submitList(List<DetailsModel> newList) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new Diff(items, newList));
        items.clear();
        if (newList != null) items.addAll(newList);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public AddDetailsAdp.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_cardview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddDetailsAdp.ViewHolder holder, int position) {
        DetailsModel item = items.get(position);
        holder.userName1.setText(item.getuName());
        holder.userId1.setText(String.valueOf(item.getUId()));
        holder.progress1.setProgress(item.getProgress());

        if (item.getProgress() >= 100) {
            holder.textStatus1.setText("Completed");
        } else if (item.getProgress() >= 50) {
            holder.textStatus1.setText("Halfway There");
        } else {
            holder.textStatus1.setText("In Progress");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddDetailsActivity.class);
            intent.putExtra("clientId", String.valueOf(item.getUId()));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

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

    static class Diff extends DiffUtil.Callback {
        private final List<DetailsModel> oldL, newL;

        Diff(List<DetailsModel> o, List<DetailsModel> n) {
            this.oldL = o == null ? new ArrayList<>() : o;
            this.newL = n == null ? new ArrayList<>() : n;
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
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldL.get(oldItemPosition).getUId() == newL.get(newItemPosition).getUId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            DetailsModel a = oldL.get(oldItemPosition), b = newL.get(newItemPosition);
            return a.getUId() == b.getUId()
                    && a.getProgress() == b.getProgress()
                    && safe(a.getuName()).equals(safe(b.getuName()));
        }

        private String safe(String s) {
            return s == null ? "" : s;
        }
    }
}
