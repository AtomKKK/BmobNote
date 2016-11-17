package com.jkxy.notebook;

import android.app.Application;

import com.jkxy.notebook.config.Constants;

import cn.bmob.v3.Bmob;

/**
 * Created by Think
 * <p/>
 * 用于初始化全局变量
 */
public class MyApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化Bmob
        Bmob.initialize(this, Constants.BMOB_API_KEY);
    }
}
