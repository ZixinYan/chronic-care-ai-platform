package com.zixin.utils.MyBatis;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.text.SimpleDateFormat;
import java.util.Properties;

/**
 * MyBatis原生SQL监控拦截器
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update",
                args = {MappedStatement.class, Object.class})
})
public class SqlMonitorInterceptor implements Interceptor {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private long slowSqlThreshold = 3000L;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            // 执行原方法
            Object result = invocation.proceed();

            // 计算执行时间
            long costTime = System.currentTimeMillis() - startTime;

            // 获取SQL信息
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];

            // 获取BoundSql
            BoundSql boundSql = ms.getBoundSql(parameter);
            String sql = boundSql.getSql();

            // 记录日志
            logSql(ms, sql, parameter, costTime);

            return result;

        } catch (Exception e) {
            log.error("SQL执行异常", e);
            throw e;
        }
    }

    private void logSql(MappedStatement ms, String sql, Object parameter, long costTime) {
        try {
            // 格式化SQL
            sql = sql.replaceAll("\\s+", " ").trim();

            // 获取方法ID
            String methodId = ms.getId();

            // 构建日志
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("\n==================== SQL监控 ====================");
            logBuilder.append("\n方法ID    ：").append(methodId);
            logBuilder.append("\n操作类型  ：").append(ms.getSqlCommandType());
            logBuilder.append("\n执行耗时  ：").append(costTime).append(" ms");

            if (costTime > slowSqlThreshold) {
                logBuilder.append(" 注意慢查询");
            }

            logBuilder.append("\n原始SQL   ：").append(sql);

            if (parameter != null) {
                logBuilder.append("\n参数      ：").append(formatParameter(parameter));
            }

            logBuilder.append("\n================================================");

            if (costTime > slowSqlThreshold) {
                log.warn(logBuilder.toString());
            } else {
                log.info(logBuilder.toString());
            }

        } catch (Exception e) {
            log.error("记录SQL日志异常", e);
        }
    }

    private String formatParameter(Object parameter) {
        // ... 相同的参数格式化代码
        if (parameter == null) {
            return "null";
        }
        // 复制之前的formatParameter方法
        return parameter.toString();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        String threshold = properties.getProperty("slowSqlThreshold");
        if (threshold != null) {
            this.slowSqlThreshold = Long.parseLong(threshold);
        }
    }
}