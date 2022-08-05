package com.igtools.downloader.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.igtools.downloader.R;
import com.igtools.downloader.models.MediaModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: desong
 * @Date: 2022/8/4
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryHolder> {
    List<String> thumbnails = new ArrayList<>();
    List<String> titles = new ArrayList<>();
    Context c;
    OnItemClickListener onItemClickListener;

    public HistoryAdapter(Context c) {

        this.c = c;
    }

    @NonNull
    @Override
    public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(c).inflate(R.layout.view_history, parent, false);
        return new HistoryHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {

        Glide.with(c).load(thumbnails.get(position))
                .placeholder(new ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
                .into(holder.thumbnail);

        holder.caption.setText(titles.get(position));
        holder.itemView.setOnClickListener(v -> onItemClickListener.onClick(position));


    }

    @Override
    public int getItemCount() {
        return thumbnails.size();
    }

    public void setDatas(List<String> thumbnails, List<String> titles) {
        this.thumbnails = thumbnails;
        this.titles = titles;
        notifyDataSetChanged();
    }

    class HistoryHolder extends RecyclerView.ViewHolder {

        ImageView thumbnail;
        TextView caption;

        public HistoryHolder(@NonNull View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.img_thumbnail);
            caption = itemView.findViewById(R.id.tv_caption);

        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {

        void onClick(int position);

    }
}
