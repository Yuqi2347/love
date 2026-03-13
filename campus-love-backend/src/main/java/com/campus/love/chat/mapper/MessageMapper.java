package com.campus.love.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.love.chat.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /** 查询聊天历史（含已撤回消息，绕过 MyBatis-Plus 逻辑删除过滤） */
    List<Message> selectPageForChatHistory(
            @Param("currentUserId") Long currentUserId,
            @Param("otherUserId") Long otherUserId,
            @Param("offset") int offset,
            @Param("size") int size);

    /** 撤回消息：直接写 SQL 绕过 MyBatis-Plus 对 logic-delete-field 的拦截（updateById 会跳过 deleted 字段） */
    @Update("UPDATE t_message SET deleted = 1 WHERE id = #{messageId}")
    int markAsRecalled(@Param("messageId") Long messageId);
}
