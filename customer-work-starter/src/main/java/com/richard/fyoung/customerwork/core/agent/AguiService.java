package com.richard.fyoung.customerwork.core.agent;

import com.richard.fyoung.customerwork.infra.config.CustomerWorkProperties;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.converter.AguiMessageConverter;
import io.agentscope.core.agui.encoder.AguiEventEncoder;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.richard.fyoung.customerwork.infra.config.properties.ProtocolProperties;

/**
 * AG-UI 协议服务（对应「交互协议 · AG-UI」）。
 *
 * <p>把会话 Agent 适配为标准 AG-UI 事件流：前端按 AG-UI 协议发起 {@link RunAgentInput}，
 * 服务端经 {@link AguiAgentAdapter} 产出类型化 {@link io.agentscope.core.agui.event.AguiEvent}
 * （消息开始/增量/结束、工具调用、状态变更等），再由 {@link AguiEventEncoder} 编码为 SSE 文本下发。
 * 这样任意兼容 AG-UI 的前端均可直接对接，无需自定义协议。</p>
 * @author owlzhangfq@gmail.com
 */
public interface AguiService {

    /**
     * 以 AG-UI 协议运行一轮对话，返回编码后的 SSE 事件文本流。
     */
    public abstract Flux<String> run(String sessionId, String userText);
}
