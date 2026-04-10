package com.hn.it.weibo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("comments")
public class Comment {
    
    @TableId(value = "cm_id", type = IdType.AUTO)
    private Integer id;
    
    @TableField("cm_weiboid")
    private Integer weiboid;
    
    @TableField("cm_userid")
    private Integer userid;
    
    @TableField("cm_content")
    private String content;
}
