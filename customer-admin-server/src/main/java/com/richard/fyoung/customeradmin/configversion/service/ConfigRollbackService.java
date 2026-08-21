package com.richard.fyoung.customeradmin.configversion.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.aiconfig.channel.publish.CustomerWorkConfigPublisher;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.configversion.entity.AiConfigVersion;
import com.richard.fyoung.customeradmin.configversion.entity.PublishScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.util.List;

/**
 * 配置回滚与灰度发布。
 *
 * <p><b>回滚是"把旧内容作为新版本再发一次"，不是删掉新版本</b>：删除会让发布历史出现空洞，
 * 事后无法回答"某个时间点线上跑的是哪一版"。只增不删的历史也让回滚本身可被再回滚。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ConfigRollbackService {

    /**
     * 回滚到指定版本：取该版本的内容快照重新下发，并记为一个新版本。
     *
     * @param versionId 目标版本主键
     * @param remark    回滚说明（建议写清为什么回滚，事后翻历史时这句话最有用）
     * @return 新产生的版本号
     */
    public abstract int rollback(Long versionId, String remark);

    /**
     * 灰度发布：把指定版本的内容只下发给名单内的租户。
     *
     * <p>逐租户写各自的 dataId（{@code <主dataId>-tenant-<租户码>}）。客服端按自己的租户读，
     * 读不到就回落主 dataId——因此名单外的租户继续用全量版本，客服端不需要理解"灰度"这个概念。</p>
     *
     * @return 实际下发的租户数
     */
    public abstract int grayRelease(Long versionId, List<String> tenantCodes, String remark);

    /**
     * 解析灰度租户列表（存的是 JSON 数组）。
     */
    public abstract List<String> parseGrayTenants(String grayTenantsJson);
}
