package com.hn.it.weibo.service;

import com.hn.it.weibo.entity.ReportItem;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public interface ReportService {
    public List<ReportItem> reportPublishByUser();
    
    /**
     * AI 动态报表查询（Text-to-SQL + Data-to-Chart）
     * @param prompt 用户自然语言需求
     * @param chartType 图表类型（如：bar, line, pie）
     * @return 包含查询数据和 ECharts 配置的结果
     */
    public Map<String, Object> reportDynamic(String prompt, String chartType);
    
    /**
     * AI 动态报表查询（带进度回调的流式版本）
     * @param prompt 用户自然语言需求
     * @param chartType 图表类型
     * @param progressCallback 进度回调函数 (step, message, progress)
     * @return 包含查询数据和 ECharts 配置的结果
     */
    Map<String, Object> reportDynamicWithProgress(
            String prompt, 
            String chartType,
            ProgressCallback progressCallback
    );
    
    /**
     * 进度回调接口
     */
    @FunctionalInterface
    interface ProgressCallback {
        void onProgress(int step, String message, int progress);
    }
}
