package com.richard.fyoung.customerwork.capability.semanticcache;

import com.richard.fyoung.customerwork.core.agent.MultiAgentOrchestrator;
import com.richard.fyoung.customerwork.core.support.TenantResolver;
import com.richard.fyoung.customerwork.data.knowledge.VectorMath;
import com.richard.fyoung.customerwork.data.knowledge.embedding.EmbeddingClient;
import com.richard.fyoung.customerwork.infra.config.properties.SemanticCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 语义缓存：问题向量相似度超过阈值就直接返回上次的答案，省掉一次完整的模型调用。
 *
 * <p>客服场景问题重复率极高（"怎么退货"一天可能被问几百次），此前每一次都完整打一遍模型。
 * 命中判定用向量相似度而非字符串相等——"怎么退货"和"退货流程是什么"是同一个问题，
 * 按字面比对永远命中不了。</p>
 *
 * <h3>为什么默认关闭，以及为什么必须有白名单</h3>
 *
 * <p>在客服场景无差别缓存是<b>会出数据泄露事故</b>的：两个用户都问"我的订单到哪了"，
 * 字面与语义都高度相似，但正确答案完全不同——把 A 的物流信息返回给 B，这不是体验问题而是事故。
 * 因此本类只缓存<b>与个人上下文无关的通用问答</b>，判定收口在 {@link #cacheable} 一处：</p>
 *
 * <ol>
 *   <li><b>意图白名单</b>：默认只放行 {@code consult}（政策咨询类）。查订单、退款这类天然带个人数据的意图一律不缓存；</li>
 *   <li><b>个人标识过滤</b>：问题或答案里出现 6 位以上连续数字（订单号 / 手机号 / 单据号）即跳过——
 *       这类问答必然是针对某个人的；</li>
 *   <li><b>双层隔离</b>：表上的 {@code tenant_id} 由租户拦截器自动改写（跨租户不可见），
 *       条目上的 {@code scopeId} 再按业务分区隔一层——不同租户的政策口径本就不同。</li>
 * </ol>
 *
 * <p>过滤在问题侧与答案侧各做一次：意图分类可能判错，而答案里带单号是"这条回答依赖个人数据"
 * 的直接证据，比意图更可信。</p>
 * @author owlzhangfq@gmail.com
 */
public interface SemanticCacheService {

    /**
     * 查缓存。
     *
     * <p>任何一步失败都返回空（视为未命中）而不是抛出：缓存是加速手段，
     * 它的故障不该让用户问不了问题。</p>
     *
     * @return 命中则返回上次的答案
     */
    public abstract Optional<String> lookup(String sessionId, String question);

    /**
     * 写缓存。
     *
     * <p>同样吞掉异常：写缓存失败只是下次还得再问一遍模型，不该影响本次已经答好的回复。</p>
     */
    public abstract void put(String sessionId, String question, String answer);

    /**
     * 清空某分区缓存：知识库或提示词改过之后，旧答案不再可信。
     */
    public abstract int invalidate(String scopeId);

    /**
     * 运营视角列出条目（按命中次数降序）：看清楚到底缓存了什么、哪些真的在被复用。
     */
    public abstract List<SemanticCacheEntry> list(String scopeId, int limit);

    /**
     * 定点删除单条：发现某条答得不对时不必清空整个分区。
     */
    public abstract boolean evict(Long id);
}
