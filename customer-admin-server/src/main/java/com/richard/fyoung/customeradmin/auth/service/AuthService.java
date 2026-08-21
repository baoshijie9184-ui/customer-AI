package com.richard.fyoung.customeradmin.auth.service;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.auth.config.AdminLdapProperties;
import com.richard.fyoung.customeradmin.auth.dto.ChangePasswordRequest;
import com.richard.fyoung.customeradmin.auth.dto.LoginRequest;
import com.richard.fyoung.customeradmin.auth.dto.LoginResponse;
import com.richard.fyoung.customeradmin.auth.dto.SsoLoginRequest;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.system.log.entity.SysOperationLog;
import com.richard.fyoung.customeradmin.system.log.mapper.OperationLogMapper;
import com.richard.fyoung.customeradmin.system.role.entity.SysRole;
import com.richard.fyoung.customeradmin.system.role.mapper.SysRoleMapper;
import com.richard.fyoung.customeradmin.system.user.entity.SysUser;
import com.richard.fyoung.customeradmin.system.user.entity.SysUserRole;
import com.richard.fyoung.customeradmin.system.user.mapper.SysUserMapper;
import com.richard.fyoung.customeradmin.system.user.mapper.SysUserRoleMapper;
import com.richard.fyoung.customeradmin.tenant.TenantSession;
import com.richard.fyoung.customeradmin.tenant.service.TenantService;
import com.richard.fyoung.customerwork.safety.tenant.CrossTenantOperations;
import com.richard.fyoung.customerwork.safety.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录/登出/改密。
 *
 * <p>登录（成功与失败）均需记录操作日志（需求文档 §4.3），但失败时尚无 Sa-Token 登录态，
 * 通用的 {@code OperationLogAspect}（依赖 {@code StpUtil.isLogin()} 解析操作人）无法覆盖这个场景，
 * 故本类直接调用 {@link OperationLogMapper} 记录，不复用 AOP 切面（登出/改密走已登录路径，
 * 使用 {@code @OperationLog} 注解，见 {@link com.richard.fyoung.customeradmin.auth.controller.AuthController}）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AuthService {

    public abstract LoginResponse login(LoginRequest request);

    /**
     * OA 域账号（LDAP/AD）单点登录。
     *
     * <p>密码本身交由企业 AD 域控校验（{@link LdapAuthService}），本方法不存不比对密码；
     * 首次登录自动在 {@code sys_user} 创建本地影子账号（{@code login_type=LDAP}）并按配置默认角色，
     * 后继开始复用同一行、不重复创建。</p>
     */
    public abstract LoginResponse ssoLogin(SsoLoginRequest request);

    public abstract void logout();

    public abstract void changePassword(ChangePasswordRequest request);
}
