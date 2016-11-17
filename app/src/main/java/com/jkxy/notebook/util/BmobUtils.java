package com.jkxy.notebook.util;

import android.text.TextUtils;
import android.util.Log;

import com.jkxy.notebook.bean.Image;
import com.jkxy.notebook.bean.Note;

import java.io.File;
import java.util.ArrayList;
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
import cn.bmob.v3.listener.UploadBatchListener;
import cn.bmob.v3.listener.UploadFileListener;

import static android.R.attr.value;

/**
 * Created by Think on 2016/11/1.
 */

public class BmobUtils {

    private static String TAG = BmobUtils.class.getSimpleName();

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
                            Log.i(TAG, "第" + i + "个数据批量添加成功：" + result.getCreatedAt() + "," + result.getObjectId() + "," + result.getUpdatedAt());
                        } else {
                            Log.i(TAG, "第" + i + "个数据批量添加失败：" + ex.getMessage() + "," + ex.getErrorCode());
                        }
                    }
                } else {
                    Log.i(TAG, e.getMessage());
                }
            }
        });
    }

    /**
     * 上传单个文件
     *
     * @param picPath
     */
    public static void uploadSingleFile(String picPath) {
        final BmobFile bmobFile = new BmobFile(new File(picPath));
        bmobFile.uploadblock(new UploadFileListener() {

            @Override
            public void done(BmobException e) {
                if (e == null) {
                    Log.i(TAG, "上传文件成功:" + bmobFile.getFileUrl());
                } else {
                    Log.e(TAG, "上传文件失败：" + e.getMessage());
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
                        Log.e(TAG, "服务器查询文件失败：" + e.getMessage());
                    }
                }
            });

        }

        final List<BmobObject> images = new ArrayList<BmobObject>();
        BmobFile.uploadBatch(filePaths, new UploadBatchListener() {
            @Override
            public void onSuccess(List<BmobFile> files, List<String> urls) {
                //1、files-上传完成后的BmobFile集合，是为了方便大家对其上传后的数据进行操作，例如你可以将该文件保存到表中
                //2、urls-上传文件的完整url地址
                Log.i(TAG, "insertBatchDatasWithMany -onSuccess :" + urls.size() + "-----" + files + "----" + urls);
                if (urls.size() == filePaths.length) {//如果数量相等，则代表文件全部上传完成
                    //do something
                    for (int i = 0; i < filePaths.length; i++) {
                        Image image = new Image(filePaths[i], urls.get(i), files.get(i));
                        images.add(image);
                    }
                    if (images != null && images.size() > 0) {
                        BmobUtils.insertBatch(images);
                    }
                }
            }

            @Override
            public void onError(int statuscode, String errormsg) {
                Log.i(TAG, "错误码" + statuscode + ",错误描述：" + errormsg);

            }

            @Override
            public void onProgress(int curIndex, int curPercent, int total, int totalPercent) {
                //1、curIndex--表示当前第几个文件正在上传
                //2、curPercent--表示当前上传文件的进度值（百分比）
                //3、total--表示总的上传文件数
                //4、totalPercent--表示总的上传进度（百分比）

                Log.i(TAG, "insertBatchDatasWithMany -onProgress :" + curIndex + "---" + curPercent + "---" + total + "----" + totalPercent);
            }
        });
    }

    public static void saveNote2Cloud(Note note) {

        String bmobObjectId = note.getObjectId();
        if (!TextUtils.isEmpty(bmobObjectId)) {
            //说明曾经保存到cloud
            note.update(bmobObjectId, new UpdateListener() {
                @Override
                public void done(BmobException e) {
                    if (e == null) {
                        Log.i(TAG, "服务器更新成功");
                    } else {
                        Log.e(TAG, "服务器更新失败：" + e.getMessage() + "," + e.getErrorCode());
                    }
                }
            });
        } else {
            //未曾保存到cloud, insert到cloud
            note.save(new SaveListener<String>() {
                @Override
                public void done(String objectId, BmobException e) {
                    if (e == null) {
                        Log.i(TAG, "服务器创建数据成功：" + objectId);
                    } else {
                        Log.e(TAG, "插入服务器失败：" + e.getMessage() + "," + e.getErrorCode());
                    }
                }
            });
        }

        ArrayList<String> imagePaths = null;
        boolean isPicAdded = false;
        String content = note.getContent();
        //判断note是否有图片
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
                    Log.e(TAG, "失败：" + e.getMessage() + "," + e.getErrorCode());
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
                    Log.i(TAG, "下载成功,保存路径:" + s);
                } else {
                    Log.i(TAG, "下载失败：" + e.getErrorCode() + "," + e.getMessage());
                }
            }

            @Override
            public void onProgress(Integer integer, long l) {
                Log.e(TAG,"下载进度：" + value + "," + l);

            }
        });
    }


}
