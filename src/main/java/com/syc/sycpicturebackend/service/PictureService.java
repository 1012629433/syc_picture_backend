package com.syc.sycpicturebackend.service;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.syc.sycpicturebackend.model.dto.picture.PictureQueryRequest;
import com.syc.sycpicturebackend.model.dto.picture.PictureUploadRequest;
import com.syc.sycpicturebackend.model.entity.Picture;
import com.syc.sycpicturebackend.model.entity.User;
import com.syc.sycpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;


/**
 * @author Lenovo
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-12-31 14:42:06
 */
public interface PictureService extends IService<Picture> {
    /**
     * 图片上传
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    public PictureVO upLoadPicture(MultipartFile multipartFile
            , PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取查询语句对象
     * @param pictureQueryRequest
     * @return
     */
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片包装类（单条）
     * @param picture
     * @return
     */
    public PictureVO getPictureVO(Picture picture);

    /**
     * 分页获取图片包装类
     * @param page
     * @return
     */
    public Page<PictureVO> getPictureVOByPage(Page<Picture> page);

    /**
     *修改和更新图片时进行验证
     * @param picture
     */
    public void validPicture(Picture picture);
}
