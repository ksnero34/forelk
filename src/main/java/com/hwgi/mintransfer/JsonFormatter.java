package com.hwgi.mintransfer;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.Iterator;

public class JsonFormatter {

    public static void main(String[] args) {
        // JSON 파일 경로
        String inputFilePath = "./translated_full.json";
        String outputFilePath = "./fixed_translated_full.json";

        try {
            // JSON 파일을 읽음
            Reader reader = new FileReader(inputFilePath);
            JSONTokener tokener = new JSONTokener(reader);
            JSONObject jsonObject = new JSONObject(tokener);

            // 모든 값을 처리하여 엔티티를 교정
            JSONObject fixedJsonObject = fixJsonValues(jsonObject);

            // 수정된 JSON을 파일에 다시 저장
            writeFormattedJsonToFile(fixedJsonObject, outputFilePath);
            System.out.println("JSON 파일이 수정되어 저장되었습니다: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // JSON의 모든 값을 교정하는 메소드
    private static JSONObject fixJsonValues(JSONObject jsonObject) {
        JSONObject fixedJsonObject = new JSONObject();

        // 각 키에 대해 값을 처리
        for (Iterator<String> it = jsonObject.keys(); it.hasNext();) {
            String key = it.next();
            String value = jsonObject.getString(key);

            // HTML 엔티티와 줄바꿈 문자를 원래대로 교체
            String fixedValue = fixHtmlEntitiesAndNewlines(value);

            // 수정된 값을 새로운 JSONObject에 추가
            fixedJsonObject.put(key, fixedValue);
        }

        return fixedJsonObject;
    }

    // HTML 엔티티와 줄바꿈 문자를 수정하는 메소드
    private static String fixHtmlEntitiesAndNewlines(String value) {
        // HTML 엔티티 변환: &#39;, &gt;, &lt;, &quot; 등을 원래 문자로 변환
        value = value.replace("&#39;", "'")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&quot;", "\"");

        // 잘못된 줄바꿈 문자가 있을 경우 이를 \n으로 변환
        value = value.replace("\\n", "\n");  // '\\n'을 실제 줄바꿈으로 변환

        return value;
    }

    // 포맷팅된 JSON을 파일로 저장하는 메소드
    private static void writeFormattedJsonToFile(JSONObject jsonObject, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // 들여쓰기 레벨 4로 JSON을 포맷팅하여 파일에 저장
            writer.write(jsonObject.toString(4)); // 4는 들여쓰기의 스페이스 수
        }
    }
}