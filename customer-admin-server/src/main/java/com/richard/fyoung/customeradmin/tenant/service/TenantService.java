package com.richard.fyoung.customeradmin.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.tenant.dto.TenantPageQuery;
import com.richard.fyoung.customeradmin.tenant.dto.TenantSaveRequest;
import com.richard.fyoung.customeradmin.tenant.dto.TenantVO;
import com.richard.fyoung.customeradmin.tenant.entity.SysTenant;
import com.richard.fyoung.customeradmin.tenant.entity.TenantStatus;
import com.richard.fyoung.customeradmin.tenant.mapper.SysTenantMapper;
import com.richard.fyoung.customerwork.safety.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Set;

/**
 * 租户主数据与生命周期。
 *
 * <p>本服务只管租户这一实体本身；新租户的角色与管理员账号初始化交给
 * {@link TenantProvisionService}——那是"开通"动作，涉及 RBAC 多张表，与租户 CRUD 是两件事。</p>
 * @author owlzhangfq@gmail.com
 */
public interface TenantService {

    public abstract PageResult<TenantVO> page(TenantPageQuery query);

    /**
     * 全部可用租户（运营方切换视角的下拉数据源）。
     */
    public abstract List<TenantVO> listActive();

    public abstract TenantVO get(Long id);

    public abstract Long create(TenantSaveRequest request);

    public abstract void update(TenantSaveRequest request);

    /**
     * 冻结/恢复/退租统一入口：只改状态不动数据。
     */
    public abstract void changeStatus(Long id, TenantStatus target);

    public abstract void delete(Long id);

    /**
     * 登录与接口调用前的租户可用性校验：不存在、已冻结、已退租、已过期一律拒绝。
     *
     * <p>这是"租户级熔断"的唯一落点——运营方冻结一个租户后，该租户的所有登录立刻失效。</p>
     */
    public abstract void assertAccessible(String tenantCode);

    /**
     * 租户编码是否存在且可用（运营方切换视角时校验目标租户）。
     */
    public abstract boolean existsAccessible(String tenantCode);

    public abstract SysTenant findByCode(String tenantCode);
}
