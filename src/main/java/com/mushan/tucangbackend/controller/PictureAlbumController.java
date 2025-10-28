package com.mushan.tucangbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.annotation.AuthCheck;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.model.dto.picture.PictureAlbumAddRequest;
import com.mushan.tucangbackend.model.dto.picture.PictureAlbumUpdateRequest;
import com.mushan.tucangbackend.model.dto.picture.PictureCursorQueryRequest;
import com.mushan.tucangbackend.model.entity.PictureAlbum;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.entity.UserPictureInteraction;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.vo.PictureCursorQueryVO;
import com.mushan.tucangbackend.model.vo.PictureVO;
import com.mushan.tucangbackend.service.PictureAlbumService;
import com.mushan.tucangbackend.service.UserService;
import com.mushan.tucangbackend.service.PictureService;
import com.mushan.tucangbackend.mapper.UserPictureInteractionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mushan.tucangbackend.manager.ScheduledTasksManager.SYSTEM_ADMIN_USER_ID;

/**
 * 收藏夹接口
 */
@RestController
@RequestMapping("/pictureAlbum")
@Slf4j
public class PictureAlbumController {

    @Resource
    private PictureAlbumService pictureAlbumService;
    
    @Resource
    private UserService userService;
    
    @Resource
    private PictureService pictureService;
    
    @Resource
    private UserPictureInteractionMapper userPictureInteractionMapper;

    public static final int ALBUM_lIMIT = 15;

    // region 增删改查

    /**
     * 创建收藏夹
     *
     * @param pictureAlbumAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addPictureAlbum(@RequestBody PictureAlbumAddRequest pictureAlbumAddRequest, HttpServletRequest request) {
        if (pictureAlbumAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        PictureAlbum pictureAlbum = new PictureAlbum();
        BeanUtils.copyProperties(pictureAlbumAddRequest, pictureAlbum);
        
        // 参数校验
        if (pictureAlbum.getName() == null || pictureAlbum.getName().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏夹名称不能为空");
        }
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        pictureAlbum.setUserId(loginUser.getId());
        pictureAlbum.setPictureCount(0);
        pictureAlbum.setViewCount(0);
        pictureAlbum.setCreateTime(new java.util.Date());
        
        boolean result = pictureAlbumService.save(pictureAlbum);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        
        return ResultUtils.success(pictureAlbum.getId());
    }

    /**
     * 删除收藏夹
     *
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePictureAlbum(@RequestParam("id") Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);
        PictureAlbum pictureAlbum = pictureAlbumService.getById(id);
        if (pictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 只有创建者可以删除自己的收藏夹
        if (!pictureAlbum.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        
        // 先清空收藏夹中的所有收藏记录
        boolean result = pictureAlbumService.deleteAlbumWithPictures(id);
        return ResultUtils.success(result);
    }

    /**
     * 更新收藏夹 (管理员专用)
     *
     * @param pictureAlbumUpdateRequest
     * @param
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> updatePictureAlbum(@RequestBody PictureAlbumUpdateRequest pictureAlbumUpdateRequest) {
        if (pictureAlbumUpdateRequest == null || pictureAlbumUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        PictureAlbum pictureAlbum = new PictureAlbum();
        BeanUtils.copyProperties(pictureAlbumUpdateRequest, pictureAlbum);
        
        // 参数校验
        if (pictureAlbum.getName() == null || pictureAlbum.getName().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏夹名称不能为空");
        }

        PictureAlbum oldPictureAlbum = pictureAlbumService.getById(pictureAlbum.getId());
        if (oldPictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        boolean result = pictureAlbumService.updateById(pictureAlbum);
        return ResultUtils.success(result);
    }

    /**
     * 编辑收藏夹 (用户专用)
     *
     * @param pictureAlbumUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPictureAlbum(@RequestBody PictureAlbumUpdateRequest pictureAlbumUpdateRequest, HttpServletRequest request) {
        if (pictureAlbumUpdateRequest == null || pictureAlbumUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        PictureAlbum pictureAlbum = new PictureAlbum();
        BeanUtils.copyProperties(pictureAlbumUpdateRequest, pictureAlbum);
        
        // 参数校验
        if (pictureAlbum.getName() == null || pictureAlbum.getName().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏夹名称不能为空");
        }

        User loginUser = userService.getLoginUser(request);
        PictureAlbum oldPictureAlbum = pictureAlbumService.getById(pictureAlbum.getId());
        if (oldPictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 只有创建者可以更新自己的收藏夹
        if (!oldPictureAlbum.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        boolean result = pictureAlbumService.updateById(pictureAlbum);
        return ResultUtils.success(result);
    }

    /**
     * 根据 ID 获取收藏夹
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<PictureAlbum> getPictureAlbumById(@RequestParam("id") Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        PictureAlbum pictureAlbum = pictureAlbumService.getById(id);
        if (pictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        return ResultUtils.success(pictureAlbum);
    }

    /**
     * 分页获取收藏夹列表
     *
     * @param current
     * @param pageSize
     * @param request
     * @return
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<PictureAlbum>> listPictureAlbumByPage(@RequestParam(defaultValue = "1") int current,
                                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                                   HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        // 构造查询条件：查询当前用户的收藏夹
        QueryWrapper<PictureAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());

        // 分页查询
        Page<PictureAlbum> page = pictureAlbumService.page(new Page<>(current, pageSize), queryWrapper);
        return ResultUtils.success(page);
    }

    /**
     * 根据用户ID获取其公开的收藏夹列表
     *
     * @param userId 用户ID
     * @return 用户公开的收藏夹列表
     */
    @GetMapping("/list/user/{userId}")
    public BaseResponse<List<PictureAlbum>> listUserPublicAlbums(@PathVariable Long userId) {

        // 参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

        // 构造查询条件：指定用户创建的公开收藏夹
        QueryWrapper<PictureAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId)
                .eq("isPublic", 1) // 公开的收藏夹
                .orderByDesc("createTime")
                .last("LIMIT " + ALBUM_lIMIT);

