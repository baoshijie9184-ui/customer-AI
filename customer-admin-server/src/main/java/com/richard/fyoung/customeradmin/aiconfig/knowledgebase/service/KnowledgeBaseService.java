package com.richard.fyoung.customeradmin.aiconfig.knowledgebase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.client.KnowledgeSearchClient;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.dto.KnowledgeBaseOptionVO;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.dto.KnowledgeBaseSaveRequest;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.dto.KnowledgeBaseTestResult;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.dto.KnowledgeBaseVO;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.entity.AiAgentKnowledgeBase;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.entity.AiKnowledgeBase;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.mapper.AiAgentKnowledgeBaseMapper;
import com.richard.fyoung.customeradmin.aiconfig.knowledgebase.mapper.AiKnowledgeBaseMapper;
import com.richard.fyoung.customeradmin.common.crypto.AesGcmCryptoUtil;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.config.AdminRagProperties;
import com.richard.fyoung.customerwork.data.rag.search.KnowledgeBaseEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * RAG 知识库配置管理：CRUD + AppKey 加密存储 + 启停 + 连通性测试 + <b>保存门禁</b>。
 *
 * <p><b>保存门禁</b>（需求硬要求）：新建必测；编辑仅在连接参数（baseUrl/appId/apiKey/contentType/
 * extraHeaders）真正发生变更时才测——只改名称/备注/阈值/启停不重测，避免每次改个备注都去打一次外部
 * 服务、也避免外部服务临时抖动时连改备注都改不了。实测不通过直接抛 {@link BizException} 阻止保存
 * （同 {@code AgentService#assertPrimaryModelConnectivity} 的现场实测模式，不读 test_status 字段）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface KnowledgeBaseService {

    public abstract PageResult<KnowledgeBaseVO> page(PageQuery query);

    public abstract KnowledgeBaseVO get(Long id);

    /**
     * 智能体表单下拉：只给可用的知识库（启用 + 连通性测试成功），不可用的不允许被绑定。
     */
    public abstract List<KnowledgeBaseOptionVO> options();

    public abstract void create(KnowledgeBaseSaveRequest request);

    public abstract void update(Long id, KnowledgeBaseSaveRequest request);

    public abstract void delete(Long id);

    /**
     * 启用/停用（生命周期），不改动其余字段。停用后运行时立即不再参与检索。
     */
    public abstract void updateStatus(Long id, int status);

    /**
     * 连通性测试：用固定探测语句真实发一次检索请求，派发到独立线程池执行并硬性超时兜底，
     * 不占用调用方（Tomcat）线程。Controller 侧以 {@link CompletableFuture} 异步返回。
     */
    public abstract CompletableFuture<KnowledgeBaseTestResult> testConnectivity(Long id);
}
