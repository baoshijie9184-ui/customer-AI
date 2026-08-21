package com.richard.fyoung.customerwork.capability.slotfilling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.Map;
import java.util.Optional;

/**
 * 多轮槽位收集服务（借鉴 AliGo「事项收集智能体」）：按 (sessionId, form) 维护收集进度，
 * 每轮抽取/补全后给出"还缺哪个→追问"或"齐了→可执行"。
 *
 * <p>存储委托给 {@link SlotFillingStore} SPI：默认 {@link InMemorySlotFillingStore}（进程内），
 * 生产可声明自己的实现（如 JDBC / Redis）覆盖默认，保证重启不丢失收集进度。</p>
 *
 * <p>抽取规则（确定性、可离线测）：</p>
 * <ol>
 *   <li>若上一轮在追问某<b>自由文本</b>槽位，则本轮整句作为其值；</li>
 *   <li>对所有<b>带正则</b>且未填的槽位，尝试从本轮文本抽取；</li>
 *   <li>取第一个 required 且未填的槽位追问；全填则完成并清理会话状态。</li>
 * </ol>
 * @author owlzhangfq@gmail.com
 */
public interface SlotFillingService {

    /**
     * 提交一轮用户输入，推进表单收集。
     */
    public abstract SlotFillingResult submit(String sessionId, SlotFillingForm form, String userText);

    /**
     * 放弃当前会话的某表单收集（用户中途取消）。
     */
    public abstract void reset(String sessionId, String formName);

    /**
     * 只读窥视某会话某表单的当前收集进度（供故障诊断，不推进/不创建状态）。
     */
    public abstract Optional<SlotFillingProgress> peek(String sessionId, String formName);
}
