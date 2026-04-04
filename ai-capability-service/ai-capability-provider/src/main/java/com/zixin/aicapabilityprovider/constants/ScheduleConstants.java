package com.zixin.aicapabilityprovider.constants;

import java.util.HashMap;
import java.util.Map;

public final class ScheduleConstants {

    private ScheduleConstants() {}

    public static final class Status {
        public static final String PENDING = "PENDING";
        public static final String IN_PROGRESS = "IN_PROGRESS";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";

        private static final Map<String, String> DESC_MAP = new HashMap<>();
        static {
            DESC_MAP.put(PENDING, "待执行");
            DESC_MAP.put(IN_PROGRESS, "进行中");
            DESC_MAP.put(COMPLETED, "已完成");
            DESC_MAP.put(CANCELLED, "已取消");
        }

        public static String getDesc(String status) {
            return DESC_MAP.getOrDefault(status, status);
        }

        public static String getAllOptions() {
            return "PENDING(待执行), IN_PROGRESS(进行中), COMPLETED(已完成), CANCELLED(已取消)";
        }
    }

    public static final class Priority {
        public static final int LOW = 1;
        public static final int MEDIUM = 2;
        public static final int HIGH = 3;
        public static final int URGENT = 4;

        private static final Map<Integer, String> DESC_MAP = new HashMap<>();
        static {
            DESC_MAP.put(LOW, "低优先级");
            DESC_MAP.put(MEDIUM, "中优先级");
            DESC_MAP.put(HIGH, "高优先级");
            DESC_MAP.put(URGENT, "紧急");
        }

        public static String getDesc(Integer priority) {
            if (priority == null) return "";
            return DESC_MAP.getOrDefault(priority, "");
        }

        public static String getAllOptions() {
            return "1(低优先级), 2(中优先级), 3(高优先级), 4(紧急)";
        }
    }

    public static final class ScheduleCategory {
        public static final int FOLLOW_UP = 1;
        public static final int CONSULTATION = 2;
        public static final int EXAMINATION = 3;
        public static final int MEDICATION = 4;
        public static final int EDUCATION = 5;
        public static final int OTHER = 99;

        private static final Map<Integer, String> DESC_MAP = new HashMap<>();
        static {
            DESC_MAP.put(FOLLOW_UP, "随访");
            DESC_MAP.put(CONSULTATION, "问诊");
            DESC_MAP.put(EXAMINATION, "检查");
            DESC_MAP.put(MEDICATION, "用药");
            DESC_MAP.put(EDUCATION, "健康教育");
            DESC_MAP.put(OTHER, "其他");
        }

        public static String getDesc(Integer category) {
            if (category == null) return "";
            return DESC_MAP.getOrDefault(category, "");
        }

        public static String getAllOptions() {
            return "1(随访), 2(问诊), 3(检查), 4(用药), 5(健康教育), 99(其他)";
        }
    }

    public static final class DefaultValues {
        public static final String DEFAULT_STATUS = Status.PENDING;
        public static final int DEFAULT_PRIORITY = Priority.MEDIUM;
        public static final int DEFAULT_CATEGORY = ScheduleCategory.OTHER;
        public static final String DEFAULT_SCHEDULE_DAY = "";
        public static final long DEFAULT_START_TIME = 0L;
        public static final long DEFAULT_END_TIME = 0L;
    }
}
