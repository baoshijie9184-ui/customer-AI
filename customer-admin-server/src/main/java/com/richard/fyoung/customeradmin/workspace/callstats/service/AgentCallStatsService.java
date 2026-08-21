package com.richard.fyoung.customeradmin.workspace.callstats.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workspace.callstats.config.AppAgentCallStatsGatewayProvider;
import com.richard.fyoung.customeradmin.workspace.callstats.dto.AgentCallSegmentVO;
import com.richard.fyoung.customeradmin.workspace.callstats.dto.AgentCallStatsDetailVO;
import com.richard.fyoung.customeradmin.workspace.callstats.dto.AgentCallStatsPageVO;
import com.richard.fyoung.customeradmin.workspace.callstats.dto.AgentCallStatsQuery;
import com.richard.fyoung.customeradmin.workspace.callstats.dto.AgentCallStatsRowVO;
import com.richard.fyoung.customeradmin.workspace.callstats.dto.AgentCallStatsSource;
import com.richard.fyoung.customeradmin.workspace.callstats.dto.AgentCallStatsSummaryVO;
import com.richard.fyoung.customeradmin.workspace.callstats.dto.AgentCallTrendVO;
import com.richard.fyoung.customeradmin.workspace.callstats.jdbc.AgentCallStatsGateway;
import com.richard.fyoung.customeradmin.workspace.callstats.jdbc.AgentCallStatsQueryParam;
import com.richard.fyoung.customeradmin.workspace.callstats.jdbc.AgentCallStatsTrendRow;
import com.richard.fyoung.customerwork.data.calllog.TrendGranularity;
import com.richard.fyoung.customerwork.data.calllog.entity.AgentCallLogDO;
import com.richard.fyoung.customerwork.data.calllog.entity.AgentCallSegmentDO;
import com.richard.fyoung.customerwork.data.calllog.entity.AgentCallSummaryDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能体调用耗时统计查询服务：按 {@code source} 路由到 ADMIN 本库 / APP 客服端库门面，做分页/详情/汇总/
 * 趋势/删除，并把持久层 DO 转成前端契约 VO（时间戳→{@code yyyy-MM-dd HH:mm:ss}、问题/回答预览截断）。
 *
 * <p>读侧 page/summary/trend 走 admin 的 ext Mapper（带 sessionType 过滤 + trend 各段平均）；详情按 id 取
 * 主记录（starter BaseMapper#selectById）+ 明细段；删除复用 starter Store（主记录 + 分段级联）。APP 源不可
 * 达时门面构建阶段即抛 {@link ResultCode#CUSTOMER_WORK_UNAVAILABLE}（见 {@link AppAgentCallStatsGatewayProvider}）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AgentCallStatsService {

    /**
     * 分页查询（{total, rows}）。
     */
    public abstract AgentCallStatsPageVO page(AgentCallStatsQuery query);

    /**
     * 明细（全量字段 + 分段列表）。id 不存在抛 {@link ResultCode#RESOURCE_NOT_FOUND}。
     */
    public abstract AgentCallStatsDetailVO detail(long id, String source);

    /**
     * 汇总统计。
     */
    public abstract AgentCallStatsSummaryVO summary(AgentCallStatsQuery query);

    /**
     * 趋势聚合（按天/小时，含各段平均耗时）。
     */
    public abstract List<AgentCallTrendVO> trend(AgentCallStatsQuery query);

    /**
     * 删除一条调用（主记录 + 分段级联，复用 starter Store）。
     */
    public abstract boolean delete(long id, String source);
}
