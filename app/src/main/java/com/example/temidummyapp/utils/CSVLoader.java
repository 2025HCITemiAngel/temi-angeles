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
            is = context.getAssets().open("booth_program.csv");
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

                // CSV 파싱: 큰따옴표로 감싸진 쉼표를 올바르게 처리
                String[] tokens = parseCSVLine(line);

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
                
                // 원본 소요시간 문자열 저장
                values.put("소요시간_원본", 소요시간);

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

    /**
     * CSV 라인을 파싱하여 큰따옴표로 감싸진 쉼표를 올바르게 처리
     * 예: "나의 왕자님, 공주님을 찾아라!" -> 하나의 필드로 인식
     */
    private static String[] parseCSVLine(String line) {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentToken = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // 큰따옴표 시작/끝
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 이스케이프된 큰따옴표 ("")
                    currentToken.append('"');
                    i++; // 다음 문자 건너뛰기
                } else {
                    // 큰따옴표 토글
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 큰따옴표 밖의 쉼표는 필드 구분자
                tokens.add(currentToken.toString());
                currentToken = new StringBuilder();
            } else {
                // 일반 문자
                currentToken.append(c);
            }
        }

        // 마지막 토큰 추가
        tokens.add(currentToken.toString());

        return tokens.toArray(new String[0]);
    }

    private static String safe(String[] arr, int index) {
        if (arr.length > index && arr[index] != null) {
            String value = arr[index].trim();
            // 큰따옴표 제거 (파싱 후 남아있을 수 있음)
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            // SQL 인젝션 방지
            return value.replace("'", "''");
        }
        return "";
    }

    private static Integer parseMinutes(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;

        // 범위 형식 처리 (예: "5~10분", "10~20분" → 뒷자리 숫자 추출)
        Pattern rangePattern = Pattern.compile("(\\d+)\\s*~\\s*(\\d+)");
        Matcher rangeMatcher = rangePattern.matcher(trimmed);
        if (rangeMatcher.find()) {
            try {
                // 뒷자리 숫자(최대값) 추출
                return Integer.parseInt(rangeMatcher.group(2));
            } catch (NumberFormatException ignored) {
            }
        }

        // 일반 숫자 추출 (예: "5분", "10분")
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
