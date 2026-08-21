package com.richard.fyoung.customerwork.capability.assist;

import org.springframework.stereotype.Service;

/**
 * 坐席辅助（Agent Assist，借鉴 AliGo 坐席辅助）：根据用户消息给人工坐席实时话术建议 + 知识提示 + 工具推荐。
 *
 * <p>规则匹配、离线确定性；可作为人工坐席工作台的旁挂提示，不直接发送给用户。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AgentAssistService {

    /**
     * 按用户消息给出坐席建议（规则优先级：退款 &gt; 物流 &gt; 投诉 &gt; 发票 &gt; 默认）。
     */
    public abstract AssistSuggestion suggest(String userMessage);
}
