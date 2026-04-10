package com.hn.it.weibo.web;

import com.hn.it.weibo.web.dto.RespEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
public class GlobalController {
    
    // 图片存储目录
    private static final String IMG_DIR = "D:/ideaprj/weibo/static/imgs/";
    
    /**
     * 上传图片
     * POST /api/v1/upload/images
     * 需要鉴权
     */
    @PostMapping("/images")
    public RespEntity uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 1. 检查文件是否为空
            if (file.isEmpty()) {
                return RespEntity.error(400, "上传文件为空");
            }
            
            // 2. 检查文件格式
            String originalName = file.getOriginalFilename();
            if (originalName == null || (!originalName.toLowerCase().endsWith(".jpg") 
                    && !originalName.toLowerCase().endsWith(".png"))) {
                return RespEntity.error(400, "只支持 jpg、png 格式的图片");
            }
            
            // 3. 检查文件大小（5MB = 5 * 1024 * 1024 bytes）
            if (file.getSize() > 5 * 1024 * 1024) {
                return RespEntity.error(400, "图片大小不能超过 5MB");
            }
            
            // 4. 生成新文件名（UUID + 时间戳）
            String newFileName = generateFileName(originalName);
            
            // 5. 确保文件夹存在
            File imgFolder = new File(IMG_DIR);
            if (!imgFolder.exists()) {
                imgFolder.mkdirs();
            }
            
            // 6. 保存文件
            File destFile = new File(IMG_DIR + newFileName);
            file.transferTo(destFile);
            
            // 7. 返回新文件名
            Map<String, Object> result = new HashMap<>();
            result.put("imgUrl", newFileName);
            return RespEntity.success("上传成功", result);
            
        } catch (IOException e) {
            return RespEntity.error(500, "上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 生成唯一的文件名
     * 格式：UUID_ 时间戳。扩展名
     */
    private String generateFileName(String originalName) {
        // 1. 获取扩展名（.jpg, .png 等）
        String extension = "";
        if (originalName != null && !originalName.isEmpty() && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        
        // 2. 生成 UUID（去掉了横杠）
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        // 3. 生成时间戳
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        
        // 4. 组合：UUID_ 时间戳。扩展名
        return uuid + "_" + timestamp + extension;
    }
}
