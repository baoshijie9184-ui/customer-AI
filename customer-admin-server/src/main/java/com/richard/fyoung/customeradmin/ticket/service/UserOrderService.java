package com.richard.fyoung.customeradmin.ticket.service;

import com.richard.fyoung.customeradmin.ticket.client.CustomerWorkTicketClient;
import com.richard.fyoung.customeradmin.ticket.dto.OrderDetailVO;
import com.richard.fyoung.customeradmin.ticket.dto.OrderPageQuery;
import com.richard.fyoung.customeradmin.ticket.dto.OrderPageResult;
import org.springframework.stereotype.Service;

/**
 * 用户订单服务：坐席对订单的查询/改址/取消全部薄中转到 {@link CustomerWorkTicketClient}
 * （订单数据在 8080 侧，本模块不建业务表）。
 * @author owlzhangfq@gmail.com
 */
public interface UserOrderService {

    public abstract OrderPageResult page(OrderPageQuery query);

    public abstract OrderDetailVO detail(String orderId);

    public abstract void modifyAddress(String orderId, String newAddress);

    public abstract void cancel(String orderId, String reason);
}
