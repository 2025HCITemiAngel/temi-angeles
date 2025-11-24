package com.example.temidummyapp.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EventDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "event_db";
    private static final int DB_VERSION = 3;

    public EventDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 테이블 생성 (Excel 컬럼에 맞게)
        String createTable = "CREATE TABLE IF NOT EXISTS events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "분야 TEXT, " +
                "대제목 TEXT, " +
                "한줄소개 TEXT, " +
                "사전모집여부 TEXT, " +
                "참여대상 TEXT, " +
                "소요시간 INTEGER, " +
                "소요시간_원본 TEXT, " +
                "체험기간 TEXT, " +
                "체험시간 TEXT, " +
                "url TEXT, " +
                "이미지파일 TEXT" +
                ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // 소요시간_원본 컬럼 추가
            db.execSQL("ALTER TABLE events ADD COLUMN 소요시간_원본 TEXT");
        }
        if (oldVersion < 3) {
            // 이미지파일 컬럼 추가
            db.execSQL("ALTER TABLE events ADD COLUMN 이미지파일 TEXT");
        }
    }
}