        List<PictureAlbum> userAlbums = pictureAlbumService.list(queryWrapper);
        return ResultUtils.success(userAlbums);
    }


    /**
     * 获取当前用户的所有收藏夹
     *
     * @param request
     * @return
     */
    @GetMapping("/list/my")
    public BaseResponse<List<PictureAlbum>> listMyPictureAlbums(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        
        // 查询当前用户的所有收藏夹
        QueryWrapper<PictureAlbum> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<PictureAlbum> pictureAlbumList = pictureAlbumService.list(queryWrapper);
        
        return ResultUtils.success(pictureAlbumList);
    }

    /**
     * 查询收藏夹内的图片
     *
     * @param albumId 收藏夹ID
     * @param current 当前页码
     * @param pageSize 页面大小
     * @param request HTTP请求
     * @return 图片分页列表
     */
    @GetMapping("/list/pictures")
    public BaseResponse<Page<PictureVO>> listPicturesInAlbum(
            @RequestParam("albumId") Long albumId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        
        // 参数校验
        if (albumId == null || albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏夹ID不合法");
        }
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 检查收藏夹是否存在
        PictureAlbum pictureAlbum = pictureAlbumService.getById(albumId);
        if (pictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "收藏夹不存在");
        }
        
        // 检查是否有权限访问该收藏夹（创建者或公开的收藏夹）
        if (!pictureAlbum.getUserId().equals(loginUser.getId()) && pictureAlbum.getIsPublic() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该收藏夹");
        }

        // 构造查询条件：查询指定收藏夹内的图片
        QueryWrapper<UserPictureInteraction> interactionQueryWrapper = new QueryWrapper<>();
        interactionQueryWrapper.eq("albumId", albumId)
                .eq("userId", loginUser.getId()) // 添加用户ID过滤条件
                .eq("type", 1); // 1 表示收藏
        
        // 分页查询收藏记录
        Page<UserPictureInteraction> interactionPage = new Page<>(current, pageSize);
        Page<UserPictureInteraction> interactionResultPage = userPictureInteractionMapper.selectPage(
                interactionPage, interactionQueryWrapper);
        
        // 获取图片ID列表
        List<Long> pictureIds = interactionResultPage.getRecords().stream()
                .map(UserPictureInteraction::getPictureId)
                .collect(Collectors.toList());
        
        // 查询图片信息
        List<Picture> pictureList = pictureIds.isEmpty() ? 
                new ArrayList<>() : pictureService.listByIds(pictureIds);
        
        // 转换为VO对象
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> pictureService.getPictureVO(picture, request))
                .collect(Collectors.toList());
        
        // 构造返回的分页结果
        Page<PictureVO> pictureVOPage = new Page<>(current, pageSize, interactionResultPage.getTotal());
        pictureVOPage.setRecords(pictureVOList);
        
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 游标查询收藏夹内的图片
     *
     * @param albumId 收藏夹ID
     * @param pictureCursorQueryRequest 游标查询请求
     * @param request HTTP请求
     * @return 图片游标查询结果
     */
    @GetMapping("/list/pictures/cursor")
    public BaseResponse<PictureCursorQueryVO> listPicturesInAlbumByCursor(
            @RequestParam("albumId") Long albumId,
            PictureCursorQueryRequest pictureCursorQueryRequest,
            HttpServletRequest request) {
        
        // 参数校验
        if (albumId == null || albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "收藏夹ID不合法");
        }
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 检查收藏夹是否存在
        PictureAlbum pictureAlbum = pictureAlbumService.getById(albumId);
        if (pictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "收藏夹不存在");
        }
        
        // 检查是否有权限访问该收藏夹（创建者或公开的收藏夹）
        if (!pictureAlbum.getUserId().equals(loginUser.getId()) && pictureAlbum.getIsPublic() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该收藏夹");
        }
        
        // 限制每次查询的数据量
        pictureCursorQueryRequest.setPageSize(
                Math.min(pictureCursorQueryRequest.getPageSize(), 20));
        
        // 调用服务方法查询指定收藏夹内的图片
        PictureCursorQueryVO result = pictureService.listUserFavoritedPicturesByAlbum(
                albumId, pictureCursorQueryRequest, loginUser, request);
        
        return ResultUtils.success(result);
    }

    /**
     * 获取精选收藏夹列表（所有用户看到的内容相同）
     *
     * @param limit 限制数量，默认为10
     * @return 精选收藏夹列表
     */
    @GetMapping("/featured")
    public BaseResponse<List<PictureAlbum>> getFeaturedAlbums(@RequestParam(defaultValue = "10") int limit) {
        // 首先尝试从缓存中获取今日精选收藏夹
        List<PictureAlbum> featuredAlbums = pictureAlbumService.getTodayFeaturedAlbumsFromCache(limit);

        // 如果缓存中没有，则实时计算
        if (featuredAlbums == null) {
            featuredAlbums = pictureAlbumService.getFeaturedAlbums(limit);
        }

        return ResultUtils.success(featuredAlbums);
    }

    /**
     * 为当前用户推荐感兴趣的收藏夹
     * 基于用户的历史行为分析兴趣偏好并推荐相关收藏夹
     *
     * @param limit 限制数量，默认为10
     * @param request HTTP请求
     * @return 推荐的收藏夹列表
     */
    @GetMapping("/recommend")
    public BaseResponse<List<PictureAlbum>> getRecommendedAlbums(
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        
        // 首先尝试从缓存中获取用户推荐收藏夹
        List<PictureAlbum> recommendedAlbums = pictureAlbumService.getUserRecommendedAlbumsFromCache(loginUser.getId(), limit);

        // 如果缓存中没有，则实时计算
        if (recommendedAlbums == null) {
            recommendedAlbums = pictureAlbumService.getRecommendedAlbumsForUser(loginUser, limit);
        }

        return ResultUtils.success(recommendedAlbums);
    }

    /**
     * 获取所有系统创建的热门图片收藏夹
     * 这些收藏夹由系统自动创建和维护，包含各类别的热门图片
     *
     * @param limit 限制数量
     * @return 系统热门收藏夹列表
     */
    @GetMapping("/hot/system")
    public BaseResponse<List<PictureAlbum>> listSystemHotPictureAlbums(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        
        // 首先尝试从缓存中获取系统热门收藏夹
        List<PictureAlbum> hotAlbums = pictureAlbumService.getSystemHotAlbumsFromCache(limit);
        
        // 如果缓存中没有，则实时计算
        if (hotAlbums == null) {
            hotAlbums = pictureAlbumService.getSystemHotAlbums(limit);
        }

        return ResultUtils.success(hotAlbums);
    }
    

    // endregion
}