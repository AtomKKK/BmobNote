package com.jkxy.notebook.util;

import android.content.ContentValues;
import android.text.TextUtils;

import com.jkxy.notebook.bean.Image;
import com.jkxy.notebook.bean.Note;
import com.jkxy.notebook.db.NoteDAO;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.DownloadFileListener;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.QueryListListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;
import cn.bmob.v3.listener.UploadFileListener;

import static android.R.attr.value;

/**
 * Created by Think on 2016/11/1.
 */

public class BmobUtils {


    private static List<String> mUrls = new ArrayList<>();
    private static Note mNote;
    private static String mObjectId;


    /**
     * 批量插入操作
     * insertBatch
     *
     * @return void
     * @throws
     */
    public static void insertBatch(List<BmobObject> files) {

        new BmobBatch().insertBatch(files).doBatch(new QueryListListener<BatchResult>() {
            @Override
            public void done(List<BatchResult> o, BmobException e) {
                if (e == null) {
                    for (int i = 0; i < o.size(); i++) {
                        BatchResult result = o.get(i);
                        BmobException ex = result.getError();
                        if (ex == null) {
                            Logger.d("第" + i + "个数据批量添加成功：" + result.getCreatedAt() + "," + result.getObjectId() + "," + result.getUpdatedAt());

                        } else {
                            Logger.e("第" + i + "个数据批量添加失败：" + ex.getMessage() + "," + ex.getErrorCode());
                        }
                    }


                    if (mNote != null) {
                        mNote.setImageUrl(mUrls);
                        Logger.d("mNote.getImageUrl():" + mNote.getImageUrl().toString());
                    }
                } else {
                    Logger.d(e.getMessage());
                }
            }
        });
    }

    /**
     * 上传单个文件
     *
     * @param picPath
     */
    public static void uploadSingleFile(final String picPath) {
        Logger.d("即将上传的文件地址：" + picPath);
        final BmobFile bmobFile = new BmobFile(new File(picPath));
        bmobFile.uploadblock(new UploadFileListener() {

            @Override
            public void done(BmobException e) {
                if (e == null) {
                    Logger.d("上传文件成功:" + bmobFile.getFileUrl());
                    mUrls.add(bmobFile.getFileUrl());
                    Image image = new Image(picPath, bmobFile.getFileUrl());
                    saveData2Cloud(image);
                } else {
                    Logger.e("上传文件失败：" + e.getMessage());
                }
            }
        });
    }


