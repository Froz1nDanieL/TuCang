package com.mushan.tucangbackend.controller;


import com.mushan.tucangbackend.common.BaseResponse;
import com.mushan.tucangbackend.common.ResultUtils;
import com.mushan.tucangbackend.model.entity.AiGenHistory;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.vo.AiGenHistoryVO;
import com.mushan.tucangbackend.service.AiGenHistoryService;
import com.mushan.tucangbackend.manager.auth.SpaceUserAuthManager;
import com.mushan.tucangbackend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/aiGenHistory")
public class AiGenHistoryController {

    @Resource
    private AiGenHistoryService aiGenHistoryService;

    @Resource
    private UserService userService;

    /**
     * 获取当前用户的AI生成历史记录
     *
     * @return AI生成历史记录列表
     */
    @GetMapping("/list")
    public BaseResponse<List<AiGenHistoryVO>> listUserAiGenHistories(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<AiGenHistoryVO> aiGenHistoryList = aiGenHistoryService.listUserAiGenHistories(loginUser.getId());
        return ResultUtils.success(aiGenHistoryList);
    }

}