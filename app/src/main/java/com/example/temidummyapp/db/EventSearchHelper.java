package com.example.temidummyapp.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

public class EventSearchHelper {

    private EventDatabase dbHelper;

    public EventSearchHelper(Context context) {
        dbHelper = new EventDatabase(context);
    }

    // 안전하게 null 체크된 버전
    public ArrayList<HashMap<String, String>> search(String 분야, String 사전모집, String 대상, int 최대시간) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<HashMap<String, String>> results = new ArrayList<>();

        String sql = "SELECT * FROM events WHERE 1=1 ";
        if (분야 != null && 분야.length() > 0) sql += "AND 분야='" + 분야 + "' ";
        if (사전모집 != null && 사전모집.length() > 0) sql += "AND 사전모집여부='" + 사전모집 + "' ";
        if (대상 != null && 대상.length() > 0) sql += "AND 참여대상='" + 대상 + "' ";
        if (최대시간 > 0) sql += "AND 소요시간<=" + 최대시간;

        Cursor cursor = db.rawQuery(sql, null);
        try {
            int idx분야 = cursor.getColumnIndex("분야");
            int idx대제목 = cursor.getColumnIndex("대제목");
            int idx사전모집 = cursor.getColumnIndex("사전모집여부");
            int idx대상 = cursor.getColumnIndex("참여대상");
            int idx시간 = cursor.getColumnIndex("소요시간");
            int idxURL = cursor.getColumnIndex("url");

            while (cursor.moveToNext()) {
                HashMap<String, String> item = new HashMap<>();

                item.put("분야", safeGet(cursor, idx분야));
                item.put("대제목", safeGet(cursor, idx대제목));
                item.put("사전모집여부", safeGet(cursor, idx사전모집));
                item.put("참여대상", safeGet(cursor, idx대상));
                item.put("소요시간", safeGet(cursor, idx시간));
                item.put("url", safeGet(cursor, idxURL));

                results.add(item);
            }
        } catch (Exception e) {
            Log.e("EventSearchHelper", "DB search error: " + e.getMessage());
        } finally {
            cursor.close();
            db.close();
        }

        return results;
    }

    // 안전하게 인덱스 검사 후 값 반환
    private String safeGet(Cursor cursor, int columnIndex) {
        if (columnIndex >= 0) {
            String value = cursor.getString(columnIndex);
            return value != null ? value : "";
        }
        return "";
    }
}
