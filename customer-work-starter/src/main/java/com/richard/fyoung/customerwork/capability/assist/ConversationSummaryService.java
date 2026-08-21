package com.richard.fyoung.customerwork.capability.assist;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customerwork.data.chatlog.ChatMessage;
import com.richard.fyoung.customerwork.data.chatlog.ChatMessageStore;
import com.richard.fyoung.customerwork.infra.config.CustomerWorkProperties;
import com.richard.fyoung.customerwork.data.ticket.TicketActorType;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 会话总结建议服务（智能路由中控·会话总结）：对整段会话历史做一次性 LLM 总结，产出结构化
 * {@link ConversationSummary} 供接手坐席快速了解上下文。
 *
 * <p><b>fail-open（与敏感词的 fail-closed 相反）：</b>总结是"增强"能力——模型不可达 / 空响应 / 不守 JSON
 * 格式时，一律降级到规则版 {@link AgentAssistService} 的建议 + 历史原文进摘要（{@code fromModel=false}），
 * <b>绝不抛异常打断转人工与对话主链路</b>。这与"总结/推荐挂了不能影响转人工本身"的定位一致。</p>
 *
 * <p>一次性调用手法与 {@code ModelVisionOcrService} / admin 侧 {@code GitAssistantService.callModelOnce}
 * 一致（单条 user 消息、不带工具、收集全部文本块拼接）。结果按 sessionId 进有界缓存，供转人工时预生成、
 * 坐席工作台随后免二次 LLM 调用拉取。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ConversationSummaryService {

    /**
     * 对指定会话做整段总结。<b>永不抛异常</b>：任何失败都降级为规则版摘要返回。结果同时进缓存供
     * {@link #findLatest} 免二次调用拉取。
     */
    public abstract ConversationSummary summarize(String sessionId);

    /**
     * 拉取转人工时预生成并缓存的最近一次摘要（无则空，坐席工作台可据此决定是否触发按需生成）。
     */
    public abstract Optional<ConversationSummary> findLatest(String sessionId);
}
