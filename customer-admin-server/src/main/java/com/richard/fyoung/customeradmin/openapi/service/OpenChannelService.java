package com.richard.fyoung.customeradmin.openapi.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.entity.AiChannelRobot;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.entity.AiChannelSession;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.mapper.AiChannelRobotMapper;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.mapper.AiChannelSessionMapper;
import com.richard.fyoung.customeradmin.common.crypto.AesGcmCryptoUtil;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.openapi.dto.OpenChannelRobotVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 开放 API 渠道能力：机器人配置下发（含解密明文 + 版本号）、外部用户会话解析/重置、对话前的授权校验。
 *
 * <p>会话解析对同一外部用户复用同一 {@code sessionId} 保持多轮上下文；重置生成新 sessionId 开启新会话。
 * 并发首解析可能撞 (channelType, appKey, externalUserId) 唯一键，捕获 {@link DuplicateKeyException}
 * 后回查已存在记录返回，保证幂等（防御式编程单点收敛在此）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface OpenChannelService {

    /**
     * 返回启用状态的渠道机器人（可选按 channelType 过滤），携带解密明文 appSecret 与版本号。
     */
    public abstract List<OpenChannelRobotVO> listRobots(String channelType);

    /**
     * 解析外部用户对应的会话：已存在则复用，不存在则创建（sessionId = ch-<uuid>）。
     */
    public abstract String resolveSession(String channelType, String appKey, String externalUserId);

    /**
     * 重置外部用户会话：已存在则生成新 sessionId 覆盖，不存在则等同 resolve（创建）。
     */
    public abstract String resetSession(String channelType, String appKey, String externalUserId);

    /**
     * 对话前授权校验：agentCode 必须有启用的渠道机器人绑定，否则拒绝
     * （防止开放 API 借 agentCode 任意调用未授权智能体）。
     */
    public abstract void requireAgentBound(String agentCode);
}
