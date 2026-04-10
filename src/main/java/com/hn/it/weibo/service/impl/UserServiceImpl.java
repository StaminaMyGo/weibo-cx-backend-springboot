package com.hn.it.weibo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hn.it.weibo.entity.User;
import com.hn.it.weibo.mapper.UserMapper;
import com.hn.it.weibo.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
}