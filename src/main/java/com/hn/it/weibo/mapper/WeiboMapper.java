package com.hn.it.weibo.mapper; // 注意包名

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hn.it.weibo.entity.User;
import com.hn.it.weibo.entity.Weibo;
import org.apache.ibatis.annotations.Mapper;

@Mapper // 告诉 Spring 这是一个 Mapper 组件
public interface WeiboMapper extends BaseMapper<Weibo> {
    // 继承 BaseMapper 后，你自动拥有了 insert, selectById, update 等方法
    // 不需要在里面写任何代码，直接用就行
}