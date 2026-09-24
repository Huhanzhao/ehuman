package com.ehuman.core.config;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.ehuman.dao.entity.TbGraphLogDo;
import com.ehuman.dao.service.TbGraphLogDoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class NodeStatePersistenceListener implements GraphLifecycleListener {

    @Autowired
    private TbGraphLogDoService tbGraphLogDoService;

    @Override
    public void onStart(String nodeId, Map<String, Object> state, RunnableConfig config) {
        log.debug("=========================图开始执行: {}=========================", nodeId);
    }

    @Override
    public void before(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
        // 注意：before 有4个参数，第4个是 curTime
        log.debug("=========================节点执行前: {}, 时间: {}=========================", nodeId, curTime);
        // 保存进入时的状态快照
        TbGraphLogDo tbGraphLogDo = new TbGraphLogDo()
                .setState(state.toString())
                .setNodeId(nodeId)
                .setThreadId(config.threadId().orElse(""));
        tbGraphLogDoService.save(tbGraphLogDo);
    }

    @Override
    public void after(String nodeId, Map<String, Object> state, RunnableConfig config, Long curTime) {
        // 注意：after 也有 curTime 参数
        log.debug("=========================节点执行后: {}, 时间: {}=========================", nodeId, curTime);
        // 保存出去时的状态快照
//        TbGraphLogDo tbGraphLogDo = new TbGraphLogDo()
//                .setState(state.toString())
//                .setNodeId(nodeId)
//                .setThreadId(config.threadId().orElse(""));
//        tbGraphLogDoService.save(tbGraphLogDo);
    }

    @Override
    public void onError(String nodeId, Map<String, Object> state, Throwable ex, RunnableConfig config) {
        // 注意：onError 的参数顺序是 (nodeId, state, ex, config)
        log.debug("=========================执行出错: {}, 错误: {}=========================", nodeId, ex.getMessage(), ex);
        // 保存错误状态
        TbGraphLogDo tbGraphLogDo = new TbGraphLogDo()
                .setState(state.toString())
                .setNodeId(nodeId)
                .setErrorMessage(ex.getMessage())
                .setThreadId(config.threadId().orElse(""));
        tbGraphLogDoService.save(tbGraphLogDo);
    }

    @Override
    public void onComplete(String nodeId, Map<String, Object> state, RunnableConfig config) {
        log.debug("=========================图执行完成: {}=========================", nodeId);
    }
}