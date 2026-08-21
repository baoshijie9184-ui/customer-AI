package com.richard.fyoung.customerwork.core.service;

import com.richard.fyoung.customerwork.core.agent.CustomerServiceAgentFactory;
import com.richard.fyoung.customerwork.data.calllog.AgentCallMeta;
import com.richard.fyoung.customerwork.data.calllog.AgentCallSessionType;
import com.richard.fyoung.customerwork.infra.config.CustomerWorkProperties;
import com.richard.fyoung.customerwork.core.dto.IntentResult;
import com.richard.fyoung.customerwork.infra.counter.InMemoryWindowCounter;
import com.richard.fyoung.customerwork.infra.lock.InMemorySessionLock;
import com.richard.fyoung.customerwork.infra.lock.SessionLock;
import com.richard.fyoung.customerwork.capability.csat.CsatService;
import com.richard.fyoung.customerwork.capability.semanticcache.SemanticCacheService;
import com.richard.fyoung.customerwork.safety.quota.InMemoryTenantQuotaStore;
import com.richard.fyoung.customerwork.safety.quota.QuotaDecision;
import com.richard.fyoung.customerwork.safety.quota.TenantQuotaGuard;
import com.richard.fyoung.customerwork.safety.sensitiveword.SensitiveWordFilter;
import com.richard.fyoung.customerwork.safety.sensitiveword.SensitiveWordStreamGuard;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 客服会话服务（AgentScope 2.0 迁移版，对应②"会话恢复与上下文装配"与⑤"状态持久化"）。
 *
 * <p><b>2.0 状态模型</b>：Agent 不再自持会话状态，也不再需要手工 {@code saveTo/loadIfExists}。
 * 会话状态按 {@code (userId, sessionId)} 由框架在 {@code call/stream} 链路自动写入 / 恢复
 * {@link io.agentscope.core.state.AgentStateStore}（见 {@code SessionConfig}）。本服务通过
 * {@link RuntimeContext} 把"租户 + 会话"传入每次调用，单实例即可并发服务多租户多会话。</p>
 *
 * <p>进程内仍保留一个有界 LRU 热 Agent 缓存，仅用于摊薄"按会话装配 Agent"的构建开销
 * （并非状态存储）；缓存未命中时新建的 Agent 会在首次 {@code call} 时自动从 StateStore 恢复历史。</p>
 * @author owlzhangfq@gmail.com
 */
public interface CustomerServiceService {

    /**
     * 处理一条用户消息，返回完整回复（非流式）。
     *
     * @param sessionId 会话 ID（来自接入层，可含租户前缀如 tenantA:conv-1）
     * @param userText  用户输入文本
     * @return 助手回复文本（Mono，非阻塞）
     */
    public abstract Mono<String> chat(String sessionId, String userText);

    /**
     * 处理一条用户消息，流式返回增量文本（对应⑤ 逐 token 渲染）。
     *
     * <p>订阅框架的细粒度事件流 {@code streamEvents(...)}，只取正文增量 {@code TEXT_BLOCK_DELTA} 下发。
     * 会话状态由框架在流结束后自动持久化。</p>
     *
     * <p><b>为什么不用 {@code stream(msgs, options, ctx)}</b>：那组重载已标记 {@code forRemoval}，且它按
     * {@code isLast=true} 回放"每轮推理整段 + 最终 AGENT_RESULT 全文"，消费侧必须自己做两级去重才不会让
     * 用户看到同一段话重复 2-3 遍。细粒度事件把"增量"和"汇总"拆成了不同事件类型，去重逻辑随之消失。</p>
     *
     * @return 增量文本片段流（Flux，非阻塞）
     */
    public abstract Flux<String> chatStream(String sessionId, String userText);

    /**
     * 结构化意图识别（对应 3.3"结构化输出"）。
     *
     * <p>用 ReActAgent 的结构化输出能力，让模型严格按 {@link IntentResult} 的 Schema 返回。
     * 用一次性独立 Agent 与独立 sessionId 做分类，绝不污染真实对话会话的记忆。</p>
     *
     * <p>结构化输出走的是框架 fallback 工具路径（{@code generate_response} 被当作普通工具塞给模型，
     * 框架本身不会用 {@code tool_choice} 强制调用，见 agentscope-java #1852/#1699）：模型若这一轮
     * 没有主动选择调用该工具，{@link Msg#hasStructuredData()} 就会是 false，这不是异常情况，是该
     * 路径本身"不保证命中"的已知限制。提示词里显式要求模型必须调用该工具，只是提高命中率，不能
     * 保证 100% 生效；未命中时走 other 兜底，交由人工处理，不影响主链路可用性。</p>
     *
     * @return 结构化意图；模型未产出结构化数据或调用异常时，返回一个标注为 other 的兜底结果
     */
    public abstract Mono<IntentResult> classifyIntent(String sessionId, String userText);

    /**
     * 安全中断当前会话正在执行的 Agent（对应 3.1"安全中断 / 实时打断"）。
     *
     * @return 是否存在可中断的活跃会话
     */
    public abstract boolean interrupt(String sessionId);

    /**
     * 主动结束并清理会话：移除热缓存、删除持久化状态。
     *
     * <p>不再显式清理会话锁——锁对象由 {@link SessionLock} 实现自行回收
     * （进程内实现按使用者计数摘除，分布式实现靠 lease 过期），
     * 这里若强行清理反而可能把正在使用中的锁摘掉。</p>
     */
    public abstract void endSession(String sessionId);

    /**
     * 热配置刷新：清空进程内热 Agent 缓存，使下一次会话请求按最新配置（提示词/MCP/maxIters）重建 Agent。
     *
     * <p>只清热缓存，<b>不动</b> {@code AgentStateStore}（会话短期状态）与 {@code SessionLock}（会话锁）：
     * "淘汰即重建、状态从 StateStore 恢复"是既有 LRU 淘汰路径已验证的行为，热更新复用同一路径即可，不会
     * 丢失任何进行中会话的上下文。模型链的热替换走 {@code MutableDelegatingModel#swap}，与本方法互补
     * （模型链是共享单例，无需重建 Agent 即生效；提示词/MCP/maxIters 绑定在 Agent 上，需重建）。</p>
     */
    public abstract void flushHotAgents();

    /**
     * 清理空闲超时会话（由定时任务调用）：移除超过 timeoutMinutes 未活跃的会话缓存与状态。
     *
     * @param timeoutMinutes 空闲超时（分钟）；<=0 不清理
     * @return 清理的会话数
     */
    public abstract int cleanupIdleSessions(int timeoutMinutes);

    /**
     * 对话兜底回复文本（chat 调用失败时返回）。公开以便合成监控据此判定探测是否走了兜底。
     */
    public static final String FALLBACK_REPLY = "抱歉，系统繁忙，已为您记录问题，建议稍后再试或转人工坐席。";

    /**
     * 配额超限时的回复文本。
     *
     * <p>与 {@link #FALLBACK_REPLY} 分开：前者是"系统故障"，用户重试有意义；
     * 配额超限重试无用，措辞必须让人知道该去找谁。</p>
     */
    public static final String QUOTA_EXCEEDED_REPLY = "本期服务额度已用尽，请联系管理员提升额度后再试。";
}
