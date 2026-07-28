package com.mushan.tucangbackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureReviewRequest implements Serializable {
  
    /**  
     * id  
     */  
    private Long id;  
  
    /**  
     * 状态：0-待审核, 1-通过, 2-拒绝  
     */  
    private Integer reviewStatus;  
  
    /**  
     * 审核信息  
     */  
    private String reviewMessage;  

    /**
     * 标准拒绝原因。通过时为空。
     */
    private String reasonCode;
  
  
    private static final long serialVersionUID = 1L;  
}
