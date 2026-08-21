package com.richard.fyoung.customeradmin.sqlconfig.engine;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.config.AdminSqlConfigProperties;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlQueryMetaVO;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlQueryParamMetaVO;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlQueryResultVO;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlDefine;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlDefineParam;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlFieldTransform;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlDefineMapper;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlDefineParamMapper;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlFieldTransformMapper;
import com.richard.fyoung.customerwork.infra.sqlkit.DefaultValueResolver;
import com.richard.fyoung.customerwork.infra.sqlkit.ParamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通用 SQL 查询执行引擎：按 defineKey 执行已配置的只读 SQL，返回动态列结果。
 *
 * <p>执行流程：查启用的 define → 查参数元数据 → 参数处理（必填校验/类型转换/默认值表达式解析/
 * 分页参数换算 offset）→ 取只读连接模板并设查询超时与最大行数兜底 → 有 count_sql 先查总数 →
 * query_sql 从 {@code ResultSetMetaData} 按列序取列名（getColumnLabel 拿 AS 别名）→ 应用列转换器
 * → 组装结果（含耗时）。执行异常记错误码日志后转 {@link BizException}，不把堆栈泄露给前端。</p>
 * @author owlzhangfq@gmail.com
 */
public interface SqlQueryService {

    /**
     * 通用查询页元数据：描述 + 是否自动执行 + 是否有总数 SQL + 参数表单元数据（默认值/下拉已解析）。
     */
    public abstract SqlQueryMetaVO meta(String defineKey);

    /**
     * 通用查询（分页）。
     */
    public abstract SqlQueryResultVO execute(String defineKey, Map<String, Object> params);

    /**
     * 导出（把分页页码/页大小强制换算为 offset=0 / pageSize=maxRows，导出上限即 maxRows）。
     */
    public abstract SqlQueryResultVO executeForExport(String defineKey, Map<String, Object> params);

    /**
     * 即席（adhoc）只读查询：对指定数据源直接执行调用方传入的任意 SQL（SQL 客户端用）。
     *
     * <p>与 {@link #execute} 不同，SQL 文本来自调用方而非预配置 {@code SqlDefine}，故先经
     * {@link SqlValidator#validateReadOnly} 文本层拦截（仅 SELECT/WITH、拒绝多语句），再走连接级
     * {@code readOnly=true} 的只读模板双保险；{@code maxRows} 兜底截断，{@code total} 记实际返回行数。</p>
     */
    public abstract SqlQueryResultVO executeAdhoc(Long datasourceId, String rawSql);

    /**
     * 列出数据源下所有数据库（SQL 客户端左侧库树用）。元数据浏览、高频，不走 adhoc 审计。
     * 用 information_schema 只读查询，走连接级 readOnly 模板。
     */
    public abstract List<String> listDatabases(Long datasourceId);

    /**
     * 列出指定库下所有表（点库懒加载）。database 走参数绑定，无拼接注入风险。
     */
    public abstract List<String> listTables(Long datasourceId, String database);
}
