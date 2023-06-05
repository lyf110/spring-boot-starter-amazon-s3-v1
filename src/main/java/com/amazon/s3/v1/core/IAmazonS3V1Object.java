package com.amazon.s3.v1.core;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.model.*;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * @author liuyangfang
 * @description 关于Amazon S3 操作Object对象的API相关接口
 * @since 2023/5/31 14:37:08
 */
public interface IAmazonS3V1Object {

    /**
     * 列出桶中的所有对象信息
     *
     * @param bucketName 桶名称
     * @return 桶中的所有对象信息
     * @throws SdkClientException SdkClientException
     */
    Optional<ObjectListing> listObjects(String bucketName) throws SdkClientException;


    /**
     * 列出桶中指定前缀的所有对象信息
     *
     * @param bucketName 桶名称
     * @param prefix     对象前缀
     * @return 桶中指定前缀的所有对象信息
     * @throws SdkClientException SdkClientException
     */
    Optional<ObjectListing> listObjects(String bucketName, String prefix)
            throws SdkClientException;


    /**
     * 列出桶中的所有对象信息
     *
     * @param listObjectsRequest listObjectsRequest
     * @return 桶中的所有对象信息
     * @throws SdkClientException SdkClientException
     */
    Optional<ObjectListing> listObjects(ListObjectsRequest listObjectsRequest)
            throws SdkClientException;

    /**
     * 列出桶中的所有对象信息V2版本
     *
     * @param bucketName 桶名称
     * @return 桶中的所有对象信息
     * @throws SdkClientException SdkClientException
     */
    Optional<ListObjectsV2Result> listObjectsV2(String bucketName) throws SdkClientException;

    /**
     * 列出桶中指定前缀的所有对象信息V2版本
     *
     * @param bucketName 桶名称
     * @param prefix     对象前缀
     * @return 桶中指定前缀的所有对象信息
     * @throws SdkClientException SdkClientException
     */
    Optional<ListObjectsV2Result> listObjectsV2(String bucketName, String prefix) throws SdkClientException;


    /**
     * 列出桶中的所有对象信息V2版本
     *
     * @param listObjectsV2Request listObjectsV2Request
     * @return 桶中的所有对象信息
     * @throws SdkClientException SdkClientException
     */
    Optional<ListObjectsV2Result> listObjectsV2(ListObjectsV2Request listObjectsV2Request) throws SdkClientException;

    /**
     * 对象拷贝方法, 拷贝到目标桶中的对象名相同
     *
     * @param srcBucketName  源对象所在的桶
     * @param srcObjectName  源对象名称
     * @param destBucketName 目标对象所在的桶
     * @return 复制结果
     */
    Optional<CopyObjectResult> copyObject(String srcBucketName, String srcObjectName, String destBucketName);


    /**
     * 对象拷贝的参数为对象的封装方法
     *
     * @param copyObjectRequest 拷贝的对象参数封装
     * @return 拷贝结果
     */
    Optional<CopyObjectResult> copyObject(CopyObjectRequest copyObjectRequest);


    /**
     * 对象拷贝方法
     *
     * @param srcBucketName  源对象所在的桶
     * @param srcObjectName  源对象名称
     * @param destBucketName 目标对象所在的桶
     * @param destObjectName 目标对象
     * @return 复制结果
     */
    Optional<CopyObjectResult> copyObject(String srcBucketName, String srcObjectName, String destBucketName, String destObjectName);


    /**
     * 删除一个或多个对象
     *
     * @param deleteObjectsRequest 删除的对象的参数封装
     */
    Optional<DeleteObjectsResult> deleteObjects(DeleteObjectsRequest deleteObjectsRequest);


    /**
     * 删除多个对象，这些对象没有使用多版本控制
     *
     * @param bucketName  桶名称
     * @param keyNameList 对象名称集合
     * @return 删除结果
     */
    Optional<DeleteObjectsResult> deleteObjectsWithNoVersionBucket(
            String bucketName,
            List<String> keyNameList);

    /**
     * 删除多个对象，这些对象没有使用多版本控制
     *
     * @param bucketName  桶名称
     * @param keyNameList 对象名称集合
     * @param quiet       是否启用安静模式，启用的话，就只报告错误，默认为false
     * @return 删除结果
     */
    Optional<DeleteObjectsResult> deleteObjectsWithNoVersionBucket(
            String bucketName,
            List<String> keyNameList,
            boolean quiet);


    /**
     * 删除多个对象
     *
     * @param bucketName        桶名称
     * @param objectVersionList 对象版本信息集合
     * @return 删除结果
     */
    Optional<DeleteObjectsResult> deleteObjects(
            String bucketName,
            List<DeleteObjectsRequest.KeyVersion> objectVersionList);

    /**
     * 删除多个对象
     *
     * @param bucketName        桶名称
     * @param objectVersionList 对象版本信息集合
     * @param quiet             是否启用安静模式，启用的话，就只报告错误，默认为false
     * @return 删除结果
     */
    Optional<DeleteObjectsResult> deleteObjects(
            String bucketName,
            List<DeleteObjectsRequest.KeyVersion> objectVersionList,
            boolean quiet);


    /**
     * 删除一个对象
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     */
    void deleteObject(String bucketName, String objectName);

