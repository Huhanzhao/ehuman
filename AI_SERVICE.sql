CREATE TABLE `TB_AI_CHAT_MEMORY` (
                                     `ID` bigint NOT NULL COMMENT '主键id',
                                     `MEMORY_ID` bigint NOT NULL COMMENT '记忆id',
                                     `MESSAGE` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '聊天消息',
                                     `ROLE` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色',
                                     `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
                                     `UPDATE_TIME` datetime NOT NULL COMMENT '更新时间',
                                     `DELETE_FLAG` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
                                     PRIMARY KEY (`ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;



CREATE TABLE `tb_user_grace` (
                                 `id` bigint NOT NULL,
                                 `user_id` bigint DEFAULT NULL COMMENT '用户id',
                                 `grace` int DEFAULT NULL COMMENT '用户好感度0-100',
                                 `create_by` varchar(255) DEFAULT NULL COMMENT '创建人',
                                 `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(255) DEFAULT NULL COMMENT '更新人',
                                 `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                 `delete_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '删除标识',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `grace_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;