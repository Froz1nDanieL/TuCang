package com.mushan.tucangbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.manager.auth.StpKit;
import com.mushan.tucangbackend.manager.upload.FilePictureUpload;
import com.mushan.tucangbackend.model.dto.file.UploadPictureResult;
import com.mushan.tucangbackend.model.dto.user.UserCursorQueryRequest;
import com.mushan.tucangbackend.model.dto.user.UserEditRequest;
import com.mushan.tucangbackend.model.dto.user.UserQueryRequest;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.UserRoleEnum;
import com.mushan.tucangbackend.model.vo.LoginUserVO;
import com.mushan.tucangbackend.model.vo.UserCursorQueryVO;
import com.mushan.tucangbackend.model.vo.UserVO;
import com.mushan.tucangbackend.service.UserService;
import com.mushan.tucangbackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mushan.tucangbackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author Danie
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-07-27 14:55:17
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private FilePictureUpload filePictureUpload;
    
    /**
     * 用户注册
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 3. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    /**
     *获取加密密码
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "mushan";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 记录用户登录态到 Sa-token，便于空间鉴权时使用，注意保证该用户信息与 SpringSession 中的信息过期时间一致
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public Boolean editUser(User loginUser,UserEditRequest userEditRequest){
        Long id = loginUser.getId();
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "用户不存在");
        User user = this.getById(id);
        
        // 更新用户信息
        String userName = userEditRequest.getUserName();
        String userProfile = userEditRequest.getUserProfile();
        String userAvatar = userEditRequest.getUserAvatar();

        user.setUserName(userName);
        user.setUserProfile(userProfile);
        user.setUserAvatar(userAvatar);

        user.setEditTime(new Date());
        return this.updateById(user);
    }

    @Override
    public String uploadAvatar(User loginUser, MultipartFile avatar) {
        String uploadPathPrefix = String.format("avatar/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPicture(avatar, uploadPathPrefix);
        return uploadPictureResult.getUrl();
    }

    /**
     * 获取脱敏后的用户视图信息
     * @param user
     * @return
     */

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user,loginUserVO);
        return loginUserVO;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        
        // 查询用户的图片数量和热度值
        Long userId = user.getId();
        if (userId != null) {
            Integer pictureCount = this.baseMapper.getUserPictureCount(userId);
            Integer heatValue = this.baseMapper.getUserHeatValue(userId);
            userVO.setPictureCount(pictureCount != null ? pictureCount : 0);
            userVO.setHeatValue(heatValue != null ? heatValue : 0);
        }
        
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    @Override
    public UserCursorQueryVO getActiveUserRanking(UserCursorQueryRequest cursorQueryRequest) {
        // 设置默认分页大小
        int pageSize = cursorQueryRequest.getPageSize();
        if (pageSize <= 0) {
            pageSize = 10;
        }
        pageSize = Math.min(pageSize, 20); // 限制最大分页大小

        // 通过联表查询统计用户创建的图片数量，并按数量排序
        List<User> userList = this.baseMapper.getActiveUserRanking(cursorQueryRequest.getCursorId(), pageSize);
        
        // 构建返回结果
        return buildUserCursorQueryVO(userList, pageSize);
    }

    @Override
    public UserCursorQueryVO getPopularUserRanking(UserCursorQueryRequest cursorQueryRequest) {
        // 设置默认分页大小
        int pageSize = cursorQueryRequest.getPageSize();
        if (pageSize <= 0) {
            pageSize = 10;
        }
        pageSize = Math.min(pageSize, 20); // 限制最大分页大小
        
        // 通过联表查询统计用户创建的图片的热度值（点赞数+收藏数），并按热度值排序
        List<User> userList = this.baseMapper.getPopularUserRanking(cursorQueryRequest.getCursorId(), pageSize);
        
        // 构建返回结果
        return buildUserCursorQueryVO(userList, pageSize);
    }
    
    /**
     * 构建UserCursorQueryVO对象
     *
     * @param userList 用户列表
     * @param pageSize 页面大小
     * @return UserCursorQueryVO对象
     */
    private UserCursorQueryVO buildUserCursorQueryVO(List<User> userList, int pageSize) {
        UserCursorQueryVO result = new UserCursorQueryVO();
        if (CollUtil.isEmpty(userList)) {
            result.setUserList(new ArrayList<>());
            result.setNextCursorId(null);
            result.setHasMore(false);
            return result;
        }

        // 转换为VO对象
        List<UserVO> userVOList = userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());

        result.setUserList(userVOList);

        // 计算下一个游标
        if (userList.size() < pageSize) {
            result.setNextCursorId(null);
            result.setHasMore(false);
        } else {
            User lastUser = userList.get(userList.size() - 1);
            result.setNextCursorId(lastUser.getId());
            result.setHasMore(true);
        }

        return result;
    }
}