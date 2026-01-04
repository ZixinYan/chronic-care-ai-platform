package com.zixin.accountprovider.consts;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class Enum {
    /**
     * 更新字段校验
     */
    public enum UpdateUserField {
        nickname, gender, avatarUrl, address, birthday;

        private static final Set<String> FIELD_NAMES =
                Arrays.stream(UpdateUserField.values())
                        .map(UpdateUserField::name)
                        .collect(Collectors.toSet());

        public static boolean contains(String field) {
            return FIELD_NAMES.contains(field);
        }
    }
}
