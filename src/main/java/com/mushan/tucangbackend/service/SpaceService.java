package com.mushan.tucangbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mushan.tucangbackend.model.dto.space.SpaceAddRequest;
import com.mushan.tucangbackend.model.dto.space.SpaceQueryRequest;
import com.mushan.tucangbackend.model.entity.Space;
import com.mushan.tucangbackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Danie
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-08-04 15:01:50
*/
public interface SpaceService extends IService<Space> {


    /**
     * 添加空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     * @param space
     */

    void validSpace(Space space, boolean add);

    /**
     * 获取查询包装类
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间单条封装类
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);


    /**
     * 获取空间分页封装类
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);


    void fillSpaceBySpaceLevel(Space space);

    void checkSpaceAuth(User loginUser, Space space);
}
