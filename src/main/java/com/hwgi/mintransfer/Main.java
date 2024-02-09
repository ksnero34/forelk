package com.hwgi.mintransfer;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException, ParseException {
        // Google Cloud Translate 객체 생성
        Translate translate = TranslateOptions.newBuilder().setApiKey("").build()
                .getService();
        // 입력 파일 경로
        String inputFilePath = "part5.json";

        // 출력 파일 경로
        String outputFilePath = "ko-KR5.json";
        Reader reader = new FileReader(inputFilePath);

        JSONParser parser = new JSONParser();
        final JSONObject input = (JSONObject) parser.parse(reader); // 입력 json파일
        JSONObject output = new JSONObject(); // 출력 json파일
        int progress = input.size();

        input.keySet().forEach(key -> { //for each로 하나씩 돌리기
            Translation translation = translate.translate(
                    (String)input.get(key),
                    TranslateOption.sourceLanguage("ja"),
                    TranslateOption.targetLanguage("ko")
                    );
            output.put(key, translation.getTranslatedText());
            System.out.println(output.size()+" / "+progress);
        });

        //번역된 json 파일에 쓰기
        try {
			FileWriter file = new FileWriter(outputFilePath);
			file.write(output.toJSONString());
			file.flush();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

    }

}
