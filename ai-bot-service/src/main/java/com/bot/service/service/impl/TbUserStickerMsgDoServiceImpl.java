package com.bot.service.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bot.service.entity.TbUserStickerMsgDo;
import com.bot.service.mapper.TbUserStickerMsgDoMapper;
import com.bot.service.service.TbUserStickerMsgDoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_sticker_msg】的数据库操作Service实现
* @createDate 2026-05-31 21:57:18
*/
@Service
public class TbUserStickerMsgDoServiceImpl extends ServiceImpl<TbUserStickerMsgDoMapper, TbUserStickerMsgDo>
    implements TbUserStickerMsgDoService{

    @Override
    public String getAllStickerType(Long botId) {
        List<String> list = lambdaQuery().list().stream().map(TbUserStickerMsgDo::getEmo).distinct().toList();
        return String.join(",", list);
    }

    @Override
    public TbUserStickerMsgDo getStickerMsgId(String stickerType) {
        return lambdaQuery()
                .eq(TbUserStickerMsgDo::getEmo, stickerType)
                .last("ORDER BY RANDOM() LIMIT 1")
                .one();
    }
}




