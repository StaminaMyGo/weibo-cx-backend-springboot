package com.hn.it.weibo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("users")
public class User {

    @TableId(value = "user_id", type = IdType.AUTO)
    private int id;

    @TableField("user_nickname")
    private String nickName;

    @TableField("user_loginname")
    private String loginName;

    @TableField("user_loginpwd")
    private String loginPwd;

    @TableField("user_photo")
    private String photo;

    @TableField("user_score")
    private int score;

    @TableField("user_attionCount")
    private int attionCount;

    // Getter 和 Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNickName() { return nickName; }
    public void setNickName(String nickName) { this.nickName = nickName; }

    public String getLoginName() { return loginName; }
    public void setLoginName(String loginName) { this.loginName = loginName; }

    public String getLoginPwd() { return loginPwd; }
    public void setLoginPwd(String loginPwd) { this.loginPwd = loginPwd; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getAttionCount() { return attionCount; }
    public void setAttionCount(int attionCount) { this.attionCount = attionCount; }
}