package com.richard.fyoung.customeradmin.aiconfig.agent.service;

import com.richard.fyoung.customeradmin.aiconfig.agent.dto.AgentMemoryVO;
import com.richard.fyoung.customeradmin.aiconfig.agent.entity.AiAgent;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workspace.memory.AgentMemorySnapshot;
import com.richard.fyoung.customeradmin.workspace.memory.AgentMemoryStore;
import com.richard.fyoung.customeradmin.workspace.memory.AgentMemorySyncService;
import com.richard.fyoung.customeradmin.workspace.runtime.AdminAgentInstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 智能体长期记忆运维：查看/清空。
 *
 * <p>读写对象是权威存储 {@link AgentMemoryStore}（默认库表 ai_agent_memory，配置 disk-root 后为磁盘）；
 * 查看前先做一次 workspace → 权威存储的回写同步（拿到最近一轮对话刚 flush 的内容），清空时除权威存储外
 * 一并删除 workspace 下的工作副本（MEMORY.md 与 memory/ 原料目录），避免运行副本把旧记忆同步回来。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AgentMemoryService {

    /**
     * 查看长期记忆：先同步 workspace 最新变更再读权威存储；从未沉淀过时返回 {@code exists=false}。
     */
    public abstract AgentMemoryVO getMemory(Long id);

    /**
     * 清空长期记忆：删除权威存储记录 + workspace 工作副本（MEMORY.md 与 memory/ 原料目录），幂等。
     */
    public abstract void clearMemory(Long id);
}
