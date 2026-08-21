package com.richard.fyoung.customeradmin.configversion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.configversion.dto.ConfigVersionPageQuery;
import com.richard.fyoung.customeradmin.configversion.dto.ConfigVersionVO;
import com.richard.fyoung.customeradmin.configversion.entity.AiConfigVersion;
import com.richard.fyoung.customeradmin.configversion.entity.ConfigType;
import com.richard.fyoung.customeradmin.configversion.entity.PublishScope;
import com.richard.fyoung.customeradmin.configversion.mapper.AiConfigVersionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 配置版本快照的记录与检索。
 *
 * <p>只负责"记下发布了什么"，实际下发由 {@code CustomerWorkConfigPublisher} 负责——
 * 两者分开，是为了让"发布失败"也能留下一条 FAILED 记录：发布动作失败恰恰是最需要留痕的时刻。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ConfigVersionService {

    /**
     * 记录一次成功发布。
     *
     * <p>内容与上一版完全相同时不再新增版本——重复发布同样的内容只会把版本历史刷满噪音，
     * 让"这次到底改了什么"变得难以辨认。</p>
     *
     * @return 新版本号；内容未变时返回既有版本号
     */
    public abstract int recordPublish(ConfigType type, String targetCode, Long targetId, String content, String dataId, PublishScope scope, String grayTenants, Integer sourceVersion, String remark);

    /**
     * 记录一次失败的发布尝试。
     *
     * <p>失败也要留痕：排查"线上为什么还是旧配置"时，一条 FAILED 记录比什么都没有有用得多。
     * 失败版本不参与"取代上一版"——上一版仍然是线上生效的那一版。</p>
     */
    public abstract void recordFailure(ConfigType type, String targetCode, Long targetId, String content, String reason);

    public abstract PageResult<ConfigVersionVO> page(ConfigVersionPageQuery query);

    /**
     * 版本详情（含完整快照内容，供对比与回滚预览）。
     */
    public abstract ConfigVersionVO detail(Long id);

    /**
     * 某目标的全部版本（版本选择下拉与对比用）。
     */
    public abstract List<ConfigVersionVO> listByTarget(ConfigType type, String targetCode);

    /**
     * 当前生效版本（状态为 PUBLISHED 的那一条）。
     */
    public abstract Optional<AiConfigVersion> findCurrent(ConfigType type, String targetCode);

    public abstract AiConfigVersion requireVersion(Long id);
}
