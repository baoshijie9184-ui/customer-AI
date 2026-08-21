package com.richard.fyoung.customeradmin.ticket.service;

import com.richard.fyoung.customeradmin.ticket.client.CustomerWorkTicketClient;
import com.richard.fyoung.customeradmin.ticket.config.CustomerWorkClientProperties;
import com.richard.fyoung.customeradmin.ticket.dto.TicketDetailVO;
import com.richard.fyoung.customeradmin.ticket.dto.TicketMessageVO;
import com.richard.fyoung.customeradmin.ticket.dto.TicketPageQuery;
import com.richard.fyoung.customeradmin.ticket.dto.TicketPageResult;
import com.richard.fyoung.customeradmin.ticket.dto.WsCredentialVO;
import com.richard.fyoung.customerwork.safety.security.AgentAccessCredential;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

/**
 * 用户工单服务：坐席操作全部薄中转到 {@link CustomerWorkTicketClient}（工单数据在 8080 侧，
 * 本模块不建业务表）；WS 接入凭证在本地用 {@link AgentAccessCredential} 现签，不走 8080。
 * @author owlzhangfq@gmail.com
 */
public interface UserTicketService {

    public abstract TicketPageResult page(TicketPageQuery query);

    public abstract TicketDetailVO detail(String id);

    public abstract List<TicketMessageVO> messages(String id, Long beforeId, Integer limit);

    public abstract void claim(String id);

    public abstract void reply(String id, String content);

    public abstract void hold(String id, String reason);

    public abstract void resume(String id);

    public abstract void transfer(String id, String toAgent);

    public abstract void resolve(String id, String note);

    public abstract void close(String id, String reason);

    public abstract void updatePriority(String id, String priority);

    public abstract void updateCategory(String id, String category);

    /**
     * 签发坐席 WS 接入凭证：客服浏览器凭此直连 8080 的 {@code /ws/agent}。
     * 令牌用与 8080 共享的密钥现签，有效期 {@code credentialExpireHours} 小时。
     *
     * @param agentId 当前登录坐席登录名（由 Controller 从 Sa-Token 解析后传入）
     */
    public abstract WsCredentialVO issueWsCredential(String agentId);
}
