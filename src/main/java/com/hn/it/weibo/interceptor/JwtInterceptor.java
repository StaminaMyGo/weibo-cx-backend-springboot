package com.hn.it.weibo.interceptor;

import com.hn.it.weibo.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取 Token
        // 前端通常会在 Header 里放 "Authorization: Bearer <token>"
        String token = request.getHeader("Authorization");

        // 2. 如果没带 Token
        if (token == null || token.isEmpty()) {
            response.setStatus(401); // 401 未授权
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println("{\"code\": 401, \"msg\": \"未登录，请先登录\"}");
            return false; // 拦截请求，不让访问接口
        }

        // 3. 如果带了 Token，尝试解析
        try {
            // 如果前端传的是 "Bearer xxxxx"，需要去掉 "Bearer " 前缀
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 解析 Token，如果过期或签名错误，这里会抛异常
            jwtUtil.parseToken(token);

            // 解析成功，放行
            return true;
        } catch (Exception e) {
            // 解析失败（Token 无效或过期）
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().println("{\"code\": 401, \"msg\": \"Token 无效或已过期\"}");
            return false; // 拦截
        }
    }
}