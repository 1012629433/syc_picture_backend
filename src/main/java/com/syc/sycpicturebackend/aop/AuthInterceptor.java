package com.syc.sycpicturebackend.aop;

import com.syc.sycpicturebackend.annotation.AuthCheck;
import com.syc.sycpicturebackend.exception.BusinessException;
import com.syc.sycpicturebackend.exception.ErrorCode;
import com.syc.sycpicturebackend.model.entity.User;
import com.syc.sycpicturebackend.model.enums.UserRoleEnum;
import com.syc.sycpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole= authCheck.mustRole();
        UserRoleEnum mustRoleEnum=UserRoleEnum.getEnumByValue(mustRole);
        //获取当前线程绑定的 RequestAttributes 对象。这个对象包含了当前请求的各种属性，例如请求头信息、请求参数、会话属性等。
        RequestAttributes requestAttributes= RequestContextHolder.currentRequestAttributes();
        //将RequestAttributes转化为ServletRequestAttributes，然后获取当前HttpServletRequest对象
        HttpServletRequest request=((ServletRequestAttributes)requestAttributes).getRequest();
        //1.获取当前登录用户
        User loginUser=userService.getLoginUser(request);
        //2.判断被标注方法是否需要权限，不需要则放行
        if(mustRoleEnum==null){
            return joinPoint.proceed();
        }
        //3.检查用户权限是否为空，顺带检查登录态，如果为空，抛异常，没有权限
        String userRole=loginUser.getUserRole();
        UserRoleEnum userRoleEnum=UserRoleEnum.getEnumByValue(userRole);
        if(userRoleEnum==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //4.要求必须有管理员权限，单用户没有管理员权限，拒绝
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum)&&!userRoleEnum.equals(mustRoleEnum)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        //5.用户拥有权限，放行
        return joinPoint.proceed();
    }
}
