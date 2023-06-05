package com.amazon.s3.v1.template;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.amazon.s3.v1.config.AmazonS3V1Properties;
import com.amazon.s3.v1.constant.BusinessV1Constant;
import com.amazon.s3.v1.core.IAmazonS3V1Client;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.Protocol;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.amazon.s3.v1.constant.BusinessV1Constant.*;

/**
 * @author liuyangfang
 * @description 操作AmazonS3存储服务器的模板工具类
 * @since 2023/5/31 19:36:18
 */
@Slf4j
public class AmazonS3V1Template implements IAmazonS3V1Client {
    private AmazonS3 s3Client;

    @Autowired
    private AmazonS3V1Properties amazonS3Properties;

    @PostConstruct
    public void init() throws MalformedURLException {
        String endPoint = amazonS3Properties.getEndPoint();
        URL endpointUrl = new URL(endPoint);
        String protocol = endpointUrl.getProtocol();

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setSignerOverride("S3SignerType");
        clientConfig.setProtocol(Protocol.valueOf(protocol.toUpperCase()));

        // 禁用证书检查，避免https自签证书校验失败
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
        // 屏蔽 AWS 的 MD5 校验，避免校验导致的下载抛出异常问题
        System.setProperty("com.amazonaws.services.s3.disableGetObjectMD5Validation", "true");
        AWSCredentials awsCredentials = new BasicAWSCredentials(amazonS3Properties.getAccessKey(), amazonS3Properties.getSecretKey());
        // 创建 S3Client 实例
        s3Client = AmazonS3ClientBuilder.standard()
                // .withRegion(Regions.CN_NORTH_1) 这里和withEndpointConfiguration只能二选1
                .withPathStyleAccessEnabled(true)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, "minio"))
                .build();

        // 创建默认的桶
        String bucketName = amazonS3Properties.getBucketName();
        createBucket(bucketName);
    }

    /**
     * 获取默认的桶
     *
     * @return default BucketName
     */
    @Override
    public String getDefaultBucketName() {
        return amazonS3Properties.getBucketName();
    }


    /**
     * 桶是否存在
     *
     * @param bucketName 桶名
     * @return 是否存在
     */
    public boolean bucketExists(String bucketName) {
        if (StrUtil.isEmpty(bucketName)) {
            return false;
        }

        bucketName = handlerBucketName(bucketName);
        try {
            return s3Client.doesBucketExistV2(bucketName);
        } catch (SdkClientException e) {
            log.error("bucketExists failed, the cause is ", e);
            return false;
        }
    }


    @Override
    public Optional<Bucket> createBucket(String bucketName) {
        // 非空校验
        if (StrUtil.isEmpty(bucketName)) {
            log.warn("bucketName not empty");
            return Optional.empty();
        }

        // 将桶的名称转成小写，因为Amazon S3 不支持大写名称的桶名称
        bucketName = handlerBucketName(bucketName);


        // 创建桶
        try {
            if (bucketExists(bucketName)) {
                log.info("Bucket {} already exists", bucketName);
                return getBucketByName(bucketName);
            }

            Bucket bucket = s3Client.createBucket(bucketName);
            log.info("create bucket {} success", bucketName);

            // Verify that the bucket was created by retrieving it and checking its location.
            String bucketLocation = s3Client.getBucketLocation(new GetBucketLocationRequest(bucketName));
            log.info("bucket location {}", bucketLocation);
            return Optional.ofNullable(bucket);
        } catch (SdkClientException e) {
            e.printStackTrace();
            log.error("create bucket {} failed", bucketName);
            return Optional.empty();
        }
    }


    @Override
    public Optional<Bucket> getBucketByName(String bucketName) {
        List<Bucket> bucketList = s3Client.listBuckets();
        if (bucketList == null || bucketList.isEmpty()) {
            log.info("还未创建桶对象");
            return Optional.empty();
        }

        for (Bucket bucket : bucketList) {
            if (bucket.getName().equals(bucketName)) {
                return Optional.of(bucket);
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<List<Bucket>> getBucketList() {
        List<Bucket> buckets = s3Client.listBuckets();
        return Optional.ofNullable(buckets);
    }


    @Override
    public boolean deleteBucket(String bucketName) {
        try {
            // 先删除桶中的文件
            ObjectListing objectListing = s3Client.listObjects(bucketName);
            while (true) {
                for (S3ObjectSummary s3ObjectSummary : objectListing.getObjectSummaries()) {
                    s3Client.deleteObject(bucketName, s3ObjectSummary.getKey());
                }

                // 如果存储桶包含许多对象，则listObjects() 调用可能不会返回第一个列表中的所有对象。
                // 检查列表是否被截断。如果是这样，则检索对象的下一页并删除它们。
                if (objectListing.isTruncated()) {
                    objectListing = s3Client.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }

            // 删除所有对象版本 (如果是多版本存储的对象桶)。
            VersionListing versionList = s3Client.listVersions(new ListVersionsRequest().withBucketName(bucketName));
            while (true) {
                for (S3VersionSummary vs : versionList.getVersionSummaries()) {
                    s3Client.deleteVersion(bucketName, vs.getKey(), vs.getVersionId());
                }

                if (versionList.isTruncated()) {
                    versionList = s3Client.listNextBatchOfVersions(versionList);
                } else {
                    break;
                }
            }

            // 删除所有对象和对象版本后，删除bucket。
            s3Client.deleteBucket(bucketName);
        } catch (SdkClientException e) {
            log.error("delete bucket failed, the cause is ", e);
            return false;
        }

        return true;
    }

    @Override
    public void setBucketCrossOriginConfiguration(String bucketName, List<CORSRule> corsRuleList) {
        if (StrUtil.isEmpty(bucketName) || CollectionUtil.isEmpty(corsRuleList)) {
            return;
        }

        try {
            BucketCrossOriginConfiguration configuration = new BucketCrossOriginConfiguration();
            configuration.setRules(corsRuleList);

            s3Client.setBucketCrossOriginConfiguration(bucketName, configuration);
        } catch (Exception e) {
            log.error("config bucket cross failed, the cause is ", e);
        }
    }

    @Override
    public void deleteBucketCrossOriginConfiguration(String bucketName) {
        try {
            s3Client.deleteBucketCrossOriginConfiguration(bucketName);
            log.info("delete bucket cross config success");
        } catch (Exception e) {
            log.error("delete bucket cross config failed, the cause is ", e);
        }
    }

    @Override
    public Optional<BucketCrossOriginConfiguration> getBucketCrossOriginConfiguration(String bucketName) {
        if (isEmpty(bucketName)) {
            log.warn("get bucket cross config failed, because the bucket name is empty");
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(s3Client.getBucketCrossOriginConfiguration(bucketName));
        } catch (Exception e) {
            log.error("get bucket cross config failed, the cause is ", e);
            return Optional.empty();
        }
    }

    @Override
    public void deleteBucketPolicy(String bucketName) {
        if (StrUtil.isEmpty(bucketName)) {
            log.warn("delete bucket policy failed, because the bucket name is empty");
            return;
        }

        try {
            s3Client.deleteBucketPolicy(bucketName);
        } catch (SdkClientException e) {
            log.error("delete bucket policy failed, the cause is ", e);
        }
    }

    @Override
    public Optional<BucketPolicy> getBucketPolicy(String bucketName) {
        if (StrUtil.isEmpty(bucketName)) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(s3Client.getBucketPolicy(bucketName));
        } catch (SdkClientException e) {
            log.error("get bucket policy failed, the cause is ", e);
            return Optional.empty();
        }
    }

    @Override
    public void setBucketLifecycleConfiguration(String bucketName, BucketLifecycleConfiguration bucketLifecycleConfiguration) {
        try {
            s3Client.setBucketLifecycleConfiguration(bucketName, bucketLifecycleConfiguration);
            log.error("set bucket {} lifecycle success", bucketName);
        } catch (Exception e) {
            log.error("set bucket {} lifecycle failed, the cause is ", bucketName, e);
        }
    }

    @Override
    public Optional<BucketLifecycleConfiguration> getBucketLifecycleConfiguration(String bucketName) {
        try {
            return Optional.ofNullable(s3Client.getBucketLifecycleConfiguration(bucketName));
        } catch (Exception e) {
            log.error("get bucket {} lifecycle failed, the cause is ", bucketName, e);
            return Optional.empty();
        }
    }


    @Override
    public Optional<ObjectListing> listObjects(String bucketName) throws SdkClientException {
        if (StrUtil.isEmpty(bucketName)) {
            return Optional.empty();
        }

        // 将bucketName转成小写
        bucketName = handlerBucketName(bucketName);

        try {
            return Optional.ofNullable(s3Client.listObjects(bucketName));
        } catch (SdkClientException e) {
            log.error("get bucket {} object list failed, the cause is ", bucketName, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ObjectListing> listObjects(String bucketName, String prefix) throws SdkClientException {
        if (StrUtil.isEmpty(bucketName)) {
            return Optional.empty();
        }

        // 将bucketName转成小写
        bucketName = handlerBucketName(bucketName);

        try {
            if (StrUtil.isEmpty(prefix)) {
                return listObjects(bucketName);
            }

            return Optional.ofNullable(s3Client.listObjects(bucketName, prefix));
        } catch (SdkClientException e) {
            log.error("get bucket {} , prefix {}, object list failed, the cause is ", bucketName, prefix, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ObjectListing> listObjects(ListObjectsRequest listObjectsRequest) throws SdkClientException {
        if (listObjectsRequest == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(s3Client.listObjects(listObjectsRequest));
    }

    @Override
    public Optional<ListObjectsV2Result> listObjectsV2(String bucketName) throws SdkClientException {
        if (StrUtil.isEmpty(bucketName)) {
            return Optional.empty();
        }

        // 将bucketName转成小写
        bucketName = handlerBucketName(bucketName);

        try {
            return Optional.ofNullable(s3Client.listObjectsV2(bucketName));
        } catch (SdkClientException e) {
            log.error("get bucket {} object list failed, the cause is ", bucketName, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ListObjectsV2Result> listObjectsV2(String bucketName, String prefix) throws SdkClientException {
        if (StrUtil.isEmpty(bucketName)) {
            return Optional.empty();
        }

        // 将bucketName转成小写
        bucketName = handlerBucketName(bucketName);

        try {
            if (StrUtil.isEmpty(prefix)) {
                return listObjectsV2(bucketName);
            }

            return Optional.ofNullable(s3Client.listObjectsV2(bucketName, prefix));
        } catch (SdkClientException e) {
            log.error("get bucket {} , prefix {}, object list failed, the cause is ", bucketName, prefix, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ListObjectsV2Result> listObjectsV2(ListObjectsV2Request listObjectsV2Request) throws SdkClientException {
        if (listObjectsV2Request == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(s3Client.listObjectsV2(listObjectsV2Request));
    }

    @Override
    public Optional<CopyObjectResult> copyObject(String srcBucketName, String srcObjectName, String destBucketName) {
        return copyObject(srcBucketName, srcObjectName, destBucketName, srcObjectName);
    }

    @Override
    public Optional<CopyObjectResult> copyObject(CopyObjectRequest copyObjectRequest) {
        if (copyObjectRequest == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(s3Client.copyObject(copyObjectRequest));
    }

    @Override
    public Optional<CopyObjectResult> copyObject(String srcBucketName, String srcObjectName, String destBucketName, String destObjectName) {
        if (isEmpty(srcBucketName, srcObjectName, destBucketName, destObjectName)) {
            log.info("from bucket {} object name {} to bucket {} object name {} failed",
                    srcBucketName, srcObjectName, destBucketName, destObjectName);
            return Optional.empty();
        }

        if (StrUtil.equals(srcBucketName, destBucketName) && StrUtil.equals(srcObjectName, destObjectName)) {
            throw new IllegalArgumentException(String.format("un support this operation, the srcBucketName %s is same to destBucketName %s " +
                            "and the srcObjectName %s is same to destObjectName %s",
                    srcBucketName,
                    destBucketName,
                    srcObjectName,
                    destObjectName
            ));
        }

        try {
            CopyObjectResult copyObjectResult = s3Client.copyObject(srcBucketName, srcObjectName, destBucketName, destObjectName);
            log.info("from bucket {} object name {} to bucket {} object name {} success",
                    srcBucketName, srcObjectName, destBucketName, destObjectName);
            return Optional.ofNullable(copyObjectResult);
        } catch (SdkClientException e) {
            log.info("from bucket {} object name {} to bucket {} object name {} failed, the cause ",
                    srcBucketName, srcObjectName, destBucketName, destObjectName, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<DeleteObjectsResult> deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        assert deleteObjectsRequest != null;

        try {
            return Optional.ofNullable(s3Client.deleteObjects(deleteObjectsRequest));
        } catch (SdkClientException e) {
            log.error("delete Object failed, the cause is ", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<DeleteObjectsResult> deleteObjectsWithNoVersionBucket(String bucketName, List<String> keyNameList) {
        return deleteObjectsWithNoVersionBucket(bucketName, keyNameList, false);
    }

    @Override
    public Optional<DeleteObjectsResult> deleteObjectsWithNoVersionBucket(String bucketName, List<String> keyNameList, boolean quiet) {
        List<DeleteObjectsRequest.KeyVersion> keyVersionList = keyNameList
                .stream()
                .map(DeleteObjectsRequest.KeyVersion::new)
                .collect(Collectors.toList());
        return deleteObjects(bucketName, keyVersionList, quiet);
    }

    @Override
    public Optional<DeleteObjectsResult> deleteObjects(String bucketName, List<DeleteObjectsRequest.KeyVersion> objectVersionList) {
        return deleteObjects(bucketName, objectVersionList, false);
    }

    @Override
    public Optional<DeleteObjectsResult> deleteObjects(String bucketName, List<DeleteObjectsRequest.KeyVersion> objectVersionList, boolean quiet) {
        if (StrUtil.isEmpty(bucketName)) {
            log.warn("delete Multiple Objects, the bucket name is not empty");
            return Optional.empty();
        }


        if (CollectionUtil.isEmpty(objectVersionList)) {
            log.warn("delete Multiple Objects, the keyNameList is not empty");
            return Optional.empty();
        }

        try {
            // Check to make sure that the bucket is versioning-enabled.
            String bucketVersionStatus = s3Client.getBucketVersioningConfiguration(bucketName).getStatus();
            if (!BucketVersioningConfiguration.ENABLED.equalsIgnoreCase(bucketVersionStatus)) {
                log.warn("Bucket {} is not versioning-enabled.", bucketName);
                return Optional.empty();
            }
            DeleteObjectsRequest deleteObjectsRequest =
                    new DeleteObjectsRequest(bucketName).withKeys(objectVersionList).withQuiet(quiet);


            return Optional.ofNullable(s3Client.deleteObjects(deleteObjectsRequest));
        } catch (SdkClientException e) {
            log.error("delete Multiple Objects, bucket {}, keyVersionList {}, quiet {} failed, the cause is ",
                    bucketName,
                    objectVersionList,
                    quiet,
                    e);

            return Optional.empty();
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectName) {
        deleteObject(new DeleteObjectRequest(bucketName, objectName));
    }

    @Override
    public void deleteObject(DeleteObjectRequest deleteObjectRequest) {
        assert deleteObjectRequest != null;

        String bucketName = deleteObjectRequest.getBucketName();
        String objectName = deleteObjectRequest.getKey();
        if (isEmpty(bucketName, objectName)) {
            log.warn("delete bucket {}, object {} failed, the bucketName or objectName not be empty", bucketName, objectName);
            return;
        }

        try {
            s3Client.deleteObject(deleteObjectRequest);
            log.info("delete bucket {}, object {} success", bucketName, objectName);
        } catch (SdkClientException e) {
            log.error("delete bucket {}, object {} failed, the cause is ", deleteObjectRequest.getBucketName(), deleteObjectRequest.getKey(), e);
        }
    }

    @Override
    public void deleteVersion(String bucketName, String objectName, String versionId) {
        deleteVersion(new DeleteVersionRequest(bucketName, objectName, versionId));
    }


    @Override
    public void deleteVersion(DeleteVersionRequest deleteVersionRequest) {
        assert deleteVersionRequest != null;
        String bucketName = deleteVersionRequest.getBucketName();
        String objectName = deleteVersionRequest.getKey();
        String versionId = deleteVersionRequest.getVersionId();

        if (isEmpty(bucketName, objectName, versionId)) {
            log.warn("delete bucket {}, object {}, versionId {} failed, the bucketName or objectName not be empty",
                    bucketName,
                    objectName,
                    versionId);

            return;
        }

        try {
            s3Client.deleteVersion(deleteVersionRequest);
            log.info("delete bucket {}, object {}, versionId {} success",
                    bucketName,
                    objectName,
                    versionId);
        } catch (SdkClientException e) {
            log.error("delete bucket {}, object {}, versionId {} failed, the cause is ",
                    bucketName,
                    objectName,
                    versionId,
                    e);

        }
    }

    @Override
    public boolean upload(String bucketName, String objectName, String baseDir, String content) {
        if (isEmpty(bucketName, objectName, content)) {
            return false;
        }
        try {
            // Upload a text string as a new object.
            String uploadObjectName = handlerUploadObjectName(objectName, baseDir);
            s3Client.putObject(bucketName, uploadObjectName, content);
            log.info("upload bucketName:{}, objectName:{} success", bucketName, uploadObjectName);
        } catch (SdkClientException e) {
            log.error("upload bucketName:{}, objectName:{} failed, the cause: ", bucketName, objectName, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean upload(String bucketName, String objectName, String content) {
        return upload(bucketName, objectName, BusinessV1Constant.DEFAULT_UPLOAD_BASE_DIR, content);
    }

    @Override
    public boolean upload(String bucketName, File file, String baseDir, ObjectMetadata metadata) {
        try {
            String objectName = handlerUploadObjectName(file.getName(), baseDir);

            // Upload a file as a new object with ContentType and title specified.
            PutObjectRequest request = new PutObjectRequest(bucketName, objectName, file);

            // 设置上传文件的元数据
            if (metadata != null) {
                // ObjectMetadata metadata = new ObjectMetadata();
                // metadata.setContentType("plain/text");
                // metadata.addUserMetadata("title", "someTitle");
                request.setMetadata(metadata);
            }


            s3Client.putObject(request);
            log.info("upload bucketName:{}, objectName:{} success", bucketName, objectName);
        } catch (SdkClientException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            e.printStackTrace();
            log.error("upload bucketName:{}, objectName:{} failed, the cause: ", bucketName, file.getName(), e);
            return false;
        }

        return true;
    }

    @Override
    public boolean multipartUploadV1(String bucketName, File file, String baseDir) {
        long contentLength = file.length();
        long partSize = 5 * 1024 * 1024; // Set part size to 5 MB.

        try {


            // Create a list of ETag objects. You retrieve ETags for each object part uploaded,
            // then, after each individual part has been uploaded, pass the list of ETags to
            // the request to complete the upload.
            List<PartETag> partETags = new ArrayList<>();

            String objectName = handlerUploadObjectName(file.getName(), baseDir);
            // Initiate the multipart upload.
            InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, objectName);
            InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

            // Upload the file parts.
            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; i++) {
                // Because the last part could be less than 5 MB, adjust the part size as needed.
                partSize = Math.min(partSize, (contentLength - filePosition));

                // Create the request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucketName)
                        .withKey(objectName)
                        .withUploadId(initResponse.getUploadId())
                        .withPartNumber(i)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(partSize);


                // Upload the part and add the response's ETag to our list.
                UploadPartResult uploadResult = s3Client.uploadPart(uploadRequest);
                partETags.add(uploadResult.getPartETag());

                filePosition += partSize;
            }

            // Complete the multipart upload.
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, objectName,
                    initResponse.getUploadId(), partETags);


            s3Client.completeMultipartUpload(compRequest);
            log.info("upload bucketName:{}, objectName:{} success", bucketName, objectName);
        } catch (SdkClientException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            log.error("upload bucketName:{}, objectName:{} failed, the cause: ", bucketName, file.getName(), e);
            return false;
        }

        return true;
    }

    @Override
    public boolean multipartUploadV2(String bucketName, File file, String baseDir) {
        try {

            TransferManager transferManager = TransferManagerBuilder.standard()
                    .withS3Client(s3Client)
                    .build();

            // TransferManager processes all transfers asynchronously,
            // so this call returns immediately.
            String objectName = handlerUploadObjectName(file.getName(), baseDir);
            Upload upload = transferManager.upload(bucketName, objectName, file);
            log.info("Object upload started");

            // Optionally, wait for the upload to finish before continuing.
            upload.waitForCompletion();
            log.info("Object upload complete");
            log.info("upload bucketName:{}, objectName:{} success", bucketName, objectName);
        } catch (SdkClientException | InterruptedException e) {
            // The call was transmitted successfully, but Amazon S3 couldn't process
            // it, so it returned an error response.
            // Amazon S3 couldn't be contacted for a response, or the client
            // couldn't parse the response from Amazon S3.
            log.error("upload bucketName:{}, objectName:{} failed, the cause: ", bucketName, file.getName(), e);
            return false;
        }
        return true;
    }

    @Override
    public Optional<S3Object> getObject(String bucketName, String objectName) throws SdkClientException {
        if (isEmpty(bucketName, objectName)) {
            log.warn("get bucket {}, object {} info failed.", bucketName, objectName);
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(s3Client.getObject(bucketName, objectName));
        } catch (SdkClientException e) {
            log.error("get bucket {}, object {} info failed, the cause is ", bucketName, objectName, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<String>> getObjectNameList(String bucketName, int maxKeys) {
        Optional<List<S3ObjectSummary>> objectInfoOptional = getObjectInfo(bucketName, maxKeys);
        if (objectInfoOptional.isPresent()) {
            List<S3ObjectSummary> s3ObjectSummaryList = objectInfoOptional.get();
            if (CollectionUtil.isEmpty(s3ObjectSummaryList)) {
                return Optional.empty();
            }

            List<String> objectNameList = s3ObjectSummaryList.stream().map(S3ObjectSummary::getKey).collect(Collectors.toList());
            return Optional.of(objectNameList);
        }

        return Optional.empty();
    }

    @Override
    public Optional<List<S3ObjectSummary>> getObjectInfo(String bucketName, int maxKeys) {
        if (StrUtil.isEmpty(bucketName)) {
            log.warn("get Object Name List failed, the bucket not empty");
            return Optional.empty();
        }

        try {

            ListObjectsV2Request request = new ListObjectsV2Request();
            request.withBucketName(bucketName);
            if (maxKeys > 0) {
                request.withMaxKeys(maxKeys);
            }

            ListObjectsV2Result result;

            List<S3ObjectSummary> s3ObjectSummaryList = new ArrayList<>();
            // 这里会执行分批查询，防止返回过多的key，造成程序卡顿
            do {
                result = s3Client.listObjectsV2(request);
                s3ObjectSummaryList.addAll(result.getObjectSummaries());

                // If there are more than maxKeys keys in the bucket, get a continuation token
                // and list the next objects.
                String token = result.getNextContinuationToken();
                log.info("Next Continuation Token: {}", token);
                request.setContinuationToken(token);
            } while (result.isTruncated());

            return Optional.of(s3ObjectSummaryList);
        } catch (SdkClientException e) {
            log.warn("bucket {}, maxKeys {}, get Object Name List failed, the cause is ", bucketName, maxKeys, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getPresignedObjectUrl(String bucketName, String objectName, Duration expireTime) {
        return getPresignedObjectUrl(bucketName, objectName, expireTime, HttpMethod.GET);
    }

    @Override
    public Optional<String> getPresignedObjectUrl(String bucketName, String objectName, Duration expireTime, HttpMethod httpMethod) {
        if (isEmpty(bucketName, objectName) || expireTime == null || httpMethod == null) {
            log.warn("bucket {}, object {}, expireTime {}, get Presigned Object {} Url failed," +
                    " the bucketName or ObjectName or expireTime not empty", bucketName, objectName, expireTime, httpMethod);
            return Optional.empty();
        }

        try {
            // Set the presigned URL to expire after one hour.
            Date expiration = new Date();
            long expTimeMillis = Instant.now().toEpochMilli();
            expTimeMillis += expireTime.toMillis();
            expiration.setTime(expTimeMillis);

            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(bucketName, objectName)
                            .withMethod(HttpMethod.GET)
                            .withExpiration(expiration);
            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);

            return Optional.ofNullable(url.toString());
        } catch (SdkClientException e) {
            log.warn("bucket {}, object {}, expireTime {}, get Presigned Object {} Url failed, the cause is ", bucketName, objectName, expireTime, httpMethod, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getPresignedObjectPutUrl(String bucketName, String objectName, Duration expireTime) {
        return getPresignedObjectUrl(bucketName, objectName, expireTime, HttpMethod.PUT);
    }

    /**
     * 处理桶名称，因为Amazon S3 不支持大写字母的，所以需要将桶名称转成小写
     *
     * @param bucketName 桶名称
     * @return 转成小写后的桶名称
     */
    private String handlerBucketName(String bucketName) {
        // 非空校验
        if (StrUtil.isEmpty(bucketName)) {
            return bucketName;
        }

        return bucketName.toLowerCase(Locale.ENGLISH);
    }


    /**
     * 返回按照年月日
     * 子类如果想要自定义ObjectName的处理规则，可以重写此方法实现
     *
     * @param objectName 对象名称
     * @param baseDir    基础路径
     * @return 处理后的对象名称
     */
    protected String handlerUploadObjectName(String objectName, String baseDir) {
        StringBuilder stringBuilder = new StringBuilder(FILE_SEPARATOR);
        stringBuilder.append(baseDir);
        stringBuilder.append(FILE_SEPARATOR);
        stringBuilder.append(LocalDateTime.now().format(FILE_NAME_PATTERN));
        stringBuilder.append(FILE_SEPARATOR);
        stringBuilder.append(UUID.randomUUID().toString(true));
        stringBuilder.append(FILENAME_LINK);
        stringBuilder.append(objectName);
        return stringBuilder.toString();
    }

    /**
     * 批量字符串非空校验
     *
     * @param args 字符串
     * @return true: 为空，false: 不为空
     */
    private boolean isEmpty(String... args) {
        if (args == null || args.length == 0) {
            return true;
        }

        for (String arg : args) {
            if (StrUtil.isEmpty(arg)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public AmazonS3 getS3Client() {
        return this.s3Client;
    }
}
