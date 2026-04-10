package com.hn.it.weibo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("attentions")
public class Attention {
    
    @TableId(value = "att_id", type = IdType.AUTO)
    private Integer id;
    
    @TableField("att_userid")
    private Integer userid;
    
    @TableField("att_marstid")
    private Integer marstid;
}
