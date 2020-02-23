package com.hu.vlogedit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hu.vlogedit.R;
import com.hu.vlogedit.model.VideoEditInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @description 视频剪辑进度条中的RecyclerView的Adapter，存放视频各段提取的图片和时间节点的数据
 */
public class TrimVideoAdapter extends RecyclerView.Adapter {
    // 视频各段提取帧和时间节点列表
    private List<VideoEditInfo> lists = new ArrayList<>();
    // 用于动态填充视图
    private LayoutInflater inflater;

    private int itemW;
    private Context context;

    /**
     * 构造函数
     * @param context 上下文环境
     * @param itemW 视图中item的宽度
     */
    public TrimVideoAdapter(Context context, int itemW) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.itemW = itemW;
    }

    /**
     * 获取视图中绑定的数据源
     * @return
     */
    public List<VideoEditInfo> getDatas() {
        return lists;
    }

    /**
     * 设置每个子项的布局
     * @param parent
     * @param viewType
     * @return
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // 加载帧布局
        return new VideoHolder(inflater.inflate(R.layout.video_thumb_item_layout, parent, false));
    }

    /**
     * 为每个子项绑定数据
     * @param holder 子项泛型
     * @param position
     */
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        VideoHolder viewHolder = (VideoHolder) holder;
        // 加载帧图片到子项
        Glide.with(context)
            .load(lists.get(position).path)
            .into(viewHolder.img);
    }

    /**
     * 返回子项数量
     * @return
     */
    @Override
    public int getItemCount() {
        return lists.size();
    }

    /**
     * 子项泛型
     * 该对象表示RecyclerView中子项的类型
     */
    private final class VideoHolder extends RecyclerView.ViewHolder {
        // 帧图片
        public ImageView img;
        // 构造函数
        VideoHolder(View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.thumb);
            // 设置子项的布局参数
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) img
                .getLayoutParams();
            layoutParams.width = itemW; // 子项宽度
            img.setLayoutParams(layoutParams);
        }
    }

    /**
     * 给Adapter添加数据
     * @param info 视频各段的帧及时间点信息
     */
    public void addItemVideoInfo(VideoEditInfo info) {
        lists.add(info);
        // 向末尾处添加item
        notifyItemInserted(lists.size());
    }
}
