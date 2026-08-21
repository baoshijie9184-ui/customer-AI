package com.richard.fyoung.customerworkapp.service;

import com.richard.fyoung.customerwork.data.attachment.AttachmentFileStorage;
import com.richard.fyoung.customerwork.infra.config.CustomerWorkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import com.richard.fyoung.customerwork.infra.config.properties.UserAuthProperties;

/**
 * 用户头像存储：校验（扩展名白名单 + 大小上限）、以 UUID 命名写入对象存储、返回可访问 URL（响应式）。
 *
 * <p>存储后端走 starter 的 {@link AttachmentFileStorage} SPI（MinIO），项目内<b>不落任何文件</b>。
 * 大小上限 / URL 前缀仍取 {@code customer-work.user-auth.avatar.*}。改造前落在旧本地目录的存量头像
 * 需要重新上传，或由运维把旧文件按同名 key 灌进 MinIO。</p>
 *
 * <p>大小校验仍是边收边计数、超限即中断（{@code sink.error}），故聚合进内存的字节量被上限封住，
 * 不会被一个超大文件打爆。扩展名校验为链路唯一防御点（fast-fail），非法即 400。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AvatarStorageService {

    /**
     * 存储头像文件并返回可访问 URL。
     *
     * @param filePart 上传的文件分片
     * @return 可访问的相对 URL（{@code urlPrefix + key}）
     */
    public abstract Mono<String> store(FilePart filePart);

    /**
     * 按 key 读头像字节。
     *
     * @throws IOException 对象不存在或读取失败
     */
    public abstract byte[] read(String key) throws IOException;
}
