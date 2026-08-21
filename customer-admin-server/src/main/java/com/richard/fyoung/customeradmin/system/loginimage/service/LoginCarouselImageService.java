package com.richard.fyoung.customeradmin.system.loginimage.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.system.loginimage.dto.LoginCarouselImageVO;
import com.richard.fyoung.customeradmin.system.loginimage.dto.LoginImageReorderRequest;
import com.richard.fyoung.customeradmin.system.loginimage.entity.LoginCarouselImage;
import com.richard.fyoung.customeradmin.system.loginimage.mapper.LoginCarouselImageMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录页轮播图管理：上传/列表/启停/排序/删除，以及登录页免鉴权拉取的启用图列表。
 *
 * <p>格式/大小校验与落盘收敛在 {@link LoginImageStorageService}，本服务只管 DB 记录与业务规则
 * （数量上限、排序重写、删除时联动清理磁盘文件）。</p>
 * @author owlzhangfq@gmail.com
 */
public interface LoginCarouselImageService {

    /**
     * 管理页列表：全量按 sortOrder 升序（含禁用的，禁用状态由前端标识展示）。
     */
    public abstract List<LoginCarouselImageVO> list();

    /**
     * 登录页免鉴权实时拉取：仅启用图的访问 URL，按 sortOrder 升序。
     */
    public abstract List<String> listEnabledUrls();

    /**
     * 上传新图：落盘后追加到当前排序末尾，默认启用。
     */
    public abstract LoginCarouselImageVO upload(MultipartFile file);

    public abstract void updateEnabled(Long id, boolean enabled);

    /**
     * 按前端传来的完整 id 顺序重写 sortOrder。要求 id 集合与库内完全一致，
     * 防止并发编辑下按过期列表排序造成部分记录顺序丢失。
     */
    public abstract void reorder(LoginImageReorderRequest request);

    /**
     * 删除记录（逻辑删除）并联动清理磁盘文件（清理失败不中断，见 storage 侧说明）。
     */
    public abstract void delete(Long id);
}
