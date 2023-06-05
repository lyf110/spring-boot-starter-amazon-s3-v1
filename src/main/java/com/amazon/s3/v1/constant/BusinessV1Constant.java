package com.amazon.s3.v1.constant;

import java.time.format.DateTimeFormatter;

/**
 * @author liuyangfang
 * @description 常量类
 * @since 2023/5/31 14:23:42
 */
public class BusinessV1Constant {
    public static final DateTimeFormatter FILE_NAME_PATTERN = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    public static final String FILE_SEPARATOR = "/";
    public static final String FILENAME_LINK = "-";
    public static final String DEFAULT_UPLOAD_BASE_DIR = "uploads";
}
