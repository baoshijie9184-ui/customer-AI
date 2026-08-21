package com.richard.fyoung.customeradmin.workbench.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workbench.WorkbenchConstants;
import com.richard.fyoung.customeradmin.workbench.dto.WorkbenchTokenCreateRequest;
import com.richard.fyoung.customeradmin.workbench.dto.WorkbenchTokenCreatedVO;
import com.richard.fyoung.customeradmin.workbench.dto.WorkbenchTokenVO;
import com.richard.fyoung.customeradmin.workbench.entity.WorkbenchToken;
import com.richard.fyoung.customeradmin.workbench.mapper.WorkbenchTokenMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 内网工作台个人访问令牌服务：创建（明文只返回一次）、列举、吊销、校验。
 *
 * <p>令牌明文 = {@code wbt_} + 32 字节安全随机的 URL-safe Base64；库里只存 SHA-256 哈希，
 * 校验时对入参同样哈希后比对，无法从库反推明文。</p>
 * @author owlzhangfq@gmail.com
 */
public interface WorkbenchTokenService {

    /**
     * 创建令牌，返回含明文的一次性 VO（明文之后无法再取回）。
     */
    public abstract WorkbenchTokenCreatedVO createToken(Long userId, WorkbenchTokenCreateRequest request);

    /**
     * 当前用户的令牌列表（未删除），按创建时间倒序。
     */
    public abstract List<WorkbenchTokenVO> listByUser(Long userId);

    /**
     * 吊销令牌：仅令牌所属用户可操作。
     */
    public abstract void revoke(Long userId, Long id);

    /**
     * 校验令牌明文，返回所属 userId；无效（不存在/已吊销/已过期）抛 {@link BizException}。
     * 命中则刷新 last_used_time。
     */
    public abstract Long validate(String rawToken);
}
