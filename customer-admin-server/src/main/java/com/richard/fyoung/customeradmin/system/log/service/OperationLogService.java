package com.richard.fyoung.customeradmin.system.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.system.log.entity.SysOperationLog;
import com.richard.fyoung.customeradmin.system.log.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作日志查询（只读，无增删改——需求文档只要求记录与查看）。
 * @author owlzhangfq@gmail.com
 */
public interface OperationLogService {

    /**
     * {@code PageQuery.keyword} 匹配 username；{@code PageQuery.status} 复用为 result 过滤（1成功/0失败）。
     */
    public abstract PageResult<SysOperationLog> page(PageQuery query);
}
