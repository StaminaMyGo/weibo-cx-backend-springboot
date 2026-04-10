package com.hn.it.weibo;

import com.hn.it.weibo.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class WeiboApplication implements WebMvcConfigurer {

	public static void main(String[] args) {
		SpringApplication.run(WeiboApplication.class, args);
	}

	@Autowired
	private JwtInterceptor jwtInterceptor;

	// 注册拦截器配置
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(jwtInterceptor)
				.addPathPatterns("/api/v1/**")
				.excludePathPatterns(
						// 用户模块 - 公开接口
						"/api/v1/users/register",        // 注册
						"/api/v1/users/login",           // 登录
						"/api/v1/users/*",               // 根据 ID 查询用户（单层路径变量）
						"/api/v1/users/*/followers",     // 粉丝列表
						"/api/v1/users/*/following",     // 关注列表
						// 微博模块 - 公开接口
						"/api/v1/weibos",                // 微博列表
						"/api/v1/weibos/*",              // 微博详情
						"/api/v1/weibos/*/comments",     // 评论列表
						// 文件上传 - 公开接口（注册时使用）
						"/api/v1/upload/images",         // 上传图片
						// 静态资源
						"/imgs/**"                       // 图片访问
				);
				// 需鉴权接口（默认拦截）：
				// POST/DELETE /api/v1/users/{userId}/follow - 关注/取消关注
				// GET /api/v1/users/current - 当前用户信息
				// POST /api/v1/weibos - 发布微博
				// DELETE /api/v1/weibos/{wbId} - 删除微博
				// POST /api/v1/upload/images - 上传图片
				// POST /api/v1/weibos/{wbId}/comments - 发表评论
				// DELETE /api/v1/comments/{commentId} - 删除评论
	}
}
