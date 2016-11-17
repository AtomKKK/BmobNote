package com.jkxy.notebook.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.jkxy.notebook.R;

/**
 * Created by Think
 * <p/>
 * 程序设置界面,提供退出功能
 */
public class SettingFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 显示资源文件
        addPreferencesFromResource(R.xml.preferences);
    }

}
