package com.syc.sycpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.cos.utils.StringUtils;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.exception.ThrowUtils;
import com.syc.sycpicturebackend.manager.FileManger;
import com.syc.sycpicturebackend.model.dto.file.UploadPictureResult;
import com.syc.sycpicturebackend.model.dto.picture.PictureQueryRequest;
import com.syc.sycpicturebackend.model.dto.picture.PictureUploadRequest;
import com.syc.sycpicturebackend.model.entity.Picture;
import com.syc.sycpicturebackend.model.entity.User;
import com.syc.sycpicturebackend.model.vo.PictureVO;
import com.syc.sycpicturebackend.model.vo.UserVO;
import com.syc.sycpicturebackend.service.PictureService;
import com.syc.sycpicturebackend.mapper.PictureMapper;
import com.syc.sycpicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lenovo
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-12-31 14:42:06
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    FileManger fileManger;
    @Resource
    UserService userService;

    @Override
    public PictureVO upLoadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //检查用户是否登录
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //判断是创建图片还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //判断图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        //上传图片
        String upLoadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManger.uploadPicture(multipartFile, upLoadPathPrefix);
        //构造入库图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        //如果图片不为空，表示是更新操作
        if (pictureId != null) {
            //如果是更新需要补充更新图片的id和更新时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        //图片入库
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        //返回给前端脱敏图片数据
        return PictureVO.objToVo(picture);
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        //创建查询对象
        ThrowUtils.throwIf(pictureQueryRequest==null,ErrorCode.PARAMS_ERROR);
        QueryWrapper<Picture> queryWrapper=new QueryWrapper<>();
        //获取查询字段
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        //拼接查询字段形成查询语句
        //从多字段中查找
        if(StrUtil.isNotBlank(searchText)){
            //and(name like "%?%" or introduction like "$?%")
            queryWrapper.and(qw->qw.like("name",searchText)
                    .or()
                    .like("introduction",searchText));
        }
        queryWrapper.eq(ObjUtil.isNotNull(id),"id",id);
        queryWrapper.eq(ObjUtil.isNotNull(userId),"userId",userId);
        queryWrapper.like(StrUtil.isNotBlank(name),"name",name);
        queryWrapper.like(StrUtil.isNotBlank(introduction),"introduction",introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat),"picFormat",picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category),"category",category);
        queryWrapper.eq(ObjUtil.isNotNull(picSize),"picSize",picSize);
        queryWrapper.eq(ObjUtil.isNotNull(picWidth),"picWidth",picWidth);
        queryWrapper.eq(ObjUtil.isNotNull(picHeight),"picHeight",picHeight);
        queryWrapper.eq(ObjUtil.isNotNull(picScale),"picScale",picScale);
        //json数组查询
        if(CollUtil.isNotEmpty(tags)){
            /* and (tag like "%\"Java\"%" and like "%\"Python\"%”)*/
            for (String tag : tags) {
                queryWrapper.like("tags","\""+tag+"\"");
            }
        }
        //排序
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),sortOrder.equals("ascend"),sortField);
        //返回查询语句
        return queryWrapper;
    }
    @Override
    public PictureVO getPictureVO(Picture picture) {
        //对象转为封装类
        PictureVO pictureVO=PictureVO.objToVo(picture);
        //关联创建用户
        Long userId=picture.getUserId();
        if(userId!=null&&userId>0){
            User user=userService.getById(userId);
            UserVO userVO=userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return null;
    }

    @Override
    public Page<PictureVO> getPictureVOByPage(Page<Picture> page) {
        //判断page是否为空,如果为空直接返回一个空对象
        List<Picture> pictureList=page.getRecords();
        Page<PictureVO> pictureVOPage=new Page<>(page.getCurrent(), page.getSize(),page.getTotal());
        if(CollUtil.isEmpty(pictureList)){
            return pictureVOPage;
        }
        //将picture列表进行封装
        List<PictureVO> pictureVOList=pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        //读取List中的内容，获取图片关联的用户id列表,使用set集合不允许重复,collect用于将流转化为各种数据结构
        Set<Long> userIdSet=pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
        //Collectors.groupingBy(User::getId),将用户集合进行分组，返回一个 Map<ID类型, List<User>> 结构
        //一次性查找数据库中用户信息
        Map<Long,List<User>> userIdUserListMap=userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        //补充pictureVO中的数据
        pictureVOList.forEach(
                pictureVO -> {
                    Long userId=pictureVO.getUserId();
                    User user=new User();
                    if(userIdUserListMap.containsKey(userId)){
                        user=userIdUserListMap.get(userId).get(0);
                    }
                    pictureVO.setUser(userService.getUserVO(user));
                }
        );
        //将List集合添加到page集合中
        pictureVOPage.setRecords(pictureVOList);
        //返回转化后的page对象
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        //添加if，允许图片为空，只有传递了url才进行验证
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }


}




