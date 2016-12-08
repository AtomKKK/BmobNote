package com.jkxy.notebook.fragment;


import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.jkxy.notebook.R;
import com.jkxy.notebook.activity.SplashActivity;
import com.jkxy.notebook.adapter.AllNoteListAdapter;
import com.jkxy.notebook.bean.Note;
import com.jkxy.notebook.db.NoteDAO;
import com.jkxy.notebook.service.AutoSyncService;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.CountListener;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.UpdateListener;

import static cn.bmob.v3.BmobUser.getCurrentUser;

/**
 * Created by Think
 * 显示所有Note,使用Loader实现异步加载
 */
public class AllNotesFragment extends Fragment {

    private NoteDAO mNoteDAO;
    /*
        @BindView(R.id.id_lv_all_note)
        ListView mLvNotes;*/
    @BindView(R.id.id_srl_refresh)
    SwipeRefreshLayout mSrlRefresh;
    @BindView(R.id.id_lv_all_note)
    RecyclerView mRecyclerView;
    private Unbinder unbinder;

    //    private CursorAdapter mAdapter;
    private Cursor mCursor;
    private final static int CONTEXT_UPDATE_ORDER = 0;
    private final static int CONTEXT_DELETE_ORDER = 1;
    private View root;

    private List<BmobObject> mSyncNotes = new ArrayList<>();
    private Set<Note> mAllNotes = new HashSet<>();
    List<BmobObject> images = new ArrayList<BmobObject>();


    // 自动同步功能需使用
    private Timer mTimer;
    private SyncStateReceiver mReceiver;
     private String mUserName;
    private AllNoteListAdapter mAllNoteListAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mUserName = getArguments().getString(SplashActivity.SEND_USER_NAME);

            Toast.makeText(getActivity(), "mUserName:" + mUserName, Toast.LENGTH_SHORT).show();
            mNoteDAO = new NoteDAO(getActivity(), mUserName + "_db");//用userid标记db
        } else {

            mNoteDAO = new NoteDAO(getActivity());
        }

        // 查询所有行
        mCursor = mNoteDAO.queryNote(null, null);
        // 获取同步信息,启动Service的计划任务
       /* if (MainActivity.IS_SYNC) {
            Intent intent = new Intent(getActivity(), AutoSyncService.class);
            intent.putExtra(SplashActivity.SEND_USER_NAME, mUserName);
            getActivity().startService(intent);
            // 注册广播
            mReceiver = new SyncStateReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.SYNC_BROADCAST_ACTION);
            getActivity().registerReceiver(mReceiver, filter);
        }*/
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater
            , ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_all_note, container, false);
        unbinder = ButterKnife.bind(this, root);

        initSwipeRefresh();
        initEvent();

        initRecyclerView();

//        initListView();

