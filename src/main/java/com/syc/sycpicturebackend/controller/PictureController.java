package com.syc.sycpicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.syc.sycpicturebackend.annotation.AuthCheck;
import com.syc.sycpicturebackend.common.BaseResponse;
import com.syc.sycpicturebackend.common.DeleteRequest;
import com.syc.sycpicturebackend.common.ResultUtils;
import com.syc.sycpicturebackend.constant.UserConstant;
import com.syc.sycpicturebackend.exception.BusinessException;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.exception.ThrowUtils;
import com.syc.sycpicturebackend.model.dto.picture.*;
import com.syc.sycpicturebackend.model.entity.Picture;
import com.syc.sycpicturebackend.model.enums.PictureReviewStatusEnum;
import com.syc.sycpicturebackend.model.vo.PictureTagCategory;
import com.syc.sycpicturebackend.model.entity.User;
import com.syc.sycpicturebackend.model.vo.PictureVO;
import com.syc.sycpicturebackend.service.PictureService;
import com.syc.sycpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
    @Resource
    PictureService pictureService;
    @Resource
    UserService userService;

    /**
     * 上传图片，使用文件上传方式
     * @param multipartFile
     * @param pictureUploadRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> upLoadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        PictureVO pictureVO = pictureService.upLoadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 根据url上传图片
     * @param pictureUploadRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> upLoadPictureByUrl(PictureUploadRequest pictureUploadRequest, HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        String fileUrl= pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.upLoadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片，仅图片创建者和管理员可以删除
     * @param deleteRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/delete")
    public  BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,HttpServletRequest httpServletRequest){
        //验证参数是否为空
        ThrowUtils.throwIf(deleteRequest==null||deleteRequest.getId()<0, ErrorCode.PARAMS_ERROR);
        //获取要删除的图片
        Picture oldPicture=pictureService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"未找到图片");
        //检查用户权限，只有本人和管理员才可以删除图片
        User loginUser=userService.getLoginUser(httpServletRequest);
        if(!userService.isAdmin(loginUser)&&!oldPicture.getUserId().equals(loginUser.getId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //返回删除结果
        boolean result= pictureService.removeById(oldPicture.getId());
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"删除失败");
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员）
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest httpServletRequest) {
        //检查参数
        ThrowUtils.throwIf(pictureUpdateRequest==null||pictureUpdateRequest.getId()<0,ErrorCode.PARAMS_ERROR);
        //将dto类转化为实体类
        Picture picture=new Picture();
        BeanUtil.copyProperties(pictureUpdateRequest,picture);
        //为了方便前端操作，我们将请求中的tags设置为了list集合，但picture对象中的tags为字符串，需要我们手动转化
        String tags= pictureUpdateRequest.getTags().toString();
        picture.setTags(tags);
        //判断图片是否存在
        long id=picture.getId();
        Picture oldPicture=pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        //补充审核参数
        User loginUser = userService.getLoginUser(httpServletRequest);
        pictureService.fileReviewParams(picture, loginUser);
        //更新图片
        boolean result= pictureService.updateById(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"更新失败");
        return ResultUtils.success(true);
    }

    /**
     * 查找图片（管理员）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(Long id){
        //参数检查
        ThrowUtils.throwIf(id==null||id<0,ErrorCode.PARAMS_ERROR);
        //从数据库查找图片
        Picture picture=pictureService.getById(id);
        ThrowUtils.throwIf(picture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        //返回查找结果
        return ResultUtils.success(picture);
    }

    /**
     * 根据id获取图片（封装类）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(Long id){
        //参数检查
        ThrowUtils.throwIf(id==null||id<0,ErrorCode.PARAMS_ERROR);
        //从数据库查找图片
        Picture picture=pictureService.getById(id);
        ThrowUtils.throwIf(picture==null,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        //检查图片状态,未过申图片用户无法获取
        if(picture.getReviewStatus()!=PictureReviewStatusEnum.PASS.getValue()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在");
        }
        //封装实体类
        PictureVO pictureVO=PictureVO.objToVo(picture);
        //返回查找结果
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页查找图片信息（管理员）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> getPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest){
        //检查参数
        ThrowUtils.throwIf(pictureQueryRequest==null,ErrorCode.PARAMS_ERROR);
        //根据参数获取查询语句对象
        QueryWrapper<Picture> queryWrapper=pictureService.getQueryWrapper(pictureQueryRequest);
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //查找数据库
        Page<Picture> picturePage=pictureService.page(new Page<>(current,size),queryWrapper);

        //返回分页数据
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页查找图片信息（封装类）
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> getPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest){
        //检查参数
        ThrowUtils.throwIf(pictureQueryRequest==null,ErrorCode.PARAMS_ERROR);
        //将请求的图片状态设置为过审，确保普通用户只能查看到过审的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //根据参数获取查询语句对象
        QueryWrapper<Picture> queryWrapper=pictureService.getQueryWrapper(pictureQueryRequest);
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        //限制爬虫
        ThrowUtils.throwIf(size>20,ErrorCode.PARAMS_ERROR);
        //查找数据库
        //使用mybatis框架提供的分页查询方法
        Page<Picture> picturePage=pictureService.page(new Page<>(current,size),queryWrapper);
        //返回封装后的分页数据
        return ResultUtils.success(pictureService.getPictureVOByPage(picturePage));
    }

    /**
     * 用户编辑图片
     * @param pictureEditRequest
     * @param httpServletRequest
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest httpServletRequest) {
        //参数检查
        ThrowUtils.throwIf(pictureEditRequest==null||pictureEditRequest.getId()<0,ErrorCode.PARAMS_ERROR);
        //将dto转化为实体类
        Picture picture=new Picture();
        BeanUtil.copyProperties(pictureEditRequest,picture);
        //将List集合的tags转化为String
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        //查找图片是否存在
        Picture oldPicture=pictureService.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture==null,ErrorCode.NOT_FOUND_ERROR);
        //设置编辑时间
        picture.setEditTime(new Date());
        //数据校验
        pictureService.validPicture(picture);
        //权限检查，只有创建者本人或管理员才可以编辑
        User loginUser=userService.getLoginUser(httpServletRequest);
        if(!userService.isAdmin(loginUser)&&!loginUser.getId().equals(picture.getUserId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //补充审核参数
        pictureService.fileReviewParams(picture, loginUser);
        //更新图片
        boolean result= pictureService.updateById(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"编辑失败");
        //返回结果
        return ResultUtils.success(result);
    }

    /**
     * 为用户列举常用标签
     * @return
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> pictureDoReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginuser = userService.getLoginUser(httpServletRequest);
        pictureService.pictureDoReview(pictureReviewRequest, loginuser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> upLoadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser=userService.getLoginUser(httpServletRequest);
        int upLoadPictureCount=pictureService.pictureUploadByBatch(pictureUploadByBatchRequest,loginUser);
        return ResultUtils.success(upLoadPictureCount);
    }
}
