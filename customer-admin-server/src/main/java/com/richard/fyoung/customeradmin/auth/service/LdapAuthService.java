package com.richard.fyoung.customeradmin.auth.service;

import com.richard.fyoung.customeradmin.auth.config.AdminLdapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * OA 域账号（LDAP/AD）密码校验。
 *
 * <p>直接向企业 AD 域控发起一次匿名 Simple Bind：Bind 成功即代表账号密码正确（AD 侧真正的鉴权
 * 由域控完成，本服务不做密码比对/不落库密码），失败通过 {@link NamingException} 子类型区分是
 * "用户名密码错误"还是"域控不可达"，避免把网络故障误报成登录失败。</p>
 *
 * <p>实现手法为标准的企业 LDAP 登录适配（JNDI
 * {@code InitialDirContext} + simple 认证），域控地址/UPN 后缀为可配置项。</p>
 * @author owlzhangfq@gmail.com
 */
public interface LdapAuthService {

    /**
     * @param username 不含域名后缀的登录名（如 RichardFyoung）；若用户输入时已带 {@code @xxx}，
     *                  由调用方（AuthService）先行归一化再传入。
     */
    public abstract LdapBindResult bind(String username, String password);
}
