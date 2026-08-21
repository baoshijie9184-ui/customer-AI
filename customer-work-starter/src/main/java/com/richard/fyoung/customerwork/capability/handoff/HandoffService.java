package com.richard.fyoung.customerwork.capability.handoff;

import com.richard.fyoung.customerwork.capability.routing.HandoffCreatedEnricher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * 人机切换工单服务（AI→人工接管→人工→AI 回收 的应用层闭环）。
 *
 * <p>取代 {@code HumanHandoffTools.transferToHuman} 此前"只打日志 + 生成随机字符串工单号"的空实现——
 * 转人工不再是一句无状态的话术，而是一张可查询、可流转的 {@link HandoffTicket}：AI 转出生成
 * {@code PENDING} 工单 → 坐席 {@link #claim} 接单（{@code CLAIMED}）→ 坐席处理完毕
 * {@link #resolve}（{@code RESOLVED}，会话可回收给 AI 续接）。</p>
 *
 * <p>存储委托给 {@link HandoffStore} SPI：默认 {@link InMemoryHandoffStore}（进程内，离线可测），
 * 生产可声明自己的 {@link HandoffStore} Bean（如 JDBC / Redis 实现）覆盖默认，保证工单重启不丢失。</p>
 * @author owlzhangfq@gmail.com
 */
public interface HandoffService {

    /**
     * 可选注入转人工增强器（不存在则不增强，建单行为不变）。
     */
    public abstract void setEnricher(HandoffCreatedEnricher enricher);

    /**
     * AI 转出：登记一张待接单工单（PENDING）。
     */
    public abstract HandoffTicket create(String sessionId, String reason);

    /**
     * 回写工单智能分配的分类与推荐结果（由 {@code HandoffCreatedEnricher} 异步调用）。
     *
     * <p>走本服务自身的 {@link HandoffStore}，保证与坐席工作台经本服务读到的是同一份工单。工单不存在时只
     * error 记录、不抛（fail-open：增强回写迟到于工单已被清理等边界情况不应产生异常）。</p>
     */
    public abstract void applyRoutingSuggestion(String id, String category, String requiredSkill, String priority, String emotion, String suggestedAssignees);

    /**
     * 全部工单（含已结案）。
     */
    public abstract List<HandoffTicket> list();

    /**
     * 按状态过滤（如只看 PENDING 待接单）。
     */
    public abstract List<HandoffTicket> listByStatus(HandoffStatus status);

    public abstract Optional<HandoffTicket> find(String id);

    /**
     * 坐席接单：仅 PENDING 可推进，重复接单 fast-fail。
     */
    public abstract HandoffTicket claim(String id, String operator);

    /**
     * 坐席处理完毕、回收给 AI：仅 CLAIMED 可推进，未接单先结案 fast-fail。
     */
    public abstract HandoffTicket resolve(String id, String note);
}
