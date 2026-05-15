package cn.pzhdv.blog.vo.file;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文件上传结果VO
 * 定义文件上传接口的统一返回结构，包含前端所需的核心信息
 *
 * @author PanZonghui
 * @version 1.0
 * @since 2025-12-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "UploadResultVO", description = "单个文件上传的结果信息（成功/失败状态、访问地址等）")
public class UploadResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "原始文件名（如简历.pdf）", required = true, example = "简历.pdf")
    private String fileName;       // 原始文件名（前端展示用）

    @ApiModelProperty(value = "文件访问URL（上传成功才返回，可直接用于下载/预览）",
            required = false, example = "https://xxx.com/uploads/8f2d.pdf")
    private String fileUrl;        // 核心：文件访问地址

    @ApiModelProperty(value = "文件大小（字节）", required = true, example = "2400000")
    private Long fileSize;         // 文件大小（前端可转换为MB/KB展示）

    @ApiModelProperty(value = "上传状态（true=成功，false=失败）", required = true, example = "true")
    private Boolean success;       // 上传状态（多文件上传时区分成功/失败）

    @ApiModelProperty(value = "错误信息（仅失败时返回，说明失败原因）",
            required = false, example = "文件大小超过5MB限制")
    private String errorMessage;   // 错误信息（排查问题+用户提示）

    /**
     * 快速创建“上传成功”的结果
     */
    public static UploadResultVO success(String fileName, String fileUrl, Long fileSize) {
        return UploadResultVO.builder()
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .success(true)
                .errorMessage(null)
                .build();
    }

    /**
     * 快速创建“上传失败”的结果
     */
    public static UploadResultVO failure(String fileName, Long fileSize, String errorMessage) {
        return UploadResultVO.builder()
                .fileName(fileName)
                .fileUrl(null)
                .fileSize(fileSize)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}