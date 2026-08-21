package com.richard.fyoung.customeradmin.workspace.vibecoding.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.workspace.vibecoding.dto.RollbackResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 会话 workspace 的轻量 Git 集成：把 {@code sessions/{sessionId}/} 目录现场初始化为 git 仓库，
 * 以"会话开始时的空快照"为基线提交，本轮对话产生的全部文件变更即为相对基线的 {@code git diff}。
 *
 * <p>只生成只读的 diff 信息，不做真正的业务提交——commit/push 仍由开发者在本地 IDE 完成，
 * 与需求文档 3.2 的定位一致（"辅助生成 Git 相关文本"，不代替开发者操作 Git）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface GitWorkspaceService {

    /**
     * 幂等：{@code .git} 已存在则直接返回；否则 init + 建立空基线提交，作为后续 diff 的对比基准。
     */
    public abstract void ensureRepo(Path workspace);

    /**
     * 相对基线提交的完整 unified diff 文本（含新增/修改/删除的所有文件）。
     */
    public abstract String diffAgainstBaseline(Path workspace);

    /**
     * 相对基线提交发生变更的文件相对路径清单。
     */
    public abstract List<String> changedFilesAgainstBaseline(Path workspace);

    /**
     * 会话级一键回滚：把 workspace 恢复到 baseline（首个空提交）状态——已跟踪文件 checkout 回 baseline
     * 内容（本会话内的修改/删除被还原），新增未跟踪文件连同空目录清理删除，{@code .git} 目录本身保留。
     *
     * <p>幂等：无变更时两个清单均为空、不报错；重复调用恢复到同一 baseline。</p>
     *
     * <p><b>安全约束（全链路唯一防御点）</b>：破坏性的 {@code checkout}/{@code clean} 操作必须严格限定在
     * 本 workspace 自己的 git 仓库内。执行前强校验 workspace 根目录下存在 {@code .git}——若缺失（会话
     * 从未建立 baseline），git 会向上查找父级仓库，{@code clean -fd} 可能误删项目其它未跟踪文件；因此
     * 此处 fast fail，既是"baseline 缺失"的业务校验，也是"越界删文件"的安全兜底。</p>
     *
     * @return 恢复的已跟踪文件清单 + 删除的新增文件清单
     */
    public abstract RollbackResult rollbackToBaseline(Path workspace);
}
