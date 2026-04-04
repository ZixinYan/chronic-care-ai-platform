package com.zixin.aicapabilityprovider.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

/**
 * 将排班/排版技能目录解析为 SkillsTool 可用的文件系统路径。
 */
@Slf4j
public final class SkillsDirectoryResolver {

    private SkillsDirectoryResolver() {
    }

    /**
     * @param configuredProperty 来自 yml 的目录，可为空
     * @param classpathRelative  类路径下相对路径，如 {@code skills/skills}
     * @return 可用目录的绝对路径；无法解析时返回 {@code null}（调用方可跳过 SkillsTool）
     */
    public static String resolve(String configuredProperty, String classpathRelative) {
        if (configuredProperty != null && !configuredProperty.trim().isEmpty()) {
            return configuredProperty.trim();
        }
        try {
            return new ClassPathResource(classpathRelative).getFile().getAbsolutePath();
        } catch (Exception e) {
            log.warn(
                    "技能目录未配置且无法从 classpath 解析 [{}]，将不加载 SkillsTool。请在 application.yml 中设置",
                    classpathRelative,
                    e
            );
            return null;
        }
    }
}