    /**
     * 删除一个对象
     *
     * @param deleteObjectRequest deleteObjectRequest
     */
    void deleteObject(DeleteObjectRequest deleteObjectRequest);

    /**
     * 删除对象的版本
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param versionId  版本ID
     */
    void deleteVersion(String bucketName, String objectName, String versionId);

    /**
     * 删除对象的版本
     *
     * @param deleteVersionRequest deleteVersionRequest
     */
    void deleteVersion(DeleteVersionRequest deleteVersionRequest);


    /**
     * 上传一个文本内容
     *
     * @param bucketName 桶名称
     * @param baseDir    文件上传到服务器存储的根路径
     * @param objectName 对象名称 对象键名称是 Unicode 字符序列，它采用 UTF-8 编码，
     *                   长度最大为 1,024 字节。
     * @param content    对象内容
     */
    boolean upload(String bucketName, String objectName, String baseDir, String content);


    /**
     * 上传一个文本内容
     * baseDir不指定的话默认使用uploads
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称 对象键名称是 Unicode 字符序列，它采用 UTF-8 编码，
     *                   长度最大为 1,024 字节。
     * @param content    对象内容
     */
    boolean upload(String bucketName, String objectName, String content);


    /**
     * 上传一个文件
     *
     * @param bucketName 桶名称
     * @param file       文件对象
     * @param baseDir    文件上传到服务器存储的根路径
     * @param metadata   文件的元数据
     */
    boolean upload(String bucketName, File file, String baseDir, ObjectMetadata metadata);


    /**
     * 低级别的分段上传的API
     * 低级别 API 适用于底层 Amazon S3 REST 操作，例如创建、更新和删除适用于存储桶和对象的操作。
     * 如果使用低级别分段上传 API 上传大型对象，它可以提供更好的控制。
     * 例如，可使用它暂停和恢复分段上传，在上传期间更改分段的大小，或者在事先不知道数据大小的情况下开始上传。
     * 如果您没有这些要求，请使用高级别 API 上传对象。
     *
     * @param bucketName 桶名称
     * @param file       文件名
     * @param baseDir    存储的基础路径
     * @return 上传结果 true: 成功， false: 失败
     */
    boolean multipartUploadV1(String bucketName, File file, String baseDir);


    /**
     * 高级别的分段上传的API
     * 对于上传对象，开发工具包通过提供 TransferManager 类来提供更高级别的抽象。
     * 高级别 API 是更简单的 API，其中只需几行代码，您即可将文件和流上传到 Amazon S3。
     * 除非您需要控制上传 (如前面的低级别 API 部分所述)，否则您应该使用此 API 来上传数据。
     * <p>
     * 对于较小的数据，可使用 TransferManager API 通过单个操作上传数据。
     * 但是，当数据大小达到特定的阈值后，TransferManager 会转为使用分段上传 API。
     * 如果可能，TransferManager 会使用多个线程来并发上传分段。
     * 如果分段上传失败，API 最多会重试三次失败的分段上传。
     * 但是，可以使用 TransferManagerConfiguration 类来配置这些选项。
     * <p>
     * 注意
     * 如果您将流用作数据源，TransferManager 类不会执行并发上传。
     *
     * @param bucketName 桶名称
     * @param file       文件名
     * @param baseDir    存储的基础路径
     * @return 上传结果 true: 成功， false: 失败
     */
    boolean multipartUploadV2(String bucketName, File file, String baseDir);


    /**
     * 获取对象
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 对象信息
     * @throws SdkClientException SdkClientException
     */
    Optional<S3Object> getObject(String bucketName, String objectName) throws SdkClientException;


    /**
     * 获取桶中所有的对象名称
     *
     * @param bucketName 存储的桶的名称
     * @param maxKeys    可选参数，指示要包含在响应中的最大键数。
     *                   Amazon S3可能返回的数量少于此数量，但不会返回更多。
     *                   即使未指定maxKeys，Amazon S3也会限制响应中的结果数量。
     * @return 桶中所有的对象名称
     */
    Optional<List<String>> getObjectNameList(String bucketName, int maxKeys);


    /**
     * 获取桶中所有的对象名称
     *
     * @param bucketName 存储的桶的名称
     * @param maxKeys    可选参数，指示要包含在响应中的最大键数。
     *                   Amazon S3可能返回的数量少于此数量，但不会返回更多。
     *                   即使未指定maxKeys，Amazon S3也会限制响应中的结果数量。
     * @return 桶中所有的对象名称
     */
    Optional<List<S3ObjectSummary>> getObjectInfo(String bucketName, int maxKeys);

    /**
     * 获取预签名的get请求url
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param expireTime 过期时间
     * @return 预签名的url
     */
    Optional<String> getPresignedObjectUrl(String bucketName, String objectName, Duration expireTime);


    /**
     * 获取预签名的get请求url
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param expireTime 过期时间
     * @param httpMethod 指定请求的方法
     * @return 预签名的url
     */
    Optional<String> getPresignedObjectUrl(String bucketName, String objectName, Duration expireTime, HttpMethod httpMethod);


    /**
     * 获取预签名的get请求url
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param expireTime 过期时间
     * @return 预签名的url
     */
    Optional<String> getPresignedObjectPutUrl(String bucketName, String objectName, Duration expireTime);
}
