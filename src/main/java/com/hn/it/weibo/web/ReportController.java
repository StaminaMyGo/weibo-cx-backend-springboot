package com.hn.it.weibo.web;

import com.hn.it.weibo.service.ReportService;
import com.hn.it.weibo.web.dto.RespEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ReportController {
    @Autowired
    private ReportService reportService;
    
    /**
     * 静态报表：按用户统计发布量
     */
    @GetMapping("/api/v1/q/reports/publish")
    public RespEntity getReportPublish(){
        return new RespEntity(2000,"报表查询成功",reportService.reportPublishByUser());
    }
    
    /**
     * AI 动态报表：根据自然语言生成 SQL 查询和图表配置
     * @param prompt 用户自然语言需求（如：“统计本周每天发布的微博数量”）
     * @param chartType 图表类型（如：bar, line, pie）
     */
    @GetMapping("/api/v1/g/reports/dynamic")
    public RespEntity getReportDynamic(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "bar") String chartType) {
        Map<String, Object> result = reportService.reportDynamic(prompt, chartType);
        return new RespEntity(2000, "查询成功", result);
    }
}
