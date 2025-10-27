package com.example.hi_tech_controls.adapter;

import android.content.Intent;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hi_tech_controls.R;
import com.example.hi_tech_controls.ViewDetailsActivity;
import java.util.*;

public class ClientAdapter extends RecyclerView.Adapter<ClientAdapter.VH> {

    private List<ClientModel> list = new ArrayList<>();

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_client_card, p, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        ClientModel c = list.get(i);
        h.name.setText(c.name);
        h.id.setText("ID: " + c.clientId);
        h.gpDate.setText(c.gpDate);
        h.makeName.setText(c.makeName);
        h.btn.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ViewDetailsActivity.class);
            intent.putExtra("clientId", c.clientId);
            v.getContext().startActivity(intent);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    public void update(List<ClientModel> data) {
        list.clear();
        list.addAll(data);
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, id, gpDate, makeName;
        ImageView btn;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.clientName);
            id = v.findViewById(R.id.clientId);
            gpDate = v.findViewById(R.id.gpDate);
            makeName = v.findViewById(R.id.makeName);
            btn = v.findViewById(R.id.arrowIcon);
        }
    }
}