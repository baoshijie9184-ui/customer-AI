package com.richard.fyoung.customeradmin.sqlconfig.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlDefineParamSaveRequest;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlDefineParamVO;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlDefineSaveRequest;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlDefineVO;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlFieldTransformSaveRequest;
import com.richard.fyoung.customeradmin.sqlconfig.dto.SqlFieldTransformVO;
import com.richard.fyoung.customeradmin.sqlconfig.engine.SqlValidator;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlDatasource;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlDefine;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlDefineParam;
import com.richard.fyoung.customeradmin.sqlconfig.entity.SqlFieldTransform;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlDatasourceMapper;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlDefineMapper;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlDefineParamMapper;
import com.richard.fyoung.customeradmin.sqlconfig.mapper.SqlFieldTransformMapper;
import com.richard.fyoung.customerwork.infra.sqlkit.ParamType;
import com.richard.fyoung.customerwork.infra.sqlkit.TransformType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL 定义管理：define CRUD（保存过 SqlValidator 只读校验）+ 复制 + 参数/列转换器子资源 CRUD。
 *
 * <p>删除 define 时级联删除其参数与列转换器（事务内）；复制会连带复制全部子表记录，
 * define_key 追加 {@code -copy} 后缀（冲突再追加数字）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface SqlDefineService {

    public abstract PageResult<SqlDefineVO> page(PageQuery query);

    public abstract SqlDefineVO get(Long id);

    public abstract void create(SqlDefineSaveRequest request);

    public abstract void update(Long id, SqlDefineSaveRequest request);

    public abstract void delete(Long id);

    /**
     * 复制 define + 全部参数/列转换器；define_key 追加 -copy 后缀（冲突再追加数字）。
     */
    public abstract void copy(Long id);

    public abstract List<SqlDefineParamVO> listParams(Long defineId);

    public abstract void createParam(Long defineId, SqlDefineParamSaveRequest request);

    public abstract void updateParam(Long defineId, Long paramId, SqlDefineParamSaveRequest request);

    public abstract void deleteParam(Long defineId, Long paramId);

    public abstract List<SqlFieldTransformVO> listTransforms(Long defineId);

    public abstract void createTransform(Long defineId, SqlFieldTransformSaveRequest request);

    public abstract void updateTransform(Long defineId, Long transformId, SqlFieldTransformSaveRequest request);

    public abstract void deleteTransform(Long defineId, Long transformId);
}
