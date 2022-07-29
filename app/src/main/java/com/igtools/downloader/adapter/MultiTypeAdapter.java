package com.igtools.downloader.adapter;

import android.content.Context;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.igtools.downloader.R;
import com.igtools.downloader.models.MediaModel;
import com.youth.banner.adapter.BannerAdapter;
import com.youth.banner.util.BannerUtils;

import java.util.List;

/**
 * 自定义布局,多个不同UI切换
 */
public class MultiTypeAdapter extends BannerAdapter<MediaModel, RecyclerView.ViewHolder> {
    private Context context;
    private SparseArray<RecyclerView.ViewHolder> mVHMap = new SparseArray<>();

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
                mVHMap.append(position, imageHolder);
                Glide.with(context).load(data.getUrl()).into(imageHolder.imageView);

                break;
            case 2:
                VideoHolder videoHolder = (VideoHolder) holder;
                mVHMap.append(position, videoHolder);
                videoHolder.player.setUp(data.getUrl(), true, null);
                videoHolder.player.getBackButton().setVisibility(View.GONE);
                //增加封面
                ImageView imageView = new ImageView(context);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(context).load(data.getThumbnail()).into(imageView);
                videoHolder.player.setThumbImageView(imageView);
//                videoHolder.player.startPlayLogic();
                break;

        }
    }

    @Override
    public int getItemViewType(int position) {
        //直接获取真实的实体
        return getRealData(position).getType();

    }


    public SparseArray<RecyclerView.ViewHolder> getVHMap() {
        return mVHMap;
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

        public VideoHolder(@NonNull View itemView) {
            super(itemView);
            player = itemView.findViewById(R.id.player);

        }
    }
}