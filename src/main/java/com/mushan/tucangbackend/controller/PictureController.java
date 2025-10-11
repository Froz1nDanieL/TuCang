package com.mushan.tucangbackend.controller;


import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mushan.tucangbackend.annotation.AuthCheck;
import com.mushan.tucangbackend.api.aliyunai.AliYunAiApi;
import com.mushan.tucangbackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.mushan.tucangbackend.api.aliyunai.model.CreateTextToImageTaskResponse;
import com.mushan.tucangbackend.api.aliyunai.model.GetTextToImageTaskResponse;
import com.mushan.tucangbackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.mushan.tucangbackend.api.imagesearch.model.SearchPictureByPictureRequest;
import com.mushan.tucangbackend.api.imagesearch.model.SoImageSearchResult;
import com.mushan.tucangbackend.api.imagesearch.sub.SoImageSearchApiFacade;
import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.DeleteRequest;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.constant.UserConstant;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.manager.auth.SpaceUserAuthManager;
import com.mushan.tucangbackend.manager.auth.StpKit;
import com.mushan.tucangbackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.mushan.tucangbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.mushan.tucangbackend.model.dto.picture.*;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.Space;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.PictureReviewStatusEnum;
import com.mushan.tucangbackend.model.vo.PictureAlbumVO;
import com.mushan.tucangbackend.model.vo.PictureCursorQueryVO;
import com.mushan.tucangbackend.model.vo.PictureTagCategory;
import com.mushan.tucangbackend.model.vo.PictureVO;
import com.mushan.tucangbackend.service.PictureService;
import com.mushan.tucangbackend.service.SpaceService;
import com.mushan.tucangbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();



    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
            HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        User loginUser = userService.getLoginUser(request);
        //补充审核信息
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**r
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 已经改为saToken鉴权
            // pictureService.checkPictureAuth(loginUser, picture);
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            // 已经改为saToken鉴权
        //  // 私有空间
        //  User loginUser = userService.getLoginUser(request);
        //  Space space = spaceService.getById(spaceId);
        //  ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        //  if (!loginUser.getId().equals(space.getUserId())) {
        //      throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
        //  }
        }

        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        //构建缓存key
        String pictureCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String cacheKey = DigestUtil.md5Hex(pictureCondition);
        String redisKey = String.format("tucang:listPictureVOByPage:%s", cacheKey);

        // 1. 查询本地缓存（Caffeine）
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 2. 查询分布式缓存（Redis）
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        cachedValue = valueOps.get(cacheKey);
        if (cachedValue != null) {
            // 如果命中 Redis，存入本地缓存并返回
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 3. 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 4. 更新缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 更新本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        // 更新 Redis 缓存，设置过期时间为 5 分钟
        valueOps.set(cacheKey, cacheValue, 5, TimeUnit.MINUTES);


        // 获取封装类
        return ResultUtils.success(pictureVOPage);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("人物","自然","艺术","建筑");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }


    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 收藏/取消收藏图片
     */
    @PostMapping("/favorite")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Boolean> favoritePicture(@RequestBody PictureFavoriteRequest pictureFavoriteRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Boolean result = pictureService.favoritePicture(pictureFavoriteRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 点赞/取消点赞图片
     */
    @PostMapping("/like/{pictureId}")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Boolean> likePicture(@PathVariable Long pictureId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Boolean result = pictureService.likePicture(pictureId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 以图搜图
     */
    @PostMapping("/search/picture/so")
    public BaseResponse<List<SoImageSearchResult>> searchPictureByPictureIsSo(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<SoImageSearchResult> resultList = new ArrayList<>();
        // 这个 start 是控制查询多少页, 每页是 20 条
        int start = 0;
        while (resultList.size() <= 50) {
            List<SoImageSearchResult> tempList = SoImageSearchApiFacade.searchImage(
                    oldPicture.getUrl(), start
            );
            if (tempList.isEmpty()) {
                break;
            }
            resultList.addAll(tempList);
            start += tempList.size();
        }
        return ResultUtils.success(resultList);
    }


    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingRequest createPictureOutPaintingRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingRequest == null || createPictureOutPaintingRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 创建 AI 文生图任务
     */
    @PostMapping("/text_to_image/create_task")
    public BaseResponse<CreateTextToImageTaskResponse> createTextToImageTask(
            @RequestBody CreateTextToImageRequest createTextToImageRequest,
            HttpServletRequest request) {
        if (createTextToImageRequest == null || createTextToImageRequest.getPrompt() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateTextToImageTaskResponse response = pictureService.createTextToImageTask(createTextToImageRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }

    /**
     * 查询 AI 文生图任务
     */
    @GetMapping("/text_to_image/get_task")
    public BaseResponse<GetTextToImageTaskResponse> getTextToImageTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetTextToImageTaskResponse task = aliYunAiApi.getTextToImageTask(taskId);
        return ResultUtils.success(task);
    }

    /**
     * 获取公共图库所有图片
     */
    @GetMapping("/list/public")
    public BaseResponse<List<PictureVO>> listPublicPictures(HttpServletRequest request) {
        // 构造查询条件，只查询公开图片（spaceId为null）且已审核通过的图片
        PictureQueryRequest pictureQueryRequest = new PictureQueryRequest();
        pictureQueryRequest.setNullSpaceId(true);
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 查询所有公开图片
        QueryWrapper<Picture> queryWrapper = pictureService.getQueryWrapper(pictureQueryRequest);
        List<Picture> pictureList = pictureService.list(queryWrapper);

        // 转换为VO对象
        List<PictureVO> pictureVOList = new ArrayList<>();
        for (Picture picture : pictureList) {
            PictureVO pictureVO = pictureService.getPictureVO(picture, request);
            pictureVOList.add(pictureVO);
        }

        return ResultUtils.success(pictureVOList);
    }
    
    /**
     * 游标查询图片列表（封装类）
     */
    @PostMapping("/list/cursor/vo")
    public BaseResponse<PictureCursorQueryVO> listPictureVOByCursor(@RequestBody PictureCursorQueryRequest pictureCursorQueryRequest,
                                                                    HttpServletRequest request) {
        // 限制爬虫
        long size = pictureCursorQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");
        
        // 普通用户默认只能查看已过审的数据
        pictureCursorQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        
        // 空间权限校验
        Long spaceId = pictureCursorQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            pictureCursorQueryRequest.setNullSpaceId(true);
        } else {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }
        
        // 执行游标查询
        PictureCursorQueryVO result = pictureService.listPictureVOByCursor(pictureCursorQueryRequest, request);
        return ResultUtils.success(result);
    }

    
    /**
     * 游标查询指定用户点赞的图片列表
     *
     * @param userId 用户ID
     * @param pictureCursorQueryRequest 游标查询请求
     * @param request HTTP请求
     * @return 图片游标查询结果
     */
    @PostMapping("/list/liked/{userId}")
    public BaseResponse<PictureCursorQueryVO> listUserLikedPictures(@PathVariable Long userId,
                                                                          @RequestBody PictureCursorQueryRequest pictureCursorQueryRequest,
                                                                          HttpServletRequest request) {
        // 限制爬虫
        long size = pictureCursorQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");
        
        // 参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        
        PictureCursorQueryVO result = pictureService.listUserLikedPictures(userId, pictureCursorQueryRequest, request);
        return ResultUtils.success(result);
    }
    
    /**
     * 游标查询用户收藏的图片列表
     */
    @PostMapping("/list/favorited")
    public BaseResponse<PictureCursorQueryVO> listUserFavoritedPictures(@RequestBody PictureCursorQueryRequest pictureCursorQueryRequest,
                                                                                 HttpServletRequest request) {
        // 限制爬虫
        long size = pictureCursorQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");
        
        User loginUser = userService.getLoginUser(request);
        PictureCursorQueryVO result = pictureService.listUserFavoritedPictures(pictureCursorQueryRequest, loginUser, request);
        return ResultUtils.success(result);
    }

    /**
     * 获取用户收藏夹列表（包含图片收藏状态）
     *
     * @param pictureId 图片ID
     * @param request HTTP请求
     * @return 收藏夹列表
     */
    @GetMapping("/album/list/{pictureId}")
    public BaseResponse<List<PictureAlbumVO>> listPictureAlbumsWithStatus(@PathVariable Long pictureId, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<PictureAlbumVO> result = pictureService.getPictureAlbumsByPictureId(pictureId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 将图片添加到收藏夹
     */
    @PostMapping("/album/add")
    public BaseResponse<Boolean> addPictureToAlbum(@RequestBody PictureFavoriteRequest pictureFavoriteRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Boolean result = pictureService.addPictureToAlbum(pictureFavoriteRequest, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 从收藏夹移除图片
     */
    @PostMapping("/album/remove")
    public BaseResponse<Boolean> removePictureFromAlbum(@RequestBody PictureFavoriteRequest pictureFavoriteRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Boolean result = pictureService.removePictureFromAlbum(pictureFavoriteRequest, loginUser);
        return ResultUtils.success(result);
    }
}
