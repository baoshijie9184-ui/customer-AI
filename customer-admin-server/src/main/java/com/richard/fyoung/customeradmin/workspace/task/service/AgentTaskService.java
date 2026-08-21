package com.richard.fyoung.customeradmin.workspace.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workspace.task.dto.AgentTaskPageQuery;
import com.richard.fyoung.customeradmin.workspace.task.dto.AgentTaskVO;
import com.richard.fyoung.customeradmin.workspace.task.entity.AiAgentTask;
import com.richard.fyoung.customeradmin.workspace.task.mapper.AiAgentTaskMapper;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.subagent.task.TaskRepository;
import io.agentscope.harness.agent.subagent.task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.Duration;

/**
 * 后台委派任务的管理台服务：列表 / 详情 / 取消。
 *
 * <p>不提供"新建任务"——任务由智能体在 ReAct 循环里通过 {@code agent_spawn} 自行派发，
 * 管理台是这批任务的观察与干预端，不是发起端。手工造一条任务记录并不会让任何东西真的跑起来。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AgentTaskService {

    public abstract IPage<AgentTaskVO> page(AgentTaskPageQuery query);

    /**
     * 任务详情：结果与错误信息取全文。
     */
    public abstract AgentTaskVO get(String taskId);

    /**
     * 取消任务：委托 {@link TaskRepository#cancelTask}，由它同时落取消标志与中断执行线程。
     *
     * <p>已是终态的任务也照常放行（返回后前端刷新即可看到实际状态）——取消是幂等操作，
     * 为"刚好在点击瞬间跑完"这种正常竞态报错没有意义。</p>
     */
    public abstract void cancel(String taskId);

    /**
     * 状态枚举取值，供前端下拉与参数校验对齐（避免前后端各写一份字符串常量）。
     */
    public static String[] statusOptions() {
        TaskStatus[] values = TaskStatus.values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }
}
