package com.syc.sycpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.syc.sycpicturebackend.config.CosClientConfig;
import com.syc.sycpicturebackend.exception.BusinessException;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.exception.ThrowUtils;
import com.syc.sycpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class FileManger {
    @Resource
    CosClientConfig cosClientConfig;
    @Resource
    CosManager cosManager;

    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String upLoadPathPrefix) {
        //图片校验
        validPicture(multipartFile);
        //图片上传地址
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        String uuid = RandomUtil.randomString(16);
        String fileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, fileSuffix);
        String upLoadPath = String.format("/%s/%s", upLoadPathPrefix, fileName);
        //上传图片
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(upLoadPath, null);
            multipartFile.transferTo(file);
            //上传图片
            //获取上传文件的操作结果
            PutObjectResult putObjectResult = cosManager.putPictureObject(upLoadPath, file);
            //从结果中获取图片信息
            ImageInfo imageIcon = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
            int width = imageIcon.getWidth();
            int height = imageIcon.getHeight();
            double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + upLoadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(multipartFile.getOriginalFilename()));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(width);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(picScale);
            log.info("图片类型为：" + imageIcon.getFormat());
            uploadPictureResult.setPicFormat(imageIcon.getFormat());
            //返回图片上传结果
            return uploadPictureResult;
        } catch (Exception e) {
            log.info("图片上传到存储对象失败");
            log.info("error:", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片上传失败");
        } finally {
            //删除临时文件
            this.deleteTempFile(file);
        }
    }


    /**
     * 图片校验
     *
     * @param multipartFile
     */
    private void validPicture(MultipartFile multipartFile) {
        //检查图片大小
        final long ONE_M = 1024 * 1024;
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.OPERATION_ERROR, "图片大小超过2M");
        //验证图片格式
        //允许上传的图片后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("png", "jpg", "jpeg", "webp");
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.OPERATION_ERROR, "文件类型错误");
    }

    /**
     * 删除临时文件
     *
     * @param file
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        //删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.info("file delete error,filepath={}", file.getAbsoluteFile());
        }
    }
}
