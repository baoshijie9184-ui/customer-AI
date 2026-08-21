package com.richard.fyoung.customeradmin.ops.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.ops.config.OpsGatewayProvider;
import com.richard.fyoung.customerwork.capability.csat.CsatSummary;
import com.richard.fyoung.customerwork.capability.csat.CsatSurvey;
import com.richard.fyoung.customerwork.capability.deadletter.DeadLetter;
import com.richard.fyoung.customerwork.capability.deadletter.DeadLetterStatus;
import com.richard.fyoung.customerwork.capability.knowledgegap.KnowledgeGap;
import com.richard.fyoung.customerwork.capability.prompt.PromptVersion;
import com.richard.fyoung.customerwork.capability.semanticcache.SemanticCacheEntry;
import com.richard.fyoung.customerwork.tool.backend.entity.KnowledgeDO;
import com.richard.fyoung.customerwork.tool.backend.mapper.KnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 运营闭环后台服务：五个域的读与少量运营动作。
 *
 * <p>刻意只做"读"和"薄薄一层动作"，聚合口径（如 CSAT 的满意率）一律复用 starter 的领域方法
 * （{@link CsatSummary#of}、{@link DeadLetter#reopen}），后台不自己算——两边对同一批数据
 * 给出不同结论时，运营看到的是后台这份，排查时却对着客服端那份，很难发现。</p>
 * @author owlzhangfq@gmail.com
 */
public interface OpsAdminService {

    /**
     * 缓存条目（按命中次数降序）：看清楚缓存了什么、哪些真的在被复用。
     */
    public abstract List<SemanticCacheEntry> listCache(String scopeId, int limit);

    /**
     * 定点删除单条缓存（某条答得不对时不必清空整个分区）。
     */
    public abstract boolean evictCacheEntry(Long id);

    /**
     * 清空分区缓存：知识库或提示词改过之后，旧答案不再可信。
     */
    public abstract int clearCache(String scopeId);

    /**
     * 版本历史（观测时间倒序）。
     */
    public abstract List<PromptVersion> listPromptVersions(int limit);

    /**
     * 按指纹取全文（归因时比对两版差异）。
     */
    public abstract PromptVersion getPromptVersion(String fingerprint);

    /**
     * 满意度汇总。
     *
     * <p>口径复用 {@link CsatSummary#of}：满意按 4 分及以上算（行业标准），不是平均分——
     * 平均分会被大量 3 分拉成一个看着还行的数字，掩盖真正不满的那批人。</p>
     */
    public abstract CsatSummary csatSummary(String scopeId, long startMs, long endMs);

    /**
     * 窗口内的原始调查记录（看低分留言）。
     */
    public abstract List<CsatSurvey> csatSurveys(String scopeId, long startMs, long endMs);

    /**
     * 盲区排行：反复查不到的问题，越靠前越该优先补。
     */
    public abstract List<KnowledgeGap> topKnowledgeGaps(String scopeId, int limit);

    /**
     * 从盲区一键补知识：直接往客服端库的 FAQ 表插一条。
     *
     * <p>标题正文由运营填而非拿盲区原问题照抄——用户的提问是口语化的，直接入库会污染检索质量。</p>
     *
     * @return 新建的知识条目 ID
     */
    public abstract Long fillKnowledgeGap(String title, String content, String keyword, String questionHash);

    /**
     * 按状态列出死信（待重投 / 已放弃）。
     */
    public abstract List<DeadLetter> listDeadLetters(DeadLetterStatus status, int limit);

    /**
     * 各状态计数（角标）。
     */
    public abstract long countDeadLetters(DeadLetterStatus status);

    /**
     * 人工重开一条已放弃的死信（运营确认下游恢复后触发）。
     *
     * <p>重开会清零重试次数——{@link DeadLetter#reopen} 里的逻辑，否则刚放回去就又立刻耗尽。
     * 真正的重投由客服端的巡检器执行，后台只是把它放回队列。</p>
     */
    public abstract DeadLetter reopenDeadLetter(String id);
}
