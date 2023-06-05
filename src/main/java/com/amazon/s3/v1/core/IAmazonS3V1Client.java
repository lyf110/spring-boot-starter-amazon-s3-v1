package com.amazon.s3.v1.core;

import com.amazonaws.services.s3.AmazonS3;

/**
 * @author liuyangfang
 * @description 封装了常用的Amazon S3的操作方法
 * @since 2023/5/31 14:56:58
 */
public interface IAmazonS3V1Client extends IAmazonS3V1Bucket, IAmazonS3V1Object {

    /**
     * 获取AmazonS3客户端
     *
     * @return AmazonS3客户端
     */
    AmazonS3 getS3Client();


    /**
     * 获取默认的桶名称
     *
     * @return 默认的桶名称
     */
    String getDefaultBucketName();
}
