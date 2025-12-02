package com.helper.user.entity.dto;

import lombok.Data;

@Data
public class RegisterDto {
    private String email;

    private String userId;

    private String password;

    private Integer role;
}
