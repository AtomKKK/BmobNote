package com.jkxy.notebook.bean;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by Think on 2016/11/1.
 */

public class Image extends BmobObject {

    private String localPath;
    private BmobFile image;
    private String url;


    public Image(String localPath, String url, BmobFile image) {
        this.localPath = localPath;
        this.image = image;
        this.url = url;
    }

    public Image(String localPath, String url) {
        this.localPath = localPath;
        this.url = url;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public BmobFile getImage() {
        return image;
    }

    public void setImage(BmobFile image) {
        this.image = image;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
