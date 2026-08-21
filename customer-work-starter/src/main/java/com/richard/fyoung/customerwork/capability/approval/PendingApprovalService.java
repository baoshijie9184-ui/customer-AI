package com.richard.fyoung.customerwork.capability.approval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 待审批单服务（Human-in-the-Loop 闭环的应用层实现）。
 *
 * <p><b>为何在应用层而非绑定框架内部 confirm-sink</b>：AgentScope 2.0-RC4 的运行时确认
 * （{@code RequireUserConfirmEvent} / {@code ReActAgent.CONFIRM_SINK_KEY}）未暴露 Web 友好的
 * 公共回填 API，绑定其内部协议脆弱；而退款等动作的领域模型本就是"先生成待确认工单、人工放行后执行"。
 * 故闭环落在工单审批层，与框架 Permission ASK（工具调用层闸门）<b>互补双层</b>：</p>
 * <ul>
 *   <li>Permission ASK：把关"要不要让 Agent 调用退款工具"；</li>
 *   <li>本服务：把关"工单生成后要不要真打款"，提供 approve/deny 端点闭环。</li>
 * </ul>
 *
 * <p>存储委托给 {@link ApprovalStore} SPI：默认 {@link InMemoryApprovalStore}（进程内，离线可测），
 * 生产可声明自己的 {@link ApprovalStore} Bean（如 JDBC / Redis 实现）覆盖默认，保证审批单重启不丢失。
 * approve 后由下游消费 APPROVED 状态执行实际打款（经 {@link #onApprove} 回调挂接）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface PendingApprovalService {

    /**
     * 登记一张待审批单（PENDING）。
     */
    public abstract ApprovalRequest submit(ApprovalType type, String sessionId, String orderId, String amount, String reason);

    /**
     * 全部审批单（含已决策）。
     */
    public abstract List<ApprovalRequest> list();

    /**
     * 按状态过滤（如只看 PENDING）。
     */
    public abstract List<ApprovalRequest> listByStatus(ApprovalStatus status);

    public abstract Optional<ApprovalRequest> find(String id);

    /**
     * 人工放行：推进状态并触发 onApprove 回调（下游执行实际打款）。
     */
    public abstract ApprovalRequest approve(String id, String operator);

    /**
     * 巡检重试执行失败的审批单（由 {@code ApprovalTimeoutScheduler} 定期调用）。
     *
     * <p><b>幂等性约定</b>：注册给 {@link #onApprove} 的回调必须对同一
     * {@link ApprovalRequest#getId()} 幂等（如按工单号去重后再打款），否则重试可能导致下游
     * 动作被重复执行——本方法只负责"再触发一次"，不做去重。</p>
     *
     * @param maxAttempts 最大执行尝试次数（含首次执行）；&lt;=1 表示不重试
     * @return 本次实际重试的审批单数
     */
    public abstract int retryExecutionFailures(int maxAttempts);

    /**
     * 人工拒绝：推进状态并触发 onDeny 回调；回调失败不影响拒绝决策本身（仅告警）。
     */
    public abstract ApprovalRequest deny(String id, String operator, String note);

    /**
     * 挂接 approve 决策回调（下游执行实际动作）。
     */
    public abstract void onApprove(Consumer<ApprovalRequest> callback);

    /**
     * 挂接 deny 决策回调。
     */
    public abstract void onDeny(Consumer<ApprovalRequest> callback);
}
