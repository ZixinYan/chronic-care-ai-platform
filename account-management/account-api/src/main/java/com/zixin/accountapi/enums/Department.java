package com.zixin.accountapi.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum Department {

    INTERNAL("INTERNAL", "内科"),
    ENDOCRINE("ENDOCRINE", "内分泌科"),
    CARDIOVASCULAR("CARDIOVASCULAR", "心血管科"),
    NEUROLOGY("NEUROLOGY", "神经内科"),
    GENERAL("GENERAL", "全科");

    private static final Map<String, Department> CODE_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(Department::getCode, d -> d));

    private static final Map<String, Department> NAME_MAP =
            Arrays.stream(values())
                    .collect(Collectors.toMap(Department::getName, d -> d));

    private final String code;
    private final String name;

    Department(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static Department fromCode(String code) {
        if (code == null) {
            return null;
        }
        return CODE_MAP.get(code);
    }

    public static Department fromName(String name) {
        if (name == null) {
            return null;
        }
        return NAME_MAP.get(name);
    }
}
