package com.richard.fyoung.customeradmin.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.billing.dto.UsageAggregate;
import com.richard.fyoung.customeradmin.billing.entity.CwTenantUsageDaily;
import com.richard.fyoung.customeradmin.billing.mapper.CwTenantUsageDailyMapper;
import com.richard.fyoung.customerwork.safety.tenant.CrossTenantOperations;
import com.richard.fyoung.customerwork.safety.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用量归集：把 {@code cw_agent_call_log} 的原始调用记录按「租户 + 日期 + 模型」汇总进
 * {@code cw_tenant_usage_daily}，并按当日单价算好金额。
 *
 * <p><b>为什么要落一张汇总表而不是查询时实时聚合</b>：账单要能对得上——单价会调整，
 * 实时聚合会让历史账单随调价而变动；且原始日志量级远大于汇总，按月出账时全表扫不可接受。</p>
 *
 * <p>归集可重复执行（同一天再跑一次就覆盖），因此补数据只要重跑对应日期即可。</p>
 * @author owlzhangfq@gmail.com
 */
public interface UsageAggregationService {

    /**
     * 归集指定日期的用量。
     *
     * <p>整段跑在跨租户豁免下：归集本身就是要覆盖所有租户，
     * 若按当前上下文过滤，就只会汇总到某一个租户的数据。</p>
     *
     * @return 写入的记录数
     */
    public abstract int aggregate(LocalDate statDate);
}
