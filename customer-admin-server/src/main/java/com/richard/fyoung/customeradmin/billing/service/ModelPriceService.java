package com.richard.fyoung.customeradmin.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.billing.entity.AiModelPrice;
import com.richard.fyoung.customeradmin.billing.mapper.AiModelPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * token → 金额换算。
 *
 * <p>取价规则：同一 {@code (provider, model)} 下取<b>生效时间不晚于结算时刻的最新一条</b>。
 * 调价插新行而不改旧行，历史账单据此算得回去。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ModelPriceService {

    /**
     * 按 token 用量算金额。
     *
     * <p>缓存命中的 token 是 {@code inputTokens} 的子集，按缓存价单独计，
     * 剩下的部分才按输入价计——重复计一次会把账单虚高。</p>
     *
     * @return 金额（元）；查不到单价时返回 0 并打日志，不抛异常——
     *         计费算不出来不该阻断归集任务，缺价这件事由日志和"金额为 0"的异常报表暴露
     */
    public abstract BigDecimal calculate(String provider, String modelName, long inputTokens, long outputTokens, long cachedTokens, LocalDateTime settleAt);

    /**
     * 查生效单价：provider 为空时只按模型名匹配。
     *
     * <p>日志里没有 provider（{@code cw_agent_call_log} 只记 agent 信息），
     * 归集时传空是常态，因此这里必须容忍。</p>
     */
    public abstract AiModelPrice findEffectivePrice(String provider, String modelName, LocalDateTime settleAt);
}
