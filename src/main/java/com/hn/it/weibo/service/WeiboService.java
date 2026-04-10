package com.hn.it.weibo.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hn.it.weibo.entity.Weibo;

public interface WeiboService extends IService<Weibo> {
    /**
     * AI 内容审核
     * @param title 微博标题
     * @param content 微博内容
     * @return 审核结果 JSON 对象，包含 ispass 和 reson 字段
     */
    JSONObject aiCheck(String title, String content);
}