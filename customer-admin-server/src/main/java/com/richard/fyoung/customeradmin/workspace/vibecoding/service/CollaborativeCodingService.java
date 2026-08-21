package com.richard.fyoung.customeradmin.workspace.vibecoding.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.config.AdminCollaborationProperties;
import com.richard.fyoung.customeradmin.workspace.audit.AiCodingOperation;
import com.richard.fyoung.customeradmin.workspace.audit.entity.AiCodingAuditLog;
import com.richard.fyoung.customeradmin.workspace.audit.service.AiCodingAuditService;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatNodeKind;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatStreamChunk;
import com.richard.fyoung.customeradmin.workspace.runtime.AdminAgentInstanceFactory;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.RoleStageEvent;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 多 Agent 协作编程（P3-1 降级版）：把一次需求输入串成"需求分析 → 方案设计 → 编码实现 → 自测审查"
 * 多角色顺序流水。降级说明见 {@link AdminCollaborationProperties}。
 *
 * <h3>编排思路（复用项目自研顺序编排）</h3>
 * 参照 {@code customer-work-starter} 的 {@code MultiAgentOrchestrator#sequential}——各角色输出
 * 作为下一角色输入逐步细化。本类不引入框架 SubAgent/Pipeline，只用一次性模型调用（PLAN/REVIEW 角色）
 * 与既有 VibeCoding 沙箱流（CODING 角色）在编排层串联：
 * <ul>
 *   <li><b>PLAN 角色</b>（需求分析/方案设计）：一次性模型调用输出文本，产出经 {@code role_stage} 事件推送，
 *       并累积进上下文供下游角色参考；</li>
 *   <li><b>CODING 角色</b>（编码实现）：委托 {@link VibeCodingService#stream} 走既有沙箱/file_change/
 *       test_report 链路，<b>不另起文件写入通道</b>；</li>
 *   <li><b>REVIEW 角色</b>（自测审查）：对本轮 git diff 一次性模型审查，产出经 {@code role_stage} 推送。</li>
 * </ul>
 *
 * <h3>失败语义（fast fail）</h3>
 * 任一角色失败 → 发一条 {@code STATUS_FAILED} 的 {@code role_stage} 事件明确报错并<b>中断整条流水</b>
 * （后续角色不再执行），审计记 {@link ResultCode#COLLAB_ROLE_FAILED}，绝不静默跳过。
 * @author owlzhangfq@gmail.com
 */
public interface CollaborativeCodingService {

    /**
     * 协作流水（未指定执行模式）：保留三参签名，回落全局模式语义。
     */
    public abstract Flux<ChatStreamChunk> stream(String agentCode, String sessionId, String userText);

    /**
     * 协作流水（带执行模式，无附件）：保留四参签名，供既有调用点/测试使用。
     */
    public abstract Flux<ChatStreamChunk> stream(String agentCode, String sessionId, String userText, String mode);

    /**
     * 协作流水（带执行模式 + 附件）：{@code attachmentIds} 透传给 CODING 角色的 {@link VibeCodingService#stream}。
     */
    public abstract Flux<ChatStreamChunk> stream(String agentCode, String sessionId, String userText, String mode, List<String> attachmentIds);
}
