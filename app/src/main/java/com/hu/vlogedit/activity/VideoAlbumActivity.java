package com.hu.vlogedit.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.hu.vlogedit.App;
import com.hu.vlogedit.R;
import com.hu.vlogedit.adapter.VideoGridAdapter;
import com.hu.vlogedit.base.BaseActivity;
import com.hu.vlogedit.model.LocalVideoModel;
import com.hu.vlogedit.util.VideoUtil;
import com.hu.vlogedit.view.DividerGridItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * 视频相册界面对应的Activity
 * 用于读取本地视频文件并展示
 */
public class VideoAlbumActivity extends BaseActivity implements VideoGridAdapter.OnItemClickListener{

    RecyclerView mRecyclerView;

    // 创建一个视频列表，用于存放读取的本地视频对象
    private List<LocalVideoModel> mLocalVideoModels = new ArrayList<>();

    // 创建一个Adapter
    // 用于链接视频列表的界面与数据
    private VideoGridAdapter mAdapter;

    // 获取对应Activity的layout
    @Override
    protected int getLayoutId() {
        return R.layout.activity_video_album;
    }

    // 初始化界面
    @Override
    protected void initView() {
        mRecyclerView = findViewById(R.id.recyclerView); // 界面中用于展示视频的列表
        // 设置RecyclerView为4列布局
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        // 确保RecyclerView尺寸是通过用户输入从而确保RecyclerView的尺寸是一个常数，避免重复计算
        mRecyclerView.setHasFixedSize(true);
        // 添加item间的自定义分割线
        mRecyclerView.addItemDecoration(new DividerGridItemDecoration(this));

        // 创建Adapter
        mAdapter = new VideoGridAdapter(this, mLocalVideoModels);
        // 将Adepter与RecyclerView绑定
        mRecyclerView.setAdapter(mAdapter);
        // 绑定item的点击监听器
        mAdapter.setOnItemClickListener(this);
    }

    // 初始化数据
    @Override
    protected void initData() {
        // 读取本地视频文件
        VideoUtil.getLocalVideoFiles(this)
                .subscribe(new Observer<ArrayList<LocalVideoModel>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        subscribe(d);
                    }

                    @Override
                    public void onNext(ArrayList<LocalVideoModel> localVideoModels) {
                        mLocalVideoModels = localVideoModels;
                        mAdapter.setData(mLocalVideoModels);
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {}
                });
    }

    // 覆写Adapter中的onItemClick方法
    // item被点击后，实际做的事情在这里
    @Override
    public void onItemClick(int position, LocalVideoModel model) {
        // 启动视频剪辑界面TrimVideoActivity
        Toast.makeText(App.appContext,String.valueOf(position), Toast.LENGTH_LONG).show();

        Intent intent = new Intent(this, TrimVideoActivity.class);
        intent.putExtra("videoPath", model.getVideoPath());
        startActivityForResult(intent, 100);
        finish();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // 清空视频列表
        mLocalVideoModels = null;
    }
}
