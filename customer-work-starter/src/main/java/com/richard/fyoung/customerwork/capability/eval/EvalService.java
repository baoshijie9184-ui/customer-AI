package com.richard.fyoung.customerwork.capability.eval;

import com.richard.fyoung.customerwork.capability.prompt.PromptVersionTracker;
import com.richard.fyoung.customerwork.core.service.CustomerServiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 评测编排服务——把"跑一遍标准集 → 出报告 → 对比上一版"串成一次调用。
 *
 * <p>此前 {@link IntentEvalRunner}/{@link QualityEvalRunner} 只能算出一份内存里的报告，没有调用方、
 * 没有落库、没有纵向对比：等于体温计造好了放在抽屉里。本类补上缺的那一段——每次运行落一条
 * {@link EvalRun}，并自动与上一次同类型运行比出<b>指标变化</b>和<b>回归用例</b>，
 * 让改提示词 / 换模型这类动作有据可依，而不是靠人肉体感。</p>
 *
 * <p><b>两类评测的代价不同</b>：意图评测纯离线（不调模型，可进 CI 门禁、可定时跑）；
 * 质量评测要逐条调 Agent 生成回复再调 Judge 打分，一次运行有实打实的 token 成本，
 * 故只在显式触发时跑，且缺少 {@link JudgeModel} 装配时直接 fail fast 说明原因，
 * 而不是返回一份全是中性分的假报告。</p>
 * @author owlzhangfq@gmail.com
 */
public interface EvalService {

    /**
     * 跑意图评测标准集，落库并与上一版对比。
     *
     * <p>离线确定性：不调模型、无外部依赖，适合定时跑与 CI 门禁。</p>
     */
    public abstract EvalComparison runIntent(EvalTrigger trigger, String remark);

    /**
     * 跑质量评测标准集（LLM-as-Judge），落库并与上一版对比。
     *
     * <p>每个用例用<b>独立会话</b>生成回复：共用一个会话会让前一条用例的对话历史进入后一条的上下文，
     * 评测结果随用例顺序变化而不可复现。</p>
     *
     * @throws IllegalStateException 未装配 {@link JudgeModel} 或主链路服务不可用时
     */
    public abstract EvalComparison runQuality(EvalTrigger trigger, String remark);

    /**
     * 某类型最近若干次运行（时间倒序）。
     */
    public abstract List<EvalRun> recent(EvalType type, int limit);

    /**
     * 按运行 ID 查一次运行。
     */
    public abstract Optional<EvalRun> find(String runId);

    /**
     * 取某次运行与它上一版的对比（历史回看用）。
     *
     * @return 运行不存在时返回空
     */
    public abstract Optional<EvalComparison> compareWithBaseline(String runId);
}
