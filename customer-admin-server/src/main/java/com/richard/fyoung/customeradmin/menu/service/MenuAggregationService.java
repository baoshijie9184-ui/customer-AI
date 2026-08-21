package com.richard.fyoung.customeradmin.menu.service;

import cn.dev33.satoken.stp.StpUtil;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.agent.service.AgentService;
import com.richard.fyoung.customeradmin.menu.dto.MenuNode;
import com.richard.fyoung.customeradmin.system.permission.entity.SysPermission;
import com.richard.fyoung.customeradmin.system.permission.mapper.SysPermissionMapper;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 动态菜单聚合：合并"静态菜单"（{@code sys_permission} type=1，按当前用户权限点过滤）与
 * "动态菜单"（各启用中智能体入口，运行时拼进 {@code workspace} 节点、不落库——智能体增删启停
 * 立即反映，无额外一致性同步逻辑）。
 * @author owlzhangfq@gmail.com
 */
public interface MenuAggregationService {

    /**
     * 当前登录用户可见的菜单树：静态菜单按权限点过滤 + workspace 节点下挂启用中的智能体动态节点。
     */
    public abstract List<MenuNode> buildMenuTree();
}
