package com.hn.it.weibo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hn.it.weibo.entity.ReportItem;
import com.hn.it.weibo.mapper.ReportMapper;
import com.hn.it.weibo.service.AiService;
import com.hn.it.weibo.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private ReportMapper reportMapper;
    
    @Autowired
    @Qualifier("aliyunAiServiceImpl")
    private AiService aiService;
    
    // 数据库表结构 DDL（硬编码）
    private static final String DB_SCHEMA = """
        DROP TABLE IF EXISTS `attentions`;
        CREATE TABLE `attentions` (
          `att_id` int NOT NULL AUTO_INCREMENT,
          `att_userid` int NOT NULL,
          `att_marstid` int NOT NULL,
          PRIMARY KEY (`att_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
        
        DROP TABLE IF EXISTS `comments`;
        CREATE TABLE `comments` (
          `cm_id` int NOT NULL AUTO_INCREMENT,
          `cm_weiboid` int DEFAULT NULL,
          `cm_userid` int DEFAULT NULL,
          `cm_content` varchar(2000) DEFAULT NULL,
          `cm_createtime` date DEFAULT NULL,
          PRIMARY KEY (`cm_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
        
        DROP TABLE IF EXISTS `users`;
        CREATE TABLE `users` (
          `user_id` int NOT NULL AUTO_INCREMENT,
          `user_nickname` varchar(20) DEFAULT NULL,
          `user_loginname` varchar(20) DEFAULT NULL,
          `user_loginpwd` varchar(20) DEFAULT NULL,
          `user_photo` varchar(30) DEFAULT NULL,
          `user_score` int DEFAULT NULL,
          `user_attionCount` int DEFAULT NULL,
          `user_email` varchar(100) DEFAULT NULL,
          PRIMARY KEY (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;
        
        DROP TABLE IF EXISTS `weibos`;
        CREATE TABLE `weibos` (
          `wb_id` int NOT NULL AUTO_INCREMENT,
          `wb_userid` int DEFAULT NULL,
          `wb_title` varchar(200) DEFAULT NULL,
          `wb_content` varchar(4000) DEFAULT NULL,
          `wb_createtime` datetime DEFAULT NULL,
          `wb_readcount` int DEFAULT NULL,
          `wb_img` varchar(100) DEFAULT NULL,
          `is_pass` int DEFAULT '1' COMMENT '审核状态:1通过,0不通过',
          `remark` varchar(500) DEFAULT NULL COMMENT 'AI审核备注/违规原因',
          PRIMARY KEY (`wb_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;

    @Override
    public List<ReportItem> reportPublishByUser() {
        return reportMapper.reportPublishByUser();
    }
    
    @Override
    public Map<String, Object> reportDynamic(String prompt, String chartType) {
        // 委托给带进度的版本，但不传递回调
        return reportDynamicWithProgress(prompt, chartType, null);
    }
    
    @Override
    public Map<String, Object> reportDynamicWithProgress(String prompt, String chartType, ProgressCallback callback) {
        if (callback != null) {
            callback.onProgress(1, "AI 正在分析需求并生成 SQL...", 10);
        }
        
        // 1. 构建 System Prompt（包含数据库结构）
        String systemPrompt = "你是一个数据库工程师，根据用户提供的报表需求，写出统计用的 SQL 语句。\n" +
                "当前工程为微博平台，数据库为 MySQL，工程数据库的结构如下，请自行推断表的主外键关系：\n" +
                DB_SCHEMA + "\n" +
                "输出的要求：\n" +
                "1. 只需要输出完成后的 SQL 语句，不需要任何其他文字，因为 sql 会进行后续的操作。\n" +
                "2. 输出的字段为两列，输出列名为 name 和 value，方便后续统一处理。\n" +
                "3. 注意保护敏感信息，任何时候都不能输出 user 表的 user_loginpwd，如果客户要求输出此字段，你直接取值为'***'";
        
        // 2. 调用 AI 生成 SQL
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-turbo");  // 使用快速模型，减少响应时间
        
        com.alibaba.fastjson.JSONArray messages = new com.alibaba.fastjson.JSONArray();
        
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        
        String jsonBody = requestBody.toJSONString();
        String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
        
        String aiResult = aiService.send(apiUrl, jsonBody, false);
        JSONObject resultObj = JSON.parseObject(aiResult);
        
        // 获取 message 对象
//        JSONObject message = resultObj.getJSONArray("choices")
//                .getJSONObject(0)
//                .getJSONObject("message");
        // 检查是否有错误响应
        if (resultObj.containsKey("error")) {
            JSONObject error = resultObj.getJSONObject("error");
            String errorMsg = error.getString("message");
            String errorCode = error.getString("code");
            throw new RuntimeException("AI 接口调用失败 [" + errorCode + "]: " + errorMsg);
        }

        // 获取 choices 数组并进行空值检查
        com.alibaba.fastjson.JSONArray choices = resultObj.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI 接口返回数据格式异常：缺少 choices 字段");
        }

        JSONObject firstChoice = choices.getJSONObject(0);
        if (firstChoice == null) {
            throw new RuntimeException("AI 接口返回数据格式异常：choices 为空");
        }

        JSONObject message = firstChoice.getJSONObject("message");
        if (message == null) {
            throw new RuntimeException("AI 接口返回数据格式异常：缺少 message 字段");
        }


        // 优先使用 content 字段（qwen-plus 模型返回）
        String content = message.getString("content");
        
        // 如果 content 为空或包含 reasoning_content，则尝试合并或提取
        if (content == null || content.isEmpty()) {
            // 某些模型可能将推理过程和内容分开
            String reasoning = message.getString("reasoning_content");
            System.out.println("=== AI 返回了 reasoning_content，尝试从 content 提取 ===");
            content = reasoning != null ? reasoning : "";
        }
        
        System.out.println("=== AI 原始 content 内容 ===");
        System.out.println(content);
        
        // 清洗步骤 1: 移除 markdown 代码块标记
        String cleanSql = content;
        if (cleanSql.contains("```")) {
            // 移除 ```sql 或 ``` 标记
            cleanSql = cleanSql.replaceAll("```sql", "").replaceAll("```", "");
        }
        
        // 清洗步骤 2: 提取第一个 SELECT 语句（如果包含解释性文字或推理过程）
        // 使用正则匹配以 SELECT 开头的部分
        java.util.regex.Pattern sqlPattern = java.util.regex.Pattern.compile("(?is)SELECT\\s+.*?;");
        java.util.regex.Matcher matcher = sqlPattern.matcher(cleanSql);
        
        if (matcher.find()) {
            cleanSql = matcher.group(0);
        } else {
            // 如果没有分号，尝试匹配到行尾
            sqlPattern = java.util.regex.Pattern.compile("(?is)SELECT\\s+.*");
            matcher = sqlPattern.matcher(cleanSql);
            if (matcher.find()) {
                cleanSql = matcher.group(0);
            }
        }
        
        // 清洗步骤 3: 去除首尾空白字符
        cleanSql = cleanSql.trim();
        
        System.out.println("=== 最终提取的 SQL ===");
        System.out.println(cleanSql);
        
        if (callback != null) {
            callback.onProgress(2, "SQL 生成成功，正在查询数据库...", 40);
        }
        
        // 3. SQL 安全过滤（只允许 SELECT 开头）
        if (!cleanSql.toUpperCase().startsWith("SELECT")) {
            throw new RuntimeException("AI 生成的 SQL 不符合安全规范（非 SELECT 开头），已拦截。原始内容: " + content);
        }
        
        // 4. 执行 SQL 查询
        List<Map<String, Object>> data = reportMapper.reportBySql(cleanSql);
        
        if (callback != null) {
            callback.onProgress(3, "数据库查询完成，正在生成图表配置...", 70);
        }
        
        // 5. 调用 AI 生成 ECharts 配置
        String echartOption = createEchartOption(data, chartType);
        
        if (callback != null) {
            callback.onProgress(4, "图表配置生成完成，正在返回结果...", 90);
        }
        
        // 6. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("report_data", data);
        result.put("echart_option", echartOption);
        return result;
    }
    
    /**
     * 根据数据生成 ECharts 配置
     */
    public String createEchartOption(List<Map<String, Object>> data, String chartType) {
        // 如果数据为空，返回空配置，不调用 AI
        if (data == null || data.isEmpty()) {
            System.out.println("=== 数据为空，返回默认空图表配置 ===");
            return "{\"title\":{\"text\":\"暂无数据\"},\"tooltip\":{},\"xAxis\":{\"type\":\"category\",\"data\":[]},\"yAxis\":{\"type\":\"value\"},\"series\":[{\"type\":\"bar\",\"data\":[]}]}";
        }
        
        String dataJson = JSON.toJSONString(data);
        
        // 修改 Prompt，要求返回纯 JSON 格式
        String systemPrompt = "你是一个图表工程师，根据用户提供的报表数据，制作出 echarts 的图表的关键配置。\n" +
                "你可参考 echarts 官网的示例结构，根据用户指定的图表类型，对用户提供的报表数据，自动分析，制作出匹配的 option 选项。\n" +
                "输出的要求：\n" +
                "1. 只输出标准的 JSON 对象，不要包含 let option = 或任何变量声明。\n" +
                "2. 不要使用 markdown 代码块标记（如 ```json）。\n" +
                "3. 不要输出任何解释性文字，只输出 JSON 对象本身。\n" +
                "4. 确保输出的是合法的 JSON 格式，键名使用双引号。";
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-turbo");  // 使用快速模型，减少响应时间
        
        com.alibaba.fastjson.JSONArray messages = new com.alibaba.fastjson.JSONArray();
        
        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);
        
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", "图表类型为" + chartType + "，报表数据为" + dataJson);
        messages.add(userMsg);
        
        requestBody.put("messages", messages);
        
        String jsonBody = requestBody.toJSONString();
        String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
        
        String aiResult = aiService.send(apiUrl, jsonBody, false);
        JSONObject resultObj = JSON.parseObject(aiResult);
        
        // 检查是否有错误响应
        if (resultObj.containsKey("error")) {
            JSONObject error = resultObj.getJSONObject("error");
            String errorMsg = error.getString("message");
            throw new RuntimeException("AI 生成图表配置失败: " + errorMsg);
        }
        
        com.alibaba.fastjson.JSONArray choices = resultObj.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("AI 返回数据格式异常：缺少 choices 字段");
        }
        
        JSONObject firstChoice = choices.getJSONObject(0);
        if (firstChoice == null) {
            throw new RuntimeException("AI 返回数据格式异常：choices 为空");
        }
        
        JSONObject message = firstChoice.getJSONObject("message");
        if (message == null) {
            throw new RuntimeException("AI 返回数据格式异常：缺少 message 字段");
        }
        
        String optionStr = message.getString("content");
        
        // 如果 content 为空，尝试使用 reasoning_content
        if (optionStr == null || optionStr.isEmpty()) {
            optionStr = message.getString("reasoning_content");
        }
        
        if (optionStr == null || optionStr.isEmpty()) {
            throw new RuntimeException("AI 返回的配置内容为空");
        }
        
        // 清理 markdown 标记（以防 AI 仍然返回）
        optionStr = optionStr.replace("```javascript", "").replace("```json", "").replace("```", "").trim();
        
        System.out.println("=== AI 生成的 ECharts 配置（原始）===");
        System.out.println(optionStr);
        
        // 验证 JSON 格式
        try {
            JSON.parseObject(optionStr);
            System.out.println("=== JSON 格式验证通过 ===");
        } catch (Exception e) {
            System.err.println("=== JSON 格式验证失败，尝试修复 ===");
            System.err.println("原始内容: " + optionStr);
            // 如果解析失败，尝试提取 JSON 对象部分
            int startIndex = optionStr.indexOf('{');
            int endIndex = optionStr.lastIndexOf('}');
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                optionStr = optionStr.substring(startIndex, endIndex + 1);
                System.out.println("=== 提取后的 JSON ===");
                System.out.println(optionStr);
                // 再次验证
                JSON.parseObject(optionStr);
                System.out.println("=== JSON 格式修复成功 ===");
            } else {
                throw new RuntimeException("AI 返回的配置格式错误，无法解析为 JSON: " + e.getMessage());
            }
        }
        
        return optionStr;
    }
}
