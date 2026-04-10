// com.hn.it.weibo.entity.Weibo
package com.hn.it.weibo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("weibos")
public class Weibo {
    
    @TableId(value = "wb_id", type = IdType.AUTO)
    private Integer id;       // 微博 ID
    
    @TableField("wb_userid")
    private Integer userId;   // 用户 ID
    
    @TableField("wb_title")
    private String title;     // 微博标题
    
    @TableField("wb_content")
    private String content;   // 微博内容
    
    @TableField("wb_createtime")
    private String createTime;// 创建时间
    
    @TableField("wb_readcount")
    private Integer readCount;// 阅读数
    
    @TableField("wb_img")
    private String img;       // 图片
    
    @TableField("is_pass")
    private Integer isPass;   // 审核状态：1通过，0不通过
    
    @TableField("remark")
    private String remark;    // AI审核备注/违规原因
}