//        registerForContextMenu(mLvNotes);
        registerForContextMenu(mRecyclerView);

        return root;
    }

    /*private void initListView() {
        mAdapter = new ShowNoteAdapter(getActivity(), mCursor);
        mLvNotes.setAdapter(mAdapter);
        mLvNotes.setOnItemClickListener(this);
    }*/

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);
        mAllNoteListAdapter = new AllNoteListAdapter(mCursor, getActivity());
        mRecyclerView.setAdapter(mAllNoteListAdapter);
    }


    @Override
    public void onResume() {
        super.onResume();
        refreshNoteList();
//        mAdapter.changeCursor(mCursor);
    }

    @Override
    public void onPause() {
        super.onPause();
        mCursor.close();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
            Intent intent = new Intent(getActivity(), AutoSyncService.class);
            getActivity().stopService(intent);
        }*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();

    }

    /**
     * 上下文菜单的回调函数
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        Cursor cursor = mAllNoteListAdapter.getCursor();
        cursor.moveToPosition(mAllNoteListAdapter.getPosition());
        final int itemID = cursor.getInt(cursor.getColumnIndex("_id"));


        Logger.d("点击了note：" + itemID);
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
                                    Logger.d(itemID + "服务器删除成功");
                                } else {
                                    Logger.e(itemID + "服务器删除失败：" + e.getMessage() + "," + e.getErrorCode());
                                }
                            }
                        });
                    } else {
                        Logger.e("服务器上没有这条数据，删除失败。");
                    }

                    //从本地删除
                    int i = mNoteDAO.deleteNote("_id=?", new String[]{itemID + ""});
                    Logger.d("本地服务器删除" + i + "条数据。");

                    refreshNoteList();

                }

                break;
        }
        return super.onContextItemSelected(item);
    }

    private void refreshNoteList() {
        mCursor = mNoteDAO.queryNote(null, null);
        mAllNoteListAdapter.setCursor(mCursor);
        mAllNoteListAdapter.notifyDataSetChanged();
        mAllNoteListAdapter.notifyItemInserted(mAllNoteListAdapter.getItemCount());
    }


    private void downloadNotesFromCloud() {
        BmobQuery<Note> bmobQuery = new BmobQuery<>();
        bmobQuery.addWhereEqualTo("userName", getCurrentUser().getUsername());
        bmobQuery.setLimit(50); // 返回50条数据
        // 从服务器获取数据
        bmobQuery.findObjects(new FindListener<Note>() {
            @Override
            public void done(List<Note> list, BmobException e) {

                if (e == null) {

                    for (Note note : list) {
                        ContentValues values = new ContentValues();
                        values.put("title", note.getTitle());
                        values.put("content", note.getContent());
                        values.put("create_time", note.getCreateTime());
                        values.put("update_time", note.getUpdateTime());
                        values.put("bmob_object_id", note.getObjectId());
                        if (mNoteDAO.insertNote(values) == -1) {
                            mNoteDAO.updateNote(values, "bmob_object_id=?", new String[]{note.getObjectId()});
                        }
                    }
                    mSrlRefresh.setRefreshing(false);

                    refreshNoteList();

                    Snackbar.make(root, "同步完成", Snackbar.LENGTH_SHORT).show();

                } else {
                    mSrlRefresh.setRefreshing(false);
                    Snackbar.make(root, e.getMessage(), Snackbar.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void initSwipeRefresh() {
        mSrlRefresh.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        mSrlRefresh.setSize(SwipeRefreshLayout.DEFAULT);
        mSrlRefresh.setProgressViewEndTarget(true, 200);
    }

    private void initEvent() {
        // 在下拉刷新时同步数据
        mSrlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            /**
             * 下拉刷新
             */
            @Override
            public void onRefresh() {
                Cursor cursor = mNoteDAO.queryNote(null, null);
                final int localDBCount = cursor.getCount();
                Logger.d("本地数据库有[" + localDBCount + "]条数据。");


                BmobQuery<Note> query = new BmobQuery<Note>();
                query.addWhereEqualTo("userName", mUserName);
                query.count(Note.class, new CountListener() {
                    @Override
                    public void done(Integer count, BmobException e) {
                        if (e == null) {
                            Logger.d("Bmob云服务器上有[" + count + "]条数据");
                            if (count > localDBCount) {
                                //从云端下载note
                                downloadNotesFromCloud();
                            } else {
                                mSrlRefresh.setRefreshing(false);
                            }
                        } else {
                            Logger.e("失败：" + e.getMessage() + "," + e.getErrorCode());
                        }
                    }
                });


            }
        });
    }

    /**
     * 接收Service发送的广播,更新UI
     */
    class SyncStateReceiver extends BroadcastReceiver {
        //AutoSyncService每五分钟向云端更新一次，该Receiver负责显示更新成功与否
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(AutoSyncService.SEND_SYNC_STATE);
            Snackbar.make(root, state, Snackbar.LENGTH_SHORT).show();
        }
    }

}
