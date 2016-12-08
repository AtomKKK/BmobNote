package com.jkxy.notebook.service;

import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.jkxy.notebook.R;
import com.jkxy.notebook.activity.SplashActivity;
import com.jkxy.notebook.bean.Note;
import com.jkxy.notebook.config.Constants;
import com.jkxy.notebook.db.NoteDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.QueryListListener;

/**
 * Created by Think
 * <p/>
 * 自动向Bmob后台更新数据
 */
public class AutoSyncService extends Service {

    private List<BmobObject> mNotes = new ArrayList<>();
    private Timer mTimer = new Timer();
    private ContentResolver mResolver;

    public static final String SEND_SYNC_STATE = "STATE";
    private String mUserName;
    private NoteDAO mNoteDAO;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * 执行定时任务
     */
    @Override
    public void onCreate() {
        super.onCreate();
//        mResolver = getContentResolver();

        TimerTask task = new SyncTask();
        mTimer.schedule(task, 5000, 5 * 60 * 1000); // 五分钟更新一次
//        mTimer.schedule(task, 30000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mUserName = intent.getStringExtra(SplashActivity.SEND_USER_NAME);
            mNoteDAO = new NoteDAO(this, mUserName + "_db");
        }
        return super.onStartCommand(intent, flags, startId);
    }

    class SyncTask extends TimerTask {

        @Override
        public void run() {
            mNotes.clear();
            Cursor cursor = mNoteDAO.queryNote("is_sync = ?", new String[]{"false"});
//            Cursor cursor = mResolver.query(mUri, null, "is_sync = ?", new String[]{"false"}, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Note note = new Note();
                    int noteID = cursor.getInt(0);
                    note.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                    note.setContent(cursor.getString(cursor.getColumnIndex("content")));
                    note.setCreateTime(cursor.getString(cursor.getColumnIndex("create_time")));
                    note.setUserName(BmobUser.getCurrentUser(BmobUser.class).getUsername());
                    mNotes.add(note);
                    // 标记为已同步
                    ContentValues values = new ContentValues();
                    values.put("is_sync", "true");
//                    mResolver.update(mUri, values, "_id=?", new String[]{noteID + ""});
                    mNoteDAO.updateNote(values, "_id=?", new String[]{noteID + ""});
                }
                cursor.close();

                // 向服务器发送数据

                new BmobBatch().insertBatch(mNotes).doBatch(new QueryListListener<BatchResult>() {
                    @Override
                    public void done(List<BatchResult> list, BmobException e) {
                        if (e == null) {
                            sendSyncResult2Broadcast(getString(R.string.SYCN_COMPLETE));
                        } else {
                            sendSyncResult2Broadcast(e.getMessage());
                        }
                    }
                });
            }
        }

        public void sendSyncResult2Broadcast(String message) {
            Intent intent = new Intent();
            intent.setAction(Constants.SYNC_BROADCAST_ACTION);
            intent.putExtra(SEND_SYNC_STATE, message);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
    }
}
