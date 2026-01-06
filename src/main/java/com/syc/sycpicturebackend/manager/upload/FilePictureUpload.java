package com.syc.sycpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        final long ONE_M = 1024 * 1024;
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.OPERATION_ERROR, "图片大小超过2M");
        //验证图片格式
        //允许上传的图片后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("png", "jpg", "jpeg", "webp");
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.OPERATION_ERROR, "文件类型错误");
    }

    @Override
    protected String getOriginalFileName(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void ProcessFile(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }

    @Override
    protected String getFileType(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return FileUtil.getSuffix(multipartFile.getOriginalFilename());
    }

}
