package com.hn.it.weibo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hn.it.weibo.entity.Weibo;
import com.hn.it.weibo.mapper.WeiboMapper;
import com.hn.it.weibo.service.AiService;
import com.hn.it.weibo.service.WeiboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class WeiboServiceImpl extends ServiceImpl<WeiboMapper, Weibo> implements WeiboService {

    @Autowired
    @Qualifier("aliyunAiServiceImpl")
    private AiService aiService;
    
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    @Override
    public JSONObject aiCheck(String title, String content) {
        // 1. 构建 System Prompt（去除换行符）
        String systemPrompt = "你是一个严谨的内容审核员，需要审核用户发表的博客的标题和内容是否包含非法的内容。非法内容包括反动言论、粗言烂语、违反法规政策的相关词汇。输出规则：1.仅输出一个标准的JSON对象，不要包含markdown标记或解释性文字。2.如果内容违规，ispass设为0，并在reson字段说明原因。3.如果内容合规，ispass设为1，reson为空字符串。";

        // 2. 使用 JSONObject 构建请求 Body，避免 JSON 格式错误
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-plus");
        
        com.alibaba.fastjson.JSONArray messages = new com.alibaba.fastjson.JSONArray();
        
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", "标题:" + title + ",内容:" + content);
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        
        String jsonBody = requestBody.toJSONString();
        System.out.println("=== 发送给 AI 的请求 Body ===");
        System.out.println(jsonBody);

        try {
            String result = aiService.send(API_URL, jsonBody, false);
            
            // 解析 AI 返回的 JSON
            JSONObject resultObj = JSON.parseObject(result);
            JSONObject firstChoice = resultObj.getJSONArray("choices").getJSONObject(0);
            String aiContent = firstChoice.getJSONObject("message").getString("content");
            
            // 清理可能存在的 markdown 标记或多余空格
            aiContent = aiContent.replace("```json", "").replace("```", "").trim();
            
            return JSON.parseObject(aiContent);
        } catch (Exception e) {
            throw new RuntimeException("AI 审核服务异常，请稍后重试", e);
        }
    }
}