package com.amazon.s3.v1.core;

import com.amazonaws.services.s3.model.*;

import java.util.List;
import java.util.Optional;

/**
 * @author liuyangfang
 * @description 关于Amazon操作Bucket桶的API相关接口
 * @since 2023/5/31 14:36:11
 */
public interface IAmazonS3V1Bucket {
    /**
     * 创建桶
     *
     * @param bucketName 桶名称
     * @return 创建结果
     */
    Optional<Bucket> createBucket(String bucketName);


    /**
     * 获取指定桶名称的桶对象
     *
     * @param bucketName 桶名称
     * @return 桶对象
     */
    Optional<Bucket> getBucketByName(String bucketName);

    /**
     * 获取所有的桶集合
     *
     * @return 所有的桶集合
     */
    Optional<List<Bucket>> getBucketList();


    /**
     * 删除桶，这里需要先删除桶中的文件，然后才能删除桶
     *
     * @param bucketName 桶名称
     * @return 删除结果
     */
    boolean deleteBucket(String bucketName);


    /**
     * 配置跨域信息
     *
     * @param bucketName   桶对象
     * @param corsRuleList 跨域规则
     */
    void setBucketCrossOriginConfiguration(String bucketName, List<CORSRule> corsRuleList);


    /**
     * 删除跨域配置信息
     *
     * @param bucketName 桶
     */
    void deleteBucketCrossOriginConfiguration(String bucketName);

    /**
     * 获取指定桶的跨域信息配置
     *
     * @param bucketName 桶名称
     * @return 指定桶的跨域信息配置
     */
    Optional<BucketCrossOriginConfiguration> getBucketCrossOriginConfiguration(String bucketName);


    /**
     * 删除桶的策略信息
     *
     * @param bucketName 桶名称
     */
    void deleteBucketPolicy(String bucketName);

    /**
     * 获取桶的策略信息
     *
     * @param bucketName 桶名称
     * @return 策略信息
     */
    Optional<BucketPolicy> getBucketPolicy(String bucketName);

    /**
     * 设置桶的生命周期规则
     *
     * @param bucketName                   桶的名字
     * @param bucketLifecycleConfiguration 桶的生命周期规则
     */
    void setBucketLifecycleConfiguration(String bucketName, BucketLifecycleConfiguration bucketLifecycleConfiguration);


    /**
     * 获取桶的生命周期规则
     *
     * @param bucketName 桶的名称
     * @return 桶的生命周期规则
     */
    Optional<BucketLifecycleConfiguration> getBucketLifecycleConfiguration(String bucketName);
}
