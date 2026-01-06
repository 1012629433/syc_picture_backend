package com.syc.sycpicturebackend.manager.upload;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.syc.sycpicturebackend.config.CosClientConfig;
import com.syc.sycpicturebackend.exception.BusinessException;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.manager.CosManager;
import com.syc.sycpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    CosClientConfig cosClientConfig;
    @Resource
    CosManager cosManager;


    /**
     * 图片上传
     * @param inputSource
     * @param upLoadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String upLoadPathPrefix) {
        //1. 图片校验
        validPicture(inputSource);
        //2. 图片上传地址
        //todo
        String originalFileName=getOriginalFileName(inputSource);
        String fileSuffix =FileUtil.getSuffix(originalFileName);
        //如果从文件名中没有获取文件类型，则调用相应方法，主要针对url获取图片的方式
        if(StrUtil.isBlank(fileSuffix)){
            fileSuffix=this.getFileType(inputSource);
        }
        String uuid = RandomUtil.randomString(16);
        String fileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, fileSuffix);
        String upLoadPath = String.format("/%s/%s", upLoadPathPrefix, fileName);
        //上传图片
        File file = null;
        try {
            //3. 创建临时文件，获取临时文件到服务器
            file = File.createTempFile(upLoadPath, null);
            //todo
            ProcessFile(inputSource,file);
            //4. 上传图片到对象存储
            //获取上传文件的操作结果
            PutObjectResult putObjectResult = cosManager.putPictureObject(upLoadPath, file);
            //5. 从结果中获取图片信息，封装返回结果
            ImageInfo imageIcon = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            return buildResult(upLoadPath, originalFileName, file,imageIcon);
        } catch (Exception e) {
            log.info("图片上传到存储对象失败");
            log.info("error:", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片上传失败");
        } finally {
            //6. 删除临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 封装返回结果
     * @param upLoadPath
     * @param originalFileName
     * @param file
     * @param imageIcon 对象存储返回的图片信息
     * @return
     */
    private UploadPictureResult buildResult( String upLoadPath, String originalFileName, File file,ImageInfo imageIcon) {
        int width = imageIcon.getWidth();
        int height = imageIcon.getHeight();
        double picScale = NumberUtil.round(width * 1.0 / height, 2).doubleValue();
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + upLoadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(width);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(picScale);
        log.info("图片类型为：" + imageIcon.getFormat());
        uploadPictureResult.setPicFormat(imageIcon.getFormat());
        //返回图片上传结果
        return uploadPictureResult;
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

    /**
     * 处理输入源，校验文件（本地文件或url）
     *
     * @param inputSource
     * @return
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 处理输入源获取文件原始名称
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFileName(Object inputSource);

    /**
     * 处理输入源生成本地临时文件
     * @param inputSource
     * @param file
     */
    protected abstract void ProcessFile(Object inputSource, File file) throws IOException;

    /**
     * 获取文件类型
     * @param inputSource
     * @return
     */
    protected abstract String getFileType(Object inputSource);

}
