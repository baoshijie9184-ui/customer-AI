package com.richard.fyoung.customeradmin.workspace.chat.service;

import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatMessageAttachmentVO;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatMessageVO;
import com.richard.fyoung.customeradmin.workspace.chat.dto.ChatSessionSummary;
import com.richard.fyoung.customeradmin.workspace.chat.mapper.ChatSessionStateQueryMapper;
import com.richard.fyoung.customeradmin.workspace.runtime.AgentInstanceCache;
import com.richard.fyoung.customeradmin.workspace.runtime.AgentStateAccessor;
import com.richard.fyoung.customerwork.data.attachment.AttachmentStore;
import com.richard.fyoung.customerwork.data.attachment.ChatAttachment;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 历史会话查询：会话列表（SQL 级分页）+ 重新打开某次历史会话的完整消息。
 *
 * <p>不新增数据库表——直接读框架自建的会话状态表（批次六起已换成
 * {@link io.agentscope.extensions.mysql.state.MysqlAgentStateStore}，重启不丢）。历史列表按
 * {@link ChatSessionStateQueryMapper} 在 SQL 侧完成分页与排序（{@code session_id LIKE '{agentCode}:%'}
 * 走主键最左前缀），每页只对本页会话逐个 {@link AgentStateAccessor#resolve} 取完整 {@link AgentState}
 * 组装摘要——早期实现枚举该智能体全部 sessionId、逐个 resolve 整段 longtext blob 只为算预览/消息数，
 * 会话数一多就把全部 blob 拉进内存，故改成分页。chat 与 vibecoding 共用同一套 session 状态
 * （只是 sessionId 不同），历史列表天然把两者混在一起，不做类型区分——如实符合"同一智能体下的历史
 * 对话"这个需求本身的粒度，不做额外的会话类型元数据设计。</p>
 *
 * <p>分页列表不走缓存（每页仅 20 个 blob，成本可控，不值得为它引入分页缓存复杂度）；
 * {@link ChatHistoryCache} 只缓存单次历史会话的消息内容。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ChatHistoryService {

    /**
     * 历史会话列表（按最后更新时间倒序、SQL 级分页）。mapper 只查出本页会话 id（带
     * {@code {agentCode}:} 前缀），去前缀后逐个 resolve 完整上下文组装摘要——context 为空的会话跳过，
     * 因此本页返回条数可能略少于 {@code size}（顺序仍以 SQL 的 updated_at 倒序为准）。
     *
     * @param page 页码（从 1 起）
     * @param size 每页条数
     */
    public abstract PageResult<ChatSessionSummary> listSessions(String agentCode, long page, long size);

    /**
     * 30 分钟读缓存命中直接返回；未命中回源 MySQL（{@link AgentStateStore}）后回填缓存。
     */
    public abstract List<ChatMessageVO> getMessages(String agentCode, String sessionId);

    /**
     * 单条会话摘要：给定 agentCode+sessionId 直接解析，不走"列出全部再过滤"——Projects 详情页按需查
     * 单条会话时用（一个项目里的会话可能横跨很多智能体，没必要把每个智能体的全部会话都拉一遍）。
     * 会话已查不到内容（比如底层状态被清理）时返回空。
     */
    public abstract Optional<ChatSessionSummary> getSessionSummary(String agentCode, String sessionId);

    /**
     * 默认每页条数（与 {@code ChatController#sessions} 的 {@code size} 默认值保持一致）。
     */
    public static final long DEFAULT_PAGE_SIZE = 20;
}
