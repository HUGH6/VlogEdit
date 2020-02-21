package com.hu.vlogedit.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hu.vlogedit.R;
import com.hu.vlogedit.base.BaseActivity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * MainActivity，暂时作为主Activity
 * 展示app的主要功能
 */
public class MainActivity extends BaseActivity {
    // PxPermissions库
    // 用于动态获取系统权限
    private RxPermissions mRxPermissions;

    // 获取对应Activity的layout
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }


    @Override
    protected void initView() {
        // 实例化RxPermissions对象，用于后续获取权限使用
        mRxPermissions = new RxPermissions(this);

        Button albumBtn = findViewById(R.id.album_btn);
        albumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeAlbum(view);
            }
        });
    }

    @Override
    protected void initData() {}

    /**
     * 打开视频相册
     * @param view
     */
    public void takeAlbum(View view) {
        // 请求权限
        mRxPermissions
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .subscribe(new Observer<Boolean>() {
                @Override
                public void onSubscribe(Disposable d) {
                    subscribe(d);
                }

                @Override
                public void onNext(Boolean granted) {
                    // 已获取权限
                    if (granted) {
                        // 启动视频相册的Activity
                        Intent intent = new Intent(MainActivity.this, VideoAlbumActivity.class);
                        startActivityForResult(intent, 100);
                    } else {
                        Toast.makeText(MainActivity.this, "请授权！", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Throwable e) {}

                @Override
                public void onComplete() {}
            });
    }
}