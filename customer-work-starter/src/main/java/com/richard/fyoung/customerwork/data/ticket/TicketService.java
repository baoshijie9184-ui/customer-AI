package com.richard.fyoung.customerwork.data.ticket;

import com.richard.fyoung.customerwork.core.common.PageResult;
import com.richard.fyoung.customerwork.infra.transaction.CustomerWorkTransactionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 工单业务服务（应用层入口）：把 {@link Ticket} 充血实体的状态机流转编排成"校验→流转→持久化→
 * 追加事件→发布事件"的统一闭环。
 *
 * <p>每次成功流转都追加一条 {@link TicketEvent} 审计事件。JDBC 模式在同一事务内写入数据库 Outbox，
 * 提交后异步投递监听器；内存模式保持同步通知。抢单走存储层 {@code claimAtomically} 条件更新。</p>
 * @author owlzhangfq@gmail.com
 */
public interface TicketService {

    /**
     * 为会话建单（幂等）：若该会话已有活跃工单直接返回，避免同一会话重复开单。
     * 新单初始 AI_SERVING，工单号 {@code TK-<uuid>}。
     */
    public abstract Ticket createForSession(String sessionId, String userId, String title, TicketCategory category);

    /**
     * 请求转人工：按会话定位活跃工单（找不到 fast-fail），推进 AI_SERVING → WAITING_AGENT。
     * 仅当发生真实流转（实体返回 true）时才追加事件并广播——已在人工链路的幂等空转不重复发事件。
     */
    public abstract Ticket requestHandoff(String sessionId, String reason, TicketActorType actorType, String actorId);

    /**
     * 坐席接单：走存储层原子抢单（条件更新），失败（已被抢 / 非 WAITING_AGENT）fast-fail，
     * 语义等价 HTTP 409 Conflict。成功后追加 CLAIM 事件并广播。
     */
    public abstract Ticket claim(String ticketId, String agentId);

    /**
     * 撤销转人工：WAITING_AGENT → AI_SERVING。
     */
    public abstract Ticket cancelHandoff(String ticketId, TicketActorType actorType, String actorId);

    /**
     * 挂起：PROCESSING → ON_HOLD。
     */
    public abstract Ticket hold(String ticketId, TicketActorType actorType, String actorId);

    /**
     * 恢复处理：ON_HOLD → PROCESSING。
     */
    public abstract Ticket resume(String ticketId, TicketActorType actorType, String actorId);

    /**
     * 转回队列：PROCESSING|ON_HOLD → WAITING_AGENT（清坐席）。
     */
    public abstract Ticket transferToPool(String ticketId, TicketActorType actorType, String actorId);

    /**
     * 转派其他坐席：PROCESSING 态换绑坐席。
     */
    public abstract Ticket transferToAgent(String ticketId, String newAgentId, TicketActorType actorType, String actorId);

    /**
     * 标记处理完毕：PROCESSING → WAITING_CONFIRM。
     */
    public abstract Ticket markResolved(String ticketId, String note, TicketActorType actorType, String actorId);

    /**
     * 用户确认：WAITING_CONFIRM → RESOLVED。
     */
    public abstract Ticket confirm(String ticketId, TicketActorType actorType, String actorId);

    /**
     * 用户驳回：WAITING_CONFIRM → PROCESSING。
     */
    public abstract Ticket reject(String ticketId, String reason, TicketActorType actorType, String actorId);

    /**
     * 关闭工单：AI_SERVING|RESOLVED|WAITING_CONFIRM → CLOSED。
     */
    public abstract Ticket close(String ticketId, String reason, TicketActorType actorType, String actorId);

    /**
     * 强制关闭：任意非 CLOSED 态直达 CLOSED（空闲超时自动结束 / 用户强制结束用）。
     * 走统一闭环追加 FORCE_CLOSE 事件并广播（下游 WS 监听器据此实时推关闭事件给前端）。
     */
    public abstract Ticket forceClose(String ticketId, String reason, TicketActorType actorType, String actorId);

    /**
     * 重新打开：RESOLVED|CLOSED → WAITING_AGENT。
     */
    public abstract Ticket reopen(String ticketId, String reason, TicketActorType actorType, String actorId);

    /**
     * 重新打开回 AI 自助：RESOLVED|CLOSED → AI_SERVING（用户端"重新开始对话"专用）。
     */
    public abstract Ticket reopenToAi(String ticketId, String reason, TicketActorType actorType, String actorId);

    /**
     * 变更优先级（任意非 CLOSED 态）。
     */
    public abstract Ticket changePriority(String ticketId, TicketPriority priority, TicketActorType actorType, String actorId);

    /**
     * 变更分类（任意非 CLOSED 态）。
     */
    public abstract Ticket changeCategory(String ticketId, TicketCategory category, TicketActorType actorType, String actorId);

    /**
     * 回填工单标题（仅当原标题为空白时生效）：成功回填才持久化，且<b>不发事件</b>——标题补白不是
     * 状态流转，无需进审计轨迹，也不必广播给坐席工作台。
     *
     * @return true 表示发生了回填并已持久化；false 表示工单已有标题或给定标题为空白
     */
    public abstract boolean fillTitle(String ticketId, String title);

    /**
     * 刷新会话活跃工单的用户最后活跃时间（用户每发一条消息时调用）：仅当该会话有活跃工单时生效，
     * <b>不发事件、不广播</b>——刷新活跃时间不是状态流转（与 {@link #fillTitle} 同属非流转持久化）。
     *
     * @return true 表示命中活跃工单并已刷新；false 表示会话无活跃工单（无需刷新）
     */
    public abstract boolean touchUserActive(String sessionId);

    public abstract Optional<Ticket> find(String ticketId);

    public abstract Optional<Ticket> findActiveBySession(String sessionId);

    /**
     * 查该用户的活跃工单（非 CLOSED/RESOLVED 的最新一张，用于用户级唯一活跃会话去重）。
     */
    public abstract Optional<Ticket> findActiveByUser(String userId);

    public abstract PageResult<Ticket> findPage(TicketQuery query);

    /**
     * 按状态查全部（SLA 巡检等批量扫描用）。
     */
    public abstract List<Ticket> findByStatus(TicketStatus status);

    public abstract List<TicketEvent> findEvents(String ticketId);
}
