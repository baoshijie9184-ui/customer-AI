package com.richard.fyoung.customeradmin.contentguard.service;

import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.contentguard.config.ContentGuardGatewayProvider;
import com.richard.fyoung.customeradmin.contentguard.dto.ContentGuardCountVO;
import com.richard.fyoung.customeradmin.contentguard.dto.SensitiveWordHitLogPageQuery;
import com.richard.fyoung.customeradmin.contentguard.dto.SensitiveWordHitLogVO;
import com.richard.fyoung.customeradmin.contentguard.dto.SensitiveWordHitStatsVO;
import com.richard.fyoung.customeradmin.contentguard.jdbc.ContentGuardCountRow;
import com.richard.fyoung.customeradmin.contentguard.jdbc.ContentGuardGateway;
import com.richard.fyoung.customeradmin.contentguard.jdbc.SensitiveWordHitLogExtMapper;
import com.richard.fyoung.customeradmin.contentguard.jdbc.SensitiveWordHitLogQueryParam;
import com.richard.fyoung.customerwork.safety.sensitiveword.entity.SensitiveWordHitLogEntity;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 敏感词命中看板：明细分页 + 四组统计（动作分布 / 方向分布 / Top 命中词 / 时间趋势）。
 *
 * <p>明细与统计共用同一套筛选条件，保证图表和列表永远在讲同一批数据——两者用不同条件是看板最常见的坑。</p>
 * @author owlzhangfq@gmail.com
 */
public interface SensitiveWordHitLogService {

    /**
     * 命中明细分页。
     */
    public abstract PageResult<SensitiveWordHitLogVO> page(SensitiveWordHitLogPageQuery query);

    /**
     * 看板统计。
     */
    public abstract SensitiveWordHitStatsVO stats(SensitiveWordHitLogPageQuery query);
}
