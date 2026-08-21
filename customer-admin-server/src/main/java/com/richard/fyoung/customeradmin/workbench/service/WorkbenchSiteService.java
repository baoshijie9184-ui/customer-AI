package com.richard.fyoung.customeradmin.workbench.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.common.crypto.AesGcmCryptoUtil;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workbench.WorkbenchConstants;
import com.richard.fyoung.customeradmin.workbench.dto.WorkbenchAgentSiteVO;
import com.richard.fyoung.customeradmin.workbench.dto.WorkbenchSiteSaveRequest;
import com.richard.fyoung.customeradmin.workbench.dto.WorkbenchSiteVO;
import com.richard.fyoung.customeradmin.workbench.entity.WorkbenchSite;
import com.richard.fyoung.customeradmin.workbench.mapper.WorkbenchSiteMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 内网工作台系统账号本：CRUD + 密码加密存储/留空不改/脱敏回显 + 明文密码按需解密（供前端复制）。
 *
 * <p>密码处理沿用 {@code SqlDatasourceService} 的手法（AES/GCM 密文入库，列表 VO 回显
 * {@code mask(decrypt(...))}；明文只在 {@link #getSecret} 敏感读接口按需解密）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface WorkbenchSiteService {

    public abstract PageResult<WorkbenchSiteVO> page(PageQuery query);

    public abstract void create(WorkbenchSiteSaveRequest request);

    public abstract void update(Long id, WorkbenchSiteSaveRequest request);

    public abstract void delete(Long id);

    /**
     * 解密返回明文密码（供前端"复制密码"用）。记录不存在或未配置密码时 fast fail。
     */
    public abstract String getSecret(Long id);

    /**
     * 按登录页 host 反查启用站点，返回账号+明文密码+自动登录配置（供 ScriptCat 脚本）。
     *
     * <p>url 存的是完整访问地址（可能带 path），先用 like 缩小候选再用 URI 精确比对 host，
     * 避免 like 的子串误匹配（如 git.x.com 命中 xgit.x.com）。</p>
     */
    public abstract WorkbenchAgentSiteVO findAgentSiteByHost(String host);

    /**
     * 所有启用站点的 host 去重列表，供脚本生成 @match。
     */
    public abstract List<String> listEnabledHosts();
}
