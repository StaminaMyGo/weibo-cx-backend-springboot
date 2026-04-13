package com.hn.it.weibo.web;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hn.it.weibo.entity.User;
import com.hn.it.weibo.entity.Weibo;
import com.hn.it.weibo.service.UserService;
import com.hn.it.weibo.service.WeiboService;
import com.hn.it.weibo.utils.JwtUtil;
import com.hn.it.weibo.web.dto.RespEntity;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Date;
import java.util.stream.Collectors;

import java.nio.file.Files;
import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.stream.Stream;


@RestController
@RequestMapping("/api/v1/weibos")
public class WeiboController {

    private static final Logger logger = LoggerFactory.getLogger(WeiboController.class);

    // 阿里云 DashScope API Key
    @Value("${my.aliyun.api-key}")
    private  String DASHSCOPE_API_KEY;

    // 邮件接收地址
    private static final String REJECT_EMAIL_TO = "1247079250@qq.com";

    @Autowired
    private WeiboService weiboService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 1. 发布微博
     * POST /api/v1/weibos
     * 需要鉴权
     */
    @PostMapping
    public RespEntity publishWeibo(@RequestHeader("Authorization") String authorization,
                                   @RequestBody Map<String, String> weiboData) {
        try {
            // 解析 Token 获取当前用户 ID
            String token = authorization.replace("Bearer ", "");
            Claims claims = jwtUtil.parseToken(token);
            Long userId = ((Number) claims.get("userId")).longValue();

            // 参数校验
            String title = weiboData.get("title");
            String content = weiboData.get("content");
            String img = weiboData.get("img");

            if (title == null || title.isEmpty() || content == null || content.isEmpty()) {
                return RespEntity.error(400, "参数错误：标题和内容不能为空");
            }

            // 调用 AI 进行审核
            try {
                System.out.println("=== 开始调用 AI 审核 ===");
                System.out.println("标题: " + title);
                System.out.println("内容: " + content);
                
                JSONObject resultJson = weiboService.aiCheck(title, content);
                System.out.println("AI 审核结果: " + resultJson.toJSONString());
                
                Integer isPass = resultJson.getInteger("ispass");
                String reson = resultJson.getString("reson");

                if (isPass == 0) {
                    // 审核不通过，拦截发布
                    System.out.println("审核未通过，原因: " + reson);
                    // 发送邮件通知
                    sendRejectEmail(reson);
                    return RespEntity.error(400, "内容审核未通过：" + reson);
                }
                System.out.println("审核通过");
            } catch (Exception e) {
                // AI 接口超时或报错，阻止发布
                System.err.println("=== AI 审核异常 ===");
                e.printStackTrace();
                return RespEntity.error(500, "AI 审核服务异常，发布已拦截，请稍后重试");
            }

            // 创建微博
            Weibo weibo = new Weibo();
            weibo.setUserId(userId.intValue());
            weibo.setTitle(title);
            weibo.setContent(content);
            weibo.setImg(img);
            weibo.setCreateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            weibo.setReadCount(0); // 初始化阅读量为 0
            weibo.setIsPass(1); // AI 审核通过
            weibo.setRemark(""); // 合规内容备注为空

            weiboService.save(weibo);

            // 返回微博信息
            Map<String, Object> result = new HashMap<>();
            result.put("id", weibo.getId());
            result.put("userid", weibo.getUserId());
            result.put("title", weibo.getTitle());
            result.put("createtime", weibo.getCreateTime());

            return RespEntity.success("发布成功", result);
        } catch (Exception e) {
            e.printStackTrace(); // 打印真实异常堆栈，方便排查
            return RespEntity.error(401, "Token 无效或已过期（详见控制台日志）");
        }
    }

    /**
     * 2. 微博列表（分页 + 搜索）
     * GET /api/v1/weibos
     * 无需鉴权
     */
    @GetMapping
    public RespEntity getWeiboList(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                   @RequestParam(required = false) String keyword) {
        // 分页查询
        Page<Weibo> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Weibo> queryWrapper = new QueryWrapper<>();

        // 关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.like("wb_title", keyword).or().like("wb_content", keyword);
        }

        queryWrapper.orderByDesc("wb_id"); // 按 ID 降序
        Page<Weibo> resultPage = weiboService.page(page, queryWrapper);

