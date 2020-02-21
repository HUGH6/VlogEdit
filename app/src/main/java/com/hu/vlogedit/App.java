package com.hu.vlogedit;

import android.app.Application;
import android.content.Context;

/**
 * @description
 * Android提供了一个Application类，每当应用程序启动的时候，系统就会自动将这个类进行初始化
 * 定制一个自己的Application类，以便于管理程序内一些全局的状态信息，比如说全局Context
 */
public class App extends Application {

    // 全局Context
    public static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }
}
