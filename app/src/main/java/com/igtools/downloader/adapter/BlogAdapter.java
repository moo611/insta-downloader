package com.igtools.downloader.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.igtools.downloader.R;
import com.igtools.downloader.activities.BlogDetailsActivity;
import com.igtools.downloader.models.BlogModel;

import java.util.ArrayList;
import java.util.List;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.BlogViewHolder> {

    Context c;
    List<BlogModel> blogs = new ArrayList<>();

    public BlogAdapter(Context c, List<BlogModel> blogs) {
        this.c = c;
        this.blogs = blogs;
    }

    public void setDatas(List<BlogModel> blogs) {
        this.blogs=blogs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BlogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(c).inflate(R.layout.item_blog, parent, false);
        return new BlogViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BlogViewHolder holder, int position) {

        Glide.with(c).load(blogs.get(position).getDisplayUrl())
                .placeholder(new ColorDrawable(ContextCompat.getColor(c, R.color.gray_1))).into(holder.imgThumbnail);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                c.startActivity(new Intent(c, BlogDetailsActivity.class).putExtra("shortCode",blogs.get(position).getShortCode()));

            }
        });

        String typename = blogs.get(position).getTypeName();
        if (typename.equals("GraphSidecar")){
            holder.imgCollections.setVisibility(View.VISIBLE);
        }else{
            holder.imgCollections.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return blogs.size();
    }

    class BlogViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumbnail;
        ImageView imgCollections;
        public BlogViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumbnail = itemView.findViewById(R.id.img_thumbnail);
            imgCollections = itemView.findViewById(R.id.img_collections);
        }
    }

}
