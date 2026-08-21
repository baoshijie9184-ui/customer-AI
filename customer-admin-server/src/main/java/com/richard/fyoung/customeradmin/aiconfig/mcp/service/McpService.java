package com.richard.fyoung.customeradmin.aiconfig.mcp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgentMcp;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.mapper.AiAgentMcpMapper;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.aiconfig.mcp.dto.McpDebugCallRequest;
import com.richard.fyoung.customeradmin.aiconfig.mcp.dto.McpDebugCallResult;
import com.richard.fyoung.customeradmin.aiconfig.mcp.dto.McpDebugToolVO;
import com.richard.fyoung.customeradmin.aiconfig.mcp.dto.McpSaveRequest;
import com.richard.fyoung.customeradmin.aiconfig.mcp.dto.McpTestResult;
import com.richard.fyoung.customeradmin.aiconfig.mcp.dto.McpVO;
import com.richard.fyoung.customeradmin.aiconfig.mcp.entity.AiMcp;
import com.richard.fyoung.customeradmin.aiconfig.mcp.mapper.AiMcpMapper;
import com.richard.fyoung.customeradmin.aiconfig.mcp.runtime.AdminMcpFactory;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageQuery;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workspace.runtime.AgentInstanceCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * MCP 管理。
 * @author owlzhangfq@gmail.com
 */
public interface McpService {

    public abstract PageResult<McpVO> page(PageQuery query);

    public abstract McpVO get(Long id);

    public abstract void create(McpSaveRequest request);

    public abstract void update(Long id, McpSaveRequest request);

    public abstract void delete(Long id);

    /**
     * 连通性测试：与模型测试同一手法，派发到独立线程池执行，硬性超时兜底，不占用调用方（Tomcat）线程。
     */
    public abstract CompletableFuture<McpTestResult> testConnectivity(Long id);

    /**
     * 调试面板 · 列工具：跟连通性测试同一手法派发到独立线程池，避免 {@code AdminMcpFactory} 内部的
     * {@code .block()} 占用 Tomcat 请求线程；异常统一包成 {@link BizException}，让前端拿到具体错误文案
     * 而不是泛泛的 500。
     */
    public abstract CompletableFuture<List<McpDebugToolVO>> listDebugTools(Long id);

    /**
     * 调试面板 · 单次调用工具：同上分发到独立线程池；工具调用本身可能有副作用（比如真的下了单/查了数据），不做重试。
     */
    public abstract CompletableFuture<McpDebugCallResult> callDebugTool(Long id, McpDebugCallRequest request);
}
