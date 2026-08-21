package com.richard.fyoung.customeradmin.workspace.vibecoding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.message.service.SiteMessageService;
import com.richard.fyoung.customeradmin.workspace.audit.AiCodingOperation;
import com.richard.fyoung.customeradmin.workspace.audit.entity.AiCodingAuditLog;
import com.richard.fyoung.customeradmin.workspace.audit.service.AiCodingAuditService;
import com.richard.fyoung.customeradmin.workspace.runtime.AdminAgentInstanceFactory;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.CommitMessageResponse;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.GitDiffSummary;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.PrDescriptionResponse;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.ReviewIssue;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.ReviewResult;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.ReviewTaskVO;
import com.richard.fyoung.customeradmin.workspace.vibecoding.entity.CodeReviewTask;
import com.richard.fyoung.customeradmin.workspace.vibecoding.mapper.CodeReviewTaskMapper;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * VibeCoding Git 助手：基于会话 workspace 相对基线的 git diff，一次性调用模型（不经过 ReAct 工具
 * 循环，纯文本生成）生成 diff 摘要 / commit message / PR description。
 *
 * <p>与 {@link VibeCodingService#stream} 共享同一份能力校验与会话目录解析，但完全不复用
 * {@code ChatService}——那条链路是多轮 ReAct 对话，会触发文件读写等工具；这里只需要模型的
 * 纯文本摘要能力，直接拿 {@link AdminAgentInstanceFactory#buildModelForAgent} 现场构建的
 * {@link Model} 一次性调用，派发到独立线程池，不占用 Tomcat 请求线程（与 {@code McpService}
 * 连通性测试同一手法）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface GitAssistantService {

    /**
     * diff 摘要：无变更时直接返回空摘要，不额外调用模型。
     */
    public abstract CompletableFuture<GitDiffSummary> diffSummary(String agentCode, String sessionId);

    /**
     * 生成 commit message：无变更时直接报错，不调用模型（没有内容可总结）。
     */
    public abstract CompletableFuture<CommitMessageResponse> commitMessage(String agentCode, String sessionId, String style);

    /**
     * 生成 PR description：无变更时直接报错，不调用模型。
     */
    public abstract CompletableFuture<PrDescriptionResponse> prDescription(String agentCode, String sessionId);

    /**
     * 提交 AI 代码审查任务（需求 P0-2 §4.2，提交-轮询模型）：模型分钟级调用不再阻塞前端。
     * 同步段完成校验并落 {@link CodeReviewTask#STATUS_RUNNING} 任务行、立即返回 taskId；
     * 真正的模型审查在 {@link #GIT_ASSISTANT_EXECUTOR} 异步执行，完成后回写结果并发站内信通知。
     *
     * <p>{@code userId} 必须在同步段（Web 请求线程）由调用方捕获传入——异步线程脱离 Sa-Token 上下文，
     * 拿不到当前登录用户。</p>
     *
     * @return 审查任务 id（前端据此轮询 {@link #getReviewTask}）
     */
    public abstract Long submitReview(String agentCode, String sessionId, Long userId);

    /**
     * 查询审查任务（轮询入口）：校验归属，非本人/不存在快速失败。SUCCESS 时反序列化结果一并返回。
     */
    public abstract ReviewTaskVO getReviewTask(Long taskId, Long userId);
}
