package com.igtools.downloader.adapter;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
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

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: desong
 * @Date: 2022/8/4
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryHolder> {
    List<String> thumbnails = new ArrayList<>();
    List<String> titles = new ArrayList<>();
    List<String> usernames = new ArrayList<>();
    List<String> avatars = new ArrayList<>();
    Context c;
    OnItemClickListener onItemClickListener;

    public HistoryAdapter(Context c) {

        this.c = c;
    }

    @NonNull
    @Override
    public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(c).inflate(R.layout.item_history, parent, false);
        return new HistoryHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {

        Glide.with(c).load(thumbnails.get(position))
                .placeholder(new ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
                .into(holder.thumbnail);

        holder.caption.setText(titles.get(position));
        holder.itemView.setOnClickListener(v -> onItemClickListener.onClick(position));

        Glide.with(c)
                .load(avatars.get(position))
                .circleCrop()
                .placeholder(new ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
                .into(holder.avatar);
        holder.username.setText(usernames.get(position));

    }

    @Override
    public int getItemCount() {
        return thumbnails.size();
    }

    public void setDatas(List<String> thumbnails, List<String> titles, List<String> usernames, List<String> avatars) {
        this.thumbnails = thumbnails;
        this.titles = titles;
        this.usernames = usernames;
        this.avatars = avatars;
        notifyDataSetChanged();
    }

    class HistoryHolder extends RecyclerView.ViewHolder {

        ImageView thumbnail;
        TextView caption;
        ImageView avatar;
        TextView username;

        public HistoryHolder(@NonNull View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.img_thumbnail);
            caption = itemView.findViewById(R.id.tv_caption);
            avatar = itemView.findViewById(R.id.img_avatar);
            username = itemView.findViewById(R.id.tv_username);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {

        void onClick(int position);

    }
}
