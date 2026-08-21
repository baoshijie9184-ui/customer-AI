package com.richard.fyoung.customeradmin.dict.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.richard.fyoung.customeradmin.common.exception.BizException;
import com.richard.fyoung.customeradmin.common.result.ResultCode;
import com.richard.fyoung.customeradmin.dict.config.DictGatewayProvider;
import com.richard.fyoung.customeradmin.dict.dto.DictItemSaveRequest;
import com.richard.fyoung.customeradmin.dict.dto.DictItemVO;
import com.richard.fyoung.customeradmin.dict.dto.DictOptionVO;
import com.richard.fyoung.customeradmin.dict.dto.DictTypeSaveRequest;
import com.richard.fyoung.customeradmin.dict.dto.DictTypeVO;
import com.richard.fyoung.customeradmin.dict.jdbc.DictGateway;
import com.richard.fyoung.customerwork.data.dict.entity.DictItemEntity;
import com.richard.fyoung.customerwork.data.dict.entity.DictTypeEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * 字典管理：类型 + 字典项两级 CRUD，及消费端下拉选项查询。
 *
 * <p><b>全量取回、不做分页 SQL，是刻意的</b>：字典本来就是"就几条数据、不值当建表"的场景，
 * 单类型几条到几十条、类型总数也就几十个；真涨到需要分页的量级，说明这数据不该进字典。</p>
 *
 * <p>写的是客服端库 {@code cw_dict_type} / {@code cw_dict_item}（单一数据真源），客服端
 * {@code DictStore}（store-mode=jdbc）读同两张表，改动即时可见，无缓存同步问题。</p>
 * @author owlzhangfq@gmail.com
 */
public interface DictService {

    /**
     * 全部字典类型（含停用，编码升序），附各类型的字典项数量。
     */
    public abstract List<DictTypeVO> listTypes();

    /**
     * 新增类型；编码唯一冲突抛 {@link ResultCode#RESOURCE_DUPLICATE}。
     */
    public abstract void createType(DictTypeSaveRequest request);

    /**
     * 编辑类型（名称/备注/启停）；类型编码不允许变更。
     */
    public abstract void updateType(Long id, DictTypeSaveRequest request);

    /**
     * 删除类型；仍有字典项时拒绝（先清空项，避免留下孤儿项）。
     */
    public abstract void deleteType(Long id);

    /**
     * 某类型下全部字典项（含停用，sort 升序）。
     */
    public abstract List<DictItemVO> listItems(String dictType);

    /**
     * 新增字典项；同类型下键唯一冲突抛 {@link ResultCode#RESOURCE_DUPLICATE}。
     */
    public abstract void createItem(String dictType, DictItemSaveRequest request);

    /**
     * 编辑字典项（键/文案/排序/启停/备注）；不允许挪类型。改键时校验同类型下唯一。
     */
    public abstract void updateItem(Long id, DictItemSaveRequest request);

    /**
     * 删除字典项。
     */
    public abstract void deleteItem(Long id);

    /**
     * 某类型下启用项的下拉选项（sort 升序）；类型停用或不存在返回空列表（消费端按无字典配置降级）。
     */
    public abstract List<DictOptionVO> options(String dictType);
}
