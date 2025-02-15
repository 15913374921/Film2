package com.example.partner.common.enums;

import com.example.partner.common.Constants;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public enum MovieCodeEnum {

    POPULAR("POPULAR",  "popular:" + LocalDate.now()),
    DAILY("DAILY","daily:" + LocalDate.now()),
    WEEK("WEEK","week:" + LocalDate.now()),
    UNKNOWN("","");

    private final String type;
    private final String value;

    MovieCodeEnum(String type,String value) {
        this.type = type;
        this.value = value;
    }

    public static String getValue(String type) {
        MovieCodeEnum[] values = values();
        for (MovieCodeEnum codeEnum : values) {
            if(type.equals(codeEnum.type)) {
                return codeEnum.value;
            }
        }
        return "";
    }

    public static MovieCodeEnum getEnum(String type) {
        MovieCodeEnum[] values = values();
        for (MovieCodeEnum codeEnum : values) {
            if(type.equals(codeEnum.type)) {
                return codeEnum;
            }
        }
        return UNKNOWN;
    }
}