        // 构建返回数据
        List<Map<String, Object>> list = resultPage.getRecords().stream().map(weibo -> {
            Map<String, Object> weiboMap = new HashMap<>();
            weiboMap.put("id", weibo.getId());
            weiboMap.put("userid", weibo.getUserId());
            weiboMap.put("title", weibo.getTitle());
            weiboMap.put("content", weibo.getContent());
            weiboMap.put("createtime", weibo.getCreateTime());
            weiboMap.put("readcount", weibo.getReadCount());
            weiboMap.put("img", weibo.getImg());

            // 关联查询用户信息
            User user = userService.getById(weibo.getUserId());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("nickname", user.getNickName());
                userInfo.put("photo", user.getPhoto());
                weiboMap.put("user_info", userInfo);
            }

            return weiboMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", resultPage.getTotal());
        result.put("pages", resultPage.getPages());
        result.put("pageNum", resultPage.getCurrent());
        result.put("pageSize", resultPage.getSize());
        result.put("list", list);

        return RespEntity.success("查询成功", result);
    }

    /**
     * 3. 微博详情
     * GET /api/v1/weibos/{wbId}
     * 无需鉴权
     */
    @GetMapping("/{wbId}")
    public RespEntity getWeiboDetail(@PathVariable Integer wbId) {
        Weibo weibo = weiboService.getById(wbId);
        if (weibo == null) {
            return RespEntity.error(404, "微博不存在");
        }

        // 阅读量 +1（安全处理 null 值）
        int currentReadCount = (weibo.getReadCount() == null) ? 0 : weibo.getReadCount();
        weibo.setReadCount(currentReadCount + 1);
        weiboService.updateById(weibo);

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("id", weibo.getId());
        result.put("userid", weibo.getUserId());
        result.put("title", weibo.getTitle());
        result.put("content", weibo.getContent());
        result.put("createtime", weibo.getCreateTime());
        result.put("readcount", weibo.getReadCount());
        result.put("img", weibo.getImg());

        // 关联查询用户信息
        User user = userService.getById(weibo.getUserId());
        if (user != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("nickname", user.getNickName());
            userInfo.put("photo", user.getPhoto());
            userInfo.put("attionCount", user.getAttionCount());
            result.put("user_info", userInfo);
        }

        return RespEntity.success("查询成功", result);
    }

    /**
     * 4. 删除自己的微博
     * DELETE /api/v1/weibos/{wbId}
     * 需要鉴权
     */
    @DeleteMapping("/{wbId}")
    public RespEntity deleteWeibo(@RequestHeader("Authorization") String authorization,
                                  @PathVariable Integer wbId) {
        try {
            // 解析 Token 获取当前用户 ID
            String token = authorization.replace("Bearer ", "");
            Claims claims = jwtUtil.parseToken(token);
            Long currentUserId = ((Number) claims.get("userId")).longValue();

            // 查询微博
            Weibo weibo = weiboService.getById(wbId);
            if (weibo == null) {
                return RespEntity.error(404, "微博不存在");
            }

            // 验证是否是自己的微博
            if (!weibo.getUserId().equals(currentUserId.intValue())) {
                return RespEntity.error(403, "无权限删除他人微博");
            }

            // 删除微博
            weiboService.removeById(wbId);

            return RespEntity.success("删除成功", null);
        } catch (Exception e) {
            return RespEntity.error(401, "Token 无效或已过期");
        }
    }

    /**
     * AI 生成图片
     * POST /api/v1/weibos/generate-image
     */
    @PostMapping("/generate-image")
    public RespEntity generateImage(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody Map<String, String> requestData) {
        try {
            String title = requestData.getOrDefault("title", "");
            String content = requestData.getOrDefault("content", "");

            if (title.isEmpty() && content.isEmpty()) {
                return RespEntity.error(400, "标题或内容不能为空");
            }

            // 构造提示词
            String prompt = String.format("微博配图。标题：%s，内容：%s。风格要求：精美、写实、社交媒体风格。", title, content);

            // 调用 AI 接口获取临时链接
            String aiImageUrl = callAliYunAi(prompt);

            // 下载并保存到本地，返回本地文件名
            String localFileName = downloadAndSaveImage(aiImageUrl);

            Map<String, Object> result = new HashMap<>();
            result.put("imageUrl",  "/imgs/" + localFileName);
            return RespEntity.success("生成成功", result);
        } catch (Exception e) {
            e.printStackTrace();
            return RespEntity.error(500, "AI 生成失败：" + e.getMessage());
        }
    }

    /**
     * 调用阿里云 AI 图片生成接口
     */
    private String callAliYunAi(String prompt) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", "wan2.6-image");

        ObjectNode input = payload.putObject("input");
        ArrayNode messages = input.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode contentArray = message.putArray("content");
        ObjectNode textContent = contentArray.addObject();
        textContent.put("text", prompt);

