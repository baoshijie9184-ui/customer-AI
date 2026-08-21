package com.richard.fyoung.customeradmin.system.loginimage.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customerwork.data.attachment.AttachmentFileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 登录页轮播图文件存储：写入对象存储（starter 的 {@link AttachmentFileStorage} SPI，
 * {@code customer-work.attachment.storage.type=minio} 时即 MinIO），通过
 * {@link com.richard.fyoung.customeradmin.system.loginimage.controller.LoginImagePublicController}
 * 映射的 {@code /api/login-images/**} 对外提供访问 URL。
 *
 * <p>URL 契约与改造前一致（{@code /api/login-images/{key}}），但<b>不再有本地盘</b>：
 * 项目内不落任何文件，{@code sys_login_carousel_image.image_url} 里改造前写入的地址
 * （对应旧 {@code ./data/login-images} 下的文件）需要重新上传，或由运维把旧文件按同名 key 灌进 MinIO。</p>
 * @author owlzhangfq@gmail.com
 */
public interface LoginImageStorageService {

    /**
     * @return 可访问 URL（相对路径，前端拼自身 origin 即可直接展示）。
     */
    public abstract String store(MultipartFile file);

    /**
     * 按相对 key 读图片字节（供 {@code LoginImagePublicController} 出图）。
     *
     * @throws IOException 对象不存在或读取失败
     */
    public abstract byte[] read(String key) throws IOException;

    /**
     * 按访问 URL 删除图片对象。删除记录的主链路在 DB 侧，对象清理失败只记 error 不中断
     * （残留对象不影响功能，可运维期清理）。
     */
    public abstract void delete(String imageUrl);

    /**
     * 与 {@code LoginImagePublicController} 的映射保持一致，改这里要同步改那边。
     */
    public static final String URL_PREFIX = "/api/login-images/";
}
