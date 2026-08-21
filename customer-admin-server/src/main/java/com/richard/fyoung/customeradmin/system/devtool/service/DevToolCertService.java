package com.richard.fyoung.customeradmin.system.devtool.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.system.devtool.dto.DevToolCertInfo;
import com.richard.fyoung.customeradmin.system.devtool.dto.DevToolCertMatchResponse;
import com.richard.fyoung.customeradmin.system.devtool.dto.DevToolCertParseResponse;
import com.richard.fyoung.customeradmin.system.devtool.dto.DevToolCsrInfo;
import com.richard.fyoung.customeradmin.system.devtool.dto.DevToolKeystoreParseResponse;
import com.richard.fyoung.customeradmin.system.devtool.dto.DevToolPrivateKeyExportResponse;
import com.richard.fyoung.customerwork.devtool.CertDevToolOps;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 开发者工具箱 · 证书解析（页面侧）。
 *
 * <p><b>解析能力不在本类</b>：核心实现是 starter 的 {@link CertDevToolOps}（纯函数、无 Spring 依赖），
 * 同一份实现同时供 {@code devtoolbox} 系统工具的 {@code cert_parse} / {@code cert_match} 暴露给智能体
 * ——页面与智能体走同一套解析逻辑，不存在两处实现漂移的可能。本类只做两件事：把 Ops 的结果对象转成
 * 页面 VO，把 {@link IllegalArgumentException} 转成 {@link BizException} 交给全局异常处理器。</p>
 *
 * <p>隐私边界继承自 Ops：输入只在请求内存中解析，不落库、不写日志。</p>
 * @author owlzhangfq@gmail.com
 */
public interface DevToolCertService {

    /**
     * 解析 PEM 文本中的全部证书与 CSR 块（页面侧回显每张证书的 PEM，便于把证书链拆开取用）。
     */
    public abstract DevToolCertParseResponse parse(String pemContent);

    /**
     * 私钥与证书匹配校验。
     */
    public abstract DevToolCertMatchResponse match(String certPem, String privateKeyPem);

    /**
     * 解析 PFX/JKS 密钥库（页面独有：智能体没有文件上传通道，不在系统工具里暴露）。
     */
    public abstract DevToolKeystoreParseResponse parseKeystore(byte[] data, String password);

    /**
     * 导出密钥库指定条目的私钥 PEM（由页面显式动作触发，不随条目列举一起返回）。
     */
    public abstract DevToolPrivateKeyExportResponse exportPrivateKey(byte[] data, String password, String alias, String keyPassword);
}
