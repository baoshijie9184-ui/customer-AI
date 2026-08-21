package com.richard.fyoung.customerwork.observability.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customerwork.capability.approval.ApprovalRequest;
import com.richard.fyoung.customerwork.capability.approval.ApprovalStatus;
import com.richard.fyoung.customerwork.capability.approval.PendingApprovalService;
import com.richard.fyoung.customerwork.capability.handoff.HandoffService;
import com.richard.fyoung.customerwork.capability.handoff.HandoffStatus;
import com.richard.fyoung.customerwork.capability.handoff.HandoffTicket;
import com.richard.fyoung.customerwork.core.memory.FactLog;
import com.richard.fyoung.customerwork.core.memory.FactRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务数据分析聚合服务（运营视角：这段时间业务运转得怎么样）。
 *
 * <p>聚合审批（{@code cw_approval}）、人机切换（{@code cw_handoff_ticket}）、质检
 * （{@code FactLog}）三个已有数据源，按时间窗口计算业务指标——这些是 Prometheus 技术指标
 * 无法直接给出的"业务级周期报告"（如"过去 7 天平均放行率""平均接单时长"），数据源本身已具备
 * 时间戳与完整状态机，只是此前没有聚合视角。</p>
 *
 * <p>审批/人机切换维度的时间窗过滤在应用层完成（基于 Store SPI 已有的 {@code list()} 全量读取），
 * 未新增任何按时间范围查询的 SQL 方法——这两类"工单"表体量小（业务上是可控的案例数，非海量流水），
 * 全量加载后在内存过滤是合理的权衡，避免为聚合视角单独扩宽三个 Store SPI。</p>
 * @author owlzhangfq@gmail.com
 */
public interface BusinessAnalyticsService {

    /**
     * 聚合指定时间窗口 [windowStartMs, windowEndMs) 的业务报表。
     *
     * @param tenantId 质检维度所属租户；为空时质检维度返回空占位（见 {@link QualityStats}）
     */
    public abstract BusinessAnalyticsReport report(long windowStartMs, long windowEndMs, String tenantId);
}
