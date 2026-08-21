package com.richard.fyoung.customeradmin.system.role.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.system.permission.entity.SysPermission;
import com.richard.fyoung.customeradmin.system.permission.mapper.SysPermissionMapper;
import com.richard.fyoung.customeradmin.system.role.dto.RoleSaveRequest;
import com.richard.fyoung.customeradmin.system.role.dto.RoleVO;
import com.richard.fyoung.customeradmin.system.role.entity.SysRole;
import com.richard.fyoung.customeradmin.system.role.entity.SysRolePermission;
import com.richard.fyoung.customeradmin.system.role.mapper.SysRoleMapper;
import com.richard.fyoung.customeradmin.system.role.mapper.SysRolePermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色/权限分配管理。
 *
 * <p>{@code role_code=super_admin} 不可编辑/删除（防止误操作导致系统失去管理入口），
 * 其权限点在读取时合成为全量权限 ID（实际不落 {@code sys_role_permission}，
 * 见 {@link com.richard.fyoung.customeradmin.config.AdminStpInterfaceImpl} 的特判）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface RoleService {

    public abstract PageResult<RoleVO> page(PageQuery query);

    public abstract RoleVO get(Long id);

    public abstract void create(RoleSaveRequest request);

    public abstract void update(Long id, RoleSaveRequest request);

    public abstract void delete(Long id);
}
