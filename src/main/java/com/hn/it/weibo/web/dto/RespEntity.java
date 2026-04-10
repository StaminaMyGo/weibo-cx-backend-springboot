package com.hn.it.weibo.web.dto;

import lombok.Data;

/**
 * 统一响应结果类
 * @param <T> 泛型，代表 data 字段的具体类型（例如 User, Weibo, List<String> 等）
 */
@Data
public class RespEntity<T> {

    private Integer code;
    private String msg;
    private T data; // 这里使用泛型 T

    // 私有构造函数，强制使用静态方法创建对象
    public RespEntity(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // --- 成功的静态方法 ---

    // 成功且返回数据
    public static <T> RespEntity<T> success(T data) {
        return new RespEntity<>(200, "操作成功", data);
    }

    // 成功且返回数据（自定义消息）
    public static <T> RespEntity<T> success(String msg, T data) {
        return new RespEntity<>(200, msg, data);
    }

    // 成功但不返回数据
    public static <T> RespEntity<T> success() {
        return new RespEntity<>(200, "操作成功", null);
    }

    // --- 失败的静态方法 ---

    public static <T> RespEntity<T> error(Integer code, String msg) {
        return new RespEntity<>(code, msg, null);
    }

    public static <T> RespEntity<T> error(String msg) {
        return new RespEntity<>(500, msg, null);
    }
}