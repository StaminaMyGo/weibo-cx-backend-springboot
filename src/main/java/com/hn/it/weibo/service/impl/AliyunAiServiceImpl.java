package com.hn.it.weibo.service.impl;

import com.hn.it.weibo.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service("aliyunAiServiceImpl")
public class AliyunAiServiceImpl implements AiService {
//    sk-46a614aeb6e04c45bd76b201389e692d
    @Value("${my.aliyun.api-key}")
    private String apiKey;

    @Override
    public String send(String url, String jsonBody, boolean isAsync) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest.Builder builder = HttpRequest.newBuilder();
            builder.uri(URI.create(url));
            builder.header("Content-Type", "application/json");
            builder.header("Authorization", "Bearer " + apiKey);
            
            if (isAsync) {
                builder.header("X-DashScope-Async", "enable");
            }
            
            builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            HttpRequest request = builder.build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("AI 接口状态码: " + response.statusCode());
            System.out.println("AI 接口响应: " + response.body());
            
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI 接口调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String askStateAsync(String taskid) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/tasks/" + taskid))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("查询任务状态失败: " + e.getMessage(), e);
        }
    }
}
