package com.richard.fyoung.customerwork.infra.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 主动服务：订单状态主动通知 / 满意度回访 / 营销触达。
 *
 * <p>消息文案在此组装，推送委托给可替换的 {@link NotificationChannel}（默认日志，生产复用飞书/钉钉等
 * Channel 推送）。由订单状态变更事件、定时任务或运营后台触发。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ProactiveNotificationService {

    /**
     * 订单状态主动通知（已发货 / 已签收等）。
     */
    public abstract Mono<Void> notifyOrderStatus(String orderId, String status, String target);

    /**
     * 满意度回访。
     */
    public abstract Mono<Void> sendSatisfactionSurvey(String orderId, String target);
}
