package com.hn.it.weibo.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWeiboConfig implements WebMvcConfigurer {
    @Value("${file.upload.image-dir}")
    private String imgDir;

    @Value("${file.static.path-prefix:/imgs/}")
    private String staticPathPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        // 映射 /imgs/** 到本地文件夹
        String resourceLocation = "file:" + imgDir;
        if (!resourceLocation.endsWith("/")) {
            resourceLocation += "/";
        }
//        registry.addResourceHandler("/imgs/**")
//                .addResourceLocations("file:E:/E_projects/weibo/static/imgs/");
        registry.addResourceHandler(staticPathPrefix + "**")
                .addResourceLocations(resourceLocation);

    }

    /**
     * MyBatis-Plus 分页插件配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInnerInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
