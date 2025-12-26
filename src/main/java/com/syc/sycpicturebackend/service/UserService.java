package com.syc.sycpicturebackend.service;

import cn.hutool.http.server.HttpServerRequest;
import com.syc.sycpicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.syc.sycpicturebackend.model.vo.LoginUserVO;

/**
* @author Lenovo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-12-24 16:27:16
*/
public interface UserService extends IService<User> {
    /**
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return 新用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     *
     * @param userAccount
     * @param userPassword
     * @return 已脱敏用户信息
     */

    LoginUserVO userLogin(String userAccount, String userPassword, HttpServerRequest request);

    String getEncryptPassword(String userPassword);
}
