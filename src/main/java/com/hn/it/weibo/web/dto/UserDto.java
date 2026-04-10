package com.hn.it.weibo.web.dto;

public class UserDto {
    // 对应 @RequestBody 接收的字段
    private String nickName;
    private String loginName;
    private String loginPwd;

    // 必须提供无参构造方法（Spring 反序列化 JSON 时需要）
    public UserDto() {}

    // Getter 和 Setter 方法
    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getLoginPwd() {
        return loginPwd;
    }

    public void setLoginPwd(String loginPwd) {
        this.loginPwd = loginPwd;
    }
}