package com.syc.sycpicturebackend.controller;

import com.syc.sycpicturebackend.common.BaseResponse;
import com.syc.sycpicturebackend.common.ResultUtils;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.exception.ThrowUtils;
import com.syc.sycpicturebackend.model.dto.user.UserRegisterRequest;
import com.syc.sycpicturebackend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    UserService userService;

    /**
     * 用户注册
     *
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result=userService.userRegister(userAccount,userPassword,checkPassword);
        return ResultUtils.success(result);
    }
}