    private static void saveData2Cloud(BmobObject bmobObject) {
        bmobObject.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                if (e == null) {
                    Logger.d("创建数据成功：" + objectId);

                } else {
                    Logger.e("失败：" + e.getMessage() + "," + e.getErrorCode());
                }
            }
        });
    }

    /**
     * 上传图片并保存object image
     *
     * @param filePaths
     */
    public static void uploadPics2Cloud(final String[] filePaths) {

        //批量上传未保存在服务器的文件
        BmobQuery<Image> query = new BmobQuery<Image>();
        for (final String filePath : filePaths) {
            query.addWhereEqualTo("localPath", filePath);
            query.findObjects(new FindListener<Image>() {
                @Override
                public void done(List<Image> list, BmobException e) {
                    if (e == null) {
                        if (list.size() == 0) {
                            //此图片在云端不存在
                            uploadSingleFile(filePath);
                        }
                    } else {
                        Logger.e("服务器查询文件失败：" + e.getMessage());
                    }
                }
            });
        }

    }

    private static void saveNote2Cloud(Note note) {
        mUrls.clear();
        ArrayList<String> imagePaths = null;
        boolean isPicAdded = false;
        String content = note.getContent();
        //判断note是否有图片
        judgeIsPicsIncluded(note, imagePaths, isPicAdded, content);

        String bmobObjectId = note.getBmobObjectId();
        Logger.d("当前bmobObjectId=" + bmobObjectId);
        Logger.d("mUrls.size()=" + mUrls.size());
        if (mUrls.size() > 0) {
            note.setImageUrl(mUrls);
        }
        if (!TextUtils.isEmpty(bmobObjectId)) {
            //说明曾经保存到cloud
            note.update(bmobObjectId, new UpdateListener() {
                @Override
                public void done(BmobException e) {
                    if (e == null) {

                        Logger.d("服务器更新成功");
                    } else {
                        Logger.e("服务器更新失败：" + e.getMessage() + "," + e.getErrorCode());
                    }
                }
            });
        } else {
            //未曾保存到cloud, insert到cloud
            note.save(new SaveListener<String>() {
                @Override
                public void done(String objectId, BmobException e) {
                    if (e == null) {
                        mObjectId = objectId;
                        Logger.d("服务器创建数据成功：" + objectId);
                    } else {
                        Logger.e("插入服务器失败：" + e.getMessage() + "," + e.getErrorCode());
                    }
                }
            });
        }

    }

    public static void judgeIsPicsIncluded(Note note, ArrayList<String> imagePaths, boolean isPicAdded, String content) {
        if (content.trim().length() > 0 && content.contains("☆")) {
            String[] contents = content.split("☆");

            ArrayList<BmobFile> bmobFiles = new ArrayList<BmobFile>();
            int picQuantity = 0;

            for (String imagePath : contents) {
                if (imagePath.startsWith("/") && (imagePath.endsWith(".jpg") || imagePath.endsWith(".png"))) {
                    picQuantity++;
                    final BmobFile bmobFile = new BmobFile(new File(imagePath));
                    bmobFiles.add(bmobFile);

                    if (!isPicAdded) {
                        imagePaths = new ArrayList<String>();
                        isPicAdded = true;
                    }
                    if (imagePaths != null) {
                        imagePaths.add(imagePath);
                    }
                }
            }
            //如果有图片
            if (picQuantity > 0) {

                final String[] filePaths = (String[]) imagePaths.toArray(new String[0]);
                //上传图片
                BmobUtils.uploadPics2Cloud(filePaths);
            }
            note.setImages(bmobFiles);
        }
    }

    /**
     * 将笔记保存到本地数据库
     * 仅保存文字内容，图片内容以地址文本形式保存
     */
    public static void SyncNotes(Note note, NoteDAO mNoteDAO, int mNoteID) {

        saveNote2Local(note, mNoteDAO, mNoteID);
        saveNote2Cloud(note);
    }

    private static void saveNote2Local(Note note, NoteDAO mNoteDAO, int mNoteID) {
        String title = note.getTitle();
        String content = note.getContent();
        if (content.trim().equals("") && title.trim().equals("")) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        Logger.d("SyncNotes mObjectId: " + mObjectId);
        if (!TextUtils.isEmpty(mObjectId)) {
            note.setBmobObjectId(mObjectId);
            values.put("bmob_object_id", mObjectId);
        }
        int rowID = -1;
        note.setUpdateTime(TextFormatUtil.formatDate(new Date()));
        values.put("update_time", note.getUpdateTime());
        if (mNoteID != -1) {
            //修改note
            //此note在本地数据库里已经存在了
            rowID = mNoteDAO.updateNote(values, "_id=?", new String[]{mNoteID + ""});
            Logger.d("本地数据库更新一条数据");
        } else {
            //新建note
            //新建note到本地数据库
            values.put("create_time", note.getUpdateTime());
            rowID = (int) mNoteDAO.insertNote(values);
            Logger.d("本地数据库插入一条数据");
        }


        if (rowID != -1) {
            Logger.d("content to save to DB: " + content);
        }
    }


    public static String obtaiFileUrlByLocalPath(String localPath) {
        String url = null;
        Image image = queryImage("localPath", localPath);
        if (image != null) {
            url = image.getUrl();
        } else {
            return null;
        }
        return url;
    }

    public static Image queryImage(String selection, String value) {
        BmobQuery<Image> query = new BmobQuery<>();
        final Image[] image = {null};
        query.addWhereEqualTo(selection, value);
        query.findObjects(new FindListener<Image>() {
            @Override
            public void done(List<Image> list, BmobException e) {
                if (e == null) {
                    image[0] = list.get(0);

                } else {
                    Logger.e("失败：" + e.getMessage() + "," + e.getErrorCode());
                }
            }
        });
        return image[0];
    }

    public static void downloadFile(String filename, String url, File file) {

        BmobFile bmobFile = new BmobFile(filename, "", url);
        bmobFile.download(file, new DownloadFileListener() {
            @Override
            public void done(String s, BmobException e) {
                if (e == null) {
                    Logger.d("下载成功,保存路径:" + s);
                } else {
                    Logger.e("下载失败：" + e.getErrorCode() + "," + e.getMessage());
                }
            }

            @Override
            public void onProgress(Integer integer, long l) {
                Logger.d("下载进度：" + value + "," + l);

            }
        });
    }


}
