package com.zixin.utils.MyBatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * 元对象处理器（核心实现）
 */
@Component
public class CustomMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // 获取当前Unix毫秒时间戳
        long now = System.currentTimeMillis();

        // 设置创建时间（如果字段为空）
        this.strictInsertFill(metaObject, "createTime", Long.class, now);

        // 设置更新时间（如果字段为空）
        this.strictInsertFill(metaObject, "updateTime", Long.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 获取当前Unix毫秒时间戳
        long now = System.currentTimeMillis();

        // 设置更新时间（无论是否有值，都更新）
        this.setFieldValByName("updateTime", now, metaObject);
    }
}