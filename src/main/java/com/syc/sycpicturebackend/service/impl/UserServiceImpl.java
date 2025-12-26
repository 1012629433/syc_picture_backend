package com.syc.sycpicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syc.sycpicturebackend.exception.BusinessException;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.exception.ThrowUtils;
import com.syc.sycpicturebackend.model.entity.User;
import com.syc.sycpicturebackend.model.enums.UserRoleEnum;
import com.syc.sycpicturebackend.model.vo.LoginUserVO;
import com.syc.sycpicturebackend.service.UserService;
import com.syc.sycpicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author Lenovo
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-12-24 16:27:16
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper,User>
        implements UserService{

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名过短");
        }
        if (userPassword.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }

        //2.检查用户账号是否与数据库中数据重复
        //MyBatis-Plus 提供的条件构造器，用于构建 SQL 查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //调用了 eq 方法，表示添加一个 等于（=） 条件
        queryWrapper.eq("UserAccount", userAccount);
        //执行查询并返回符合条件的记录数
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已存在");
        }

        //3.密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        //4.插入数据到数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        //编译器确保正确,避免人工编写时产生失误
        user.setUserRole(UserRoleEnum.USER.getValue());
        // MyBatis-Plus 提供的数据持久化方法,保存后 user.getId() 会获得生成的主键值
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误，注册失败");
        }
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServerRequest httpServerRequest) {
        //1.校验
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount,userPassword),ErrorCode.PARAMS_ERROR,"参数为空");
        ThrowUtils.throwIf(userAccount.length()<4,ErrorCode.PARAMS_ERROR,"账号错误");
        ThrowUtils.throwIf(userPassword.length()<4,ErrorCode.PARAMS_ERROR,"密码错误");
        //2.对用户传递的密码进行加密
        //加密是为了保证明文对明文，密文对密文，加密后才能与数据库中加密的密码进行比较
        String encryptPassword=getEncryptPassword(userPassword);
        //3.查询数据库用户是否存在
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",userPassword);
        User user=this.baseMapper.selectOne(queryWrapper);
        //不存在，抛异常，用户不存在
        ThrowUtils.throwIf(user==null,ErrorCode.PARAMS_ERROR,"用户不存在或密码错误");
        log.info("user login failed,userAccount can match userPassword");
        //4.保存用户登录态
        return null;
    }


    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "syc_picture";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

}




