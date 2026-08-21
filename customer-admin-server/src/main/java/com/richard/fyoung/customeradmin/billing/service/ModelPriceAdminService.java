package com.richard.fyoung.customeradmin.billing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.richard.fyoung.customeradmin.billing.entity.AiModelPrice;
import com.richard.fyoung.customeradmin.billing.mapper.AiModelPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 模型单价维护。
 *
 * <p><b>只增不改</b>：调价插一条新的生效记录，旧记录留着让历史账单算得回去。
 * 因此这里没有 update——真要改错录的价，删掉那条再插新的。</p>
 * @author owlzhangfq@gmail.com
 */
public interface ModelPriceAdminService {

    public abstract List<AiModelPrice> list();

    public abstract Long create(AiModelPrice request);

    public abstract void delete(Long id);
}
