package com.mushan.tucangbackend.model.vo.admin;

import com.mushan.tucangbackend.model.vo.UserVO;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

@Data
public class AdminMeVO implements Serializable {

    private UserVO user;
    private Set<String> permissions;

    private static final long serialVersionUID = 1L;
}
