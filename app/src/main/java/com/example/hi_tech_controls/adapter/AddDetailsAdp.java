package com.example.hi_tech_controls.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.AddDetailsActivity;
import com.example.hi_tech_controls.R;

import java.util.ArrayList;

public class AddDetailsAdp extends RecyclerView.Adapter<AddDetailsAdp.ViewHolder> {

    Context context;
    ArrayList<DetailsModel> arrayData;

    public AddDetailsAdp(Context context, ArrayList<DetailsModel> detailsData) {
        this.context = context;
        this.arrayData = detailsData;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_cardview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DetailsModel item = arrayData.get(position);
        holder.userName1.setText(item.getuName());
        holder.userId1.setText(String.valueOf(item.getUId()));
        holder.progress1.setProgress(item.getProgress());

        if (item.getProgress() >= 100) {
            holder.textStatus1.setText("Completed");
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
        return arrayData != null ? arrayData.size() : 0;
    }

    public void updateList(ArrayList<DetailsModel> newList) {
        this.arrayData = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView userId1, userName1, textStatus1;
        ProgressBar progress1;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userId1 = itemView.findViewById(R.id.showId);
            userName1 = itemView.findViewById(R.id.showUsername);
            textStatus1 = itemView.findViewById(R.id.textStatus);
            progress1 = itemView.findViewById(R.id.progressStatusBar);
        }
    }
}
