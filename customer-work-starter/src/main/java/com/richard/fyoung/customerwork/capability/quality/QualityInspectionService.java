package com.richard.fyoung.customerwork.capability.quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话质检（借鉴 AliGo 质检/数据飞轮）：对坐席/Agent 的回复做合规与服务规范打分，离线确定性。
 *
 * <p>扣分项：</p>
 * <ul>
 *   <li><b>资金违规承诺</b>（已打款 / 立即到账 / 保证退 / 一定赔）——严重，单条 -40 且直接判不通过；</li>
 *   <li><b>绝对化表述</b>（绝对 / 100% / 肯定没问题）——单条 -10；</li>
 *   <li><b>服务禁语</b>（不知道 / 不可能 / 你自己）——单条 -15。</li>
 * </ul>
 * @author owlzhangfq@gmail.com
 */
public interface QualityInspectionService {

    /**
     * 对一组（坐席/Agent）回复做质检。
     */
    public abstract QualityReport inspect(List<String> replies);
}
