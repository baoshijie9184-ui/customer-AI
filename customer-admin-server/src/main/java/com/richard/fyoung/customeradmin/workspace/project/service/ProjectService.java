package com.richard.fyoung.customeradmin.workspace.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatSessionSummary;
import com.richard.fyoung.customeradmin.workspace.chat.service.ChatHistoryService;
import com.richard.fyoung.customeradmin.workspace.project.dto.AddSessionRequest;
import com.richard.fyoung.customeradmin.workspace.project.dto.ProjectSaveRequest;
import com.richard.fyoung.customeradmin.workspace.project.dto.ProjectSessionVO;
import com.richard.fyoung.customeradmin.workspace.project.dto.ProjectVO;
import com.richard.fyoung.customeradmin.workspace.project.entity.AiProject;
import com.richard.fyoung.customeradmin.workspace.project.entity.AiProjectSession;
import com.richard.fyoung.customeradmin.workspace.project.mapper.AiProjectMapper;
import com.richard.fyoung.customeradmin.workspace.project.mapper.AiProjectSessionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Projects：跨智能体的会话分组管理。
 *
 * <p>权限点复用 {@code workspace}（跟一级菜单"智能体工作区"同一使用场景），不区分"谁能管理项目"与
 * "谁能把会话加进项目"——项目管理不是需要精细化 RBAC 收紧的敏感操作，见 V11 迁移注释。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ProjectService {

    /**
     * 项目数量通常不大（跟角色/智能体一个量级），列表 + 挑选器共用同一个不分页接口，简单直接。
     */
    public abstract List<ProjectVO> list(String keyword);

    public abstract void create(ProjectSaveRequest request);

    public abstract void update(Long id, ProjectSaveRequest request);

    /**
     * 删除项目连带清掉关联的会话条目——项目-会话关联"属于"项目本身，不是独立有价值的数据，级联删不用二次确认。
     */
    public abstract void delete(Long id);

    /**
     * 项目详情：逐条把关联会话解析成"预览+所属智能体+时间"，会话已经查不到内容的标 stale，不抛错、不中断整个列表。
     */
    public abstract List<ProjectSessionVO> listSessions(Long id);

    /**
     * 幂等：同一会话重复加入同一项目不报错，直接当已经在项目里处理（前端不用先查一遍再决定按钮态）。
     */
    public abstract void addSession(Long id, AddSessionRequest request);

    public abstract void removeSession(Long id, String agentCode, String sessionId);
}
