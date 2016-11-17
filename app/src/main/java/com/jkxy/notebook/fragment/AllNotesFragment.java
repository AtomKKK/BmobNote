package com.jkxy.notebook.fragment;


import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.jkxy.notebook.bean.Note;
import com.jkxy.notebook.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import cn.bmob.v3.BmobBatch;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BatchResult;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.QueryListListener;
import cn.bmob.v3.listener.UpdateListener;

/**
 * Created by Think
 * 显示所有Note,使用Loader实现异步加载
 */
public class AllNotesFragment extends Fragment implements AdapterView.OnItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {
    private com.jkxy.notebook.db.NoteDAO mNoteDAO;
    private ListView mLvNotes;
    private SwipeRefreshLayout mSrlRefresh;
    private CursorAdapter mAdapter;
    private Cursor mCursor;
    private final static int CONTEXT_UPDATE_ORDER = 0;
    private final static int CONTEXT_DELETE_ORDER = 1;
    private View root;

    private List<BmobObject> mSyncNotes = new ArrayList<>();
    private Set<Note> mAllNotes = new HashSet<>();
    List<BmobObject> images = new ArrayList<BmobObject>();
    public static final String TAG = AllNotesFragment.class.getSimpleName();

    // 自动同步功能需使用
    private Timer mTimer;
    private SyncStateReceiver mReceiver;
    //    private String mUserId;
    private String mUserName;

    public AllNotesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
//            mUserId = getArguments().getString(SplashActivity.SEND_USER_ID);
            mUserName = getArguments().getString(com.jkxy.notebook.activity.SplashActivity.SEND_USER_NAME);

            Toast.makeText(getActivity(), "mUserName:" + mUserName, Toast.LENGTH_SHORT).show();
            mNoteDAO = new com.jkxy.notebook.db.NoteDAO(getActivity(), mUserName + "_db");//用userid标记db
        } else {

            mNoteDAO = new com.jkxy.notebook.db.NoteDAO(getActivity());
        }

        // 查询所有行
        mCursor = mNoteDAO.queryNote(null, null);
        // 获取同步信息,启动Service的计划任务
        if (com.jkxy.notebook.activity.MainActivity.IS_SYNC) {
            Intent intent = new Intent(getActivity(), com.jkxy.notebook.service.AutoSyncService.class);
            intent.putExtra(com.jkxy.notebook.activity.SplashActivity.SEND_USER_NAME, mUserName);
            getActivity().startService(intent);
            // 注册广播
            mReceiver = new SyncStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(com.jkxy.notebook.config.Constants.SYNC_BROADCAST_ACTION);
            getActivity().registerReceiver(mReceiver, filter);
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater
            , ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_all_note, container, false);
        mLvNotes = (ListView) root.findViewById(R.id.id_lv_all_note);

        mSrlRefresh = (SwipeRefreshLayout) root.findViewById(R.id.id_srl_refresh);
        mSrlRefresh.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        mSrlRefresh.setSize(SwipeRefreshLayout.DEFAULT);
        mSrlRefresh.setProgressViewEndTarget(true, 200);
        // 在下拉刷新时同步数据
        mSrlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            ArrayList<String> imagePaths = null;
            boolean isPicAdded = false;

            /**
             * 下拉刷新时,向Bmob后台同步数据
             */
            @Override
            public void onRefresh() {
//                final Uri uri = Uri.parse("content://com.terry.NoteBook");
//                Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                Cursor cursor = mNoteDAO.queryNote(null, null);

                Toast.makeText(getActivity(), "cursor:" + cursor.getCount(), Toast.LENGTH_SHORT).show();
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        //查询本地数据库
                        while (cursor.moveToNext()) {
                            Note note = new Note();
                            int noteID = cursor.getInt(0);
                            note.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                            String contentString = cursor.getString(cursor.getColumnIndex("content"));
                            if (contentString.trim().length() > 0 && contentString.contains("☆")) {
                                String[] contents = contentString.split("☆");

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
                                    com.jkxy.notebook.util.BmobUtils.uploadPics2Cloud(filePaths);
                                }
                                note.setImages(bmobFiles);
                            }
                            note.setContent(cursor.getString(cursor.getColumnIndex("content")));
                            note.setCreateTime(cursor.getString(cursor.getColumnIndex("create_time")));
                            // 将本地数据库中所有笔记添加到集合mAllNotes中
                            mAllNotes.add(note);
                            // 获取当前用户
                            note.setUserName(BmobUser.getCurrentUser().getUsername());
                            if (cursor.getString(cursor.getColumnIndex("is_sync")).equals("false")) {
                                ContentValues values = new ContentValues();
                                values.put("is_sync", "true");
//                                getActivity().getContentResolver().update(uri, values, "_id=?", new String[]{noteID + ""});
                                mNoteDAO.updateNote(values, "_id=?", new String[]{noteID + ""});
                                //将is_sync 为false的note添加到集合mSyncNotes
                                mSyncNotes.add(note);
                            }
                        }
                        cursor.close();
                        // 批量向服务器上传数据数据
                        uploadNotes2Cloud();
                    } else {
                        downloadNotesFromCloud();
                    }
                }
            }
        });

        mAdapter = new com.jkxy.notebook.adapter.ShowNoteAdapter(getActivity(), mCursor);
