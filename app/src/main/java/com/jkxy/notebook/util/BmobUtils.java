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

    private static String TAG = BmobUtils.class.getSimpleName();
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
                            Logger.d(TAG, "第" + i + "个数据批量添加成功：" + result.getCreatedAt() + "," + result.getObjectId() + "," + result.getUpdatedAt());

                        } else {
                            Logger.e(TAG, "第" + i + "个数据批量添加失败：" + ex.getMessage() + "," + ex.getErrorCode());
                        }
                    }


                    if (mNote != null) {
                        mNote.setImageUrl(mUrls);
                        Logger.d(TAG, "mNote.getImageUrl():" + mNote.getImageUrl().toString());
                    }
                } else {
                    Logger.d(TAG, e.getMessage());
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
        Logger.d(TAG, "即将上传的文件地址：" + picPath);
        final BmobFile bmobFile = new BmobFile(new File(picPath));
        bmobFile.uploadblock(new UploadFileListener() {

            @Override
            public void done(BmobException e) {
                if (e == null) {
                    Logger.d(TAG, "上传文件成功:" + bmobFile.getFileUrl());
                    Image image = new Image(picPath, bmobFile.getFileUrl());
                    saveData2Cloud(image);
                } else {
                    Logger.e(TAG, "上传文件失败：" + e.getMessage());
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
                    Logger.e("bmob", "失败：" + e.getMessage() + "," + e.getErrorCode());
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
/*
        final List<BmobObject> images = new ArrayList<BmobObject>();
        BmobFile.uploadBatch(filePaths, new UploadBatchListener() {
            @Override
            public void onSuccess(List<BmobFile> files, List<String> urls) {
                //1、files-上传完成后的BmobFile集合，是为了方便大家对其上传后的数据进行操作，例如你可以将该文件保存到表中
                //2、urls-上传文件的完整url地址
                Logger.d(TAG, "insertBatchDatasWithMany -onSuccess :" + urls.size() + "-----" + files + "----" + urls);
                if (urls.size() == filePaths.length) {//如果数量相等，则代表文件全部上传完成
                    //do something
                    for (int i = 0; i < filePaths.length; i++) {
                        Image image = new Image(filePaths[i], urls.get(i), files.get(i));
                        images.add(image);
                    }
                    if (images != null && images.size() > 0) {
                        BmobUtils.insertBatch(images);
//                        Note note = new Note();
                        mUrls.clear();
                        mUrls = urls;

                    }
                }
            }

            @Override
            public void onError(int statuscode, String errormsg) {
                Logger.d(TAG, "错误码" + statuscode + ",错误描述：" + errormsg);

            }

            @Override
            public void onProgress(int curIndex, int curPercent, int total, int totalPercent) {
                //1、curIndex--表示当前第几个文件正在上传
                //2、curPercent--表示当前上传文件的进度值（百分比）
                //3、total--表示总的上传文件数
                //4、totalPercent--表示总的上传进度（百分比）

                Logger.d(TAG, "insertBatchDatasWithMany -onProgress :" + curIndex + "---" + curPercent + "---" + total + "----" + totalPercent);
            }
        });*/
    }

    public static void saveNote2Cloud(Note note) {

        ArrayList<String> imagePaths = null;
        boolean isPicAdded = false;
        String content = note.getContent();
        //判断note是否有图片
        judgeIsPicsIncluded(note, imagePaths, isPicAdded, content);

        String bmobObjectId = note.getBmobObjectId();
        Logger.d(TAG, "当前bmobObjectId=" + bmobObjectId);
        if (!TextUtils.isEmpty(bmobObjectId)) {
            //说明曾经保存到cloud
            note.update(bmobObjectId, new UpdateListener() {
                @Override
                public void done(BmobException e) {
                    if (e == null) {
                        Logger.d(TAG, "服务器更新成功");
                    } else {
                        Logger.e(TAG, "服务器更新失败：" + e.getMessage() + "," + e.getErrorCode());
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
                        Logger.d(TAG, "服务器创建数据成功：" + objectId);
                    } else {
                        Logger.e(TAG, "插入服务器失败：" + e.getMessage() + "," + e.getErrorCode());
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
    public static void SyncNotes2LocalDB(Note note, NoteDAO mNoteDAO, int mNoteID) {
        String title = note.getTitle();
        String content = note.getContent();
        if (content.trim().equals("") && title.trim().equals("")) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        Logger.d(TAG, "SyncNotes2LocalDB mObjectId: " + mObjectId);
        if (!TextUtils.isEmpty(mObjectId)) {

            values.put("bmob_object_id", mObjectId);
        }
        int rowID = -1;
        note.setUpdateTime(TextFormatUtil.formatDate(new Date()));
        values.put("update_time", note.getUpdateTime());
        if (mNoteID != -1) {
            //修改note
            //此note在本地数据库里已经存在了
            rowID = mNoteDAO.updateNote(values, "_id=?", new String[]{mNoteID + ""});
            Logger.d(TAG, "本地数据库更新一条数据");
        } else {
            //新建note
            //新建note到本地数据库
            values.put("create_time", note.getUpdateTime());
            rowID = (int) mNoteDAO.insertNote(values);
            Logger.d(TAG, "本地数据库插入一条数据");
        }


        if (rowID != -1) {
            Logger.d(TAG, "content to save to DB: " + content);
        }
    }

    private static void setNote(Note note) {

        mNote = note;

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
                    Logger.e(TAG, "失败：" + e.getMessage() + "," + e.getErrorCode());
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
                    Logger.d(TAG, "下载成功,保存路径:" + s);
                } else {
                    Logger.e(TAG, "下载失败：" + e.getErrorCode() + "," + e.getMessage());
                }
            }

            @Override
            public void onProgress(Integer integer, long l) {
                Logger.d(TAG, "下载进度：" + value + "," + l);

            }
        });
    }


}
