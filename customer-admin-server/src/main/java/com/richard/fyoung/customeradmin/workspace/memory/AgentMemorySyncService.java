package com.richard.fyoung.customeradmin.workspace.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 长期记忆同步：权威存储（{@link AgentMemoryStore}）与框架工作副本（{@code {workspace}/MEMORY.md}）
 * 之间的双向同步。
 *
 * <ul>
 *   <li><b>水合</b>（{@link #hydrate}）：构建智能体实例时把权威副本写到 workspace，保证换机/重启/
 *       清理 workspace 后框架仍能读到历史记忆；权威存储为空而 workspace 有存量文件时反向入库（存量迁移）。</li>
 *   <li><b>回写</b>（{@link #persistIfChanged}）：对话轮次结束后把 workspace 文件的变更保存回权威存储；
 *       内容未变化时跳过写入。</li>
 * </ul>
 *
 * <p>两个方法都不抛异常（同步失败不应打断对话主链路，也不应阻断实例构建），失败只记 error 日志；
 * 这是记忆同步链路的唯一异常兜底点，{@code AgentMemoryStore} 实现内部不再兜底。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AgentMemorySyncService {

    /**
     * 水合：权威存储 → workspace/MEMORY.md（构建实例时调用）；权威侧为空且 workspace 有存量文件时反向入库。
     */
    public abstract void hydrate(String agentCode, Path workspace);

    /**
     * 回写：workspace/MEMORY.md → 权威存储（对话轮次结束后调用）；文件不存在或内容未变化时跳过。
     */
    public abstract void persistIfChanged(String agentCode, Path workspace);
}
