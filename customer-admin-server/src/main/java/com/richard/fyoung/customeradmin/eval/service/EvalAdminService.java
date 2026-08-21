package com.richard.fyoung.customeradmin.eval.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.eval.config.EvalGatewayProvider;
import com.richard.fyoung.customeradmin.eval.config.EvalTriggerClient;
import com.richard.fyoung.customerwork.capability.eval.EvalComparison;
import com.richard.fyoung.customerwork.capability.eval.EvalRun;
import com.richard.fyoung.customerwork.capability.eval.EvalRunStore;
import com.richard.fyoung.customerwork.capability.eval.EvalType;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * 评测后台服务：读客服端库的运行记录、触发新一轮评测。
 *
 * <p>本类刻意只有"读"和"转发触发"两件事，不含任何评分或对比算法——那些都在 starter 的
 * {@link EvalComparison} 与 {@code EvalService} 里。后台再实现一遍等价逻辑，两边迟早对同一批数据
 * 给出不同结论，而运营看到的是后台这一份，排查时却对着客服端那一份，很难发现。</p>
 * @author owlzhangfq@gmail.com
 */
public interface EvalAdminService {

    /**
     * 某类型最近若干次运行（时间倒序），用于趋势线与列表。
     */
    public abstract List<EvalRun> recent(EvalType type, int limit);

    /**
     * 单次运行详情（含失败明细与完整原始指标）。
     */
    public abstract EvalRun detail(String runId);

    /**
     * 某次运行与它上一版的对比。
     *
     * <p>对比逻辑直接复用 starter 的 {@link EvalComparison#of}，后台不重算。</p>
     */
    public abstract EvalComparison comparison(String runId);

    /**
     * 触发一次评测（转发到客服端执行），返回本次与上一版的对比。
     */
    public abstract EvalComparison trigger(EvalType type, String remark);
}
