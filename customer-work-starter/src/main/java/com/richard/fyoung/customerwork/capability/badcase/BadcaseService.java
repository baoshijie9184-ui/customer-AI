package com.richard.fyoung.customerwork.capability.badcase;

import com.richard.fyoung.customerwork.capability.eval.EvalCaseSource;
import com.richard.fyoung.customerwork.capability.eval.EvalCaseStore;
import com.richard.fyoung.customerwork.capability.eval.EvalType;
import com.richard.fyoung.customerwork.capability.eval.PersistedEvalCase;
import com.richard.fyoung.customerwork.data.chatlog.ChatMessage;
import com.richard.fyoung.customerwork.data.chatlog.ChatMessageStore;
import com.richard.fyoung.customerwork.data.ticket.TicketActorType;
import com.richard.fyoung.customerwork.tool.backend.entity.KnowledgeDO;
import com.richard.fyoung.customerwork.tool.backend.mapper.KnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * badcase 回流服务——把"记录下来的失败"变成"改进过的系统"。
 *
 * <p>此前负反馈与质检失败只写进 {@code cw_fact_log} 就结束了，{@code FeedbackService} 的注释里
 * 明确写着"诚实边界：只记录，不自动回流知识库"。缺的就是本类：一条 badcase 从待筛选出发，
 * 走到两个出口——<b>补知识库</b>（下次能答对）和<b>加评测用例</b>（下次答错立刻被发现）。
 * 这两个出口不互斥，一条值得处理的 badcase 通常两件事都该做。</p>
 *
 * <p><b>刻意不做自动回流</b>：模型答错的原因千差万别（知识缺失、检索没召回、话术不当、用户表述歧义），
 * 自动把用户的负反馈灌进知识库，等于让最不满的那批用户直接改写知识——这是投毒面。
 * 人工筛选这一步是必要的，本类要做的是把这一步的成本降到"看两眼点一下"。</p>
 *
 * <p>记录动作（{@link #record}）是旁路：失败只记日志，绝不阻断用户提交反馈或质检流程；
 * 而采纳/忽略是运营的主动操作，失败必须抛出——静默失败会让人以为处理过了，同一条被反复翻出来。</p>
 *
 * <p>两个协作者<b>可空</b>而不是用 {@code ObjectProvider}：聊天留痕与知识库后端都是可选能力
 * （各自的 store-mode 决定装没装），而本类还要能在后台侧用跨库 Mapper 直接组装出来——
 * 那里没有 Spring 容器可供惰性查找。可空字段让两种装配路径共用同一个构造器。</p>
 * @author owlzhangfq@gmail.com
 */
public interface BadcaseService {

    /**
     * 记录一条 badcase（旁路，失败不阻断主链路）。
     *
     * <p>会尝试从聊天留痕回查"用户问了什么、AI 答了什么"：只给运营一个 messageId，
     * 筛选界面就没法用——没人能凭一串 ID 判断该不该回流。</p>
     *
     * @return 记录成功返回 badcase，失败返回空
     */
    public abstract Optional<Badcase> record(BadcaseSource source, String sessionId, String messageId, String detail);

    /**
     * 按条件查询待筛选队列。
     */
    public abstract List<Badcase> query(BadcaseQuery query);

    /**
     * 按条件计数（分页总数与"待筛 N 条"角标共用）。
     */
    public abstract long count(BadcaseStatus status, BadcaseSource source);

    /**
     * 按 ID 查一条。
     */
    public abstract Optional<Badcase> find(String badcaseId);

    /**
     * 采纳为知识库条目：把答错的那块知识补上。
     *
     * <p>标题、内容、关键词由运营填写而非从原文照抄——用户的原始提问是口语化的，
     * 直接当知识条目会污染检索质量。</p>
     *
     * @throws IllegalStateException 知识库未走 jdbc、badcase 不存在或已采纳过时
     */
    public abstract Badcase adoptAsKnowledge(String badcaseId, String title, String content, String keyword, String operator);

    /**
     * 采纳为评测用例：把这次翻车固化成回归防护。
     *
     * @param caseId   用例编号（运营指定，同类型内唯一）
     * @param evalType 归入哪类评测
     * @param expected INTENT 传期望意图（空表示期望快车道不命中）；QUALITY 传期望要点
     * @throws IllegalStateException badcase 不存在、已采纳过，或用例编号已被占用时
     */
    public abstract Badcase adoptAsEvalCase(String badcaseId, String caseId, EvalType evalType, String expected, String category, String operator);

    /**
     * 忽略：噪声反馈或质检误报。
     */
    public abstract Badcase ignore(String badcaseId, String reason, String operator);
}
