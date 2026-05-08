package com.zixin.bloodglucoseprovider.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zixin.bloodglucoseapi.dto.PredictGlucoseRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PythonPredictClient {

    private static final Gson GSON = new Gson();

    @Value("${python.predict.service.url:http://127.0.0.1:8080}")
    private String pythonServiceUrl;

    @Value("${python.predict.service.api-key:}")
    private String apiKey;

    @Value("${python.predict.service.timeout:30}")
    private int timeoutSeconds;

    private final HttpClient httpClient;

    public PythonPredictClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public PythonPredictResponse predict(PredictGlucoseRequest request) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cbg", nullToEmptyList(request.getCbg()));
        requestBody.put("finger", nullToEmptyList(request.getFinger()));
        requestBody.put("basal", nullToEmptyList(request.getBasal()));
        requestBody.put("hr", nullToEmptyList(request.getHr()));
        requestBody.put("gsr", nullToEmptyList(request.getGsr()));
        requestBody.put("carbInput", nullToEmptyList(request.getCarbInput()));
        requestBody.put("bolus", nullToEmptyList(request.getBolus()));
        requestBody.put("mealStatus", request.getMealStatus() != null ? request.getMealStatus() : 1);
        requestBody.put("predictHours", request.getPredictHours() != null ? request.getPredictHours() : 3);

        String url = pythonServiceUrl + "/api/v1/glucose/predict";
        String jsonBody = GSON.toJson(requestBody);

        log.info("PythonPredictClient - 调用Python预测服务, url: {}, body: {}", url, jsonBody);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("X-API-Key", apiKey);
            }

            HttpRequest httpRequest = requestBuilder.build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            int statusCode = httpResponse.statusCode();
            String responseBody = httpResponse.body();

            log.info("PythonPredictClient - Python服务响应, statusCode: {}, body: {}", statusCode, responseBody);

            if (statusCode == 200) {
                Type responseType = new TypeToken<PythonPredictResponse>() {}.getType();
                PythonPredictResponse response = GSON.fromJson(responseBody, responseType);
                log.info("PythonPredictClient - 预测成功, predictHours: {}, resultCount: {}",
                        response.getPredictHours(), response.getGlucoseMgDl() != null ? response.getGlucoseMgDl().size() : 0);
                return response;
            } else {
                log.error("PythonPredictClient - Python服务返回错误, statusCode: {}, body: {}", statusCode, responseBody);
                throw new RuntimeException("Python预测服务返回错误: HTTP " + statusCode + ", " + responseBody);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("PythonPredictClient - 调用Python预测服务异常, url: {}", url, e);
            throw new RuntimeException("调用Python预测服务异常: " + e.getMessage(), e);
        }
    }

    private List<Double> nullToEmptyList(List<Double> list) {
        return list != null ? list : Collections.emptyList();
    }

    @lombok.Data
    public static class PythonPredictResponse {
        private int intervalMinutes;
        private int predictHours;
        private List<Double> glucoseMgDl;
    }
}
