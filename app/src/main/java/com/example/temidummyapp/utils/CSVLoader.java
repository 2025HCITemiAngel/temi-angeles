package com.example.temidummyapp.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.content.ContentValues;
import android.util.Log;

import com.example.temidummyapp.db.EventDatabase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CSVLoader {

    public static void loadCSVToDB(Context context) {
        EventDatabase dbHelper = new EventDatabase(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        InputStream is = null;
        BufferedReader reader = null;

        try {
            // assets 폴더의 CSV 파일 읽기
            is = context.getAssets().open("coss_event_final_v2.csv");
            reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

            db.beginTransaction();
            // 중복 삽입 방지를 위해 기존 데이터 삭제
            db.delete("events", null, null);
            String line;
            boolean isFirst = true;

            while ((line = reader.readLine()) != null) {
                // 첫 번째 헤더 줄 건너뛰기
                if (isFirst) {
                    isFirst = false;
                    continue;
                }

                // 쉼표(,) 기준으로 나누기
                String[] tokens = line.split(",");

                String 분야 = safe(tokens, 0);
                String 대제목 = safe(tokens, 1);
                String 한줄소개 = safe(tokens, 2);
                String 사전모집여부 = safe(tokens, 3);
                String 참여대상 = safe(tokens, 4);
                String 소요시간 = safe(tokens, 5);
                String 체험기간 = safe(tokens, 6);
                String 체험시간 = safe(tokens, 7);
                String url = safe(tokens, 8);

                ContentValues values = new ContentValues();
                values.put("분야", 분야);
                values.put("대제목", 대제목);
                values.put("한줄소개", 한줄소개);
                values.put("사전모집여부", 사전모집여부);
                values.put("참여대상", 참여대상);

                Integer duration = parseMinutes(소요시간);
                if (duration != null) {
                    values.put("소요시간", duration);
                } else {
                    values.putNull("소요시간");
                }

                values.put("체험기간", 체험기간);
                values.put("체험시간", 체험시간);
                values.put("url", url);

                db.insert("events", null, values);
            }

            db.setTransactionSuccessful();

            Log.d("CSVLoader", "✅ CSV data successfully inserted into DB");

        } catch (Exception e) {
            Log.e("CSVLoader", "❌ CSV import failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignore) {
                }
            }
            if (db != null && db.inTransaction()) {
                db.endTransaction();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    private static String safe(String[] arr, int index) {
        if (arr.length > index && arr[index] != null) {
            return arr[index].trim().replace("'", "''");
        }
        return "";
    }

    private static Integer parseMinutes(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}
