package com.richard.fyoung.customeradmin.system.devtool.service;

import com.richard.fyoung.customeradmin.aiconfig.systemtool.tool.SystemToolHttpGuard;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.system.devtool.dto.DevToolHttpSendRequest;
import com.richard.fyoung.customeradmin.system.devtool.dto.DevToolHttpSendResponse;
import com.richard.fyoung.customerwork.safety.security.HttpTargetForbiddenException;
import com.richard.fyoung.customerwork.devtool.HttpProxyDevToolOps;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 开发者工具箱 HTTP 请求工具（页面侧）：代理前端发起真实 HTTP(S) 调用（浏览器直连会被目标站 CORS 拦截，
 * 故由后端出网）。
 *
 * <p><b>执行核心不在本类</b>：请求组装、受限头跳过、响应截断、异常翻译都在 starter 的
 * {@link HttpProxyDevToolOps}（无 Spring 依赖的纯执行核心）。本类只做两件事：DTO ↔ starter 结果对象的
 * 转换，以及把 starter 异常转成 {@link BizException} 交给全局异常处理器。</p>
 *
 * <p>安全说明（与智能体 httpclient 工具同一套收口）：SSRF 面复用 {@link SystemToolHttpGuard} 承载的
 * 策略（默认拒内网/环回、放公网，配 {@code admin.system-tool.http.allowed-hosts} 白名单后仅放行
 * 白名单内 host），由执行核心在发起请求前调用；配套禁止自动跟随重定向，3xx 作为终态结果返回并带回
 * Location。TLS 走 JDK 默认信任链校验，不做 trust-all。</p>
 * @author owlzhangfq@gmail.com
 */
public interface DevToolHttpService {

    /**
     * 发起一次 HTTP(S) 调用并返回执行结果；目标服务不可达/超时等收敛进 error 字段，
     * SSRF 拦截与参数非法则 fast fail 抛业务异常。
     */
    public abstract DevToolHttpSendResponse send(DevToolHttpSendRequest request);
}
