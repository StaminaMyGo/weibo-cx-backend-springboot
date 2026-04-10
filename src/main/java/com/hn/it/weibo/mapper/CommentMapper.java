package com.hn.it.weibo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hn.it.weibo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
