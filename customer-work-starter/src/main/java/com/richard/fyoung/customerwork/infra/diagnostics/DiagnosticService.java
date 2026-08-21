package com.richard.fyoung.customerwork.infra.diagnostics;

import com.richard.fyoung.customerwork.capability.approval.ApprovalRequest;
import com.richard.fyoung.customerwork.capability.approval.PendingApprovalService;
import com.richard.fyoung.customerwork.capability.dialog.DialogStageService;
import com.richard.fyoung.customerwork.core.memory.FactLog;
import com.richard.fyoung.customerwork.observability.AuditQuery;
import com.richard.fyoung.customerwork.core.service.SessionStateManager;
import com.richard.fyoung.customerwork.capability.slotfilling.SlotFillingForm;
import com.richard.fyoung.customerwork.capability.slotfilling.SlotFillingService;
import com.richard.fyoung.customerwork.core.support.TenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话故障诊断聚合服务（线上定位的核心：一次拉齐分散在多个存储里的会话现场）。
 *
 * <p>把 StateStore / 对话阶段 / 槽位进度 / 审批单 / 审计事件 / 质检事实六个数据源聚合成一个
 * {@link SessionDiagnostic}。<b>本类是"防御式聚合唯一应存在的地方"</b>：每个数据源独立 try/catch，
 * 单个源不可用（如 MySQL 瞬断、审计后端未接入）只标注到 {@code degradedSources} 并降级，
 * 绝不让诊断工具自身崩溃——因为它恰恰要在系统部分故障时还能用。</p>
 * @author owlzhangfq@gmail.com
 */
public interface DiagnosticService {

    /**
     * 以默认条数聚合诊断。
     */
    public abstract SessionDiagnostic diagnose(String sessionId);

    /**
     * 聚合会话诊断全景。
     *
     * @param sessionId  会话 ID（可含租户前缀）
     * @param auditLimit 审计事件返回上限
     * @param factLimit  质检事实返回上限（取最近的）
     */
    public abstract SessionDiagnostic diagnose(String sessionId, int auditLimit, int factLimit);

    /**
     * 审计事件默认返回条数。
     */
    public static final int DEFAULT_AUDIT_LIMIT = 20;

    /**
     * 质检事实默认返回条数。
     */
    public static final int DEFAULT_FACT_LIMIT = 20;
}
