package com.example.yangtianrui.notebook.activity;

import android.content.ContentValues;
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
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yangtianrui.notebook.R;
import com.example.yangtianrui.notebook.bean.Note;
import com.example.yangtianrui.notebook.db.NoteDAO;
import com.example.yangtianrui.notebook.util.BmobUtils;
import com.example.yangtianrui.notebook.util.SDCardUtils;
import com.example.yangtianrui.notebook.util.TextFormatUtil;
import com.example.yangtianrui.notebook.util.UriUtils;
import com.example.yangtianrui.notebook.widget.PictureAndTextEditorView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import cn.bmob.v3.datatype.BmobFile;

/**
 * Created by yangtianrui on 16-5-22.
 */
public class NoteDetailActivity extends AppCompatActivity {

    public static final String SENDED_NOTE_ID = "note_id";
    private EditText mEtTitle;
    //    private LineEditText mEtContent;
    private Button mBtnModify;
    private Toolbar mToolbar;
    private NoteDAO mNoteDAO;
    private Cursor mCursor;
    private Note mNote;
    private int mNoteID = -1;
    private PictureAndTextEditorView mEditText;
    private Button mInsertPicBtn;
    private TextView mNoteTextView;
    private TextView mTvTitle;


    private String TAG = NoteDetailActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);
        mToolbar = (Toolbar) findViewById(R.id.id_toolbar_detail);
        mToolbar.setTitle("Node Detail");
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
        initData();
        initView();


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
            }

            mToolbar.setTitle(mNote.getTitle());

        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    private void initView() {

        mNoteTextView = (TextView) findViewById(R.id.note_text);
        mEditText = (PictureAndTextEditorView) findViewById(R.id.edit_text);

        mEtTitle = (EditText) findViewById(R.id.id_et_title);
        mTvTitle = (TextView) findViewById(R.id.id_tv_title);

        if (mNoteID != -1) {
            //新建note
            mNoteTextView.setVisibility(View.GONE);
            mTvTitle.setVisibility(View.GONE);
            mEditText.setVisibility(View.VISIBLE);
            mEtTitle.setVisibility(View.VISIBLE);
            mEtTitle.setText(mNote.getTitle());
        } else {
            mNoteTextView.setVisibility(View.VISIBLE);
            mTvTitle.setVisibility(View.VISIBLE);
            mEditText.setVisibility(View.GONE);

            mEtTitle.setVisibility(View.GONE);
            mTvTitle.setText(mNote.getTitle());
        }



        /* 替换富文本 20161024 STA */
        String content = mNote.getContent();
        Log.i(TAG, "content from DB :" + mNote.getContent());
        if (mNote.getContent().length() > 0) {
//            String[] contents = content.substring(1, content.length() - 1).split(",");
            String[] contents = content.split("☆");
            ArrayList<String> contentList = new ArrayList<>();

            String tempContent = "";

            for (int i = 0; i < contents.length; i++) {
                tempContent += contents[i] + "\n";
                if (contents[i] != "") {

                    contentList.add(contents[i]);
                }

            }
//            String textContent = tempContent.replaceAll("\\n", "\n");
            Log.i(TAG, "final text to shown :" + tempContent);

            for (String line : contentList) {
                if (line.startsWith("/") && (line.endsWith(".jpg") || line.endsWith(".png"))) {
                    mEditText.insertBitmap(line);
                } else {
                    mEditText.append(line);
                }

            }


//            mEditText.setmContentList(contentList);

            mNoteTextView.setText(tempContent);

        }
//        mEditText.setText(mNote.getContent());

//        mEditText.clearFocus();

        mInsertPicBtn = (Button) findViewById(R.id.button_add_picture);
        mInsertPicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                PicturePickUtils.selectPicFromLocal(RichTextActivity.this,888);//获取手机本地图片的代码，大家可以自行实现
                /*Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image*//*");
                startActivityForResult(intent, 0);*/

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
                Log.i(TAG, "onTextChanged: " + mEditText.getmContentList().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.i(TAG, "afterTextChanged: " + mEditText.getText().toString());


            }
        });
        mEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "getSelectionStart: " + mEditText.getSelectionStart());
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

        /* 替换富文本 20161024 END */
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /*@Override
    public void onClick(View v) {
        if (v.getId() == R.id.id_btn_modify) {
            SyncNotes2LocalDB();
        } else {
            onBackPressed();
        }
    }*/


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
                        Log.d(TAG, "image path is : " + imagePath);
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


    /**
     * 将笔记保存到本地数据库
     */
    private void SyncNotes2LocalDB() {
        String title = mEtTitle.getText().toString();
        String content = mEditText.getText().toString();
//        String content = mEditText.getmContentList().toString();
        if (content.trim().equals("") && title.trim().equals("")) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("content", content);
        values.put("create_time", mNote.getCreateTime());
        String createTime = mNote.getCreateTime();
        int rowID = -1;
        Cursor queryCursor = mNoteDAO.queryNote("create_time=?", new String[]{createTime});
        if (queryCursor != null && queryCursor.getCount() > 0) {
            //此note在本地数据库里已经存在了
            mNote.setUpdateTime(TextFormatUtil.formatDate(new Date()));
            rowID = mNoteDAO.updateNote(values, "_id=?", new String[]{mNoteID + ""});
        } else {
            //新建note到本地数据库
            rowID = (int) mNoteDAO.insertNote(values);
        }

        if (rowID != -1) {
            Log.i(TAG, "content to save to DB: " + content);
            Toast.makeText(this, "修改或添加成功", Toast.LENGTH_SHORT).show();
//            getContentResolver().notifyChange(Uri.parse("content://com.terry.NoteBook"), null);
        }
    }

    private void SyncNotes2Cloud() {
        String title = mEtTitle.getText().toString();
        String content = mEditText.getText().toString();
        if (content.trim().equals("") && title.trim().equals("")) {
            return;
        }

        Note note = new Note();
        note.setTitle(title);
        note.setContent(content);
        note.setCreateTime(mNote.getCreateTime());

        ArrayList<String> imagePaths = null;
        boolean isPicAdded = false;

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


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SyncNotes2LocalDB();
        if (mCursor != null) {
            mCursor.close();
        }
    }
}
