package com.hwgi.mintransfer;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws IOException, ParseException, InterruptedException {
        // config.properties 파일에서 API 키 읽기
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream("./config.properties")) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        // Google Cloud Translate 객체 생성
        String apiKey = properties.getProperty("google.api.key");
        Translate translate = TranslateOptions.newBuilder().setApiKey(apiKey).build().getService();

        // 원본 파일 경로
        String inputFilePath = "./origin.json";

        // 파싱된 파일을 저장할 디렉토리 경로
        String outputDirectory = "./parts/";
        new File(outputDirectory).mkdir();  // 디렉토리 생성

        // JSON 파일 파싱 및 분할 처리
        Reader reader = new FileReader(inputFilePath);
        JSONParser parser = new JSONParser();
        JSONObject input = (JSONObject) parser.parse(reader); // 전체 JSON 객체

        int fileIndex = 1;  // part 파일 번호
        int chunkSize = 10000; // 각 파일 당 10,000 줄
        int currentCount = 0;  // 현재 줄 수 카운트
        JSONObject part = new JSONObject();  // 각 파트 JSON 객체

        // Iterator를 사용하여 key-value 순차 접근
        Iterator<?> keys = input.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            part.put(key, input.get(key));
            currentCount++;

            // 10,000 줄마다 part 파일로 나눔
            if (currentCount == chunkSize || !keys.hasNext()) {
                // 파일로 저장
                String partFileName = outputDirectory + "part" + fileIndex + ".json";
                writeJsonToFile(part, partFileName);
                fileIndex++;
                currentCount = 0;
                part.clear();  // 새로 저장할 JSON 객체 초기화
            }
        }

        // ExecutorService를 사용하여 멀티 스레드로 각 파트 파일 번역
        ExecutorService executorService = Executors.newFixedThreadPool(5); // 5개의 스레드 사용
        for (int i = 1; i < fileIndex; i++) {
            final int partIndex = i;
            executorService.submit(() -> {
                try {
                    String partFilePath = outputDirectory + "part" + partIndex + ".json";
                    String translatedFilePath = outputDirectory + "ko-KR_part" + partIndex + ".json";
                    translateJsonFile(partFilePath, translatedFilePath, translate);
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            });
        }

        // ExecutorService 종료 및 모든 스레드가 끝날 때까지 대기
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.HOURS);

        // 번역이 끝난 후 파일 합치기
        String finalMergedFilePath = "./translated_full.json";
        mergeTranslatedFiles(outputDirectory, finalMergedFilePath, fileIndex);
    }

    // JSON 객체를 파일로 저장하는 메소드
    private static void writeJsonToFile(JSONObject json, String filePath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(json.toJSONString());
        }
    }

    // JSON 파일을 번역하는 메소드
    private static void translateJsonFile(String inputFilePath, String outputFilePath, Translate translate) throws IOException, ParseException {
        Reader reader = new FileReader(inputFilePath);
        JSONParser parser = new JSONParser();
        JSONObject input = (JSONObject) parser.parse(reader);
        JSONObject output = new JSONObject();
        int progress = input.size();
        long threadId = Thread.currentThread().getId(); // 현재 스레드 ID 가져오기

        input.keySet().forEach(key -> {
            Translation translation = translate.translate(
                    (String) input.get(key),
                    TranslateOption.sourceLanguage("ja"),
                    TranslateOption.targetLanguage("ko")
            );
            output.put(key, translation.getTranslatedText());
            System.out.println("Thread " + threadId + ": " + output.size() + " / " + progress); // 스레드 ID 출력
        });

        // 번역된 JSON 파일에 쓰기
        try (FileWriter file = new FileWriter(outputFilePath)) {
            file.write(output.toJSONString());
        }
    }

    // 모든 번역된 파일을 하나로 합치는 메소드
    private static void mergeTranslatedFiles(String directoryPath, String outputFilePath, int totalParts) throws IOException, ParseException {
        JSONObject mergedOutput = new JSONObject();
        JSONParser parser = new JSONParser();

        // 각 번역된 파일을 읽고 하나의 JSON 객체로 합침
        for (int i = 1; i < totalParts; i++) {
            String partFilePath = directoryPath + "ko-KR_part" + i + ".json";
            Reader reader = new FileReader(partFilePath);
            JSONObject part = (JSONObject) parser.parse(reader);
            mergedOutput.putAll(part);  // 파트 파일의 내용을 하나로 병합
        }

        // 병합된 파일을 출력
        writeJsonToFile(mergedOutput, outputFilePath);
        System.out.println("All parts merged into " + outputFilePath);
    }
}