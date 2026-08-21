package com.richard.fyoung.customeradmin.aiconfig.knowledgebase.runtime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.client.KnowledgeSearchClient;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.dto.KnowledgeBaseTestResult;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.entity.AiAgentKnowledgeBase;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.entity.AiKnowledgeBase;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.mapper.AiAgentKnowledgeBaseMapper;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.mapper.AiKnowledgeBaseMapper;
import com.richard.fyoung.customeradmin.common.crypto.AesGcmCryptoUtil;
import com.richard.fyoung.customerwork.data.rag.search.KnowledgeBaseEndpoint;
import com.richard.fyoung.customerwork.data.rag.search.KnowledgeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 每轮对话的知识库检索（对话 + VibeCoding 共用）：只负责"按提问召回并渲染成注入块"，
 * <b>不负责往哪儿注入</b>——注入由 {@link KnowledgeRetrievalMiddleware} 在推理阶段完成。
 *
 * <p><b>触发方式刻意不做成工具</b>：做成 {@code @Tool} 要靠模型自己决定调不调、调几次，
 * 对"每轮都应该带上业务知识"的客服/编码场景不可靠。这里改成每轮推理前自动检索、把召回内容
 * 随消息一起送进模型，模型无需决策。</p>
 *
 * <p><b>零开销约定</b>：智能体没绑定任何知识库时不发任何请求（只查一次关联表就返回 null）。</p>
 *
 * <p><b>为什么本类只返回块、不再拼接用户消息文本</b>（评审后改造）：早先的实现把召回块拼在用户
 * 消息文本尾部，导致注入内容随用户消息一起进了框架 {@code AgentState} 被持久化——历史接口会把
 * {@code <retrieved_knowledge>} 原样回显给用户，且该块进了会话记忆后每轮都会重发给模型、
 * token 随命中轮次线性累积。现在改为只返回渲染好的块，由中间件在 {@code onReasoning} 阶段挂成
 * 一条<b>瞬态</b>消息，只对本次模型调用可见，绝不进会话历史。</p>
 *
 * <p><b>唯一防御点</b>：本类的 {@link #retrieve} 捕获一切异常并回退为 null（=不注入），配合
 * {@link KnowledgeSearchClient#searchAll} 的单库降级，保证检索链路任何故障都不会打断对话。</p>
 * @author owlzhangfq@gmail.com
 */
public interface KnowledgeRetrievalService {

    /**
     * 按提问检索并渲染成注入块。未绑定知识库 / 召回为空 / 任何异常，一律返回 {@code null}
     * （= 本轮不注入；召回为空时绝不注入空标签，避免污染上下文）。
     *
     * <p><b>本方法会阻塞调用线程</b>（内部是同步 {@code HttpClient.send}，上限由
     * {@code admin.rag.retrieval-timeout-seconds} 兜死）。调用方必须保证它不跑在 Tomcat 请求线程上——
     * 唯一调用方 {@link KnowledgeRetrievalMiddleware} 用 {@code Schedulers.boundedElastic()} 承接。</p>
     *
     * @param agentCode 智能体编码
     * @param query     检索用的用户原始提问（VibeCoding 链路要用未被路径指引污染的原文）
     * @return 渲染好的注入块；不需要注入时返回 null
     */
    public abstract String retrieve(String agentCode, String query);
}
