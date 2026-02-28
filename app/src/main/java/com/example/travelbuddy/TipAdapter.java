package com.example.travelbuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    private List<Tip> tipList;

    public TipAdapter(List<Tip> tipList) {
        this.tipList = tipList;
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tip, parent, false);
        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {
        Tip tip = tipList.get(position);
        holder.title.setText(tip.title);
        holder.location.setText(tip.location);
    }

    @Override
    public int getItemCount() {
        return tipList.size();
    }

    static class TipViewHolder extends RecyclerView.ViewHolder {
        TextView title, location;

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tipTitle);
            location = itemView.findViewById(R.id.tipLocation);
        }
    }
}
