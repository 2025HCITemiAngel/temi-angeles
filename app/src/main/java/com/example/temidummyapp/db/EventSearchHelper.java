package com.example.temidummyapp.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventSearchHelper {

    private EventDatabase dbHelper;

    public EventSearchHelper(Context context) {
        dbHelper = new EventDatabase(context);
    }

    // 안전하게 null 체크된 버전 (다중 선택 지원)
    public ArrayList<HashMap<String, String>> search(List<String> 분야목록, String 사전모집, List<String> 대상목록, List<Integer> 최대시간목록) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<HashMap<String, String>> results = new ArrayList<>();

        String sql = "SELECT * FROM events WHERE 1=1 ";
        
        // 분야: OR 조건 (여러 분야 선택 가능)
        if (분야목록 != null && !분야목록.isEmpty()) {
            sql += "AND (";
            for (int i = 0; i < 분야목록.size(); i++) {
                if (i > 0) sql += " OR ";
                String 분야 = 분야목록.get(i);
                if (분야 != null) {
                    // SQL 인젝션 방지: 작은따옴표 이스케이프
                    분야 = 분야.replace("'", "''");
                    sql += "분야='" + 분야 + "'";
                }
            }
            sql += ") ";
        }
        
        if (사전모집 != null && 사전모집.length() > 0) sql += "AND 사전모집여부='" + 사전모집 + "' ";
        
        // 참여대상: OR 조건 (여러 대상 선택 가능)
        if (대상목록 != null && !대상목록.isEmpty()) {
            sql += "AND (";
            for (int i = 0; i < 대상목록.size(); i++) {
                if (i > 0) sql += " OR ";
                String 대상 = 대상목록.get(i);
                
                // 각 버튼에 해당하는 "이상" 버전과 "누구나" 포함
                if (대상 != null) {
                    if (대상.equals("초등학생")) {
                        // 초등학생 선택 시: 초등학생 이상 + 누구나
                        sql += "(참여대상='초등학생 이상' OR 참여대상='누구나')";
                    } else if (대상.equals("중학생")) {
                        // 중학생 선택 시: 중학생 이상 + 누구나
                        sql += "(참여대상='중학생 이상' OR 참여대상='누구나')";
                    } else if (대상.equals("고등학생")) {
                        // 고등학생 선택 시: 고등학생 이상 + 누구나
                        sql += "(참여대상='고등학생 이상' OR 참여대상='누구나')";
                    } else if (대상.equals("누구나")) {
                        // 누구나 선택 시: 누구나만
                        sql += "참여대상='누구나'";
                    } else {
                        // 기타 경우 (호환성 유지, SQL 인젝션 방지)
                        String safe대상 = 대상.replace("'", "''");
                        sql += "참여대상='" + safe대상 + "'";
                    }
                }
            }
            sql += ") ";
        }
        
        // 소요시간: OR 조건 (여러 시간 선택 가능)
        if (최대시간목록 != null && !최대시간목록.isEmpty()) {
            sql += "AND (";
            for (int i = 0; i < 최대시간목록.size(); i++) {
                if (i > 0) sql += " OR ";
                sql += "소요시간<=" + 최대시간목록.get(i);
            }
            sql += ") ";
        }

        Log.d("EventSearchHelper", "SQL: " + sql);
        
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(sql, null);
            if (cursor == null) {
                Log.e("EventSearchHelper", "Cursor is null");
                return results;
            }
            
            int idx분야 = cursor.getColumnIndex("분야");
            int idx대제목 = cursor.getColumnIndex("대제목");
            int idx한줄소개 = cursor.getColumnIndex("한줄소개");
            int idx사전모집 = cursor.getColumnIndex("사전모집여부");
            int idx대상 = cursor.getColumnIndex("참여대상");
            int idx시간 = cursor.getColumnIndex("소요시간");
            int idx시간원본 = cursor.getColumnIndex("소요시간_원본");
            int idx체험기간 = cursor.getColumnIndex("체험기간");
            int idx체험시간 = cursor.getColumnIndex("체험시간");
            int idxURL = cursor.getColumnIndex("url");

            while (cursor.moveToNext()) {
                HashMap<String, String> item = new HashMap<>();

                item.put("분야", safeGet(cursor, idx분야));
                item.put("대제목", safeGet(cursor, idx대제목));
                item.put("한줄소개", safeGet(cursor, idx한줄소개));
                item.put("사전모집여부", safeGet(cursor, idx사전모집));
                item.put("참여대상", safeGet(cursor, idx대상));
                item.put("소요시간", safeGet(cursor, idx시간));
                item.put("소요시간_원본", safeGet(cursor, idx시간원본));
                item.put("체험기간", safeGet(cursor, idx체험기간));
                item.put("체험시간", safeGet(cursor, idx체험시간));
                item.put("url", safeGet(cursor, idxURL));

                results.add(item);
            }
        } catch (Exception e) {
            Log.e("EventSearchHelper", "DB search error: " + e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    Log.e("EventSearchHelper", "Error closing cursor", e);
                }
            }
            if (db != null && db.isOpen()) {
                try {
                    db.close();
                } catch (Exception e) {
                    Log.e("EventSearchHelper", "Error closing db", e);
                }
            }
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
