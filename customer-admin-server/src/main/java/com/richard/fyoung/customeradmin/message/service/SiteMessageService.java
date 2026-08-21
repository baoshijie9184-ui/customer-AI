package com.richard.fyoung.customeradmin.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.page.PageResult;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.message.dto.SiteMessageVO;
import com.richard.fyoung.customeradmin.message.entity.SiteMessage;
import com.richard.fyoung.customeradmin.message.mapper.SiteMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.time.LocalDateTime;

/**
 * 通用站内消息服务：投递（供任意业务域调用）、当前用户的分页查询、未读数、标记已读。
 *
 * <p>投递入口 {@link #send} 不依赖 Web 上下文（接收人 userId 由调用方显式传入），可在后台异步
 * 线程里安全调用（如 AI 代码审查异步完成后的回调线程）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface SiteMessageService {

    /**
     * 投递一条站内消息（通用入口）。title/bizType 为必填业务约束，交由调用方保证；这里只做落库。
     *
     * @param userId  接收人（admin 用户 id）
     * @param title   标题
     * @param content 正文（可空）
     * @param bizType 业务类型（如 {@code CODE_REVIEW}）
     * @param bizId   业务主键（可空）
     * @param link    前端跳转路由（可空）
     */
    public abstract void send(Long userId, String title, String content, String bizType, String bizId, String link);

    /**
     * 分页查询当前用户的站内消息：可选按已读标记过滤，未读优先、同组按创建时间倒序。
     *
     * @param userId   当前用户
     * @param readFlag 已读标记过滤（null 表示不过滤）
     */
    public abstract PageResult<SiteMessageVO> page(Long userId, Integer readFlag, long pageNum, long pageSize);

    /**
     * 当前用户的未读消息数。
     */
    public abstract long unreadCount(Long userId);

    /**
     * 标记单条消息已读：校验归属，非本人/不存在快速失败。已读幂等。
     */
    public abstract void markRead(Long id, Long userId);

    /**
     * 标记当前用户全部未读消息为已读。
     */
    public abstract void markAllRead(Long userId);
}
