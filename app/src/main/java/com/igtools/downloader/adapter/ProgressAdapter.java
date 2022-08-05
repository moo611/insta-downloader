package com.igtools.downloader.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.igtools.downloader.R;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: desong
 * @Date: 2022/8/1
 */
public class ProgressAdapter extends RecyclerView.Adapter<ProgressAdapter.ProgressHolder> {

    List<Integer> progressList = new ArrayList<>();
    Context c;

    public ProgressAdapter(Context c, List<Integer> progressList) {
        this.c = c;
        this.progressList = progressList;
    }

    @NonNull
    @Override
    public ProgressHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(c).inflate(R.layout.item_progress, parent, false);

        return new ProgressHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressHolder holder, int position) {

        int progress = progressList.get(position);
        holder.progressBar.setProgress(progress);
        holder.tvProgress.setText(progress + "%");

    }


    public void update(List<Integer> progressList) {
        this.progressList = progressList;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return progressList.size();
    }

    class ProgressHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        TextView tvProgress;

        public ProgressHolder(@NonNull View itemView) {
            super(itemView);

            progressBar = itemView.findViewById(R.id.progress_bar);
            tvProgress = itemView.findViewById(R.id.tv_progress);
        }
    }

}
