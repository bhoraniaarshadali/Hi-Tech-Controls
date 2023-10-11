package com.example.hi_tech_controls.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hi_tech_controls.R;

import java.util.ArrayList;

public class AddDetailsAdp extends RecyclerView.Adapter<AddDetailsAdp.ViewHolder> {

    Context context;
    ArrayList<DetailsModel> arrayData;
    ProgressBar progressBar;

    public AddDetailsAdp(Context context, ArrayList<DetailsModel> detailsData) {
        this.context = context;
        this.arrayData = detailsData;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_cardview, parent, false);
        Toast.makeText(context, "In Adapter", Toast.LENGTH_SHORT).show();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.userName1.setText(arrayData.get(position).getuName());
        holder.userId1.setText(arrayData.get(position).getUId() + "");
        holder.progress1.setProgress(arrayData.get(position).getProgress());
    }


    @Override
    public int getItemCount() {
        return arrayData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView userId1, userName1, textStatus1;
        ProgressBar progress1;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);


            userId1 = (TextView) itemView.findViewById(R.id.showId);
            userName1 = (TextView) itemView.findViewById(R.id.showUsername);
            textStatus1 = (TextView) itemView.findViewById(R.id.textStatus);
            progress1 = (ProgressBar) itemView.findViewById(R.id.progressStatusBar);
        }

    }
}

