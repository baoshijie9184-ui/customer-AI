package com.richard.fyoung.customeradmin.workspace.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.aiconfig.model.entity.AiModelConfig;
import com.richard.fyoung.customeradmin.aiconfig.model.mapper.AiModelConfigMapper;
import com.richard.fyoung.customeradmin.aiconfig.model.runtime.AdminModelFactory;
import com.richard.fyoung.customeradmin.common.crypto.AesGcmCryptoUtil;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.config.AdminKnowledgeProperties;
import com.richard.fyoung.customeradmin.workspace.audit.AiCodingOperation;
import com.richard.fyoung.customeradmin.workspace.audit.entity.AiCodingAuditLog;
import com.richard.fyoung.customeradmin.workspace.audit.service.AiCodingAuditService;
import com.richard.fyoung.customeradmin.workspace.knowledge.dto.KnowledgeAskResponse;
import com.richard.fyoung.customeradmin.workspace.knowledge.dto.KnowledgeSearchHit;
import com.richard.fyoung.customeradmin.workspace.knowledge.entity.AiCodeKnowledgeChunk;
import com.richard.fyoung.customeradmin.workspace.knowledge.entity.AiCodeKnowledgeIndex;
import com.richard.fyoung.customeradmin.workspace.knowledge.mapper.AiCodeKnowledgeChunkMapper;
import com.richard.fyoung.customeradmin.workspace.knowledge.mapper.AiCodeKnowledgeIndexMapper;
import com.richard.fyoung.customerwork.data.knowledge.CodeChunker;
import com.richard.fyoung.customerwork.data.knowledge.VectorMath;
import com.richard.fyoung.customerwork.data.knowledge.embedding.EmbeddingClient;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 代码知识库服务（P3-2 降级版）：显式触发建索引（扫描源码 → 类/方法级切块 → DashScope 真实 Embedding →
 * 向量入库），语义 top-k 检索与检索增强问答（应用层余弦相似度）。降级差异见 {@link AdminKnowledgeProperties}。
 *
 * <h3>关键约束</h3>
 * <ul>
 *   <li>源码路径必须落在 {@code admin.knowledge.allowed-roots} 白名单下（防路径穿越/越权读盘）；</li>
 *   <li>Embedding Key 缺失 fast fail（{@link ResultCode#KNOWLEDGE_EMBEDDING_NOT_CONFIGURED}），不静默降级回关键词；</li>
 *   <li>构建显式触发、进度可查（索引行 status + chunk_count 增量更新），不做自动监听。</li>
 * </ul>
 * @author owlzhangfq@gmail.com
 */
public interface KnowledgeService {

    /**
     * 触发构建/重建索引：请求线程内校验路径 + 落库 BUILDING 行 + 创建审计条目（操作人取自 Sa-Token），
     * 重活（扫描/切块/Embedding/入库）派到后台线程执行，接口立即返回索引ID供前端轮询进度。
     */
    public abstract Long buildIndex(String indexName, String sourcePath);

    /**
     * 索引列表（全量，按创建时间倒序；数据量小，不分页）。
     */
    public abstract List<AiCodeKnowledgeIndex> listIndexes();

    /**
     * 查单个索引（含 status/chunk_count，供进度轮询）。
     */
    public abstract AiCodeKnowledgeIndex getIndex(Long indexId);

    /**
     * 删除索引及其全部分块。
     */
    public abstract void deleteIndex(Long indexId);

    /**
     * 语义检索 top-k（应用层余弦相似度）。异步执行，Embedding/相似度不占 Tomcat 线程。
     */
    public abstract CompletableFuture<List<KnowledgeSearchHit>> search(Long indexId, String query, Integer topK);

    /**
     * 检索增强问答：检索 top-k → 拼上下文 → 一次性模型作答，回答带出处。
     */
    public abstract CompletableFuture<KnowledgeAskResponse> ask(Long indexId, String question, Integer topK);
}
