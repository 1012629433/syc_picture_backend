package com.syc.sycpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.syc.sycpicturebackend.model.entity.User;
import com.syc.sycpicturebackend.service.UserService;
import com.syc.sycpicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author Lenovo
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-12-24 16:27:16
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




