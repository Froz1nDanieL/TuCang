package com.mushan.tucangbackend.service.impl;

import com.mushan.tucangbackend.model.entity.Picture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PictureSpaceUsageTest {

    @Test
    void reuploadShouldOnlyApplySizeDeltaAndKeepPictureCount() {
        PictureServiceImpl pictureService = new PictureServiceImpl();
        Picture oldPicture = new Picture();
        oldPicture.setPicSize(100L);
        Picture newPicture = new Picture();
        newPicture.setPicSize(150L);

        assertEquals(50L, pictureService.calculateSpaceSizeDelta(oldPicture, newPicture));
        assertEquals(0L, pictureService.calculateSpaceCountDelta(oldPicture));
    }

    @Test
    void newUploadShouldApplyFullSizeAndIncreasePictureCount() {
        PictureServiceImpl pictureService = new PictureServiceImpl();
        Picture newPicture = new Picture();
        newPicture.setPicSize(150L);

        assertEquals(150L, pictureService.calculateSpaceSizeDelta(null, newPicture));
        assertEquals(1L, pictureService.calculateSpaceCountDelta(null));
    }
}
