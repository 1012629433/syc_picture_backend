package com.syc.sycpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syc.sycpicturebackend.exception.BusinessException;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.exception.ThrowUtils;
import com.syc.sycpicturebackend.manager.upload.FilePictureUpload;
import com.syc.sycpicturebackend.manager.upload.PictureUploadTemplate;
import com.syc.sycpicturebackend.manager.upload.UrlPictureUpload;
import com.syc.sycpicturebackend.model.dto.file.UploadPictureResult;
import com.syc.sycpicturebackend.model.dto.picture.PictureQueryRequest;
import com.syc.sycpicturebackend.model.dto.picture.PictureReviewRequest;
import com.syc.sycpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.syc.sycpicturebackend.model.dto.picture.PictureUploadRequest;
import com.syc.sycpicturebackend.model.entity.Picture;
import com.syc.sycpicturebackend.model.entity.User;
import com.syc.sycpicturebackend.model.enums.PictureReviewStatusEnum;
import com.syc.sycpicturebackend.model.vo.PictureVO;
import com.syc.sycpicturebackend.model.vo.UserVO;
import com.syc.sycpicturebackend.service.PictureService;
import com.syc.sycpicturebackend.mapper.PictureMapper;
import com.syc.sycpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.io.IOException;
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
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {
    @Resource
    FilePictureUpload filePictureUpload;
    @Resource
    UrlPictureUpload urlPictureUpload;
    @Resource
    UserService userService;

    @Override
    public PictureVO upLoadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        //检查用户是否登录
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        //判断是创建图片还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            /*
            *因为开放给普通用户上传图片的功能了，所以要进行权限校验，原有代码就不合适了
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.PARAMS_ERROR, "图片不存在");
            */
            Picture oldPicture = this.getById(pictureId);
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        //上传图片,按用户划分目录
        String upLoadPathPrefix = String.format("public/%s", loginUser.getId());
        //判断上传图片的请求是文件上传还是url上传,默认设置为文件上传
        PictureUploadTemplate pictureUploadTemplate=filePictureUpload;
        if(inputSource instanceof String){
            pictureUploadTemplate=urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, upLoadPathPrefix);
        //构造入库图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        //默认从对象存储的解析结果中获取名称
        String picName=uploadPictureResult.getPicName();
        //如果外层传来了图片名称，则使用外层传来的图片名称
        if(pictureUploadRequest!=null&&StrUtil.isNotBlank(pictureUploadRequest.getNamePrefix())){
            picName=pictureUploadRequest.getNamePrefix();
        }
        picture.setName(picName);
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
        //补充审核参数
        this.fileReviewParams(picture, loginUser);
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
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();

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
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage),"reviewMessage",reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category),"category",category);
        queryWrapper.eq(ObjUtil.isNotNull(picSize),"picSize",picSize);
        queryWrapper.eq(ObjUtil.isNotNull(picWidth),"picWidth",picWidth);
        queryWrapper.eq(ObjUtil.isNotNull(picHeight),"picHeight",picHeight);
        queryWrapper.eq(ObjUtil.isNotNull(picScale),"picScale",picScale);
        queryWrapper.eq(ObjUtil.isNotNull(reviewStatus),"reviewStatus",reviewStatus);
        queryWrapper.eq(ObjUtil.isNotNull(reviewerId),"reviewerId",reviewerId);
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

    @Override
    public void pictureDoReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        //读取参数信息
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        //将请求中的参数转化为枚举类型，避免出错
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        //判断请求，不允许将状态设置为待审核
        if (id == null || pictureReviewRequest.getReviewStatus() == null || PictureReviewStatusEnum.REVIEWING.equals(pictureReviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //检查图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //判断修改的状态是否与原状态重复
        if (oldPicture.getReviewStatus().equals(pictureReviewStatusEnum.getValue())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重新审核");
        }
        //封装信息，进行更新
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(loginUser.getId());
        picture.setReviewTime(new Date());
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审核失败");

    }

    @Override
    public void fileReviewParams(Picture picture, User loginUser) {
        //管理员自动过审
        if (userService.isAdmin(loginUser)) {
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
        } else {
            //非管理员，其他修改图片的操作都需要重新过审
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }


    }

    @Override
    public Integer pictureUploadByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        //1. 校验参数
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        String searchText = pictureUploadByBatchRequest.getSearchText();
        String namePrefix=pictureUploadByBatchRequest.getNamePrefix();
        //如果前缀为空，默认使用搜索词
        if(StrUtil.isBlank(namePrefix)){
            namePrefix=searchText;
        }
        //格式化抓取数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "每次最多30条");
        //抓取内容，指定从bing中获取图片
        String fetchUrl = String.format("https://cn.bing.com/images/search?q=%s&mmasync=1", searchText);
        //2. 解析内容
        Document document;
        try {
            //获取请求页面的dom对象
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败：", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        //根据dom对象,根据类选择器获取div元素
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtils.throwIf(ObjUtil.isEmpty(div), ErrorCode.OPERATION_ERROR, "获取元素失败");
        Elements imgElementList = div.select("img.mimg");
        //3. 上传图片
        int upLoadCount = 0;
        for ( Element element : imgElementList ) {
            //获取element元素的属性，这里是url
            String fileUrl = element.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过{}", fileUrl);
                continue;
            }
            //处理文件上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            //如果找到了？占位符
            if (questionMarkIndex > -1) {
                //只获取？之前的地址，例如tse3-mm.cn.bing.net/th/id/OIP-C.mv-5y09PMO5fIYkPQr1pLQAAAA?w=115，
                // ？后面拼接的是查询条件，我们不需要，前面才是图片的url地址
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //创建一个上传图片的请求，因为是新上传图片所以不需要id,而且我们使用url上传图片，
            // 但url是我们抓取的，也不需要通过pictureUploadRequest获取，所以不需要给它设置参数
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setNamePrefix(namePrefix);
            if(StrUtil.isBlank(pictureUploadByBatchRequest.getNamePrefix())){
                pictureUploadRequest.setNamePrefix(namePrefix+(upLoadCount+1));
            }
            try {
                PictureVO pictureVO = upLoadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id{}", pictureVO.getId());
                upLoadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                //单个图片上传失败不中断上传过程
                continue;
            }
            //当图片上传数量大于等于我们设定数量是，停止上传
            if (upLoadCount >= count) {
                break;
            }
        }
        //4. 返回结果
        return upLoadCount;
    }


}




