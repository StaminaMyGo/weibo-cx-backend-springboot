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
          `wb_pass` tinyint DEFAULT '0',
          `wb_remark` varchar(500) DEFAULT NULL,
          PRIMARY KEY (`wb_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;

    @Override
    public List<ReportItem> reportPublishByUser() {
        return reportMapper.reportPublishByUser();
    }
    
    @Override
    public Map<String, Object> reportDynamic(String prompt, String chartType) {
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
        requestBody.put("model", "qwen3.5-plus");
        
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
        JSONObject message = resultObj.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message");
        
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
        
        // 3. SQL 安全过滤（只允许 SELECT 开头）
        if (!cleanSql.toUpperCase().startsWith("SELECT")) {
            throw new RuntimeException("AI 生成的 SQL 不符合安全规范（非 SELECT 开头），已拦截。原始内容: " + content);
        }
        
        // 4. 执行 SQL 查询
        List<Map<String, Object>> data = reportMapper.reportBySql(cleanSql);
        
        // 5. 调用 AI 生成 ECharts 配置
        String echartOption = createEchartOption(data, chartType);
        
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
        String dataJson = JSON.toJSONString(data);
        
        String systemPrompt = "你是一个图表工程师，根据用户提供的报表数据，制作出 echarts 的图表的关键配置。\n" +
                "你可参考 echarts 官网的示例结构，根据用户指定的图表类型，对用户提供的报表数据，自动分析，制作出匹配的 option 选项。\n" +
                "输出的要求：\n" +
                "1. 只需要 let option = ... 的内容输出，不要输出其他任何文字，因为获得 option 结构后会进行后续处理。";
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen3.5-plus");
        
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
        String optionStr = resultObj.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
        
        // 清理 markdown 标记
        optionStr = optionStr.replace("```javascript", "").replace("```", "").replace("```json", "").trim();
        System.out.println("AI 生成的 ECharts 配置: " + optionStr);
        
        return optionStr;
    }
}
