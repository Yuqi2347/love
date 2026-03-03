package com.campus.love.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.love.chat.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
