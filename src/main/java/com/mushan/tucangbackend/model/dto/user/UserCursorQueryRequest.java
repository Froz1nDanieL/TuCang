package com.mushan.tucangbackend.model.dto.user;

import com.mushan.tucangbackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户游标查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserCursorQueryRequest extends PageRequest implements Serializable {

    /**
     * 游标ID，用于分页查询
     */
    private Long cursorId;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（ascend/descend）
     */
    private String sortOrder;

    private static final long serialVersionUID = 1L;
}