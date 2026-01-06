package com.syc.sycpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.syc.sycpicturebackend.exception.BusinessException;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl=(String) inputSource;
        //非空校验
        ThrowUtils.throwIf(fileUrl==null, ErrorCode.PARAMS_ERROR,"文件地址不能为空");
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
            response= HttpUtil.createRequest(Method.HEAD,fileUrl).execute();
            //未正常返回，无需执行其他，有些可能不支持head请求
            if(response.getStatus()!= HttpStatus.HTTP_OK){
                return;
            }
            //检查文件类型
            String contentType=response.header("Content-Type");
            if(StrUtil.isNotBlank(contentType)){
                //允许的文件类型
                final List<String> fileTypeList= Arrays.asList("image/jpg","image/png","image/jpg","image/jpeg","image/webp");
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

    @Override
    protected String getOriginalFileName(Object inputSource) {
        String fileUrl=(String) inputSource;
        return  FileUtil.mainName(fileUrl);
    }

    @Override
    protected void ProcessFile(Object inputSource, File file) throws IOException {
        String fileUrl=(String) inputSource;
        HttpUtil.downloadFile(fileUrl,file);
    }

    @Override
    protected String getFileType(Object inputSource) {
        //因为在获取文件类型这个方法执行前已经进行过校验了，无法通过校验也不会执行本方法，因此在这里不再进行判断
        String fileUrl = (String) inputSource;
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            //如果无法通过请求头获取类型，默认设置为jpg格式
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return "jpg";
            }
            //返回文件类型
            String contentType = httpResponse.header("Content - Type");
            if (contentType != null && contentType.startsWith("image/")) {
                contentType = contentType.substring("image/".length());
                return contentType;
            } else {
                // 处理不符合预期格式的情况，例如设置默认图片类型
                return "jpg";
            }
        } finally {
            //如果建立了连接，则执行完毕后需要关闭连接
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
    }

}
