package com.richard.fyoung.customerworkapp.chat;

import com.richard.fyoung.customerwork.data.chatlog.ChatLogService;
import com.richard.fyoung.customerwork.data.chatlog.ChatMessage;
import com.richard.fyoung.customerwork.core.service.CustomerServiceService;
import com.richard.fyoung.customerwork.data.ticket.Ticket;
import com.richard.fyoung.customerwork.data.ticket.TicketActorType;
import com.richard.fyoung.customerwork.data.ticket.TicketCategory;
import com.richard.fyoung.customerwork.data.ticket.TicketService;
import com.richard.fyoung.customerwork.safety.security.UserPrincipal;
import com.richard.fyoung.customerwork.infra.ws.WsFrame;
import com.richard.fyoung.customerwork.infra.ws.WsSessionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 对话分发核心：把一条用户/坐席消息按工单当前状态路由到 AI 自助、人工排队提示、坐席转发或转人工。
 *
 * <p>所有落库（工单、消息）都是阻塞 IO，统一挪到 {@code boundedElastic} 线程执行，绝不占用 Netty 事件循环；
 * 下推走 {@link WsSessionRegistry}（非阻塞 Sink）。AI 流式回复的订阅寿命随返回的 {@link Mono} 绑定到 WS
 * 入站处理（连接关闭 → 入站取消 → 流式订阅一并取消），不泄漏。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ChatDispatchService {

    /**
     * 处理一条用户消息：校验会话归属 → 定位/新建工单 → 落库 → 关键词/状态路由。
     *
     * @return 处理完成信号（含 AI 流式全过程，寿命绑定调用方订阅）
     */
    public abstract Mono<Void> onUserMessage(UserPrincipal user, String sessionId, String content);

    /**
     * 用户在 WS 内主动请求转人工（type=handoff）：与关键词命中同路径。
     */
    public abstract Mono<Void> requestHandoff(UserPrincipal user, String sessionId, String reason);

    /**
     * 处理一条坐席消息：校验受理归属 → 落库 → 推给用户（离线只落库不报错）。
     */
    public abstract Mono<Void> onAgentMessage(String agentId, String ticketId, String content);
}
