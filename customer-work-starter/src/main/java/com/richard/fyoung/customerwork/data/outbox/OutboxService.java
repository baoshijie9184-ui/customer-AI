package com.richard.fyoung.customerwork.data.outbox;

import com.richard.fyoung.customerwork.infra.config.properties.OutboxProperties;
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
import java.util.UUID;

/**
 * Outbox 发布与投递服务：租约保证多实例不并发处理同一条消息，失败按指数退避。
 */
public interface OutboxService {

    /**
     * 写 Outbox；失败必须抛出，由外层同库事务回滚业务变更。
     */
    public abstract OutboxMessage publish(String type, String aggregateId, String payload);

    /**
     * 执行一轮租约投递。
     */
    public abstract int dispatchDue();

    public abstract long count(OutboxStatus status);
}
