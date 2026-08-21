package com.richard.fyoung.customerwork.safety.security;

import com.richard.fyoung.customerwork.infra.config.CustomerWorkProperties;
import com.richard.fyoung.customerwork.safety.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import com.richard.fyoung.customerwork.infra.config.properties.UserAuthProperties;

/**
 * 用户登录态 JWT 签发与校验（HS256，自签自验，无状态）。
 *
 * <p>starter 提供账户与密码校验能力（{@code UserAccountService}）之外，把登录态令牌的签发/校验作为可复用的
 * 接入层基础设施一并下沉到本包（与 {@link AgentAccessCredential} 同域）；接入方（app）只做装配与业务编排。
 * 密钥取 {@code customer-work.user-auth.jwt-secret}；有效期取 {@code jwt-expire-hours}。为避免过短密钥触发
 * jjwt 的 WeakKeyException，统一把配置密钥做 SHA-256 摘要派生出 256bit 定长签名密钥（既满足 HS256 强度要求，
 * 又不对运维配置的密钥长度做硬约束）。</p>
 *
 * <p><b>安全提示</b>：默认密钥仅供本地开发，生产必须用环境变量 {@code CW_USER_JWT_SECRET} 覆盖。</p>
 * @author owlzhangfq@gmail.com
 */
public interface UserJwtService {

    /**
     * 签发默认租户的登录态令牌。
     *
     * <p>保留三参重载是为了不破坏已接入的下游（本类是 starter 的公开 API）。
     * 多租户部署必须用四参版本显式传租户，否则所有用户都会落到默认租户。</p>
     */
    public abstract String issue(String userId, String username, String nickname);

    /**
     * 签发登录态令牌：subject=userId，附带 username / nickname / tenant，过期时间为签发时刻 + 有效期。
     *
     * @return 已签名的紧凑 JWT 字符串
     */
    public abstract String issue(String userId, String username, String nickname, String tenantId);

    /**
     * 令牌到期的绝对时间戳（毫秒）：供登录响应回传给前端做续期提示。
     */
    public abstract long expiresAtMs();

    /**
     * 校验令牌：验签 + 有效期（jjwt 内部校验 exp），全通过返回主体，否则 empty。
     *
     * <p>过期 / 篡改 / 格式非法统一收敛为 {@link Optional#empty()}——不向调用方区分失败原因。</p>
     */
    public abstract Optional<UserPrincipal> verify(String token);
}
