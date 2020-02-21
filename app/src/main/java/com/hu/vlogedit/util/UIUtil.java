package com.hu.vlogedit.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import android.content.res.Resources;
import com.hu.vlogedit.App;

/**
 * 界面工具类
 * 包括一些和界面相关的辅助函数
 */

public class UIUtil {
    // 获取app的context
    public static Context getContext() {
        return App.appContext;
    }

    // 获取存在系统的资源
    public static Resources getResources() {
        return getContext().getResources();
    }
    // 获取屏幕宽度
    public static int getScreenWidth(Activity activity) {
        WindowManager manager = activity.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        return width;
    }

    // 获取屏幕宽度
    public static int getScreenWidth() {
        // DisplayMetrics是Android提供的记述屏幕的有关信息的一种结构，
        // 诸如其尺寸，密度和字体缩放的一般信息。
        DisplayMetrics dm = UIUtil.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }
}
