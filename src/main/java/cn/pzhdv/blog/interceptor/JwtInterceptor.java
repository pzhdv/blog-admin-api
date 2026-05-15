package cn.pzhdv.blog.interceptor;

import cn.pzhdv.blog.context.UserContext;
import cn.pzhdv.blog.entity.SystemUser;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import cn.pzhdv.blog.utils.IpUtils;
import cn.pzhdv.blog.utils.JwtTokenUtil;
import cn.pzhdv.blog.utils.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT令牌认证拦截器
 * 拦截需要登录的接口，校验Token有效性，解析用户信息存入上下文
 *
 * @author PanZonghui
 * @since 2025-12-31
 */
@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    public JwtInterceptor(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {

        String requestUri = request.getRequestURI();
        String clientIp = IpUtils.getClientIpForLog(request);
        log.debug("拦截请求 - 路径：{}，客户端IP：{}", requestUri, clientIp);

        // ====================== 放行跨域预检请求 ======================
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. 提取Token
        String token = jwtTokenUtil.extractTokenFromRequest(request);

        // 2. Token为空
        if (!StringUtils.hasText(token)) {
            log.warn("Token验证失败 - 未携带令牌，路径：{}，IP：{}", requestUri, clientIp);
            ResponseUtil.out(response, ResultUtil.error(ResultCode.NOT_LOGIN));
            return false;
        }

        // 3. Token无效/过期
        boolean valid = jwtTokenUtil.validateAccessToken(token);
        if (!valid) {
            log.warn("Token验证失败 - 令牌无效或已过期，路径：{}，IP：{}", requestUri, clientIp);
            ResponseUtil.out(response, ResultUtil.error(ResultCode.TOKEN_EXPIRED));
            return false;
        }

        // 4. 解析用户信息（增加异常处理）
        try {
            String userId = jwtTokenUtil.getUserIdFromAccessToken(token);
            String username = jwtTokenUtil.getUsernameFromAccessToken(token);

            // 用户信息判空
            if (!StringUtils.hasText(userId) || !StringUtils.hasText(username)) {
                log.warn("Token验证失败 - 用户信息为空，路径：{}，IP：{}", requestUri, clientIp);
                ResponseUtil.out(response, ResultUtil.error(ResultCode.TOKEN_INVALID));
                return false;
            }

            // 存入上下文
            SystemUser user = new SystemUser();
            user.setUserId(Long.parseLong(userId));
            user.setUserName(username);
            UserContext.setUser(user);

            log.info("Token验证成功 - 路径：{}，IP：{}，用户：{}", requestUri, clientIp, username);
            return true;

        } catch (NumberFormatException e) {
            log.error("Token解析失败 - 用户ID格式错误，路径：{}，IP：{}", requestUri, clientIp, e);
            ResponseUtil.out(response, ResultUtil.error(ResultCode.TOKEN_INVALID));
            return false;
        }
    }

    /**
     * 请求完成后清理ThreadLocal，防止用户串线
     */
    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex) {
        UserContext.clear();
        log.debug("ThreadLocal用户信息已清理，请求路径：{}", request.getRequestURI());
    }
}