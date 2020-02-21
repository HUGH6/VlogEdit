package com.hu.vlogedit.base;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.hu.vlogedit.R;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * 自定义抽象Activity基类
 * 继承自AppCompactActivity
 *
 */
public abstract class BaseActivity extends AppCompatActivity {
    // 用于统一管理多个Disposable的订阅
    private CompositeDisposable mDisposables = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        setContentView(getLayoutId());

        initView();
        initData();
    }

    // 获取当前Activity对应的layout
    protected abstract int getLayoutId();

    // 初始化
    protected void init() {};

    // 用于进行界面初始化等操作
    protected void initView() {};

    // 用于进行数据初始化等操作
    protected void initData() {};

    // 订阅
    public void subscribe(Disposable disposable) {
        mDisposables.add(disposable);
    }

    // 清除订阅
    public void unsubscribe() {
        if (mDisposables != null && !mDisposables.isDisposed()) {
            mDisposables.dispose();
            mDisposables.clear();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribe();
    }

    /**
     * 暂未使用的方法
     */

    //    protected void addFragment(int containerViewId, Fragment fragment) {
//        FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
//        transaction.add(containerViewId, fragment);
//        transaction.commit();
//    }
//


    // 菜单的响应事件，根据ItemId辨别响应事件
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            // 返回按钮操作事件
//            case android.R.id.home:
//                FragmentManager fm = getSupportFragmentManager();
//                if (fm != null && fm.getBackStackEntryCount() > 0) {
//                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
//                } else {
//                    onBackPressed();
//                }
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

}

