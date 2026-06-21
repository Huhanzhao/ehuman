package com.anyfriend.core.service;

import com.anyfriend.core.job.JobManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CommonAuxiliaryService {

    @Autowired
    private JobManager jobManager;

    // todo 更新下次主动发送消息的信息
    public String updateNextSendMessageInfo() {
        return "";
    }
}
