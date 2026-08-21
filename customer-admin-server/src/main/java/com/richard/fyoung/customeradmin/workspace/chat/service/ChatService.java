package com.richard.fyoung.customeradmin.workspace.chat.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatNodeKind;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatStreamChunk;
import com.richard.fyoung.customeradmin.workspace.memory.AgentMemorySyncService;
import com.richard.fyoung.customeradmin.workspace.runtime.AdminAgentInstanceFactory;
import com.richard.fyoung.customeradmin.workspace.runtime.AgentInstanceCache;
import com.richard.fyoung.customeradmin.workspace.runtime.ToolSourceInfo;
import com.richard.fyoung.customeradmin.workspace.runtime.mode.ExecutionMode;
import com.richard.fyoung.customeradmin.workspace.runtime.mode.ExecutionModeRegistry;
import com.richard.fyoung.customeradmin.workspace.vibecoding.service.PlanConfirmationService;
import com.richard.fyoung.customeradmin.workspace.vibecoding.service.PlanConfirmationService.PlanChannel;
import com.richard.fyoung.customerwork.data.calllog.AgentCallMeta;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEndEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.ModelCallStartEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatUsage;
import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.richard.fyoung.customeradmin.contentguard.config.ContentGuardProperties;
import com.richard.fyoung.customerwork.infra.config.CustomerWorkProperties;
import com.richard.fyoung.customerwork.safety.sensitiveword.SensitiveWordFilter;
import com.richard.fyoung.customerwork.safety.sensitiveword.SensitiveWordStreamGuard;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import com.richard.fyoung.customerwork.infra.config.properties.SensitiveWordProperties;

