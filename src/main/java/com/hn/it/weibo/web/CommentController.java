package com.hn.it.weibo.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hn.it.weibo.entity.Comment;
import com.hn.it.weibo.entity.User;
import com.hn.it.weibo.entity.Weibo;
import com.hn.it.weibo.mapper.CommentMapper;
import com.hn.it.weibo.service.UserService;
import com.hn.it.weibo.service.WeiboService;
import com.hn.it.weibo.utils.JwtUtil;
import com.hn.it.weibo.web.dto.RespEntity;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private WeiboService weiboService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 发表评论
     * POST /api/v1/weibos/{wbId}/comments
     * 需要鉴权
     */
    @PostMapping("/weibos/{wbId}/comments")
    public RespEntity addComment(@RequestHeader("Authorization") String authorization,
                                 @PathVariable Integer wbId,
                                 @RequestBody Map<String, String> commentData) {
        try {
            // 解析 Token 获取当前用户 ID
            String token = authorization.replace("Bearer ", "");
            Claims claims = jwtUtil.parseToken(token);
            Long userId = ((Number) claims.get("userId")).longValue();

            // 参数校验
            String content = commentData.get("content");
            if (content == null || content.isEmpty()) {
                return RespEntity.error(400, "参数错误：评论内容不能为空");
            }

            // 创建评论
            Comment comment = new Comment();
            comment.setWeiboid(wbId);
            comment.setUserid(userId.intValue());
            comment.setContent(content);

            commentMapper.insert(comment);

            // 返回评论信息
            Map<String, Object> result = new HashMap<>();
            result.put("id", comment.getId());
            result.put("weiboid", comment.getWeiboid());
            result.put("userid", comment.getUserid());
            result.put("content", comment.getContent());

            return RespEntity.success("评论成功", result);
        } catch (Exception e) {
            return RespEntity.error(401, "Token 无效或已过期");
        }
    }

    /**
     * 获取微博的评论列表
     * GET /api/v1/weibos/{wbId}/comments
     * 无需鉴权
     */
    @GetMapping("/weibos/{wbId}/comments")
    public RespEntity getComments(@PathVariable Integer wbId) {
        // 查询该微博的所有评论
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("cm_weiboid", wbId);
        queryWrapper.orderByDesc("cm_id");
        List<Comment> comments = commentMapper.selectList(queryWrapper);

        // 构建返回数据
        List<Map<String, Object>> list = comments.stream().map(comment -> {
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", comment.getId());
            commentMap.put("weiboid", comment.getWeiboid());
            commentMap.put("userid", comment.getUserid());
            commentMap.put("content", comment.getContent());

            // 关联查询用户信息
            User user = userService.getById(comment.getUserid());
            if (user != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("nickname", user.getNickName());
                userInfo.put("photo", user.getPhoto());
                commentMap.put("user_info", userInfo);
            }

            return commentMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", list.size());
        result.put("list", list);

        return RespEntity.success("查询成功", result);
    }

    /**
     * 删除评论
     * DELETE /api/v1/comments/{commentId}
     * 需要鉴权（评论作者或微博作者可删除）
     */
    @DeleteMapping("/comments/{commentId}")
    public RespEntity deleteComment(@RequestHeader("Authorization") String authorization,
                                    @PathVariable Integer commentId) {
        try {
            String token = authorization.replace("Bearer ", "");
            Claims claims = jwtUtil.parseToken(token);
            Long currentUserId = ((Number) claims.get("userId")).longValue();

            Comment comment = commentMapper.selectById(commentId);
            if (comment == null) {
                return RespEntity.error(404, "评论不存在");
            }

            Weibo weibo = weiboService.getById(comment.getWeiboid());
            boolean isCommentAuthor = comment.getUserid().equals(currentUserId.intValue());
            boolean isWeiboAuthor = weibo != null && weibo.getUserId().equals(currentUserId.intValue());

            if (!isCommentAuthor && !isWeiboAuthor) {
                return RespEntity.error(403, "无权限删除此评论");
            }

            commentMapper.deleteById(commentId);

            return RespEntity.success("删除成功", null);
        } catch (Exception e) {
            return RespEntity.error(401, "Token 无效或已过期");
        }
    }
}
