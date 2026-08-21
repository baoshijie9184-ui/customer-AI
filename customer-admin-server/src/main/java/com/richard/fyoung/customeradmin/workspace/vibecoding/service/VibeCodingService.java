package com.richard.fyoung.customeradmin.workspace.vibecoding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.config.AdminSandboxProperties;
import com.richard.fyoung.customeradmin.workspace.audit.AiCodingOperation;
import com.richard.fyoung.customeradmin.workspace.audit.entity.AiCodingAuditLog;
import com.richard.fyoung.customeradmin.workspace.audit.service.AiCodingAuditService;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatNodeKind;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatStreamChunk;
import com.richard.fyoung.customeradmin.workspace.callstats.service.AgentCallMetaFactory;
import com.richard.fyoung.customeradmin.workspace.chat.service.ChatService;
import com.richard.fyoung.customeradmin.workspace.runtime.AdminAgentInstanceFactory;
import com.richard.fyoung.customerwork.data.calllog.AgentCallMeta;
import com.richard.fyoung.customerwork.data.calllog.AgentCallSessionType;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.FileChangeEvent;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.RollbackResult;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.TestReport;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.WorkspaceFileContent;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.WorkspaceFileNode;
import io.agentscope.core.model.ChatUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * VibeCoding：对话（复用 ChatService）+ 会话级目录隔离（{@code sessions/{sessionId}/}）+ 产物清单。
 *
 * <h3>目录结构约定</h3>
 * <pre>
 * data/admin-workspace/{agentCode}/
 *   sessions/{sessionId}/   ← 每次对话的产出物目录（本类管理此层）
 *   MEMORY.md               ← 智能体长期记忆（由 HarnessAgent 管理）
 * </pre>
 *
 * <p>HarnessAgent workspace 根目录仍是 {@code {agentCode}/}（便于 MEMORY.md 等跨会话文件访问），
 * 本类在 stream 开始前创建 {@code sessions/{sessionId}/} 子目录，并通过系统提示词约定 Agent
 * 将产出物写入该子目录，实现不同会话产出物物理隔离。</p>
 * @author owlzhangfq@gmail.com
 */
public interface VibeCodingService {

    /**
     * 当前 VibeCoding 沙箱模式（{@code admin.sandbox.mode} 全局配置，不随会话变化）："docker"｜"local"。
     */
    public abstract String sandboxMode();

    /**
     * 安全中断该会话正在执行的对话，薄委托——VibeCoding 与普通对话共用同一个 Agent 实例和会话状态，
     * 中断本身跟文件快照/审计无关，不需要额外逻辑。
     */
    public abstract boolean interrupt(String agentCode, String sessionId);

    /**
     * 流式对话（未指定执行模式）：保留旧三参签名，供协作链路/既有测试使用，回落全局模式语义。
     */
    public abstract Flux<ChatStreamChunk> stream(String agentCode, String sessionId, String userText);

    /**
     * 流式对话（带执行模式，无附件）：保留旧四参签名，供既有调用点/测试使用。
     */
    public abstract Flux<ChatStreamChunk> stream(String agentCode, String sessionId, String userText, String mode);

    /**
     * 流式对话（带执行模式 + 附件绑定）：{@code attachmentIds} 透传给 {@link ChatService#chatStream}，
     * 在请求线程同步段把附件绑定到本条用户消息（框架 Msg.id）。VibeCoding 面板上传的附件走此链路。
     */
    public abstract Flux<ChatStreamChunk> stream(String agentCode, String sessionId, String userText, String mode, List<String> attachmentIds);

    /**
     * Plan Mode 计划确认/拒绝（需求 P1-1）：完成对应挂起项，流侧中间件据此恢复或取消该高风险操作。
     * 会话归属校验隐含在 {@link PlanConfirmationService#confirm} 按 {@code (agentCode, sessionId)} 定位通道里——
     * planId 不存在/已处理/超时/服务重启后失效均 fast fail（{@link ResultCode#PLAN_CONFIRM_NOT_FOUND}）。
     */
    public abstract void confirmPlan(String agentCode, String sessionId, String planId, boolean approved, String note);

    /**
     * 本轮对话变更文件清单（相对于会话 workspace 的路径）。
     * 对比 {@link #stream} 开始前的快照与当前快照，返回新增或修改的文件。
     */
    public abstract List<String> listChangedArtifacts(String agentCode, String sessionId);

    /**
     * 列出指定会话 workspace 下的文件目录树。
     * 目录优先、同级按名称字母序排列。
     *
     * @return 根节点列表（即 sessions/{sessionId}/ 下的直接子节点）
     */
    public abstract List<WorkspaceFileNode> listWorkspaceFiles(String agentCode, String sessionId);

    /**
     * 读取指定会话 workspace 内某文件的内容。
     *
     * <p>安全校验：path 必须位于 sessions/{sessionId}/ 目录内，防止路径穿越（path traversal）。</p>
     *
     * @param agentCode 智能体编码
     * @param sessionId 会话 ID
     * @param relativePath 相对于 sessions/{sessionId}/ 的文件路径（如 {@code src/main/java/Foo.java}）
     * @return 文件内容 VO
     */
    public abstract WorkspaceFileContent readFileContent(String agentCode, String sessionId, String relativePath);

    /**
     * 会话一键回滚：把会话 workspace 恢复到对话前的 baseline 状态（新增文件删除、修改/删除的已跟踪文件
     * 还原），{@code .git} 保留，baseline 不被破坏（回滚后可继续对话建立新变更）。
     *
     * <p>local 与 docker 模式均支持：docker 模式产物经 bind mount（P1-3）实时落宿主机会话目录，
     * 会话 git 仓库（{@link GitWorkspaceService#ensureRepo}）也建在同一宿主机目录，回滚即对该目录
     * 执行 git 还原，语义与 local 一致。破坏性操作 + baseline 缺失校验的安全兜底下沉在
     * {@link GitWorkspaceService#rollbackToBaseline}。</p>
     */
    public abstract RollbackResult rollback(String agentCode, String sessionId);

    /**
     * 保存指定会话 workspace 内某文件的内容（区创建/覆盖写入）。
     *
     * <p>安全校验：path 必须位于 sessions/{sessionId}/ 目录内，防止路径穿越。</p>
     *
     * @param agentCode    智能体编码
     * @param sessionId    会话 ID
     * @param relativePath 相对于 sessions/{sessionId}/ 的文件路径
     * @param content      文件完整新内容
     */
    public abstract void saveFileContent(String agentCode, String sessionId, String relativePath, String content);
}
