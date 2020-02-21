package com.hu.vlogedit.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hu.vlogedit.App;
import com.hu.vlogedit.R;
import com.hu.vlogedit.activity.VideoAlbumActivity;
import com.hu.vlogedit.model.LocalVideoModel;
import com.hu.vlogedit.util.UIUtil;
import com.hu.vlogedit.util.VideoUtil;

import java.util.List;

/**
 * 自定义视频列表界面的适配器
 * 类VideoGridAdapter继承于RecyclerView.Adapter，
 * 并将泛型指定为自定义的内部类VideoHoler；
 */
public class VideoGridAdapter extends RecyclerView.Adapter<VideoGridAdapter.VideoHolder> {
    // 上下文环境
    private Context mContext;
    // 适配器数据源
    private List<LocalVideoModel> mDatas;
    // 单个元素的点击事件监听器
    private OnItemClickListener mOnItemClickListener;

    // 构造函数
    // 设置上下文context和数据源data
    public VideoGridAdapter(Context context, List<LocalVideoModel> data) {
        mContext = context;
        mDatas = data;
    }

    // 设置适配器数据
    public void setData(List<LocalVideoModel> datas) {
        mDatas = datas;
        // 数据集改变，通知列表进行变化
        notifyDataSetChanged();
    }

    // 获取元素个数
    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    // 设置每个子项的布局
    @Override
    public VideoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // LayoutInflater是一个视图填充器，主要是用于加载布局的；
        // 广泛应用于需要动态添加视图的时候
        return new VideoHolder(LayoutInflater.from(mContext).inflate(R.layout.item_grid_video, null, false));
    }

    // 负责将每个子项holder绑定数据。
    @Override
    public void onBindViewHolder(VideoHolder holder, int position) {
        LocalVideoModel model = mDatas.get(position);

        // Glide库,用于加载图片和视频缩略图
        Glide.with(mContext)
                .load(VideoUtil.getVideoFilePath(model.getVideoPath()))
                .into(holder.mIv);

        // 在子项上显示时长
        holder.mTvDuration.setText(VideoUtil.converSecondToTime(model.getDuration() / 1000));

        // 将positon和model的值保存到一个final变量中
        // 直接用positon和model会报错（不知道为什么）
        final int position_t = position;
        final LocalVideoModel model_t = model;
        // 在adapter中直接设置监听事件为我们自定义的点击事件
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(position_t, model_t);
                }
            }
        });
    }


    // 内部类VideoHolder，表示列表中的单个项
    // 视图控件持有类，是一个囊括本类对象里所有控件的容器，
    // 本类的作用也是为了方便，在后面不用重复去定义这些控件
    class VideoHolder extends RecyclerView.ViewHolder {
        // 视频缩略图
        ImageView mIv;
        // 视频时长
        TextView mTvDuration;

        // 构造函数
        public VideoHolder(View itemView) {
            super(itemView);
            // item布局中的缩略图
            mIv = itemView.findViewById(R.id.iv);
            // item布局中的视频时长
            mTvDuration = itemView.findViewById(R.id.tv_duration);
            // 单个item的宽度为屏幕的1/4
            int size = UIUtil.getScreenWidth() / 4;

            // LayoutParams是用于设置布局的一个类
            // 作用是子告诉控件如何布局
            FrameLayout.LayoutParams params = (LayoutParams)mIv.getLayoutParams();
            // 设置控件宽度
            params.width = size;
            // 设置控件高度
            params.height = size;
            // 应用设置
            mIv.setLayoutParams(params);
        }
    }

    // 该函数用于设置监听器
    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    // 在adapter中定义点击事件接口
    public interface OnItemClickListener {
        // 定义的元素点击事件
        void onItemClick(int position, LocalVideoModel model);
    }
}