//        getLoaderManager().initLoader(0, null, this);
        mLvNotes.setAdapter(mAdapter);
        mLvNotes.setOnItemClickListener(this);
        registerForContextMenu(mLvNotes);
        return root;
    }

    /*private void uploadPics2Cloud(final String[] filePaths) {
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

                Snackbar.make(root, "错误码" + statuscode + ",错误描述：" + errormsg, Snackbar.LENGTH_SHORT).show();
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
    }*/

    private void uploadNotes2Cloud() {
        new BmobBatch().insertBatch(mSyncNotes).
                doBatch(new QueryListListener<BatchResult>() {
                            @Override
                            public void done(List<BatchResult> list, BmobException e) {
                                if (e == null) {
                                    for (int i = 0; i < list.size(); i++) {
                                        BatchResult result = list.get(i);
                                        BmobException ex = result.getError();
                                        if (ex == null) {
                                            mSyncNotes.clear();
                                            // 从服务器下载本机没有的数据
                                            downloadNotesFromCloud();
                                            Log.i(TAG, "第" + i + "个数据批量添加成功：" + result.getCreatedAt() + "," + result.getObjectId() + "," + result.getUpdatedAt());

                                        } else {
                                            Log.i(TAG, "第" + i + "个数据批量添加失败：" + ex.getMessage() + "," + ex.getErrorCode());
                                        }
                                    }
                                } else {
                                    Log.i(TAG, "失败：" + e.getMessage() + "," + e.getErrorCode());
                                }
                            }
                        }

                );
    }

    private void downloadNotesFromCloud() {
        BmobQuery<Note> bmobQuery = new BmobQuery<>();
        bmobQuery.addWhereEqualTo("userName", BmobUser.getCurrentUser().getUsername());
        bmobQuery.setLimit(50); // 返回50条数据
        // 从服务器获取数据
        bmobQuery.findObjects(new FindListener<Note>() {
            @Override
            public void done(List<Note> list, BmobException e) {

                if (e == null) {
                    // 获取所有本地没有的数据
                    list.removeAll(mAllNotes);

//                    ContentResolver resolver = getActivity().getContentResolver();
                    // 将此数据写入数据库中
                    for (Note note : list) {
                        ContentValues values = new ContentValues();
                        values.put("title", note.getTitle());
                        values.put("content", note.getContent());
                        values.put("create_time", note.getCreateTime());
                        values.put("bmob_object_id", note.getObjectId());
                        values.put("is_sync", "true");
//                        resolver.insert(uri, values);
                        if (mNoteDAO.insertNote(values) == -1) {
                            mNoteDAO.updateNote(values, "bmob_object_id=?", new String[]{note.getObjectId()});
                        }
                    }
                    mAllNotes.clear();
                    mSrlRefresh.setRefreshing(false);
                    // 通知UI更新界面
//                    getLoaderManager().restartLoader(0, null, AllNotesFragment.this);
                    mAdapter.changeCursor(mNoteDAO.queryNote(null, null));
                    Snackbar.make(root, "同步完成", Snackbar.LENGTH_SHORT).show();

                } else {
                    mSrlRefresh.setRefreshing(false);
                    Snackbar.make(root, e.getMessage(), Snackbar.LENGTH_SHORT).show();

                }
            }
        });
    }

    /**
     * 此时重启Loader机制更新数据
     */
    @Override
    public void onResume() {
        super.onResume();
        mCursor = mNoteDAO.queryNote(null, null);
//        getLoaderManager().restartLoader(0, null, this);
        mAdapter.changeCursor(mCursor);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCursor.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
            Intent intent = new Intent(getActivity(), com.jkxy.notebook.service.AutoSyncService.class);
            getActivity().stopService(intent);
        }
    }

    /**
     * 上下文菜单的回调函数
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        //int position = mAdapter.getItem(info.position);
        int position = info.position; // list中的位置
        Cursor c = (Cursor) mAdapter.getItem(position); // CursorAdapter中getItem()返回特定的cursor对象
        final int itemID = c.getInt(c.getColumnIndex("_id"));
        switch (item.getOrder()) {
            case CONTEXT_UPDATE_ORDER: // 更新操作
                //Toast.makeText(getActivity(),"UPDATE",Toast.LENGTH_SHORT).show();
                break;
            case CONTEXT_DELETE_ORDER: // 删除操作
                Cursor deletCursor = mNoteDAO.queryNote("_id=?", new String[]{itemID + ""});
                if (deletCursor != null && deletCursor.getCount() > 0) {
                    deletCursor.moveToFirst();
                    String bmobObjectId = deletCursor.getString(deletCursor.getColumnIndex("bmob_object_id"));
                    if (!TextUtils.isEmpty(bmobObjectId)) {
                        Note note = new Note();
                        note.setObjectId(bmobObjectId);
                        //从服务器删除
                        note.delete(new UpdateListener() {
                            @Override
                            public void done(BmobException e) {
                                if (e == null) {
                                    Log.d(TAG, itemID + "服务器删除成功");
                                } else {
                                    Log.e(TAG, itemID + "服务器删除失败：" + e.getMessage() + "," + e.getErrorCode());
                                }
                            }
                        });
                    } else {
                        Log.d(TAG, "服务器上没有这条数据，删除失败。");
                    }

                    //从本地删除
                    int i = mNoteDAO.deleteNote("_id=?", new String[]{itemID + ""});
                    Log.d(TAG, "本地服务器删除" + i + "条数据。");

                }

                mAdapter.changeCursor(mNoteDAO.queryNote(null, null));

//                getLoaderManager().restartLoader(0, null, this);
                break;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * 创建上下文菜单
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Enter your choice:");
        menu.add(0, v.getId(), CONTEXT_UPDATE_ORDER, "Update");
        menu.add(0, v.getId(), CONTEXT_DELETE_ORDER, "Delete");
    }

    // 跳转到详情页
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = (Cursor) mAdapter.getItem(position); // CursorAdapter中getItem()返回特定的cursor对象
        int itemID = c.getInt(c.getColumnIndex("_id"));
//        Log.v("LOG", "AllNoteFragment itemID: " + itemID);
        Intent intent = new Intent(getActivity(), com.jkxy.notebook.activity.NoteDetailActivity.class);
        intent.putExtra(com.jkxy.notebook.activity.NoteDetailActivity.SENDED_NOTE_ID, itemID);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = Uri.parse("content://com.terry.NoteBook");

        return new CursorLoader(getActivity(), uri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


    /**
     * 接收Service发送的广播,更新UI
     */
    class SyncStateReceiver extends BroadcastReceiver {
        //AutoSyncService每五分钟向云端更新一次，该Receiver负责显示更新成功与否
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(com.jkxy.notebook.service.AutoSyncService.SEND_SYNC_STATE);
            Snackbar.make(root, state, Snackbar.LENGTH_SHORT).show();
        }
    }

}
