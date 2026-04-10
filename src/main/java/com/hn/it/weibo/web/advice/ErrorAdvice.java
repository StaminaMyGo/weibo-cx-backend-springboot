package com.hn.it.weibo.web.advice;

import com.hn.it.weibo.web.dto.RespEntity;
import io.jsonwebtoken.JwtException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class ErrorAdvice {
    
    // 专门处理 JWT 相关异常，返回 401
    @ExceptionHandler(JwtException.class)
    @ResponseBody
    public RespEntity handleJwtException(JwtException ex) {
        return new RespEntity(401, "Token 无效或已过期", null);
    }

    // 处理其他所有异常，返回 5000 并打印堆栈
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public RespEntity catchError(Exception ex){
        ex.printStackTrace(); // 在控制台打印详细错误，方便排查
        return new RespEntity(5000, ex.getMessage(), null);
    }
}
