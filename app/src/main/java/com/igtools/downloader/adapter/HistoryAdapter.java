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

import java.util.List;

/**
 * @Author: desong
 * @Date: 2022/8/4
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryHolder> {
    List<MediaModel> datas;
    Context c;

    public HistoryAdapter(Context c, List<MediaModel> datas) {

        this.c = c;
        this.datas = datas;

    }

    @NonNull
    @Override
    public HistoryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(c).inflate(R.layout.view_history, parent, false);
        return new HistoryHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryHolder holder, int position) {

        Glide.with(c).load(datas.get(position).getThumbnailUrl())
                .placeholder(new ColorDrawable(ContextCompat.getColor(c, R.color.gray_1)))
                .into(holder.thumbnail);

        holder.caption.setText(datas.get(position).getTitle());

    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    public void setDatas(List<MediaModel> datas) {
        this.datas = datas;
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
}
