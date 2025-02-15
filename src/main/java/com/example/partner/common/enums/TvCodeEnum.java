package com.example.partner.common.enums;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public enum TvCodeEnum {

    POPULAR("POPULAR",  "popular:" +LocalDate.now()),
    DAILY("DAILY","daily:" + LocalDate.now()),
    WEEK("WEEK","week:" + LocalDate.now()),
    UNKNOWN("","");

    private final String type;
    private final String value;

    TvCodeEnum(String type,String value) {
        this.type = type;
        this.value = value;
    }

    public static String getValue(String type) {
        TvCodeEnum[] values = values();
        for (TvCodeEnum codeEnum : values) {
            if(type.equals(codeEnum.type)) {
                return codeEnum.value;
            }
        }
        return "";
    }

    public static TvCodeEnum getEnum(String type) {
        TvCodeEnum[] values = values();
        for (TvCodeEnum codeEnum : values) {
            if(type.equals(codeEnum.type)) {
                return codeEnum;
            }
        }
        return UNKNOWN;
    }
}
