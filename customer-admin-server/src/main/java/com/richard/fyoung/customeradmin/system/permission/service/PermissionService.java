package com.richard.fyoung.customeradmin.system.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.menu.service.MenuVersionHolder;
import com.richard.fyoung.customeradmin.system.menu.dto.MenuReorderRequest;
import com.richard.fyoung.customeradmin.system.menu.service.MenuChangeLogService;
import com.richard.fyoung.customeradmin.system.permission.dto.PermissionSaveRequest;
import com.richard.fyoung.customeradmin.system.permission.dto.PermissionVO;
import com.richard.fyoung.customeradmin.system.permission.entity.SysPermission;
import com.richard.fyoung.customeradmin.system.permission.mapper.SysPermissionMapper;
import com.richard.fyoung.customerwork.infra.lock.DistributedLockExecutor;
import com.richard.fyoung.customerwork.infra.lock.LockAcquireTimeoutException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权限/菜单树管理（需求文档"二、菜单规划"静态菜单来源）。
 *
 * <p>菜单管理页面的"改动即时生效，发布=广播通知"模型：{@link #create}/{@link #update}/
 * {@link #delete}/{@link #reorder} 都直接落库、立刻对"重新拉一次树"的人可见；但不会主动
 * {@link MenuVersionHolder#bump()}——真正让其它在线用户前端轮询感知到变化、自动刷新菜单，
 * 要等管理员编辑完一批改动后显式调 {@link #publish()} 广播一次，避免半成品改动中途闪现给
 * 其他在线用户。</p>
 * @author owlzhangfq@gmail.com
 */
public interface PermissionService {

    /**
     * 全量权限树，按 sort 升序。
     */
    public abstract List<PermissionVO> tree();

    public abstract void create(PermissionSaveRequest request);

    public abstract void update(Long id, PermissionSaveRequest request);

    public abstract void delete(Long id);

    /**
     * 拖拽排序入口：先抢 {@link #REORDER_LOCK_KEY} 分布式锁再落库，串行化多个管理员的并发拖拽，
     * 避免后提交的一批更新基于过期的兄弟节点 sort 值覆盖先提交的结果（乱序合并）。拿不到锁直接
     * 转换为 {@link ResultCode#MENU_REORDER_CONFLICT} 业务错误，不排队等待。
     *
     * <p>真正落库经 {@link #self} 转一次自注入代理调用 {@link #applyReorder}——{@code @Transactional}
     * 是 Spring AOP 代理拦截的，本类内部直接 {@code this.applyReorder(...)} 属于自调用会绕过代理导致
     * 事务不生效；同时这个转发顺序也保证了锁释放严格发生在事务提交之后：{@link DistributedLockExecutor#execute}
     * 的 finally 解锁在 {@code action.get()}（即 {@code self.applyReorder(...)} 这次代理调用，事务已随之
     * 提交完毕）返回之后才执行，不会出现"锁已释放但改动还没提交"的窗口期。</p>
     */
    public abstract void reorder(MenuReorderRequest request);

    /**
     * 拖拽排序落库：逐条更新受影响节点的 parentId/sort；只对 parentId 真变化的节点记 MOVE 流水（同层纯调顺序不算移动）。
     */
    public abstract void applyReorder(MenuReorderRequest request);

    /**
     * 广播菜单版本变化，让其它在线用户的前端轮询感知到并自动刷新（"发布"按钮语义）。
     */
    public abstract void publish();
}
