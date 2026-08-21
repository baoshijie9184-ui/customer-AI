package com.richard.fyoung.customeradmin.sqlconfig.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.common.crypto.AesGcmCryptoUtil;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.Result;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlDatasourceSaveRequest;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlDatasourceVO;
import com.richard.fyoung.customeradmin.sqlconfig.engine.SqlDatasourceConnectionManager;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlDatasource;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlDefine;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlDatasourceMapper;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlDefineMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * SQL 配置数据源管理：CRUD + 密码加密存储/留空不改/脱敏回显 + 连通性测试 + 连接池失效。
 *
 * <p>密码处理沿用 {@code ModelConfigService} 的 AppKey 手法（AES/GCM 密文入库，VO 回显
 * {@code mask(decrypt(...))}）。数据源编辑/删除/禁用后使 {@link SqlDatasourceConnectionManager}
 * 的缓存池失效，避免继续用旧连接信息。</p>
 * @author owlzhangfq@gmail.com
 */
public interface SqlDatasourceService {

    public abstract PageResult<SqlDatasourceVO> page(PageQuery query);

    /**
     * 仅启用数据源（供 SQL 定义表单下拉选择）。
     */
    public abstract List<SqlDatasourceVO> listEnabled();

    public abstract SqlDatasourceVO get(Long id);

    public abstract void create(SqlDatasourceSaveRequest request);

    public abstract void update(Long id, SqlDatasourceSaveRequest request);

    public abstract void delete(Long id);

    /**
     * 连通性测试：解密密码后派发到独立线程池直连执行 SELECT 1，硬性超时兜底，不占用请求线程。
     * 只读探测不新增权限点（复用 view，与项目惯例一致），也不改动配置。
     */
    public abstract CompletableFuture<Result<Void>> testConnection(Long id);
}
