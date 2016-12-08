package com.jkxy.notebook.bean;
import java.util.List;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.datatype.BmobFile;

/**
 *  Created by Think on 2016/11/1.
 * <p/>
 * 记事的实体类
 * <p>
 * 添加image属性 20161024
 */
public class Note extends BmobObject {

    // 存放在数据库中的属性
    private String title;
    private String content;
    private String createTime;
    private String updateTime;


    private String bmobObjectId;

    // 辅助属性
    private String userName;
    private boolean isSync = false; // 标志该Note是否已经同步

    //添加image属性
    private List<BmobFile> images;

    public List<String> getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(List<String> imageUrl) {
        this.imageUrl = imageUrl;
    }

    private List<String> imageUrl;

    public Note(String title, String content, String createTime, List<BmobFile> images) {
        this.title = title;
        this.content = content;
        this.createTime = createTime;
        this.images = images;
    }

    public Note(String title, String content, String createTime) {
        this.title = title;
        this.content = content;
        this.createTime = createTime;
    }

    public Note() {
    }
    public String getBmobObjectId() {
        return bmobObjectId;
    }

    public void setBmobObjectId(String bmobObjectId) {
        this.bmobObjectId = bmobObjectId;
    }
    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isSync() {
        return isSync;
    }

    public void setIsSync(boolean isSync) {
        this.isSync = isSync;
    }

    public List<BmobFile> getImages() {
        return images;
    }

    public void setImages(List<BmobFile> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "Note{" +
                "title='" + title + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Note note = (Note) o;

        if (!title.equals(note.title)) return false;
        if (!content.equals(note.content)) return false;
        return createTime.equals(note.createTime);

    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + content.hashCode();
        result = 31 * result + createTime.hashCode();
        return result;
    }
}
