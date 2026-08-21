package com.richard.fyoung.customeradmin.aiconfig.channel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.aiconfig.channel.dto.ChannelBindingSaveRequest;
import com.richard.fyoung.customeradmin.aiconfig.channel.dto.ChannelBindingVO;
import com.richard.fyoung.customeradmin.aiconfig.channel.entity.AiChannelBinding;
import com.richard.fyoung.customeradmin.aiconfig.channel.mapper.AiChannelBindingMapper;
import com.richard.fyoung.customeradmin.aiconfig.channel.publish.CustomerWorkConfigPublisher;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 渠道-客服机器人运行配置绑定管理：CRUD + 手动重新发布。
 *
 * <p>绑定/编辑成功后若发布能力已启用，自动触发一次发布，把当前配置下发到 8080；
 * 「重新发布」按钮走 {@link #republish}（强制连通性探测，失败抛业务异常给前端明确反馈）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ChannelBindingService {

    /**
     * 全量列表（行数很少，不分页）：按创建时间倒序，回填智能体名称。
     */
    public abstract List<ChannelBindingVO> list();

    /**
     * 新建绑定：channelCode 唯一、agentId 必须存在；成功后触发一次发布。
     */
    public abstract void create(ChannelBindingSaveRequest request);

    /**
     * 编辑绑定：改 channelCode / agentId / 状态（channelCode 排除自身查重）；成功后触发一次发布。
     */
    public abstract void update(Long id, ChannelBindingSaveRequest request);

    public abstract void delete(Long id);

    /**
     * 手动重新发布：发布能力未启用直接拒绝；探测不过/发布失败抛业务错误码。
     */
    public abstract String republish(String channelCode);
}
