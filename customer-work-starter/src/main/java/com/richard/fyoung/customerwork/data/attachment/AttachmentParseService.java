package com.richard.fyoung.customerwork.data.attachment;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 附件解析编排入口（<b>全链路唯一防御点</b>）。
 *
 * <p>流程：入参校验（白名单 + 大小）→ 落盘 → 选解析器解析（超长截断）→ 落库 → 返回 {@link ChatAttachment}。
 * 校验不通过（不在白名单 / 超大小上限）直接抛 {@link IllegalArgumentException}，由上层接口转 4xx；解析异常
 * 统一 {@code catch(Exception)} 落 FAILED 记录并正常返回（文件已落盘、记录已落库，不把 500 抛给上传接口）。
 * fast fail 原则：白名单 / 大小校验只在本类做一次，各解析器内部不再重复校验。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AttachmentParseService {

    /**
     * 解析并存储一个附件。
     *
     * @param data      文件字节
     * @param fileName  原始文件名
     * @param sessionId 会话 ID（可空）
     * @param uploader  上传者标识（可空）
     * @param channel   来源渠道：user_chat / admin_chat / vibecoding
     * @return 附件记录（含解析文本或失败原因）
     * @throws IllegalArgumentException 文件为空 / 扩展名不在白名单 / 超过大小上限
     */
    public abstract ChatAttachment parseAndStore(byte[] data, String fileName, String sessionId, String uploader, String channel);
}
