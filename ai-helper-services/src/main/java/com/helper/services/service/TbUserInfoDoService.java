package com.helper.services.service;

import com.helper.services.entity.TbUserInfoDo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_info】的数据库操作Service
* @createDate 2026-03-08 20:04:47
*/
public interface TbUserInfoDoService extends IService<TbUserInfoDo> {

    List<TbUserInfoDo> getUserInfoByUserId(Long userId);
}
