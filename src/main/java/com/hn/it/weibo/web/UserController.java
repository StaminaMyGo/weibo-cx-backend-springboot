package com.hn.it.weibo.web;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hn.it.weibo.entity.Attention;
import com.hn.it.weibo.entity.User;
import com.hn.it.weibo.mapper.AttentionMapper;
import com.hn.it.weibo.service.UserService;
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
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AttentionMapper attentionMapper;

    /**
     * 5. 关注用户
     * POST /api/v1/users/{userId}/follow
     * 需要鉴权
     */
    @PostMapping("/{userId}/follow")
    public RespEntity followUser(@RequestHeader("Authorization") String authorization,
                                 @PathVariable Integer userId) {
        try {
            // 解析 Token 获取当前用户 ID
            String token = authorization.replace("Bearer ", "");
            Claims claims = jwtUtil.parseToken(token);
            Long currentUserId = ((Number) claims.get("userId")).longValue();

            // 不能关注自己
            if (currentUserId.intValue() == userId) {
                return RespEntity.error(400, "不能关注自己");
            }

            // 查询被关注用户是否存在
            User targetUser = userService.getById(userId);
            if (targetUser == null) {
                return RespEntity.error(404, "用户不存在");
            }

            // 检查是否已经关注
            QueryWrapper<Attention> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("att_userid", currentUserId);
            queryWrapper.eq("att_marstid", userId);
            Attention existing = attentionMapper.selectOne(queryWrapper);
            if (existing != null) {
                return RespEntity.error(400, "已关注该用户");
            }

            // 插入关注记录
            Attention attention = new Attention();
            attention.setUserid(currentUserId.intValue());
            attention.setMarstid(userId);
            attentionMapper.insert(attention);

            // 更新被关注用户的粉丝数
            targetUser.setAttionCount(targetUser.getAttionCount() + 1);
            userService.updateById(targetUser);

            return RespEntity.success("关注成功", null);
        } catch (Exception e) {
            return RespEntity.error(401, "Token 无效或已过期");
        }
    }

    /**
     * 6. 取消关注
     * DELETE /api/v1/users/{userId}/follow
     * 需要鉴权
     */
    @DeleteMapping("/{userId}/follow")
    public RespEntity unfollowUser(@RequestHeader("Authorization") String authorization,
                                   @PathVariable Integer userId) {
        try {
            // 解析 Token 获取当前用户 ID
            String token = authorization.replace("Bearer ", "");
            Claims claims = jwtUtil.parseToken(token);
            Long currentUserId = ((Number) claims.get("userId")).longValue();

            // 查询关注记录
            QueryWrapper<Attention> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("att_userid", currentUserId);
            queryWrapper.eq("att_marstid", userId);
            Attention existing = attentionMapper.selectOne(queryWrapper);
            if (existing == null) {
                return RespEntity.error(400, "未关注该用户");
            }

            // 删除关注记录
            attentionMapper.deleteById(existing.getId());

            // 更新被关注用户的粉丝数
            User targetUser = userService.getById(userId);
            if (targetUser != null) {
                targetUser.setAttionCount(Math.max(0, targetUser.getAttionCount() - 1));
                userService.updateById(targetUser);
            }

            return RespEntity.success("取消关注成功", null);
        } catch (Exception e) {
            return RespEntity.error(401, "Token 无效或已过期");
        }
    }

    /**
     * 7. 获取用户的粉丝列表
     * GET /api/v1/users/{userId}/followers
     * 无需鉴权
     */
    @GetMapping("/{userId}/followers")
    public RespEntity getFollowers(@PathVariable Integer userId) {
        // 查询用户是否存在
        User user = userService.getById(userId);
        if (user == null) {
            return RespEntity.error(404, "用户不存在");
        }

        // 查询粉丝列表（att_marstid = userId 表示被关注的人是该用户）
        QueryWrapper<Attention> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("att_marstid", userId);
        List<Attention> attentions = attentionMapper.selectList(queryWrapper);

        // 构建返回数据
        List<Map<String, Object>> list = attentions.stream().map(attention -> {
            Map<String, Object> followerMap = new HashMap<>();
            User follower = userService.getById(attention.getUserid());
            if (follower != null) {
                followerMap.put("id", follower.getId());
                followerMap.put("nickname", follower.getNickName());
                followerMap.put("photo", follower.getPhoto());
                followerMap.put("score", follower.getScore());
                followerMap.put("attionCount", follower.getAttionCount());
            }
            return followerMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", list.size());
        result.put("list", list);

        return RespEntity.success("查询成功", result);
    }

    /**
     * 8. 获取用户的关注列表
     * GET /api/v1/users/{userId}/following
     * 无需鉴权
     */
    @GetMapping("/{userId}/following")
    public RespEntity getFollowing(@PathVariable Integer userId) {
        // 查询用户是否存在
        User user = userService.getById(userId);
        if (user == null) {
            return RespEntity.error(404, "用户不存在");
        }

        // 查询关注列表（att_userid = userId 表示关注别人的人是该用户）
        QueryWrapper<Attention> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("att_userid", userId);
        List<Attention> attentions = attentionMapper.selectList(queryWrapper);

        // 构建返回数据
        List<Map<String, Object>> list = attentions.stream().map(attention -> {
            Map<String, Object> followingMap = new HashMap<>();
            User followingUser = userService.getById(attention.getMarstid());
            if (followingUser != null) {
                followingMap.put("id", followingUser.getId());
                followingMap.put("nickname", followingUser.getNickName());
                followingMap.put("photo", followingUser.getPhoto());
                followingMap.put("score", followingUser.getScore());
                followingMap.put("attionCount", followingUser.getAttionCount());
            }
            return followingMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("total", list.size());
        result.put("list", list);

        return RespEntity.success("查询成功", result);
    }

    /**
     * 1. 用户注册
     * POST /api/v1/users/register
     */
    @PostMapping("/register")
    public RespEntity register(@RequestBody Map<String, String> registerData) {
        String loginname = registerData.get("loginname");
        String loginpwd = registerData.get("loginpwd");
        String nickname = registerData.get("nickname");

        // 参数校验
        if (loginname == null || loginname.isEmpty() || loginpwd == null || loginpwd.isEmpty() || nickname == null || nickname.isEmpty()) {
            return RespEntity.error(400, "参数错误：必填参数不能为空");
        }

        // 检查用户名是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_loginname", loginname);
        User existingUser = userService.getOne(queryWrapper);
        if (existingUser != null) {
            return RespEntity.error(400, "用户名已存在");
        }

        // 创建新用户
        User newUser = new User();
        newUser.setLoginName(loginname);
        newUser.setLoginPwd(loginpwd);
        newUser.setNickName(nickname);
        
        // 处理头像（可选参数）
        String photo = registerData.get("photo");
        if (photo != null && !photo.isEmpty()) {
            newUser.setPhoto(photo);
        }

        userService.save(newUser);

        // 返回新用户信息（不包含密码）
        Map<String, Object> result = new HashMap<>();
        result.put("id", newUser.getId());
        result.put("loginname", newUser.getLoginName());
        result.put("nickname", newUser.getNickName());

        return RespEntity.success("注册成功", result);
    }

    /**
     * 2. 用户登录（获取 JWT 令牌）
     * POST /api/v1/users/login
     */
    @PostMapping("/login")
    public RespEntity login(@RequestBody Map<String, String> loginData) {
        String loginname = loginData.get("loginname");
        String loginpwd = loginData.get("loginpwd");

        // 参数校验
        if (loginname == null || loginname.isEmpty() || loginpwd == null || loginpwd.isEmpty()) {
            return RespEntity.error(400, "参数错误：用户名和密码不能为空");
        }

        // 查询用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_loginname", loginname);
        queryWrapper.eq("user_loginpwd", loginpwd);
        User user = userService.getOne(queryWrapper);

        if (user == null) {
            return RespEntity.error(400, "用户名或密码错误");
        }

        // 生成 Token
        String token = jwtUtil.createToken(user.getNickName(), (long) user.getId());

        // 返回用户信息和 Token
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("id", user.getId());
        result.put("nickname", user.getNickName());
        result.put("photo", user.getPhoto());
        result.put("score", user.getScore());
        result.put("attionCount", user.getAttionCount());

        return RespEntity.success("登录成功", result);
    }

    /**
     * 3. 获取当前登录用户信息
     * GET /api/v1/users/current
     * 需要鉴权
     */
    @GetMapping("/current")
    public RespEntity getCurrentUser(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.replace("Bearer ", "");
            Claims claims = jwtUtil.parseToken(token);
            Long userId = ((Number) claims.get("userId")).longValue();

            User user = userService.getById(userId.intValue());
            if (user == null) {
                return RespEntity.error(404, "用户不存在");
            }

            // 返回用户信息（不包含密码）
            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("nickname", user.getNickName());
            result.put("loginname", user.getLoginName());
            result.put("photo", user.getPhoto());
            result.put("score", user.getScore());
            result.put("attionCount", user.getAttionCount());

            return RespEntity.success("查询成功", result);
        } catch (Exception e) {
            return RespEntity.error(401, "Token 无效或已过期");
        }
    }

    /**
     * 4. 根据 ID 获取用户信息
     * GET /api/v1/users/{userId}
     * 无需鉴权
     */
    @GetMapping("/{userId}")
    public RespEntity getUserById(@PathVariable Integer userId) {
        User user = userService.getById(userId);
        if (user == null) {
            return RespEntity.error(404, "用户不存在");
        }

        // 返回用户信息（不包含密码）
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("nickname", user.getNickName());
        result.put("photo", user.getPhoto());
        result.put("score", user.getScore());
        result.put("attionCount", user.getAttionCount());

        return RespEntity.success("查询成功", result);
    }

    /**
     * 10. 更新用户头像
     * PUT /api/v1/users/current/avatar
     * 需要鉴权
     */
    @PutMapping("/current/avatar")
    public RespEntity updateAvatar(@RequestHeader("Authorization") String authorization,
                                   @RequestBody Map<String, String> params) {
        try {
            System.out.println("=== 头像更新接口调试 ===");
            System.out.println("收到的 Authorization: " + authorization);
            
            // 1. 解析 Token 获取用户 ID
            String token = authorization.replace("Bearer ", "");
            System.out.println("提取的 Token: " + token.substring(0, Math.min(30, token.length())) + "...");
            
            Claims claims = jwtUtil.parseToken(token);
            System.out.println("Token 解析成功，Claims: " + claims);
            
            Long userId = ((Number) claims.get("userId")).longValue();
            System.out.println("用户ID: " + userId);

            // 2. 获取新头像路径
            String photo = params.get("photo");
            System.out.println("头像路径: " + photo);
            
            if (photo == null || photo.isEmpty()) {
                return RespEntity.error(400, "参数错误：头像路径不能为空");
            }

            // 3. 更新数据库 user_photo 字段
            User user = new User();
            user.setId(userId.intValue());
            user.setPhoto(photo);
            userService.updateById(user);

            System.out.println("头像更新成功");
            return RespEntity.success("头像更新成功", null);
        } catch (Exception e) {
            System.out.println("=== 头像更新失败 ===");
            System.out.println("错误类型: " + e.getClass().getName());
            System.out.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return RespEntity.error(500, "头像更新失败: " + e.getMessage());
        }
    }
}