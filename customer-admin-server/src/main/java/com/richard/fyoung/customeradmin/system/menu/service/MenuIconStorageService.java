package com.richard.fyoung.customeradmin.system.menu.service;

import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customerwork.data.attachment.AttachmentFileStorage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 菜单图标图片上传：写入对象存储（starter 的 {@link AttachmentFileStorage} SPI，MinIO），通过
 * {@link com.richard.fyoung.customeradmin.system.menu.controller.MenuIconController}
 * 映射的 {@code /api/menu-icons/**} 对外提供访问 URL。
 *
 * <p>URL 契约与改造前一致（{@code /api/menu-icons/{key}}），但<b>不再有本地盘</b>：
 * 项目内不落任何文件，{@code sys_menu.icon} 里改造前写入的地址（对应旧 {@code ./data/menu} 下的文件）
 * 需要重新上传图标，或由运维把旧文件按同名 key 灌进 MinIO。</p>
 * @author owlzhangfq@gmail.com
 */
public interface MenuIconStorageService {

    /**
     * @return 可访问 URL（相对路径，前端拼自身 origin 即可直接 <img> 展示）。
     */
    public abstract String upload(MultipartFile file);

    /**
     * 按相对 key 读图标字节（供 {@code MenuIconController} 出图）。
     *
     * @throws IOException 对象不存在或读取失败
     */
    public abstract byte[] read(String key) throws IOException;

    /**
     * 与 {@code MenuIconController} 的映射保持一致，改这里要同步改那边。
     */
    public static final String URL_PREFIX = "/api/menu-icons/";
}
