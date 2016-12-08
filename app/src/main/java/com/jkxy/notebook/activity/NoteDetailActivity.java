package com.jkxy.notebook.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.jkxy.notebook.R;
import com.jkxy.notebook.bean.Note;
import com.jkxy.notebook.db.NoteDAO;
import com.jkxy.notebook.util.BmobUtils;
import com.jkxy.notebook.util.SDCardUtils;
import com.jkxy.notebook.util.TextFormatUtil;
import com.jkxy.notebook.util.UriUtils;
import com.jkxy.notebook.widget.PictureAndTextEditorView;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by Think on 2016/11/1.
 */
public class NoteDetailActivity extends AppCompatActivity {

    public static final String SENDED_NOTE_ID = "note_id";

    private NoteDAO mNoteDAO;
    private Cursor mCursor;
    private Note mNote;
    private int mNoteID = -1;

    @BindView(R.id.id_et_title)
    EditText mEtTitle;
    @BindView(R.id.id_toolbar_detail)
    Toolbar mToolbar;
    @BindView(R.id.edit_text)
    PictureAndTextEditorView mEditText;
    @BindView(R.id.button_add_picture)
    Button mInsertPicBtn;
    @BindView(R.id.note_text)
    TextView mNoteTextView;
    @BindView(R.id.id_tv_title)
    TextView mTvTitle;

    private String TAG = NoteDetailActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        ButterKnife.bind(this);
//        mToolbar = (Toolbar) findViewById(R.id.id_toolbar_detail);
        initToolBar();
        initData();
        initView();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        String title = mEtTitle.getText().toString();
        String content = mEditText.getText().toString();
        mNote.setTitle(title);
        mNote.setContent(content);

        Logger.d(TAG, "onBackPressed BmobObjectId:" + mNote.getBmobObjectId());

        BmobUtils.saveNote2Cloud(mNote);
        BmobUtils.SyncNotes2LocalDB(mNote, mNoteDAO, mNoteID);

        if (mCursor != null) {
            mCursor.close();
        }
    }

    private void initToolBar() {
        mToolbar.setTitle("Note Detail");
        // 显示返回按钮
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        // 监听Back键,必须放在设置back键后面
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void initData() {
        Intent intent = getIntent();
        mNote = new Note("", "", TextFormatUtil.formatDate(new Date()));
        mNoteID = intent.getIntExtra(SENDED_NOTE_ID, -1);
        mNoteDAO = new NoteDAO(this);
        if (mNoteID != -1) {
            // 如果有ID参数,从数据库中获取信息
            // 进行查询必须使用?匹配参数
            mCursor = mNoteDAO.queryNote("_id=?", new String[]{mNoteID + ""});
            if (mCursor != null && mCursor.moveToNext()) {
                mNote.setTitle(mCursor.getString(mCursor.getColumnIndex("title")));
                mNote.setContent(mCursor.getString(mCursor.getColumnIndex("content")));
                mNote.setCreateTime(mCursor.getString(mCursor.getColumnIndex("create_time")));
//                mNote.setLocalId(mCursor.getString(mCursor.getColumnIndex("create_time")));
                mNote.setBmobObjectId(mCursor.getString(mCursor.getColumnIndex("bmob_object_id")));
                Logger.d(TAG, "BmobObjectId:" + mNote.getBmobObjectId());
            }

            mToolbar.setTitle(mNote.getTitle());

        }

    }


    private void initView() {

        if (mNoteID != -1) {
            //新建note
            mNoteTextView.setVisibility(View.GONE);
            mTvTitle.setVisibility(View.GONE);
            mEditText.setVisibility(View.VISIBLE);
            mEtTitle.setVisibility(View.VISIBLE);
            mEtTitle.setText(mNote.getTitle());
        } else {
            mNoteTextView.setVisibility(View.GONE);
            mTvTitle.setVisibility(View.GONE);

            mEditText.setVisibility(View.VISIBLE);
            mEtTitle.setVisibility(View.VISIBLE);
            mTvTitle.setText(mNote.getTitle());

        }
        initEvent();
        //显示note文字内容和图片内容
        displayNote();
    }

    private void displayNote() {
    /* 替换富文本 20161024 STA */
        String content = mNote.getContent();
        Logger.d(TAG, "content from DB :" + mNote.getContent());
        if (mNote.getContent().length() > 0) {
            String[] contents = content.split("☆");
            ArrayList<String> contentList = new ArrayList<>();

            String tempContent = "";

            for (int i = 0; i < contents.length; i++) {
                tempContent += contents[i] + "\n";
                if (!TextUtils.isEmpty(contents[i])) {

                    contentList.add(contents[i]);
                }

            }
            Logger.d(TAG, "final text to shown :" + tempContent);

            for (String line : contentList) {

                if (line == null) {
                    continue;
                }
                if (line.startsWith("/") && (line.endsWith(".jpg") || line.endsWith(".png"))) {
                    Logger.d(TAG, "line :" + line);

                    mEditText.insertBitmap(line);
                } else {
                    mEditText.append(line);
                }
            }


        }
    }

    private void initEvent() {
        mInsertPicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 0);
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Logger.d(TAG, "onTextChanged: " + mEditText.getmContentList().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                Logger.d(TAG, "afterTextChanged: " + mEditText.getText().toString());


            }
        });
        mEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d(TAG, "getSelectionStart: " + mEditText.getSelectionStart());
            }
        });

        mNoteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNoteTextView.setVisibility(View.GONE);
                mEditText.setVisibility(View.VISIBLE);
            }
        });

        mTvTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTvTitle.setVisibility(View.GONE);
                mEtTitle.setVisibility(View.VISIBLE);
            }
        });
    }


    /* 选择图片后压缩并且保存到外部空间的私有目录 20161024 STA */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 0:
                    if (data != null) {
                        Uri selectedImage = data.getData();
                        final String imagePath = UriUtils.getFilePathByUri(this, selectedImage);
                        Logger.d(TAG, "image path is : " + imagePath);
                        String temp[] = imagePath.split("/");
                        String imageName = null;
                        if (temp.length > 1) {
                            imageName = temp[temp.length - 1];

                        }
                        final String finalImageName = imageName;
                        final Bitmap bitmap = mEditText.getSmallBitmap(imagePath, 480, 800);
                        //插入换行符，使图片单独占一行
                        SpannableString newLine = new SpannableString("\n");
                        int index = mEditText.getSelectionStart();
                        mEditText.getEditableText().insert(index, newLine);//插入图片前换行
                        mEditText.insertBitmap(imagePath, bitmap);
                        mEditText.getEditableText().insert(mEditText.getSelectionStart(), newLine);//插入图片后换行

                        //用线程池添加

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //保存图片到本地
                                SDCardUtils.saveBitmapToSDCardPrivateFileDir(bitmap, Environment.DIRECTORY_PICTURES, finalImageName, NoteDetailActivity.this);
                            }
                        }).start();
                    }
                default:
                    break;
            }
        }
    }




}
