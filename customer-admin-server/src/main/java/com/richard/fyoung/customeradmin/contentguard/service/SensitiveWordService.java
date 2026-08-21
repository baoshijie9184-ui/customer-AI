package com.richard.fyoung.customeradmin.contentguard.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.contentguard.config.ContentGuardGatewayProvider;
import com.richard.fyoung.customeradmin.contentguard.dto.SensitiveWordPageQuery;
import com.richard.fyoung.customeradmin.contentguard.dto.SensitiveWordSaveRequest;
import com.richard.fyoung.customeradmin.contentguard.dto.SensitiveWordVO;
import com.richard.fyoung.customeradmin.contentguard.jdbc.ContentGuardGateway;
import com.richard.fyoung.customeradmin.contentguard.jdbc.SensitiveWordQueryParam;
import com.richard.fyoung.customerwork.safety.sensitiveword.SensitiveWordAction;
import com.richard.fyoung.customerwork.safety.sensitiveword.SensitiveWordCategory;
import com.richard.fyoung.customerwork.safety.sensitiveword.entity.SensitiveWordEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 敏感词词库管理：分页查询、增删改、启停、批量导入导出。
 *
 * <p><b>写的是客服端库的 {@code cw_sensitive_word}——唯一真源</b>。后台改完不需要通知任何客服实例：
 * starter 侧的 {@code SensitiveWordRefresher} 轮询版本指纹，指纹一变就重建自动机，默认 60 秒内生效。
 * 这也是为什么这里每次写入都刷新 {@code updated_at_ms}：它就是那个指纹的组成部分，不刷新等于改了不生效。</p>
 * @author owlzhangfq@gmail.com
 */
public interface SensitiveWordService {

    /**
     * 分页查询词库。
     */
    public abstract PageResult<SensitiveWordVO> page(SensitiveWordPageQuery query);

    /**
     * 按 ID 取一条。
     */
    public abstract SensitiveWordVO get(Long id);

    /**
     * 新增。词面唯一，重复直接拒绝而不是静默覆盖——覆盖会把别人配的类目/动作悄悄改掉。
     */
    public abstract void create(SensitiveWordSaveRequest request);

    /**
     * 编辑。
     */
    public abstract void update(Long id, SensitiveWordSaveRequest request);

    /**
     * 删除。
     */
    public abstract void delete(Long id);

    /**
     * 启停。
     */
    public abstract void toggle(Long id, boolean enabled);

    /**
     * 批量导入：每行 {@code 词面,类目,动作}（类目与动作可省，分别默认 CUSTOM 与 BLOCK）。
     *
     * <p>走 upsert 而不是逐条判重后 insert：导入的语义就是"以这份清单为准"，同名词更新其类目与动作，
     * 比报错让运营逐条排查更贴合实际用法。返回实际处理条数。</p>
     */
    public abstract int importWords(List<String> lines);

    /**
     * 导出全部词条（按导入格式：词面,类目,动作），供运营备份与迁移。
     */
    public abstract List<String> exportWords();

    /**
     * 可选类目/动作枚举，供前端下拉直接渲染，避免前后端各维护一份常量。
     */
    public abstract List<String> categories();

    public abstract List<String> actions();
}
