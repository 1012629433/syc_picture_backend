package com.syc.sycpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传图片请求
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 根据url上传图片
     */
    private String fileUrl;

    /**
     * 图片的名称前缀
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;
}
