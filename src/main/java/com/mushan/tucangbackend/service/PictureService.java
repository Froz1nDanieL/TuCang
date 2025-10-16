package com.mushan.tucangbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mushan.tucangbackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.mushan.tucangbackend.api.aliyunai.model.CreateTextToImageTaskResponse;
import com.mushan.tucangbackend.api.aliyunai.model.GetTextToImageTaskResponse;
import com.mushan.tucangbackend.model.dto.picture.*;
import com.mushan.tucangbackend.model.entity.Picture;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.PictureAlbumVO;
import com.mushan.tucangbackend.model.vo.PictureCursorQueryVO;
import com.mushan.tucangbackend.model.vo.PictureVO;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Danie
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-07-29 15:19:40
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );


    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    void deletePicture(long pictureId, User loginUser);

    /**
     * 获取查询包装类
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片单条封装类
     *
     * @param picture
     * @param request
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);


    /**
     * 获取图片分页封装类
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingRequest createPictureOutPaintingRequest, User loginUser);
    
    /**
     * 创建文本生成图像任务
     *
     * @param createTextToImageRequest
     * @param loginUser
     * @return
     */
    CreateTextToImageTaskResponse createTextToImageTask(CreateTextToImageRequest createTextToImageRequest, User loginUser);

    /**
     *  查询文本生成图像任务
     *
     *  @param taskId
     *  @return
     */
    GetTextToImageTaskResponse getTextToImageTask(String taskId);

    /**
     * 校验图片
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    void clearPictureFile(Picture oldPicture);


    void checkPictureAuth(User loginUser, Picture picture);

    @Transactional(rollbackFor = Exception.class)
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
    
    /**
     * 游标查询图片
     *
     * @param pictureCursorQueryRequest
     * @param request
     * @return
     */
    PictureCursorQueryVO listPictureVOByCursor(PictureCursorQueryRequest pictureCursorQueryRequest, HttpServletRequest request);

    /**
     * 点赞/取消点赞图片
     *
     * @param pictureId 图片ID
     * @param loginUser 当前登录用户
     * @return 操作后是否点赞状态
     */
    Boolean likePicture(Long pictureId, User loginUser);

    /**
     * 收藏/取消收藏图片到收藏夹
     *
     * @param pictureFavoriteRequest 收藏请求
     * @param loginUser 当前登录用户
     * @return 操作后是否收藏状态
     */
    Boolean favoritePicture(PictureFavoriteRequest pictureFavoriteRequest, User loginUser);

    
    /**
     * 游标查询用户收藏的图片列表
     *
     * @param pictureCursorQueryRequest 游标查询请求
     * @param loginUser 当前登录用户
     * @param request HTTP请求
     * @return PictureCursorQueryVO对象
     */
    PictureCursorQueryVO listUserFavoritedPictures(PictureCursorQueryRequest pictureCursorQueryRequest, User loginUser, HttpServletRequest request);
    
    /**
     * 游标查询指定用户点赞的图片列表
     * 
     * @param userId 用户ID
     * @param pictureCursorQueryRequest 游标查询请求
     * @param request HTTP请求
     * @return PictureCursorQueryVO对象
     */
    PictureCursorQueryVO listUserLikedPictures(Long userId, PictureCursorQueryRequest pictureCursorQueryRequest, HttpServletRequest request);
    
    /**
     * 游标查询指定收藏夹内的图片列表
     *
     * @param albumId 收藏夹ID
     * @param pictureCursorQueryRequest 游标查询请求
     * @param loginUser 当前登录用户
     * @param request HTTP请求
     * @return PictureCursorQueryVO对象
     */
    PictureCursorQueryVO listUserFavoritedPicturesByAlbum(Long albumId, PictureCursorQueryRequest pictureCursorQueryRequest, User loginUser, HttpServletRequest request);

    Boolean addPictureToAlbum(PictureFavoriteRequest pictureFavoriteRequest, User loginUser);

    Boolean removePictureFromAlbum(PictureFavoriteRequest pictureFavoriteRequest, User loginUser);

    /**
     * 获取图片被收藏到的所有收藏夹
     *
     * @param pictureId 图片ID
     * @param loginUser 当前登录用户
     * @return 收藏夹列表
     */
    List<PictureAlbumVO> getPictureAlbumsByPictureId(Long pictureId, User loginUser);
    
    /**
     * 根据分类获取热门图片（按点赞数和收藏数的权重排序）
     * 热度 = 点赞数 * 0.6 + 收藏数 * 0.4
     * 
     * @param category 分类
     * @param limit 限制数量
     * @return 热门图片列表
     */
    List<Picture> getHotPicturesByPopularity(String category, int limit);
}