/**
 * 工作区对话服务：从 {@link AgentInstanceCache} 取（或惰性构建）智能体实例，流式对话。
 *
 * <p>与 {@code CustomerServiceService#chatStream} 同一套"细粒度事件流 -&gt; 展示片段"手法
 * （框架 {@code streamEvents(msgs, ctx)}），区别仅在于 Agent 实例来源：那边是启动期固定装配的
 * 单例，这里是按 agentCode 动态取的缓存实例，且底层可能是 ReActAgent 也可能是 HarnessAgent
 * （两者各自声明 {@code streamEvents}，本类按运行时类型分派，见 {@link #streamEvents}）。</p>
 *
 * <p>一轮流式对话正常结束（含内部异常被兜底成 {@link #FALLBACK_REPLY} 后正常结束的情形）后，主动调用
 * {@link ChatHistoryCache#evict} 让该智能体的历史会话列表缓存与本次会话的消息缓存立即失效——写路径本身
 * 不变（仍是 {@code MysqlAgentStateStore} 同步落库），只是让 {@link ChatHistoryService} 的 30 分钟读
 * 缓存不必等自然过期就能看到最新一轮对话，VibeCoding 复用同一个 {@code chatStream} 天然一并覆盖。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ChatService {

    /**
     * 执行模式确认/拒绝（对话链路的 Plan 确认闭环，与 VibeCoding 共用同一套
     * {@link PlanConfirmationService}）：完成对应挂起项，中间件据此恢复或取消该工具调用。
     * planId 不存在/已处理/超时/服务重启后失效均 fast fail。
     */
    public abstract void confirmPlan(String agentCode, String sessionId, String planId, boolean approved);

    /**
     * 安全中断指定会话正在执行的 Agent（协作式中断：只置一个信号，由 Agent 在推理/工具调用的
     * checkpoint 检查后才真正停下，不保证立即生效）。中断后再次对同一 sessionId 发起
     * {@link #chatStream}，框架会先无缝续跑被打断的挂起工具调用（见
     * {@link AdminAgentInstanceFactory#build} 里的 {@code enablePendingToolRecovery(true)}）。
     *
     * @return 是否成功发出中断信号；智能体运行时类型不支持中断（既非 ReActAgent 也非 HarnessAgent）时返回 false
     */
    public abstract boolean interrupt(String agentCode, String sessionId);

    /**
     * 流式对话，返回增量文本片段。智能体不存在/未启用时，{@link AgentInstanceCache#getOrBuild}
     * 同步抛出的 {@code BizException} 会在本方法返回 Flux 之前就传播给调用方（Controller 侧因此在
     * 任何 SSE 头下发之前就能拿到结构化错误响应，而不是半开的失败流）。
     */
    public abstract Flux<ChatStreamChunk> chatStream(String agentCode, String sessionId, String userText);

    /**
     * 流式对话（带执行模式，无用量观察者）：保留旧签名，供既有调用点/测试使用。
     */
    public abstract Flux<ChatStreamChunk> chatStream(String agentCode, String sessionId, String userText, String mode);

    /**
     * 流式对话（带执行模式 + 调用元数据，无用量观察者）：供对话链路（ChatController）使用，采集耗时统计。
     */
    public abstract Flux<ChatStreamChunk> chatStream(String agentCode, String sessionId, String userText, String mode, AgentCallMeta callMeta);

    /**
     * 流式对话（带执行模式 + 调用元数据 + 附件绑定，无用量观察者）：供对话链路（ChatController）使用。
     * {@code attachmentIds} 非空时在请求线程同步段把这些附件绑定到本条用户消息（框架 Msg.id）。
     *
     * <p>方法名末位加 {@code WithAttachments} 而非再重载六参：六参 {@code (…, callMeta, List)} 会与既有
     * {@code (…, callMeta, Consumer)} 在实参传 {@code null} 时产生调用歧义，故用具名方法规避。</p>
     */
    public abstract Flux<ChatStreamChunk> chatStreamWithAttachments(String agentCode, String sessionId, String userText, String mode, AgentCallMeta callMeta, List<String> attachmentIds);

    /**
     * 流式对话（带用量观察者，未指定执行模式）：保留旧签名，供既有调用点/测试使用。
     */
    public abstract Flux<ChatStreamChunk> chatStream(String agentCode, String sessionId, String userText, Consumer<ChatUsage> usageTotalObserver);

    /**
     * 流式对话（带执行模式 + 用量观察者，未带调用元数据）：保留旧签名，供既有调用点/测试使用。
     */
    public abstract Flux<ChatStreamChunk> chatStream(String agentCode, String sessionId, String userText, String mode, Consumer<ChatUsage> usageTotalObserver);

    /**
     * 流式对话（带执行模式 + 调用元数据 + 用量观察者，无附件）：保留旧签名，供 VibeCoding 既有调用点/测试使用。
     */
    public abstract Flux<ChatStreamChunk> chatStream(String agentCode, String sessionId, String userText, String mode, AgentCallMeta callMeta, Consumer<ChatUsage> usageTotalObserver);

    /**
     * 流式对话（带执行模式 + 模型用量观察者，全参核心）：流终止时（完成/错误/取消）把本轮全部模型调用的
     * token 用量汇总后回调一次 {@code usageTotalObserver}——供 VibeCoding 审计记录 token 数（需求文档 §5.3）。
     * 本轮无任何用量信息（框架/模型未返回 usage）时回调 {@code null}。
     *
     * <p>聚合方式：细粒度事件流里每次模型调用结束都会带一条 {@link ModelCallEndEvent}，其
     * {@code usage} 就是这一次调用的用量，{@code replyId} 每次调用各不相同——按 replyId 去重后求和
     * 即本轮全部模型调用（含子 Agent 的）的合计。比旧路径"从事件消息上捞 {@code Msg#getUsage()}
     * 再按 messageId 去重"更贴合语义：那条路上增量事件与最终结果消息携带的口径并不统一。</p>
     *
     * <p><b>执行模式与 Plan 确认闭环</b>：订阅时把 {@code mode}（解析为 {@link ExecutionMode}）登记进
     * {@link ExecutionModeRegistry}（键 {@code agentCode:sessionId}），供 {@code ExecutionModeMiddleware}
     * 运行时读取；并打开一个 {@link PlanChannel} 把 {@code plan}/{@code plan_result} 事件合并进 SSE 输出
     * （对话与 VibeCoding 共用同一套挂起/确认闭环）。{@code doFinally} 时摘除模式登记、关闭通道。
     * mode 未指定/非法（{@link ExecutionMode#parse} 返回 null）→ 不登记，中间件回落全局语义。</p>
     *
     * <p><b>附件绑定</b>：{@code attachmentIds} 非空时，在<b>请求线程同步段</b>（构建 Flux 前、订阅前）把这些
     * 附件绑定到本条用户消息（框架 {@code Msg.id}）——用与 Plan/历史一致的归一 {@code safeSession} 落 session_id，
     * 保证历史接口按同一口径查回。绑定是旁路动作，任何失败只记录、不打断对话主流程（见
     * {@link ChatAttachmentService#bindToMessage}）。</p>
     */
    public abstract Flux<ChatStreamChunk> chatStream(String agentCode, String sessionId, String userText, String mode, AgentCallMeta callMeta, List<String> attachmentIds, Consumer<ChatUsage> usageTotalObserver);
}
