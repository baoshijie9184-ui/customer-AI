package com.richard.fyoung.customeradmin.workspace.audit.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.workspace.audit.AiCodingOperation;
import com.richard.fyoung.customeradmin.workspace.audit.dto.AiCodingAuditQuery;
import com.richard.fyoung.customeradmin.workspace.audit.entity.AiCodingAuditLog;
import com.richard.fyoung.customeradmin.workspace.audit.mapper.AiCodingAuditLogMapper;
import io.agentscope.core.model.ChatUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.List;

/**
 * AI 编码操作审计（需求文档 §5.2/§5.3）：构建审计条目、补全结果并异步落库、分页查询。
 *
 * <h3>埋点约定（调用方须知）</h3>
 * <ul>
 *   <li>{@link #begin} 必须在<b>请求线程的同步段</b>调用——操作人取自 Sa-Token 的 ThreadLocal
 *       上下文，SSE 的 {@code doFinally} / {@code CompletableFuture} 回调线程里拿不到；</li>
 *   <li>{@link #finish} 可在任意线程调用（耗时/结果补全 + 异步落库）；</li>
 *   <li>业务动作必须自持控制流（try/catch 后调 finish），<b>不要</b>把业务 lambda 交给审计服务执行——
 *       审计是旁路，它不可用/被替换时业务必须照常运转。</li>
 * </ul>
 * @author owlzhangfq@gmail.com
 */
public interface AiCodingAuditService {

    /**
     * 开始一次审计：填充操作人（当前登录用户）、操作类型与计时起点。
     * 未登录/无 Sa-Token 上下文（如单测）时操作人留空，不阻断业务。
     */
    public abstract AiCodingAuditLog begin(AiCodingOperation operation, String agentCode, String sessionId);

    /**
     * 结束一次审计（错误码形式）：补全耗时与结果并异步落库。{@code errorCode} 为空视为成功。
     */
    public abstract void finish(AiCodingAuditLog entry, String errorCode);

    /**
     * 结束一次审计（异常形式）：{@code error} 为空视为成功，否则沿 cause 链解析错误码。
     */
    public abstract void finish(AiCodingAuditLog entry, Throwable error);

    /**
     * 把模型用量写入审计条目（{@code usage} 为空表示未发生模型调用/框架未返回，保持空值）。
     */
    public abstract void applyUsage(AiCodingAuditLog entry, ChatUsage usage);

    /**
     * 把变更文件清单序列化进审计条目（空清单存空值，省存储且查询语义明确）。
     */
    public abstract void applyChangedFiles(AiCodingAuditLog entry, List<String> changedFiles);

    /**
     * 分页查询：keyword 匹配操作人账号，status 复用为 result 过滤，其余为精确条件。
     */
    public abstract PageResult<AiCodingAuditLog> page(AiCodingAuditQuery query);
}
