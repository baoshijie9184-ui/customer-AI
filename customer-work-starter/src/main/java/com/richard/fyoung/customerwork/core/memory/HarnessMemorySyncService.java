package com.richard.fyoung.customerwork.core.memory;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.workspace.WorkspaceManager;
import java.nio.file.Path;

/**
 * Harness 分层记忆同步：权威存储（{@link HarnessMemoryStore}）与框架工作副本
 * （{@code {workspace}/MEMORY.md}）之间的双向同步。
 *
 * <ul>
 *   <li><b>水合</b>（{@link #hydrate}）：构建 HarnessAgent 时把权威副本写到 workspace，保证换机 / 重启 /
 *       清理 workspace 后框架仍读得到历史记忆；权威侧为空而 workspace 有存量文件时反向入库（存量迁移）。</li>
 *   <li><b>回写</b>（{@link #persistIfChanged}）：对话轮次结束后把 workspace 文件的变更存回权威存储；
 *       内容未变化时跳过写入。</li>
 * </ul>
 *
 * <p>两个方法都不抛异常（同步失败不该打断对话主链路，也不该阻断实例构建），失败只记 error；
 * 这是本链路的唯一异常兜底点，{@link HarnessMemoryStore} 实现内部不再兜底。
 * 与 admin-server 的 {@code AgentMemorySyncService} 是同一套手法，键空间不同故各自一份。</p>
 * @author owlzhangfq@gmail.com
 */
public interface HarnessMemorySyncService {

    /**
     * 水合：权威存储 → workspace/MEMORY.md；权威侧为空且 workspace 有存量文件时反向入库。
     */
    public abstract void hydrate(Path workspace);

    /**
     * 回写：workspace/MEMORY.md → 权威存储（对话轮次结束后调用）；文件不存在或内容未变化时跳过。
     */
    public abstract void persistIfChanged(Path workspace);

    /**
     * AgentScope 2.0.2 工作区水合：所有读写经过 WorkspaceManager，兼容本地、远程存储与沙箱。
     */
    public abstract void hydrate(WorkspaceManager workspace, RuntimeContext context, String scopeId);

    /**
     * AgentScope 2.0.2 工作区回写：按运行时命名空间读取 MEMORY.md，不绕过 AbstractFilesystem。
     */
    public abstract void persistIfChanged(WorkspaceManager workspace, RuntimeContext context, String scopeId);
}
