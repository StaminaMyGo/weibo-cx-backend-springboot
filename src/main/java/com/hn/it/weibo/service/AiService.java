package com.hn.it.weibo.service;

public interface AiService {
    /**
     * 发送请求到 AI 接口
     * @param url AI 接口地址
     * @param jsonBody 请求体 JSON 字符串
     * @param isAsync 是否异步
     * @return AI 返回的原始响应字符串
     */
    String send(String url, String jsonBody, boolean isAsync);
    
    /**
     * 查询异步任务状态
     * @param taskid 任务 ID
     * @return 任务状态 JSON 字符串
     */
    String askStateAsync(String taskid);
}
