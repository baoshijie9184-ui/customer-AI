package com.richard.fyoung.customerwork.capability.feedback;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customerwork.capability.badcase.BadcaseService;
import com.richard.fyoung.customerwork.capability.badcase.BadcaseSource;
import com.richard.fyoung.customerwork.core.memory.FactLog;
import com.richard.fyoung.customerwork.core.support.TenantResolver;
import com.richard.fyoung.customerwork.infra.config.CustomerWorkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户反馈服务（消息级点赞/点踩闭环，数据飞轮的另一条输入通道）。
 *
 * <p>与 {@link com.richard.fyoung.customerwork.capability.quality.QualityFeedbackRecorder}
 * （系统主动质检、命中规则判不通过）互补——本类是<b>用户主动表达</b>的负反馈信号，同样沉淀到
 * {@link FactLog}（三层记忆体系 L3：只追加、可审计），供离线复盘 / 后续人工筛选后回流知识库或
 * 补充评测集用例。</p>
 *
 * <p><b>两条去向，各司其职</b>：负反馈既写入 {@link FactLog}（L3 审计流水，只追加、永不改写，
 * 回答"当时发生了什么"），也登记进 {@link BadcaseService} 的待筛队列（有状态的运营工作流，
 * 回答"我们拿它做了什么"）。把处理状态塞进审计流水会破坏后者只追加的根本约定，
 * 故两者并存而非二选一。</p>
 *
 * <p><b>诚实边界</b>：不做自动回流——模型答错的原因千差万别（知识缺失、检索没召回、话术不当、
 * 用户表述歧义），把最不满那批用户的反馈直接灌进知识库是一个投毒面。筛选那一步由人做，
 * 本类负责让待筛队列里有东西可筛。</p>
 * @author owlzhangfq@gmail.com
 */
public interface FeedbackService {

    /**
     * 提交一条消息级反馈；同一 messageId 重复提交按最新一次覆盖。DOWN 类型额外沉淀事实供飞轮复盘。
     */
    public abstract MessageFeedback submit(String sessionId, String messageId, FeedbackType type, String comment);

    public abstract Optional<MessageFeedback> find(String messageId);

    public abstract List<MessageFeedback> findBySession(String sessionId);
}
