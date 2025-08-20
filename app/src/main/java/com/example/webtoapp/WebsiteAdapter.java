package com.example.webtoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WebsiteAdapter extends RecyclerView.Adapter<WebsiteAdapter.ViewHolder> {

    private List<WebsiteApp> websites;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(WebsiteApp website);
    }
    public WebsiteAdapter(List<WebsiteApp> websites, OnItemClickListener listener) {
        this.websites = websites;
        this.listener = listener;
    }
    @NonNull
    @Override
    public WebsiteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_website, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WebsiteAdapter.ViewHolder holder, int position) {
        WebsiteApp website = websites.get(position);
        holder.tvName.setText(website.getName());
        holder.tvUrl.setText(website.getUrl());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(website);
            }
        });
    }

    @Override
    public int getItemCount() {
        return websites.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvUrl;
        ImageView ivIcon;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvUrl = itemView.findViewById(R.id.tvUrl);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
