package com.hn.it.weibo.web;

import com.hn.it.weibo.service.ReportService;
import com.hn.it.weibo.web.dto.RespEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/v1/g/reports")
public class ReportController {
    @Autowired
    private ReportService reportService;
    
    // 异步线程池，用于处理 SSE 请求
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * 静态报表：按用户统计发布量
     */
    @GetMapping("/api/v1/q/reports/publish")
    public RespEntity getReportPublish(){
        return new RespEntity(2000,"报表查询成功",reportService.reportPublishByUser());
    }
    
    /**
     * AI 动态报表：根据自然语言生成 SQL 查询和图表配置（同步版本）
     * @param prompt 用户自然语言需求（如："统计本周每天发布的微博数量"）
     * @param chartType 图表类型（如：bar, line, pie）
     */
    @GetMapping("/dynamic")
    public RespEntity getReportDynamic(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "bar") String chartType) {
        Map<String, Object> result = reportService.reportDynamic(prompt, chartType);
        return new RespEntity(2000, "查询成功", result);
    }
    
    /**
     * AI 动态报表：SSE 流式推送版本（推荐）
     * 实时推送生成进度：SQL生成 → 数据查询 → 图表配置
     * @param prompt 用户自然语言需求
     * @param chartType 图表类型
     */
    @GetMapping(value = "/dynamic/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getReportDynamicStream(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "bar") String chartType) {
        
        SseEmitter emitter = new SseEmitter(180_000L); // 3分钟超时
        
        executor.execute(() -> {
            try {
                // 步骤1：开始生成 SQL
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("{\"step\":1,\"message\":\"AI 正在分析需求并生成 SQL...\",\"progress\":10}"));
                
                // 执行报表生成，并通过回调推送进度
                Map<String, Object> result = reportService.reportDynamicWithProgress(
                        prompt, 
                        chartType,
                        (step, message, progress) -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("progress")
                                        .data("{\"step\":" + step + ",\"message\":\"" + message + "\",\"progress\":" + progress + "}"));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                );
                
                // 发送最终结果
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(result));
                
                // 步骤完成
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data("{\"step\":4,\"message\":\"报表生成完成！\",\"progress\":100}"));
                
                emitter.complete();
                
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        
        // 设置超时和错误处理
        emitter.onTimeout(() -> {
            System.err.println("SSE 连接超时");
            emitter.complete();
        });
        
        emitter.onError(throwable -> {
            System.err.println("SSE 连接错误: " + throwable.getMessage());
            emitter.completeWithError(throwable);
        });
        
        return emitter;
    }
}