        ObjectNode parameters = payload.putObject("parameters");
        parameters.put("prompt_extend", true);
        parameters.put("watermark", false);
        parameters.put("n", 1);
        parameters.put("enable_interleave", true);
        parameters.put("stream", true);
        parameters.put("size", "1376*768");

        String jsonBody = mapper.writeValueAsString(payload);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + DASHSCOPE_API_KEY)
                .header("X-DashScope-SSE", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(120))
                .build();

        // 使用流式响应处理器，避免等待整个响应体结束导致超时
        HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());

        System.out.println("Aliyun API Response Status: " + response.statusCode());

        if (response.statusCode() == 200) {
            Stream<String> lines = response.body();
            Iterator<String> iterator = lines.iterator();

            while (iterator.hasNext()) {
                String line = iterator.next();
                line = line.trim();

                if (line.startsWith("data:")) {
                    String jsonStr = line.substring(5).trim();
                    if (jsonStr.isEmpty() || jsonStr.equals("[DONE]")) continue;

                    try {
                        JsonNode node = mapper.readTree(jsonStr);
                        // 检查流中是否有错误信息
                        if (node.has("code") && !node.path("code").asText().isEmpty()) {
                            String msg = node.has("message") ? node.path("message").asText() : "AI 接口报错";
                            throw new RuntimeException(msg);
                        }

                        // 尝试提取图片链接
                        JsonNode output = node.path("output");
                        if (output.has("choices")) {
                            JsonNode choices = output.path("choices");
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode choice = choices.get(0);
                                if (choice.has("message")) {
                                    JsonNode content = choice.path("message").path("content");
                                    if (content.isArray() && content.size() > 0) {
                                        JsonNode imgNode = content.get(0).path("image");
                                        if (imgNode.isTextual()) {
                                            System.out.println("AI Image URL Found: " + imgNode.asText());
                                            return imgNode.asText(); // 找到链接立即返回
                                        }
                                    }
                                }
                            }
                        }
                    } catch (RuntimeException re) {
                        throw re;
                    } catch (Exception e) { /* ignore parse error */ }
                } else if (!line.isEmpty()) {
                    // 处理非 SSE 格式的错误响应 (例如 400 错误)
                    try {
                        JsonNode node = mapper.readTree(line);
                        if (node.has("code")) {
                            String msg = node.has("message") ? node.path("message").asText() : "AI 接口报错";
                            throw new RuntimeException(msg);
                        }
                    } catch (Exception e) { /* ignore */ }
                }
            }
            throw new RuntimeException("响应解析失败：未找到图片链接");
        } else {
            throw new RuntimeException("HTTP 错误：" + response.statusCode());
        }
    }

    /**
     * 下载 AI 生成的图片并保存到本地
     */
    private String downloadAndSaveImage(String imageUrl) throws Exception {
        String IMG_DIR = "E:/E_projects/weibo/static/imgs/";
        File imgFolder = new File(IMG_DIR);
        if (!imgFolder.exists()) {
            imgFolder.mkdirs();
        }

        // 1. 下载图片
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new RuntimeException("下载 AI 图片失败，HTTP 状态码：" + response.statusCode());
        }

        byte[] imageBytes = response.body();

        // 2. 生成文件名（与 GlobalController 保持一致）
        String extension = ".png"; // AI 生成通常是 png
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String newFileName = uuid + "_" + timestamp + extension;

        // 3. 保存到本地
        File destFile = new File(IMG_DIR + newFileName);
        Files.write(destFile.toPath(), imageBytes);
        System.out.println("AI 图片已保存到: " + destFile.getAbsolutePath());
        System.out.println("=== AI 图片保存成功 ===");
        System.out.println("保存路径: " + destFile.getAbsolutePath());
        System.out.println("文件名: " + newFileName);


        return newFileName;
    }

    /**
     * 发送审核不通过邮件通知
     * @param reason 审核不通过的原因
     */
    private void sendRejectEmail(String reason) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("807097995@qq.com");
            message.setTo(REJECT_EMAIL_TO);
            message.setSubject("微博发布审核不通过通知");
            message.setText("微博发布失败，审核不通过：" + reason);
            
            mailSender.send(message);
            System.out.println("审核不通过邮件已发送至: " + REJECT_EMAIL_TO);
        } catch (Exception e) {
            // 服务器暂时不可用仅记录日志，不影响主流程
            logger.error("发送邮件失败，原因: {}", e.getMessage());
        }
    }
}
