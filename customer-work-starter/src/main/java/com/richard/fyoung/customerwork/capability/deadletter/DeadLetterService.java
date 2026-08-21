package com.richard.fyoung.customerwork.capability.deadletter;

import com.richard.fyoung.customerwork.infra.config.properties.DeadLetterProperties;
import com.richard.fyoung.customerwork.safety.tenant.CrossTenantOperations;
import com.richard.fyoung.customerwork.safety.tenant.TenantContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 死信队列服务：让"失败了就记条 error"变成"失败了会自己补回来"。
 *
 * <p>此前工具调用失败、主动通知发送失败都只落一行日志。业务量小时看不出来，量一上来就是丢单——
 * 用户以为退款申请提交了，下游其实根本没收到，而没有任何机制会发现这件事。</p>
 *
 * <p>重投按类型分发给 {@link DeadLetterHandler}：队列只有载荷，不可能知道怎么重做一次工具调用。
 * 没注册处理器的类型直接跳过并记 error，而不是反复空转——那会让队列看起来在工作、实际什么都没做，
 * 比不重投更危险。</p>
 * @author owlzhangfq@gmail.com
 */
public interface DeadLetterService {

    /**
     * 登记一条死信（旁路，永不抛出）。
     *
     * <p>调用方通常正处在一个 catch 块里——它自己就是在处理失败，此处再抛异常只会盖掉原始错误。</p>
     *
     * @param type    死信类型，须与某个 {@link DeadLetterHandler#type()} 对应
     * @param payload 重投所需的完整载荷（JSON），必须自包含
     * @param bizKey  关联业务标识（订单号/会话号），供运营检索
     */
    public abstract Optional<DeadLetter> record(String type, String payload, String bizKey, String error);

    /**
     * 跑一轮重投：取到期的死信逐条重做。
     *
     * @return 本轮成功重投的条数
     */
    public abstract int retryDue();

    /**
     * 待重投 / 已放弃列表（运营用）。
     */
    public abstract List<DeadLetter> list(DeadLetterStatus status, int limit);

    /**
     * 按状态计数。
     */
    public abstract long count(DeadLetterStatus status);

    /**
     * 人工重开一条已放弃的死信（运营确认下游恢复后触发）。
     *
     * @throws IllegalStateException 死信不存在时
     */
    public abstract DeadLetter reopen(String id);
}
