package com.syc.sycpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
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
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 已废弃，改为使用upload包的方法
 */
@Service
@Slf4j
//表示已废弃
@Deprecated
public class FileManger {
    @Resource
    CosClientConfig cosClientConfig;
    @Resource
    CosManager cosManager;

    /**
     * 图片上传
     * @param multipartFile
     * @param upLoadPathPrefix
     * @return
     */
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
//todo
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String upLoadPathPrefix) {
        //todo
        validUrl(fileUrl);
        //图片上传地址
        //todo
        String originalFileName=FileUtil.mainName(fileUrl);
        String fileSuffix = FileUtil.getSuffix(originalFileName);
        String uuid = RandomUtil.randomString(16);
        String fileName = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, fileSuffix);
        String upLoadPath = String.format("/%s/%s", upLoadPathPrefix, fileName);
        //上传图片
        File file = null;
        try {
            //创建临时文件
            file = File.createTempFile(upLoadPath, null);
            //multipartFile.transferTo(file);
            //使用hutool工具包提供的方法下载图片
            //todo
            HttpUtil.downloadFile(fileUrl,file);
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
            uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
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
     * url校验
     * @param fileUrl
     */
    private void validUrl(String fileUrl) {
        //非空校验
        ThrowUtils.throwIf(fileUrl==null,ErrorCode.PARAMS_ERROR,"文件地址不能为空");
        //检查url格式
        try {
            //利用idea提供的url创建方法来检查url是否符合格式
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件地址格式不正确");
        }
        //检查协议
        if(!fileUrl.startsWith("http")&&!fileUrl.startsWith("https")){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"仅支持 HTTP 协议或 HTTPS 协议的文件地址");
        }
        //发送head请求，检查图片是否存在，head请求只会请求获取响应头信息不会获取具体内容请求体，响应头包含图片的相关信息但不包含图片本身，节约资源
        HttpResponse response=null;
        try {
            response=HttpUtil.createRequest(Method.HEAD,fileUrl).execute();
            //未正常返回，无需执行其他，有些可能不支持head请求
            if(response.getStatus()!= HttpStatus.HTTP_OK){
                return;
            }
            //检查文件类型
            String contentType=response.header("Content-Type");
            if(StrUtil.isNotBlank(contentType)){
                //允许的文件类型
                final List<String> fileTypeList=Arrays.asList("image/jpg","image/png","image/jpg","image/jpeg","image/webp");
                ThrowUtils.throwIf(!fileTypeList.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR,"文件类型错误");
            }
            //检查图片大小
            String contentLengthStr=response.header("Content-Length");
            try {
                if (StrUtil.isNotBlank(contentLengthStr)) {
                    //将字符串类型转化为long类型
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long TWO_MB = 2 * 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > TWO_MB, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
                }
            }catch (NumberFormatException e){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件大小格式错误");
            }
        } finally {
            //如果建立了连接，则执行完毕后需要关闭连接
            if(response!=null){
                response.close();
            }
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
