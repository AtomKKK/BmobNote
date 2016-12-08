package com.jkxy.notebook.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.jkxy.notebook.R;
import com.jkxy.notebook.activity.NoteDetailActivity;
import com.jkxy.notebook.adapter.ShowNoteAdapter;
import com.jkxy.notebook.db.NoteDAO;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Think
 * 根据标题查找所有匹配的note
 */
public class SearchNoteFragment extends Fragment {

    /*private EditText mEtSearch;
    private ListView mLvResult;
    private Button mBtnQuery;*/

    @BindView(R.id.id_et_search_title)
    EditText mEtSearch;
    @BindView(R.id.id_lv_found_note)
    ListView mLvResult;
    @BindView(R.id.id_btn_search)
    Button mBtnQuery;

    private CursorAdapter mAdapter;
    private NoteDAO mNoteDAO;
    private Cursor mCursor;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNoteDAO = new NoteDAO(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, root);
        /*mEtSearch = (EditText) root.findViewById(R.id.id_et_search_title);
        mLvResult = (ListView) root.findViewById(R.id.id_lv_found_note);
        mBtnQuery = (Button) root.findViewById(R.id.id_btn_search);*/
        // 查询操作
        mBtnQuery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = mEtSearch.getText().toString();
                if (title.length() > 0 && title != null) {
                    mCursor = mNoteDAO.queryNote("title like ? ", new String[]{"%" + title + "%"});
                }
                if (!mCursor.moveToNext()) {
                    Toast.makeText(getActivity(), "没有这个结果", Toast.LENGTH_SHORT).show();
                }
                mAdapter = new ShowNoteAdapter(getActivity(), mCursor);
                mLvResult.setAdapter(mAdapter);
            }
        });
        mLvResult.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) mAdapter.getItem(position); // CursorAdapter中getItem()返回特定的cursor对象
                int itemID = c.getInt(c.getColumnIndex("_id"));
                Intent intent = new Intent(getActivity(), NoteDetailActivity.class);
                intent.putExtra(NoteDetailActivity.SENDED_NOTE_ID, itemID);
                startActivity(intent);
            }
        });
        return root;
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mCursor != null) {
            mCursor.close();
        }
    }
}
