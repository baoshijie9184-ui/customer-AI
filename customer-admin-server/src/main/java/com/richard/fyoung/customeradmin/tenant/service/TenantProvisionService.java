package com.richard.fyoung.customeradmin.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.system.permission.entity.SysPermission;
import com.richard.fyoung.customeradmin.system.permission.mapper.SysPermissionMapper;
import com.richard.fyoung.customeradmin.system.role.entity.SysRole;
import com.richard.fyoung.customeradmin.system.role.entity.SysRolePermission;
import com.richard.fyoung.customeradmin.system.role.mapper.SysRoleMapper;
import com.richard.fyoung.customeradmin.system.role.mapper.SysRolePermissionMapper;
import com.richard.fyoung.customerwork.safety.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.util.List;

/**
 * 新租户开通：建一个租户内的全权管理员角色，并授予除平台专属之外的全部权限点。
 *
 * <p>不做则新租户是个空壳——没有任何角色可分配，租户管理员建了也用不了。</p>
 *
 * <p><b>为什么是"新建角色 + 授权限"而不是"复制平台角色"</b>：平台侧的角色是按运营方职责划分的
 * （超管、运维、只读等），对租户没有意义；租户要的是"我这边的管理员"。复制反而会把
 * 平台的角色语义连同其权限一起漏给租户。</p>
 * @author owlzhangfq@gmail.com
 */
public interface TenantProvisionService {

    /**
     * 为指定租户初始化内建角色。
     *
     * <p>整段跑在目标租户的上下文里，插入 {@code sys_role} / {@code sys_role_permission} 时
     * 由租户拦截器自动补 {@code tenant_id}——这样就不需要在实体上挂租户字段、也不会写错归属。</p>
     */
    public abstract void provision(String tenantCode);

    /**
     * 租户内建管理员角色编码。
     */
    public static final String TENANT_ADMIN_ROLE_CODE = "tenant_admin";
}
