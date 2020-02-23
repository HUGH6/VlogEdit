package com.hu.vlogedit.view;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import java.lang.ref.WeakReference;

/**
 * 自定义加载框
 * @description 封装ProgressDialog
 */

public class NormalProgressDialog extends ProgressDialog implements DialogInterface.OnCancelListener {
    // 对上下文context的弱引用
    private WeakReference<Context> mContextWeakReference;
    // 添加了volatile关键字，保证了变量的可见性
    private volatile static NormalProgressDialog sDialog;
    // 构造函数
    public NormalProgressDialog(Context context) {
        this(context, -1);
    }
    // 构造函数
    public NormalProgressDialog(Context context, int theme) {
        super(context, theme);

        mContextWeakReference = new WeakReference<Context>(context);
        // 设置监听
        setOnCancelListener(this);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Context context = mContextWeakReference.get();
        if (context != null) {
            //取消网络请求...
        }
    }

    // 显示加载框
    public static synchronized void showLoading(Context context) {
        showLoading(context, "loading...");
    }

    // 显示加载框
    public static synchronized void showLoading(Context context, CharSequence message) {
        showLoading(context, message, true);
    }

    // 显示加载框
    public static synchronized void showLoading(Context context, CharSequence message, boolean cancelable) {
        try {
            if (sDialog != null && sDialog.isShowing()&& !((Activity) context).isFinishing()) {
                sDialog.dismiss();
            }
            sDialog = new NormalProgressDialog(context);
            sDialog.setMessage(message);
            sDialog.setCancelable(cancelable);

            if (sDialog != null && !sDialog.isShowing() && context != null && !((Activity) context).isFinishing()) {
                sDialog.show();
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    // 停止加载框
    public static synchronized void stopLoading() {
        try {
            if (sDialog != null && sDialog.isShowing()) {
                sDialog.dismiss();
            }
            sDialog = null;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    // 停止加载框
    public static  void stopLoading(Activity activity) {
        if (activity!=null&&!activity.isFinishing()){
            stopLoading();
        }
    }
}
