package com.jkxy.notebook;

import android.app.Application;

import com.jkxy.notebook.config.Constants;
import com.orhanobut.logger.Logger;

import cn.bmob.v3.Bmob;

/**
 * Created by Think
 * <p/>
 * 用于初始化全局变量
 */
public class MyApplication extends Application {


    private static final String TAG = "NOTEBOOK";

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.init(TAG);
        // 初始化Bmob
        Bmob.initialize(this, Constants.BMOB_API_KEY);

        /*Logger
                .init(TAG)                 // default PRETTYLOGGER or use just init()
                .methodCount(3)                 // default 2
                .hideThreadInfo()               // default shown
                .logLevel(LogLevel.NONE) ;       // default LogLevel.FULL
//                .methodOffset(2);                // default 0
//                .logAdapter(new AndroidLogAdapter()); //default AndroidLogAdapter*/
    }
}

