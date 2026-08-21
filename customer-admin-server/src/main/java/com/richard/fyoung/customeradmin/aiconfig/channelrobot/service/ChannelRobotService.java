package com.richard.fyoung.customeradmin.aiconfig.channelrobot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.ChannelType;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.SessionMode;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.dto.ChannelRobotSaveRequest;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.dto.ChannelRobotVO;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.entity.AiChannelRobot;
import com.richard.fyoung.customeradmin.aiconfig.channelrobot.mapper.AiChannelRobotMapper;
import com.richard.fyoung.customeradmin.common.crypto.AesGcmCryptoUtil;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 渠道机器人 CRUD：AppSecret 加密落库/留空不改、channelType 枚举校验、agentCode 必须绑定到启用的
 * 智能体（fast fail）。列表 VO 不回密文，仅回 {@code hasSecret} 布尔。
 *
 * <p>密码/密钥处理沿用 {@code WorkbenchSiteService} 手法（AES-GCM 密文入库，编辑留空复用原密文）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ChannelRobotService {

    /**
     * 分页查询：channelType 精确筛选 + keyword 模糊匹配 robotName/appKey/agentCode，按创建时间倒序。
     */
    public abstract IPage<ChannelRobotVO> page(long current, long size, String channelType, String keyword);

    public abstract void create(ChannelRobotSaveRequest request);

    public abstract void update(Long id, ChannelRobotSaveRequest request);

    public abstract void delete(Long id);
}
