package com.richard.fyoung.customerwork.data.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;

/**
 * 用户账户服务：注册与登录校验。
 *
 * <p>密码加密/校验在本层完成（BCrypt，慢哈希抗爆破），实体只持有哈希、绝不接触明文。注册前置
 * 用户名唯一校验（重名 fast-fail）；登录校验对"用户不存在 / 密码错 / 账户停用"统一返回
 * {@link Optional#empty()}——不向调用方泄露失败的具体原因（避免用户名枚举）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface UserAccountService {

    /**
     * 注册新账户：用户名唯一，密码 BCrypt 加密后落库。
     *
     * @throws IllegalStateException 用户名已存在
     */
    public abstract UserAccount register(String username, String rawPassword, String nickname, String phone);

    /**
     * 登录校验：用户名存在、账户启用、密码匹配三者全满足才返回账户，否则统一返回 empty。
     */
    public abstract Optional<UserAccount> verifyLogin(String username, String rawPassword);

    public abstract Optional<UserAccount> findById(String id);

    /**
     * 更新用户头像：查出账户 → 充血实体自改头像 → 持久化。
     *
     * @throws IllegalStateException 账户不存在
     * @return 已更新头像的账户
     */
    public abstract UserAccount updateAvatar(String userId, String avatarUrl);
}
