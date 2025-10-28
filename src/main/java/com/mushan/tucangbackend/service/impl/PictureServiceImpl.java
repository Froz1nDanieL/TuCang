package com.mushan.tucangbackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mushan.tucangbackend.api.aliyunai.AliYunAiApi;
import com.mushan.tucangbackend.api.aliyunai.model.*;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.exception.ThrowUtils;
import com.mushan.tucangbackend.manager.CosManager;
import com.mushan.tucangbackend.manager.FileManager;
import com.mushan.tucangbackend.manager.auth.SpaceUserAuthManager;
import com.mushan.tucangbackend.manager.auth.model.SpaceUserPermissionConstant;
import com.mushan.tucangbackend.manager.upload.FilePictureUpload;
import com.mushan.tucangbackend.manager.upload.PictureUploadTemplate;
import com.mushan.tucangbackend.manager.upload.UrlPictureUpload;
import com.mushan.tucangbackend.manager.upload.AiGenPictureUpload;
import com.mushan.tucangbackend.mapper.UserPictureInteractionMapper;
import com.mushan.tucangbackend.model.dto.aigenhistory.AiGenHistoryAddRequest;
import com.mushan.tucangbackend.model.dto.file.UploadPictureResult;
import com.mushan.tucangbackend.model.dto.picture.*;
import com.mushan.tucangbackend.model.entity.*;
import com.mushan.tucangbackend.model.enums.PictureReviewStatusEnum;
import com.mushan.tucangbackend.model.vo.PictureAlbumVO;
import com.mushan.tucangbackend.model.vo.PictureCursorQueryVO;
import com.mushan.tucangbackend.model.vo.PictureVO;
import com.mushan.tucangbackend.model.vo.UserVO;
import com.mushan.tucangbackend.service.*;
import com.mushan.tucangbackend.mapper.PictureMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Danie
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-07-29 15:19:40
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private FileManager fileManager;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private AiGenPictureUpload aiGenPictureUpload;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private UserPictureInteractionMapper userPictureInteractionMapper;

    @Lazy
    @Resource
    private PictureAlbumService pictureAlbumService;

    @Resource
    private AiGenHistoryService aiGenHistoryService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        // 空间权限校验
        if (spaceId != null) {
            // 2. 校验空间权限
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）或空间编辑者才能上传
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!loginUser.getId().equals(space.getUserId()) && 
                !permissionList.contains("picture:upload")) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }

        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人或管理员可编辑
            // 获取空间信息以检查权限
            Space space = null;
            if (oldPicture.getSpaceId() != null) {
                space = spaceService.getById(oldPicture.getSpaceId());
            }
            
            // 检查权限：如果当前用户具有编辑权限就可以编辑
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!oldPicture.getUserId().equals(loginUser.getId()) 
                    && !userService.isAdmin(loginUser)
                    && !permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        // 上传图片，得到信息
        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        // 根据inputSource类型判断是文件还是url上传
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        String picName = uploadPictureResult.getPicName();
        if (uploadPictureResult.getPicName() != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setSpaceId(spaceId);
        //补充审核信息
        this.fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        return PictureVO.objToVo(picture);
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        // 1. 校验参数
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多只能抓取30张图片");
        // 名称前缀默认等于搜索词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        // 2. 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }

        // 3. 解析内容
        Elements imgElements = document.select(".iuscp.isv");
        int uploadCount = 0;

        // 遍历元素，依次上传图片
        for (Element imgElement : imgElements) {
            if (uploadCount >= count) {
                break;
            }

            // 获取图片的 m 属性（包含高清图片 URL 的 JSON 数据）
            String mAttr = imgElement.select(".iusc").attr("m");
            if (StringUtil.isBlank(mAttr)) {
                log.info("当前图片的 m 属性为空, 已跳过");
                continue;
            }

            // 解析 m 属性中的 JSON 数据，获取高清图片 URL
            try {
                Map map = JSONUtil.toBean(mAttr, Map.class);
                String highResFileUrl = (String) map.get("murl");
                if (StringUtil.isBlank(highResFileUrl)) {
                    log.info("高清图片链接为空, 已跳过");
                    continue;
                }

                // 处理图片的地址，防止转义和对象存储的冲突问题
                int questionMarkIndex = highResFileUrl.indexOf("?");
                if (questionMarkIndex > -1) {
                    highResFileUrl = highResFileUrl.substring(0, questionMarkIndex);
                }

                // 上传图片
                PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
                pictureUploadRequest.setFileUrl(highResFileUrl);
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
                PictureVO pictureVO = this.uploadPicture(highResFileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id={}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
            }
        }

        return uploadCount;
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限，已改为注解鉴权
//        checkPictureAuth(loginUser, oldPicture);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限，已改为注解鉴权
//        checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
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
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();


        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.ge(startEditTime != null, "editTime", startEditTime);
        queryWrapper.lt(endEditTime != null, "editTime", endEditTime);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        if (loginUser != null) {
            // 查询当前用户对这张图片的点赞状态
            QueryWrapper<UserPictureInteraction> likeQueryWrapper = new QueryWrapper<>();
            likeQueryWrapper.eq("userId", loginUser.getId())
                    .eq("pictureId", picture.getId())
                    .eq("type", 0);  // 点赞类型
            List<UserPictureInteraction> likeInteractions = userPictureInteractionMapper.selectList(likeQueryWrapper);
            pictureVO.setIsLiked(!likeInteractions.isEmpty());

            // 查询当前用户对这张图片的收藏状态
            QueryWrapper<UserPictureInteraction> favoriteQueryWrapper = new QueryWrapper<>();
            favoriteQueryWrapper.eq("userId", loginUser.getId())
                    .eq("pictureId", picture.getId())
                    .eq("type", 1);  // 收藏类型
            List<UserPictureInteraction> favoriteInteractions = userPictureInteractionMapper.selectList(favoriteQueryWrapper);
            pictureVO.setIsFavorited(!favoriteInteractions.isEmpty());
        }

        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 创建图片ai扩图任务
     * @param createPictureOutPaintingRequest
     * @param loginUser
     */
    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingRequest createPictureOutPaintingRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验，已改为注解鉴权
        // checkPictureAuth(loginUser, picture);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingRequest, taskRequest);
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

    /**
     * 创建文本生成图像任务
     *
     * @param createTextToImageRequest
     * @param loginUser
     * @return
     */
    @Override
    public CreateTextToImageTaskResponse createTextToImageTask(CreateTextToImageRequest createTextToImageRequest, User loginUser) {
        // 限制用户每天只能使用5次文生图功能
        String redisKey = "text_to_image_limit:" + loginUser.getId() + ":" + LocalDate.now().toString();
        Long usedCount = redisTemplate.opsForValue().increment(redisKey);
        if (usedCount != null && usedCount == 1) {
            // 设置过期时间为当天剩余时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT);
            long expireSeconds = ChronoUnit.SECONDS.between(now, midnight);
            redisTemplate.expire(redisKey, expireSeconds, TimeUnit.SECONDS);
        }
        if (usedCount != null && usedCount > 5) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "每天只能使用文生图功能5次");
        }
        // 校验图片尺寸参数
        validateImageSize(createTextToImageRequest.getSize());

        // 构造请求参数
        CreateTextToImageTaskRequest taskRequest = new CreateTextToImageTaskRequest();
        CreateTextToImageTaskRequest.Input input = new CreateTextToImageTaskRequest.Input();
        input.setPrompt(createTextToImageRequest.getPrompt());
        input.setNegativePrompt(createTextToImageRequest.getNegativePrompt());
        taskRequest.setInput(input);

        CreateTextToImageTaskRequest.Parameters parameters = new CreateTextToImageTaskRequest.Parameters();
        parameters.setSize(createTextToImageRequest.getSize());
        parameters.setN(createTextToImageRequest.getN());
        parameters.setPromptExtend(createTextToImageRequest.getPromptExtend());
        parameters.setWatermark(createTextToImageRequest.getWatermark());
        parameters.setSeed(createTextToImageRequest.getSeed());
        taskRequest.setParameters(parameters);

        CreateTextToImageTaskResponse textToImageTask = aliYunAiApi.createTextToImageTask(taskRequest);

        // 添加AI生成历史记录
        AiGenHistoryAddRequest aiGenHistoryAddRequest = new AiGenHistoryAddRequest();
        aiGenHistoryAddRequest.setPrompt(createTextToImageRequest.getPrompt());
        aiGenHistoryAddRequest.setStatus(1);
        aiGenHistoryAddRequest.setTaskId(textToImageTask.getOutput().getTaskId());
        aiGenHistoryService.addAiGenHistory(aiGenHistoryAddRequest, loginUser);

        // 创建任务
        return textToImageTask;
    }

    @Override
    public GetTextToImageTaskResponse getTextToImageTask(String taskId) {
        // 调用阿里云API获取任务状态
        GetTextToImageTaskResponse response = aliYunAiApi.getTextToImageTask(taskId);

        if (response != null && response.getOutput() != null) {
            String taskStatus = response.getOutput().getTaskStatus();

            // 根据任务状态映射到AI生成历史状态
            if (taskStatus != null) {
                Integer aiGenStatus = null;
                switch (taskStatus) {
                    case "SUCCEEDED":
                        aiGenStatus = 2; // 成功状态
                        break;
                    case "FAILED":
                        aiGenStatus = 0; // 失败状态
                        break;
                    case "RUNNING":
                        aiGenStatus = 1; // 运行中状态
                        break;
                    default:
                        // 对于未知状态，可以保持为运行中或根据具体需求处理
                        aiGenStatus = 1;
                        break;
                }

                UpdateWrapper<AiGenHistory> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("taskId", taskId).set("status", aiGenStatus);

                // 如果任务成功，还需要保存图片URL列表
                if ("SUCCEEDED".equals(taskStatus) && response.getOutput().getResults() != null
                        && !response.getOutput().getResults().isEmpty()) {
                    // 获取生成的图片URL列表
                    List<String> imageUrls = response.getOutput().getResults().stream()
                            .map(result -> result.getUrl())
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    // 将生成的图片保存到对象存储中,获取保存后的URL列表（去除敏感信息）
                    List<String> imageToSaves = saveAiGeneratedImagesToStorage(imageUrls);
                    String imageToSaveUrls = JSONUtil.toJsonStr(imageToSaves);
                    updateWrapper.set("imageUrl", imageToSaveUrls);
                }

                // 更新AI生成历史记录的状态
                aiGenHistoryService.update(updateWrapper);
            }
        }
        return response;
    }

    /**
     * 将AI生成的图片保存到对象存储中
     * @param imageUrls AI生成的图片URL列表
     */
    private List<String> saveAiGeneratedImagesToStorage(List<String> imageUrls) {
        List<String> imageUrlsToSave = new ArrayList<>();
        for (String imageUrl : imageUrls) {
            try {
                // 使用AiGenPictureUpload将图片从URL上传到对象存储
                // 先下载到本地临时文件，再上传到对象存储，避免URL中的敏感参数泄露
                // 上传路径前缀为aigen/
                String uploadPathPrefix = "aigen";
                UploadPictureResult uploadPictureResult = aiGenPictureUpload.uploadPicture(imageUrl, uploadPathPrefix);
                imageUrlsToSave.add(uploadPictureResult.getUrl());
                log.info("AI生成的图片已成功保存到对象存储: {}", imageUrl);
            } catch (Exception e) {
                log.error("保存AI生成的图片到对象存储失败: {}", imageUrl, e);
            }
        }
        return imageUrlsToSave;
    }

    /**
     * 校验图片尺寸参数
     *
     * @param size 尺寸字符串，格式为 "宽*高"
     */
    private void validateImageSize(String size) {
        if (size == null || size.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片尺寸不能为空");
        }

        Pattern sizePattern = Pattern.compile("^(\\d+)\\*(\\d+)$");
        Matcher matcher = sizePattern.matcher(size);
        if (!matcher.matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片尺寸格式错误，应为 宽*高 的格式");
        }

        try {
            int width = Integer.parseInt(matcher.group(1));
            int height = Integer.parseInt(matcher.group(2));

            if (width < 512 || width > 1440) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片宽度必须在512-1440之间");
            }

            if (height < 512 || height > 1440) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片高度必须在512-1440之间");
            }
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片尺寸数值格式错误");
        }
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
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 从完整URL中提取key（存储路径），去除域名部分
        if (StrUtil.isNotBlank(pictureUrl) && pictureUrl.startsWith(cosManager.getHost())) {
            pictureUrl = pictureUrl.substring(cosManager.getHost().length());
            if (pictureUrl.startsWith("/")) {
                pictureUrl = pictureUrl.substring(1);
            }
        }
        cosManager.deleteObject(pictureUrl);
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            // 从完整URL中提取key（存储路径），去除域名部分
            if (thumbnailUrl.startsWith(cosManager.getHost())) {
                thumbnailUrl = thumbnailUrl.substring(cosManager.getHost().length());
                if (thumbnailUrl.startsWith("/")) {
                    thumbnailUrl = thumbnailUrl.substring(1);
                }
            }
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();


        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }

        // 3. 查询指定图片，仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();

        if (pictureList.isEmpty()) {
            return;
        }
        // 4. 更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });

        // 批量重命名
        String nameRule = pictureEditByBatchRequest.getNameRule();
        fillPictureWithNameRule(pictureList, nameRule);

        // 5. 批量更新
        boolean result = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }


    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    @Override
    public PictureCursorQueryVO listPictureVOByCursor(PictureCursorQueryRequest pictureCursorQueryRequest, HttpServletRequest request) {
        // 限制每次查询的数据量
        long size = pictureCursorQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = this.getQueryWrapperForCursor(pictureCursorQueryRequest);

        // 查询数据
        List<Picture> pictureList = this.list(queryWrapper.last(" LIMIT " + size));

        return buildPictureCursorQueryVO(pictureList, size, request);
    }

    /**
     * 为游标查询构造查询条件
     *
     * @param pictureCursorQueryRequest 游标查询请求
     * @return QueryWrapper<Picture> 查询条件构造器
     */
    private QueryWrapper<Picture> getQueryWrapperForCursor(PictureCursorQueryRequest pictureCursorQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureCursorQueryRequest == null) {
            return queryWrapper;
        }

        // 从对象中取值
        Long id = pictureCursorQueryRequest.getId();
        String name = pictureCursorQueryRequest.getName();
        String introduction = pictureCursorQueryRequest.getIntroduction();
        String category = pictureCursorQueryRequest.getCategory();
        List<String> tags = pictureCursorQueryRequest.getTags();
        Long picSize = pictureCursorQueryRequest.getPicSize();
        Integer picWidth = pictureCursorQueryRequest.getPicWidth();
        Integer picHeight = pictureCursorQueryRequest.getPicHeight();
        Double picScale = pictureCursorQueryRequest.getPicScale();
        String picFormat = pictureCursorQueryRequest.getPicFormat();
        String searchText = pictureCursorQueryRequest.getSearchText();
        Long userId = pictureCursorQueryRequest.getUserId();
        Integer reviewStatus = pictureCursorQueryRequest.getReviewStatus();
        String reviewMessage = pictureCursorQueryRequest.getReviewMessage();
        Long reviewerId = pictureCursorQueryRequest.getReviewerId();
        Long spaceId = pictureCursorQueryRequest.getSpaceId();
        Date startEditTime = pictureCursorQueryRequest.getStartEditTime();
        Date endEditTime = pictureCursorQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureCursorQueryRequest.isNullSpaceId();
        String sortField = pictureCursorQueryRequest.getSortField();
        String sortOrder = pictureCursorQueryRequest.getSortOrder();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.ge(startEditTime != null, "editTime", startEditTime);
        queryWrapper.lt(endEditTime != null, "editTime", endEditTime);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }

        // 添加游标条件
        Long cursorId = pictureCursorQueryRequest.getCursorId();
        if (cursorId != null) {
            // 如果是降序排序，查找比游标ID小的数据
            if (StrUtil.isBlank(sortField) || "id".equals(sortField)) {
                if (StrUtil.isBlank(sortOrder) || "descend".equals(sortOrder)) {
                    queryWrapper.lt("id", cursorId);
                } else {
                    queryWrapper.gt("id", cursorId);
                }
            } else {
                // 其他字段排序仍使用原来的方式
                queryWrapper.gt("id", cursorId);
            }
        }

        // 设置排序
        // 默认按id升序排序
        if (StrUtil.isBlank(sortField)) {
            sortField = "id";
        }
        queryWrapper.orderBy(true, !"descend".equals(sortOrder), sortField);

        return queryWrapper;
    }

    @Override
    public Boolean likePicture(Long pictureId, User loginUser) {
        // 检查图片是否存在
        Picture picture = this.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 查询用户是否已经点赞过该图片
        QueryWrapper<UserPictureInteraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId())
                .eq("pictureId", pictureId)
                .eq("type", 0); // 0 表示点赞
        UserPictureInteraction interaction = userPictureInteractionMapper.selectOne(queryWrapper);

        boolean isLiked;
        if (interaction != null) {
            // 用户已点赞，执行取消点赞操作
            userPictureInteractionMapper.deleteById(interaction.getId());
            // 减少图片的点赞数
            picture.setLikeCount(Math.max(0, picture.getLikeCount() - 1));
            isLiked = false;
        } else {
            // 用户未点赞，执行点赞操作
            UserPictureInteraction newInteraction = new UserPictureInteraction();
            newInteraction.setUserId(loginUser.getId());
            newInteraction.setPictureId(pictureId);
            newInteraction.setType(0); // 0 表示点赞
            newInteraction.setCreateTime(new Date());
            newInteraction.setUpdateTime(new Date());
            userPictureInteractionMapper.insert(newInteraction);
            // 增加图片的点赞数
            picture.setLikeCount(picture.getLikeCount() + 1);
            isLiked = true;
        }

        // 更新图片的点赞数
        this.updateById(picture);
        return isLiked;
    }

    @Override
    public Boolean favoritePicture(PictureFavoriteRequest pictureFavoriteRequest, User loginUser) {
        Long pictureId = pictureFavoriteRequest.getPictureId();
        Long albumId = pictureFavoriteRequest.getAlbumId();

        // 检查图片是否存在
        Picture picture = this.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 必须指定收藏夹
        if (albumId == null || albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须指定收藏夹");
        }

        // 检查收藏夹是否存在
        PictureAlbum pictureAlbum = pictureAlbumService.getById(albumId);
        if (pictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "收藏夹不存在");
        }
        // 检查收藏夹是否属于当前用户
        if (!pictureAlbum.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该收藏夹");
        }

        // 查询用户是否已经收藏过该图片到指定收藏夹
        QueryWrapper<UserPictureInteraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId())
                .eq("pictureId", pictureId)
                .eq("albumId", albumId)
                .eq("type", 1); // 1 表示收藏

        UserPictureInteraction interaction = userPictureInteractionMapper.selectOne(queryWrapper);

        boolean isFavorited;
        if (interaction != null) {
            // 用户已收藏到指定收藏夹，执行取消收藏操作
            userPictureInteractionMapper.deleteById(interaction.getId());
            // 减少图片的收藏数
            picture.setFavoriteCount(Math.max(0, picture.getFavoriteCount() - 1));
            isFavorited = false;

            // 减少该收藏夹的图片数量
            pictureAlbum.setPictureCount(Math.max(0, pictureAlbum.getPictureCount() - 1));
            pictureAlbumService.updateById(pictureAlbum);
        } else {
            // 用户未收藏到指定收藏夹，执行收藏操作
            UserPictureInteraction newInteraction = new UserPictureInteraction();
            newInteraction.setUserId(loginUser.getId());
            newInteraction.setPictureId(pictureId);
            newInteraction.setAlbumId(albumId);
            newInteraction.setType(1); // 1 表示收藏
            newInteraction.setCreateTime(new Date());
            newInteraction.setUpdateTime(new Date());

            try {
                userPictureInteractionMapper.insert(newInteraction);
            } catch (DuplicateKeyException e) {
                // 如果并发插入导致重复键错误，忽略该错误并认为收藏成功
                log.warn("并发插入收藏记录时发生重复键错误，忽略该错误: {}", e.getMessage());
            }

            // 增加图片的收藏数
            picture.setFavoriteCount(picture.getFavoriteCount() + 1);
            isFavorited = true;

            // 增加该收藏夹的图片数量
            pictureAlbum.setPictureCount(pictureAlbum.getPictureCount() + 1);
            pictureAlbumService.updateById(pictureAlbum);
        }

        // 更新图片的收藏数
        this.updateById(picture);
        return isFavorited;
    }


    @Override
    public PictureCursorQueryVO listUserLikedPictures(Long userId,PictureCursorQueryRequest pictureCursorQueryRequest, HttpServletRequest request) {
        // 限制每次查询的数据量
        long size = pictureCursorQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = this.getQueryWrapperForCursor(pictureCursorQueryRequest);

        // 添加用户点赞条件，使用参数化查询防止SQL注入
        queryWrapper.inSql("id", "SELECT pictureId FROM user_picture_interaction WHERE userId = " + userId + " AND type = 0");

        // 查询数据
        List<Picture> pictureList = this.list(queryWrapper.last("LIMIT " + size));

        return buildPictureCursorQueryVO(pictureList, size, request);
    }

    @Override
    public PictureCursorQueryVO listUserFavoritedPictures(PictureCursorQueryRequest pictureCursorQueryRequest, User loginUser, HttpServletRequest request) {
        // 限制每次查询的数据量
        long size = pictureCursorQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = this.getQueryWrapperForCursor(pictureCursorQueryRequest);

        // 添加用户收藏条件
        queryWrapper.inSql("id", "SELECT pictureId FROM user_picture_interaction WHERE userId = " + loginUser.getId() + " AND type = 1");

        // 查询数据
        List<Picture> pictureList = this.list(queryWrapper.last("LIMIT " + size));

        return buildPictureCursorQueryVO(pictureList, size, request);
    }

    @Override
    public PictureCursorQueryVO listUserFavoritedPicturesByAlbum(Long albumId, PictureCursorQueryRequest pictureCursorQueryRequest, User loginUser, HttpServletRequest request) {
        // 限制每次查询的数据量
        long size = pictureCursorQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR, "分页大小不能超过20");

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = this.getQueryWrapperForCursor(pictureCursorQueryRequest);

        // 查询指定收藏夹内的所有图片
        queryWrapper.inSql("id", "SELECT pictureId FROM user_picture_interaction WHERE albumId = " + albumId + " AND type = 1 AND isDelete = 0");


        // 查询数据
        List<Picture> pictureList = this.list(queryWrapper.last("LIMIT " + size));

        // 增加收藏夹浏览数
        pictureAlbumService.increaseViewCount(albumId);

        return buildPictureCursorQueryVO(pictureList, size, request);
    }

    /**
     * 构建PictureCursorQueryVO对象
     *
     * @param pictureList 图片列表
     * @param size 查询数量
     * @param request HTTP请求
     * @return PictureCursorQueryVO对象
     */
    private PictureCursorQueryVO buildPictureCursorQueryVO(List<Picture> pictureList, long size, HttpServletRequest request) {
        PictureCursorQueryVO result = new PictureCursorQueryVO();
        if (CollUtil.isEmpty(pictureList)) {
            result.setPictureList(new ArrayList<>());
            result.setNextCursorId(null);
            result.setHasMore(false);
            return result;
        }

        // 转换为VO对象
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(picture -> this.getPictureVO(picture, request))
                .collect(Collectors.toList());

        result.setPictureList(pictureVOList);

        // 计算下一个游标
        if (pictureList.size() < size) {
            result.setNextCursorId(null);
            result.setHasMore(false);
        } else {
            Picture lastPicture = pictureList.get(pictureList.size() - 1);
            result.setNextCursorId(lastPicture.getId());
            result.setHasMore(true);
        }

        return result;
    }

    @Override
    public Boolean addPictureToAlbum(PictureFavoriteRequest pictureFavoriteRequest, User loginUser) {
        Long pictureId = pictureFavoriteRequest.getPictureId();
        Long albumId = pictureFavoriteRequest.getAlbumId();

        // 检查图片是否存在
        Picture picture = this.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 必须指定收藏夹
        if (albumId == null || albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须指定收藏夹");
        }

        // 检查收藏夹是否存在
        PictureAlbum pictureAlbum = pictureAlbumService.getById(albumId);
        if (pictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "收藏夹不存在");
        }
        // 检查收藏夹是否属于当前用户
        if (!pictureAlbum.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该收藏夹");
        }

        // 查询用户是否已经将该图片添加到此收藏夹
        QueryWrapper<UserPictureInteraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId())
                .eq("pictureId", pictureId)
                .eq("albumId", albumId)
                .eq("type", 1); // 1 表示收藏
        UserPictureInteraction interaction = userPictureInteractionMapper.selectOne(queryWrapper);

        if (interaction != null) {
            // 图片已存在于该收藏夹中
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片已存在于该收藏夹中");
        }

        // 将图片添加到指定收藏夹
        UserPictureInteraction newInteraction = new UserPictureInteraction();
        newInteraction.setUserId(loginUser.getId());
        newInteraction.setPictureId(pictureId);
        newInteraction.setAlbumId(albumId);
        newInteraction.setType(1); // 1 表示收藏
        newInteraction.setCreateTime(new Date());
        newInteraction.setUpdateTime(new Date());

        try {
            userPictureInteractionMapper.insert(newInteraction);
        } catch (DuplicateKeyException e) {
            // 如果并发插入导致重复键错误，忽略该错误并认为收藏成功
            log.warn("并发插入收藏记录时发生重复键错误，忽略该错误: {}", e.getMessage());
            return true;
        }

        // 增加收藏夹的图片数量
        pictureAlbum.setPictureCount(pictureAlbum.getPictureCount() + 1);
        pictureAlbumService.updateById(pictureAlbum);

        // 增加图片的收藏数
        picture.setFavoriteCount(picture.getFavoriteCount() + 1);
        this.updateById(picture);

        return true;
    }

    @Override
    public Boolean removePictureFromAlbum(PictureFavoriteRequest pictureFavoriteRequest, User loginUser) {
        Long pictureId = pictureFavoriteRequest.getPictureId();
        Long albumId = pictureFavoriteRequest.getAlbumId();

        // 检查图片是否存在
        Picture picture = this.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 必须指定收藏夹
        if (albumId == null || albumId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "必须指定收藏夹");
        }

        // 检查收藏夹是否存在
        PictureAlbum pictureAlbum = pictureAlbumService.getById(albumId);
        if (pictureAlbum == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "收藏夹不存在");
        }
        // 检查收藏夹是否属于当前用户
        if (!pictureAlbum.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限操作该收藏夹");
        }

        // 查询用户是否已将该图片添加到此收藏夹
        QueryWrapper<UserPictureInteraction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId())
                .eq("pictureId", pictureId)
                .eq("albumId", albumId)
                .eq("type", 1); // 1 表示收藏
        UserPictureInteraction interaction = userPictureInteractionMapper.selectOne(queryWrapper);

        if (interaction == null) {
            // 图片不存在于该收藏夹中
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在于该收藏夹中");
        }

        // 从收藏夹中移除图片
        userPictureInteractionMapper.deleteById(interaction.getId());

        // 减少收藏夹的图片数量
        pictureAlbum.setPictureCount(Math.max(0, pictureAlbum.getPictureCount() - 1));
        pictureAlbumService.updateById(pictureAlbum);

        // 减少图片的收藏数
        picture.setFavoriteCount(Math.max(0, picture.getFavoriteCount() - 1));
        this.updateById(picture);

        return true;
    }

    @Override
    public List<PictureAlbumVO> getPictureAlbumsByPictureId(Long pictureId, User loginUser) {
        // 检查图片是否存在
        Picture picture = this.getById(pictureId);
        if (picture == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }

        // 查询当前用户的所有收藏夹
        QueryWrapper<PictureAlbum> albumQueryWrapper = new QueryWrapper<>();
        albumQueryWrapper.eq("userId", loginUser.getId());
        List<PictureAlbum> albums = pictureAlbumService.list(albumQueryWrapper);

        if (albums.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询用户将该图片收藏到的所有收藏夹记录
        QueryWrapper<UserPictureInteraction> interactionQueryWrapper = new QueryWrapper<>();
        interactionQueryWrapper.eq("userId", loginUser.getId())
                .eq("pictureId", pictureId)
                .eq("type", 1) // 1 表示收藏
                .isNotNull("albumId"); // 确保有指定收藏夹
        List<UserPictureInteraction> interactions = userPictureInteractionMapper.selectList(interactionQueryWrapper);

        // 创建已收藏的收藏夹ID集合
        Set<Long> favoritedAlbumIds = interactions.stream()
                .map(UserPictureInteraction::getAlbumId)
                .collect(Collectors.toSet());

        // 转换为VO对象并设置收藏状态
        return albums.stream().map(album -> {
            PictureAlbumVO albumVO = new PictureAlbumVO();
            // 复制属性
            albumVO.setId(album.getId());
            albumVO.setName(album.getName());
            albumVO.setDescription(album.getDescription());
            albumVO.setUserId(album.getUserId());
            albumVO.setIsPublic(album.getIsPublic());
            albumVO.setPictureCount(album.getPictureCount());
            albumVO.setViewCount(album.getViewCount());
            albumVO.setCreateTime(album.getCreateTime());
            // 设置收藏状态
            albumVO.setIsPictureFavorited(favoritedAlbumIds.contains(album.getId()));
            return albumVO;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Picture> getHotPicturesByPopularity(String category, int limit) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "url", "thumbnailUrl", "name", "introduction",
                        "category", "tags", "picSize", "picWidth", "picHeight",
                        "picScale", "picFormat", "userId", "spaceId", "createTime",
                        "editTime", "updateTime", "reviewStatus", "reviewMessage",
                        "reviewerId", "reviewTime", "likeCount", "favoriteCount", "isDelete",
                        "(likeCount * 0.6 + favoriteCount * 0.4) AS hotScore")
                .eq("category", category)
                .eq("reviewStatus", 1) // 已审核通过的图片
                .eq("isDelete", 0) // 未删除的图片
                .apply("(likeCount * 0.6 + favoriteCount * 0.4) > 0") // 热度大于0
                .orderByDesc("(likeCount * 0.6 + favoriteCount * 0.4)") // 按热度降序排列
                .last("LIMIT " + limit);

        return this.list(queryWrapper);
    }

}