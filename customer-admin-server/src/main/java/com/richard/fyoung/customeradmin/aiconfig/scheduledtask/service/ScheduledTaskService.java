package com.richard.fyoung.customeradmin.aiconfig.scheduledtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.dto.ScheduledTaskPageQuery;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.dto.ScheduledTaskRunVO;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.dto.ScheduledTaskSaveRequest;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.dto.ScheduledTaskVO;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.entity.AiScheduledTask;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.entity.AiScheduledTaskRun;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.mapper.AiScheduledTaskMapper;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.mapper.AiScheduledTaskRunMapper;
import com.richard.fyoung.customeradmin.aiconfig.scheduledtask.scheduler.ScheduledTaskChangedEvent;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.config.AdminScheduledTaskProperties;
import com.richard.fyoung.customeradmin.config.AdminSchedulerProperties;
import com.richard.fyoung.customeradmin.workspace.runtime.AdminAgentInstanceFactory;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.harness.agent.HarnessAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务管理 + 执行（对应特性「基于 AgentScope 的定时任务」）。
 *
 * <p>{@link #execute(String, String)} 是唯一的执行入口，手动触发接口与 XXL-JOB JobHandler
 * 都复用它：查任务（不存在或 disabled fast-fail）→ 按 {@code agent_id} 现场组装 Agent
 * （{@link AdminAgentInstanceFactory#build(String)}，不复用启动期缓存实例——语义同
 * {@code McpService} 引用的动态智能体运行时工厂）→ 每次执行分配全新 sessionId，状态互不污染
 * → 同步调用并落执行历史（成功/失败都落，输出截断 8000 字符，不让模型输出把日志表撑爆）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ScheduledTaskService {

    public abstract IPage<ScheduledTaskVO> page(ScheduledTaskPageQuery query);

    public abstract ScheduledTaskVO get(Long id);

    public abstract void create(ScheduledTaskSaveRequest request);

    public abstract void update(Long id, ScheduledTaskSaveRequest request);

    public abstract void delete(Long id);

    public abstract void enable(Long id);

    public abstract void disable(Long id);

    public abstract ScheduledTaskRunVO trigger(Long id);

    public abstract IPage<ScheduledTaskRunVO> runs(Long id, ScheduledTaskPageQuery query);

    /**
     * 定时任务唯一执行入口：手动触发接口与 XXL-JOB JobHandler 都复用本方法。
     *
     * <p>disabled 任务 fast-fail 拒绝（不落执行历史——从未真正尝试执行，跟"执行失败"语义不同）；
     * 一旦进入执行阶段，无论成功失败都落一条历史记录，失败也要留证方便运营排查。</p>
     */
    public abstract AiScheduledTaskRun execute(String taskCode, String triggerType);

    public static final String TRIGGER_TYPE_MANUAL = "MANUAL";

    public static final String TRIGGER_TYPE_XXL_JOB = "XXL_JOB";

    public static final String TRIGGER_TYPE_INTERNAL = "INTERNAL";

    public static final String STATUS_SUCCESS = "SUCCESS";

    public static final String STATUS_FAILED = "FAILED";
}
