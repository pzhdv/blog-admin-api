package cn.pzhdv.blog.controller;

import cn.pzhdv.blog.annotation.ApiLog;
import cn.pzhdv.blog.config.FileSizeConfig;
import cn.pzhdv.blog.vo.file.UploadResultVO;
import cn.pzhdv.blog.exception.FileValidationException;
import cn.pzhdv.blog.result.Result;
import cn.pzhdv.blog.result.ResultCode;
import cn.pzhdv.blog.result.ResultUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件上传控制器
 * 功能：单文件/批量上传、日期层级目录、安全类型校验、连接池/超时优化
 *
 * @author PanZonghui
 * @since 2025-12-31
 */
@Slf4j
@Api(tags = "文件管理模块")
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private static final String UPLOADS_DIRECTORY = "uploads/";
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    private final COSClient cosClient;
    private final FileSizeConfig fileSizeConfig;

    @Value("${tencent.cos.bucketName}")
    private String bucketName;

    @Value("${tencent.cos.baseUrl:}")
    private String cosBaseUrl;

    /**
     * 单文件上传
     */
    @ApiLog("单文件上传")
    @ApiOperation(
            value = "单文件图片上传",
            notes = "支持 JPG/PNG/GIF/WEBP，自动按日期目录存储，返回可直接访问的URL",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadResultVO> uploadSingle(
            @ApiParam(value = "上传文件", required = true, type = "file")
            @RequestPart("file") MultipartFile file) {

        log.info("【文件上传】收到单文件请求：{}", file.getOriginalFilename());

        if (file.isEmpty()) {
            return ResultUtil.error(ResultCode.FILE_NULL_ERROR);
        }

        UploadResultVO result = upload(file);
        return result.getSuccess() ? ResultUtil.ok(result) : ResultUtil.error(ResultCode.FILE_UPLOAD_FAIL, result.getErrorMessage());
    }

    /**
     * 批量文件上传
     */
    @ApiLog("批量文件上传")
    @ApiOperation(
            value = "批量图片上传",
            notes = "一次上传多张图片，自动跳过空文件，异常不中断，返回每个文件结果",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<UploadResultVO>> uploadBatch(
            @ApiParam(value = "文件数组", required = true, type = "file")
            @RequestPart("files") MultipartFile[] files) {

        log.info("【文件上传】收到批量请求，数量：{}", files.length);

        if (ObjectUtils.isEmpty(files)) {
            log.warn("【批量上传】请求中未包含有效文件");
            return ResultUtil.error(ResultCode.FILE_NULL_ERROR);
        }

        long totalSize = Arrays.stream(files).mapToLong(f -> f == null ? 0 : f.getSize()).sum();

        if (totalSize > fileSizeConfig.getTotalMaxSizeBytes()) {
            String maxStr = String.format("%.1fMB", fileSizeConfig.getTotalMaxSizeBytes() / 1024.0 / 1024.0);
            return ResultUtil.error(ResultCode.FILE_TOTAL_MAX_ERROR, maxStr);
        }

        List<UploadResultVO> results = Arrays.stream(files).map(file -> {
            if (file == null || file.isEmpty()) {
                return UploadResultVO.failure(null, 0L, "文件为空，跳过");
            }
            return safeUpload(file);
        }).collect(Collectors.toList());

        log.info("【文件上传】批量处理完成，成功数：{}", results.stream().filter(UploadResultVO::getSuccess).count());
        return ResultUtil.ok(results);
    }

    /**
     * 核心上传方法
     */
    private UploadResultVO upload(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();

        if (ObjectUtils.isEmpty(originalFilename)) {
            return UploadResultVO.failure(null, file.getSize(), "文件名称为空");
        }

        String ext = getFileExtension(originalFilename);
        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd/"));
        String objectKey = UPLOADS_DIRECTORY + datePath + UUID.randomUUID() + ext;

        try (InputStream in = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            cosClient.putObject(bucketName, objectKey, in, metadata);
            String url = buildFileUrl(objectKey);

            log.info("【上传成功】文件：{} -> {}", originalFilename, url);
            return UploadResultVO.success(originalFilename, url, file.getSize());

        } catch (Exception e) {
            log.error("【上传失败】文件：{}，异常原因：{}", originalFilename, e.getMessage());
            return UploadResultVO.failure(originalFilename, file.getSize(), e.getMessage());
        }
    }

    /**
     * 包装上传逻辑
     */
    private UploadResultVO safeUpload(MultipartFile file) {
        try {
            return upload(file);
        } catch (FileValidationException e) {
            log.warn("【校验拦截】文件：{}，原因：{}", file.getOriginalFilename(), e.getMessage());
            return UploadResultVO.failure(file.getOriginalFilename(), file.getSize(), e.getMessage());
        } catch (Exception e) {
            log.error("【系统异常】处理文件 {} 时发生未知错误", file.getOriginalFilename(), e);
            return UploadResultVO.failure(file.getOriginalFilename(), file.getSize(), "系统处理异常");
        }
    }

    /**
     * 文件统一校验逻辑
     */
    private void validateFile(MultipartFile file) {
        if (file.getSize() > fileSizeConfig.getMaxFileSizeBytes()) {
            String maxMb = String.format("%.1fMB", fileSizeConfig.getMaxFileSizeBytes() / 1024.0 / 1024.0);
            throw new FileValidationException(ResultCode.FILE_TOO_LARGE, maxMb);
        }

        String name = file.getOriginalFilename();
        if (ObjectUtils.isEmpty(name) || !name.contains(".")) {
            throw new FileValidationException(ResultCode.UNSUPPORTED_FILE_TYPE, "文件名不合法或缺少后缀");
        }

        String contentType = file.getContentType();
        if (ObjectUtils.isEmpty(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("【非法类型上传】检测到不支持的类型：{}", contentType);
            throw new FileValidationException(ResultCode.UNSUPPORTED_FILE_TYPE, "仅支持 jpg/png/gif/webp 格式");
        }
    }

    /**
     * 构建访问地址
     */
    private String buildFileUrl(String objectKey) {
        if (StringUtils.hasText(cosBaseUrl)) {
            String base = cosBaseUrl.endsWith("/") ? cosBaseUrl : cosBaseUrl + "/";
            String key = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
            return base + key;
        }
        String region = cosClient.getClientConfig().getRegion().getRegionName();
        return "https://" + bucketName + ".cos." + region + ".myqcloud.com/" + objectKey;
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(idx) : "";
    }
}