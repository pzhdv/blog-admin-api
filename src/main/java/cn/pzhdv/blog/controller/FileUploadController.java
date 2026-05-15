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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件上传控制器
 * 提供单文件/批量文件上传，对接腾讯云COS，自动校验大小、格式、文件流
 *
 * @author PanZonghui
 * @since 2025-12-31
 */
@Slf4j
@Api(tags = "文件上传")
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileUploadController {

    /**
     * 文件存储基础目录
     */
    private static final String UPLOADS_DIRECTORY = "uploads/";

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
    @ApiOperation(value = "单文件上传")
    @PostMapping(value = "/single", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadResultVO> uploadSingle(@RequestPart("file") MultipartFile file) {
        log.info("【单文件上传】开始，文件名：{}", file.getOriginalFilename());

        if (file.isEmpty()) {
            log.warn("【单文件上传】文件为空");
            return ResultUtil.error(ResultCode.FILE_NULL_ERROR);
        }

        UploadResultVO result = upload(file);
        log.info("【单文件上传】结束，结果：{}", result.getSuccess() ? "成功" : "失败");
        return ResultUtil.ok(result);
    }

    /**
     * 批量文件上传
     */
    @ApiLog("批量文件上传")
    @ApiOperation(value = "批量文件上传")
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<List<UploadResultVO>> uploadBatch(@RequestPart("files") MultipartFile[] files) {
        log.info("【批量上传】开始，文件数量：{}", files.length);

        if (files.length == 0) {
            log.warn("【批量上传】文件列表为空");
            return ResultUtil.error(ResultCode.FILE_NULL_ERROR);
        }

        // 总大小校验
        long totalSize = Arrays.stream(files)
                .mapToLong(f -> f == null ? 0 : f.getSize())
                .sum();

        if (totalSize > fileSizeConfig.getTotalMaxSizeBytes()) {
            String max = String.format("%.1fMB", fileSizeConfig.getTotalMaxSizeBytes() / 1024.0 / 1024.0);
            log.warn("【批量上传】总大小超限，当前：{}字节，最大允许：{}", totalSize, max);
            return ResultUtil.error(ResultCode.FILE_TOTAL_MAX_ERROR, max);
        }

        List<UploadResultVO> results = Arrays.stream(files)
                .parallel()
                .map(file -> {
                    if (file == null || file.isEmpty()) {
                        log.warn("【批量上传】跳过空文件");
                        return UploadResultVO.failure(null, 0L, "文件为空，已跳过");
                    }
                    return safeUpload(file);
                })
                .collect(Collectors.toList());

        log.info("【批量上传】完成，成功数：{}",
                results.stream().filter(UploadResultVO::getSuccess).count());
        return ResultUtil.ok(results);
    }

    /**
     * 实际上传逻辑
     */
    private UploadResultVO upload(MultipartFile file) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null) {
            log.error("【文件上传】文件名为空，无法上传");
            return UploadResultVO.failure(null, file.getSize(), "文件名为空");
        }

        String ext = getFileExtension(originalFilename);
        String objectKey = UPLOADS_DIRECTORY + UUID.randomUUID() + ext;

        try (InputStream in = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            cosClient.putObject(bucketName, objectKey, in, metadata);
            String url = buildFileUrl(objectKey);

            log.info("【文件上传】成功：{} → {}", originalFilename, url);
            return UploadResultVO.success(originalFilename, url, file.getSize());

        } catch (Exception e) {
            log.error("【文件上传】失败：{}", originalFilename, e);
            return UploadResultVO.failure(originalFilename, file.getSize(), "上传异常：" + e.getMessage());
        }
    }

    /**
     * 安全上传（异常不中断批量）
     */
    private UploadResultVO safeUpload(MultipartFile file) {
        try {
            return upload(file);
        } catch (FileValidationException e) {
            log.warn("【文件校验失败】{}：{}", file.getOriginalFilename(), e.getMessage());
            return UploadResultVO.failure(file.getOriginalFilename(), file.getSize(), e.getMessage());
        } catch (Exception e) {
            log.error("【文件处理异常】{}", file.getOriginalFilename(), e);
            return UploadResultVO.failure(file.getOriginalFilename(), file.getSize(), "处理失败");
        }
    }

    /**
     * 构建文件访问地址
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

    /**
     * 文件统一校验
     */
    private void validateFile(MultipartFile file) {
        long size = file.getSize();
        long max = fileSizeConfig.getMaxFileSizeBytes();

        if (size > max) {
            String maxMb = String.format("%.1fMB", max / 1024.0 / 1024.0);
            log.warn("【文件大小超限】{} 当前：{}字节 允许：{}", file.getOriginalFilename(), size, maxMb);
            throw new FileValidationException(ResultCode.FILE_TOO_LARGE, maxMb);
        }

        String name = file.getOriginalFilename();
        if (name != null && !name.contains(".")) {
            log.warn("【文件格式非法】无后缀名：{}", name);
            throw new FileValidationException(ResultCode.UNSUPPORTED_FILE_TYPE, "不支持无后缀名文件");
        }

        try {
            if (file.getInputStream().available() <= 0) {
                throw new FileValidationException(ResultCode.FILE_UPLOAD_FAIL, "文件内容为空");
            }
        } catch (Exception e) {
            throw new FileValidationException(ResultCode.FILE_UPLOAD_FAIL, "文件流异常");
        }
    }

    /**
     * 获取文件后缀
     */
    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(idx) : "";
    }
}