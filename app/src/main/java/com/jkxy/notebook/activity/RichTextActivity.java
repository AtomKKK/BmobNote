package com.jkxy.notebook.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jkxy.notebook.R;
import com.jkxy.notebook.util.SDCardUtils;
import com.jkxy.notebook.util.UriUtils;
import com.jkxy.notebook.widget.PictureAndTextEditorView;

import java.util.ArrayList;
import java.util.List;

public class RichTextActivity extends AppCompatActivity {

    private PictureAndTextEditorView mEditText;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rich_text);

        mEditText = (PictureAndTextEditorView) findViewById(R.id.edit_text);
        mButton = (Button) findViewById(R.id.button_add_picture);
        ///storage/emulated/0/Mob/cn.xfz.app/cache/images/1450915925_4457.jpg
        ///storage/emulated/0/Pictures/1450770237621.jpg
        ///storage/emulated/0/Pictures/1450769835187.jpg
        ///storage/emulated/0/Mob/cn.xfz.app/cache/images/1450684805_82970.jpg
        List<String> list = new ArrayList<>();//这里是测试用的，对于图片地址，各位还是要自己设置一下
        list.add("你说");
        list.add(PictureAndTextEditorView.mBitmapTag + "/storage/emulated/0/Mob/cn.xfz.app/cache/images/1450915925_4457.jpg");
        list.add("我在哪");
        list.add(PictureAndTextEditorView.mBitmapTag + "/storage/emulated/0/Pictures/1450770237621.jpg");
        list.add("不告诉你");
        list.add(PictureAndTextEditorView.mBitmapTag + "/storage/emulated/0/Pictures/1450769835187.jpg");
        list.add(PictureAndTextEditorView.mBitmapTag + "/storage/emulated/0/Mob/cn.xfz.app/cache/images/1450684805_82970.jpg");
        list.add("嘿嘿");
        list.add(PictureAndTextEditorView.mBitmapTag + "/storage/emulated/0/Mob/cn.xfz.app/cache/images/1450915925_4457.jpg");

//        mEditText.setmContentList(list);


        mButton.setOnClickListener(new View.OnClickListener() {
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
                Log.i("EditActivity", mEditText.getmContentList().toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.i("afterTextChanged", mEditText.getText().toString());

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 0:
                    if (data != null) {
                        Uri selectedImage = data.getData();
                        final String imagePath = UriUtils.getFilePathByUri(this, selectedImage);
                        Log.d("RichTextActivity", "imageurl path is : " + imagePath);
                        String temp[] = imagePath.split("/");
                        String imageName = null;
                        if (temp.length > 1) {
                            imageName = temp[temp.length - 1];

                        }
                        final String finalImageName = imageName;
                        final Bitmap bitmap = mEditText.getSmallBitmap(imagePath, 480, 800);
                        mEditText.insertBitmap(imagePath, bitmap);
                        //用线程池添加

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //保存图片到本地
                                SDCardUtils.saveBitmapToSDCardPrivateFileDir(bitmap, Environment.DIRECTORY_PICTURES, finalImageName, RichTextActivity.this);
                            }
                        }).start();
                    }
                default:
                    break;
            }
        }
    }

}
