package com.helper.commonservicescommon.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.helper.commonservicescommon.utils.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    /**
     * 插入时的填充策略
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        String userId = UserContext.getUserId();
        if (!userId.isEmpty()){
            this.setFieldValByName("createBy", userId, metaObject);
            this.setFieldValByName("updateBy", userId, metaObject);
        }
        this.setFieldValByName("createTime", new Date(), metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }
   
    /**
     * 更新时的填充策略
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        String userId = UserContext.getUserId();
        if (!userId.isEmpty()){
            this.setFieldValByName("updateBy", userId, metaObject);
        }
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }
}