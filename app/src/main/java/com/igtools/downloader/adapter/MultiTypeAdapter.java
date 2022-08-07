package com.igtools.downloader.adapter;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.graphics.drawable.ColorDrawable;

import com.igtools.downloader.api.okhttp.OkhttpHelper;
import com.igtools.downloader.api.okhttp.OnDownloadListener;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.igtools.downloader.R;
import com.igtools.downloader.models.MediaModel;
import com.youth.banner.adapter.BannerAdapter;
import com.youth.banner.util.BannerUtils;

import java.io.File;
import java.util.List;

/**
 * 自定义布局,多个不同UI切换
 */
public class MultiTypeAdapter extends BannerAdapter<MediaModel, RecyclerView.ViewHolder> {
    private Context context;

    String TAG = "MultiTypeAdapter";

    public MultiTypeAdapter(Context context, List<MediaModel> mDatas) {
        super(mDatas);
        this.context = context;
    }

    @Override
    public RecyclerView.ViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 1:
                return new ImageHolder(BannerUtils.getView(parent, R.layout.banner_image));
            case 2:
                return new VideoHolder(BannerUtils.getView(parent, R.layout.banner_video));

        }
        return new ImageHolder(BannerUtils.getView(parent, R.layout.banner_image));
    }

    @Override
    public void onBindView(RecyclerView.ViewHolder holder, MediaModel data, int position, int size) {
        int viewType = holder.getItemViewType();
        switch (viewType) {
            case 1:
                ImageHolder imageHolder = (ImageHolder) holder;

                Glide.with(context)
                        .load(data.getThumbnailUrl())
                        .placeholder(new ColorDrawable(ContextCompat.getColor(context, R.color.gray_1)))
                        .into(imageHolder.imageView);

                break;
            case 2:
                VideoHolder videoHolder = (VideoHolder) holder;

                videoHolder.player.setUp(data.getVideoUrl(), true, null);
                videoHolder.player.getBackButton().setVisibility(View.GONE);
                //增加封面
                ImageView imageView = new ImageView(context);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(context).load(data.getThumbnailUrl()).placeholder(new ColorDrawable(ContextCompat.getColor(context, R.color.gray_1))).into(imageView);
                videoHolder.player.setThumbImageView(imageView);

                break;

        }
    }

    @Override
    public int getItemViewType(int position) {
        //直接获取真实的实体
        return getRealData(position).getMediaType();

    }

    class ImageHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);

        }
    }

    class VideoHolder extends RecyclerView.ViewHolder {
        public StandardGSYVideoPlayer player;

        public ProgressBar progressBar;

        public VideoHolder(@NonNull View itemView) {
            super(itemView);
            player = itemView.findViewById(R.id.player);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }

    }
}