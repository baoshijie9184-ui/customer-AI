package com.richard.fyoung.customeradmin.system.menu.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.system.menu.dto.MenuChangeLogVO;
import com.richard.fyoung.customeradmin.system.menu.entity.SysMenuChangeLog;
import com.richard.fyoung.customeradmin.system.menu.mapper.SysMenuChangeLogMapper;
import com.richard.fyoung.customeradmin.system.permission.entity.SysPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * 菜单变更审计流水：{@link com.richard.fyoung.customeradmin.system.permission.service.PermissionService}
 * 每次增/删/改/拖拽移动菜单节点后调用本服务落一条流水，供"菜单管理"页面排查"谁什么时候改了什么"。
 * 只做流水记录与分页查询，不做整树快照、不支持一键回滚（见需求确认）。
 * @author owlzhangfq@gmail.com
 */
public interface MenuChangeLogService {

    /**
     * 记录一条变更流水；序列化/落库失败不影响主流程（审计是辅助能力，不能拖垮菜单编辑本身）。
     */
    public abstract void record(Long menuId, String action, SysPermission before, SysPermission after);

    /**
     * 按 menuId 过滤（不传则查全量），按时间倒序分页。
     */
    public abstract PageResult<MenuChangeLogVO> page(Long menuId, PageQuery query);
}
