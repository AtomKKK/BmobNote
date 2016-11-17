package com.jkxy.notebook.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Think on 16-5-21.
 */
public class DBHelper extends SQLiteOpenHelper {
    public static String DB_NAME = "notes";
    public final static int DB_VERSON = 5;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSON);
    }

    public DBHelper(Context context, String DBName) {
        super(context, DBName, null, DB_VERSON);
        this.DB_NAME = DBName;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + DB_NAME + "(_id integer primary key autoincrement" +
                ",title text, content text,create_time text unique, is_sync text default 'false' not null, bmob_object_id text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + DB_NAME);
        onCreate(db);
    }
}
