package com.syc.sycpicturebackend.service;

import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.syc.sycpicturebackend.model.dto.user.UserQueryRequest;
import com.syc.sycpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.syc.sycpicturebackend.model.vo.LoginUserVO;
import com.syc.sycpicturebackend.model.vo.UserVO;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-12-24 16:27:16
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @return 已脱敏用户信息
     */

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录的用户信息
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏后的登录用户信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户退出登录
     * @param request
     * @return
     */
    Boolean UserLogout(HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param list
     * @return
     */
    List<UserVO> getUserVOList(List<User> list);

    boolean addUser(User user);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 根据id删除用户
     *
     * @param id
     * @return
     */
    boolean deleteUser(Long id);
}
