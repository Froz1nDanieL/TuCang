package com.mushan.tucangbackend.service.impl;

import com.mushan.tucangbackend.api.aliyunai.AliYunAiApi;
import com.mushan.tucangbackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.mushan.tucangbackend.api.aliyunai.model.GetTextToImageTaskResponse;
import com.mushan.tucangbackend.exception.BusinessException;
import com.mushan.tucangbackend.exception.ErrorCode;
import com.mushan.tucangbackend.model.entity.AiGenHistory;
import com.mushan.tucangbackend.model.entity.User;
import com.mushan.tucangbackend.model.enums.AiGenerationTaskTypeEnum;
import com.mushan.tucangbackend.service.AiGenHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiGenerationTaskAuthorizationTest {

    private PictureServiceImpl pictureService;
    private AiGenHistoryService aiGenHistoryService;
    private AliYunAiApi aliYunAiApi;
    private User loginUser;

    @BeforeEach
    void setUp() {
        pictureService = new PictureServiceImpl();
        aiGenHistoryService = mock(AiGenHistoryService.class);
        aliYunAiApi = mock(AliYunAiApi.class);
        ReflectionTestUtils.setField(pictureService, "aiGenHistoryService", aiGenHistoryService);
        ReflectionTestUtils.setField(pictureService, "aliYunAiApi", aliYunAiApi);

        loginUser = new User();
        loginUser.setId(2L);
    }

    @Test
    void textToImageQueryRejectsTaskNotOwnedByCurrentUser() {
        when(aiGenHistoryService.getOwnedTask(
                "other-task",
                loginUser.getId(),
                AiGenerationTaskTypeEnum.TEXT_TO_IMAGE.getValue()))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pictureService.getTextToImageTask("other-task", loginUser));

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        verify(aliYunAiApi, never()).getTextToImageTask("other-task");
    }

    @Test
    void textToImageQueryAllowsTaskOwnedByCurrentUser() {
        AiGenHistory ownedTask = ownedTask(10L);
        GetTextToImageTaskResponse providerResponse = new GetTextToImageTaskResponse();
        when(aiGenHistoryService.getOwnedTask(
                "own-task",
                loginUser.getId(),
                AiGenerationTaskTypeEnum.TEXT_TO_IMAGE.getValue()))
                .thenReturn(ownedTask);
        when(aliYunAiApi.getTextToImageTask("own-task")).thenReturn(providerResponse);

        GetTextToImageTaskResponse result =
                pictureService.getTextToImageTask("own-task", loginUser);

        assertSame(providerResponse, result);
        verify(aliYunAiApi).getTextToImageTask("own-task");
    }

    @Test
    void outPaintingQueryRejectsTaskNotOwnedByCurrentUser() {
        when(aiGenHistoryService.getOwnedTask(
                "other-out-painting-task",
                loginUser.getId(),
                AiGenerationTaskTypeEnum.OUT_PAINTING.getValue()))
                .thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pictureService.getPictureOutPaintingTask(
                        "other-out-painting-task", loginUser));

        assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        verify(aliYunAiApi, never()).getOutPaintingTask("other-out-painting-task");
    }

    @Test
    void outPaintingQueryAllowsTaskOwnedByCurrentUser() {
        AiGenHistory ownedTask = ownedTask(11L);
        GetOutPaintingTaskResponse providerResponse = new GetOutPaintingTaskResponse();
        when(aiGenHistoryService.getOwnedTask(
                "own-out-painting-task",
                loginUser.getId(),
                AiGenerationTaskTypeEnum.OUT_PAINTING.getValue()))
                .thenReturn(ownedTask);
        when(aliYunAiApi.getOutPaintingTask("own-out-painting-task"))
                .thenReturn(providerResponse);

        GetOutPaintingTaskResponse result =
                pictureService.getPictureOutPaintingTask(
                        "own-out-painting-task", loginUser);

        assertSame(providerResponse, result);
        verify(aliYunAiApi).getOutPaintingTask("own-out-painting-task");
    }

    @Test
    void queryRejectsMissingLoginBeforeCallingProvider() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> pictureService.getTextToImageTask("task", null));

        assertEquals(ErrorCode.NOT_LOGIN_ERROR.getCode(), exception.getCode());
        verify(aliYunAiApi, never()).getTextToImageTask("task");
    }

    private AiGenHistory ownedTask(Long id) {
        AiGenHistory task = new AiGenHistory();
        task.setId(id);
        task.setUserId(loginUser.getId());
        return task;
    }
}
