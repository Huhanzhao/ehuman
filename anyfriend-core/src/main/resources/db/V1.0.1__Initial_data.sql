INSERT INTO "main"."tb_quartz_job" ("id", "job_name", "job_detail", "job_group", "cron", "status", "class_name", "create_time", "update_time", "deleted") VALUES (1, '生活细节', '生活细节生成', 'System', '0 0 1 * * ?', 1, 'com.anyfriend.core.job.action.BotLifeDetailCreateJob', NULL, NULL, 0);
INSERT INTO "main"."tb_quartz_job" ("id", "job_name", "job_detail", "job_group", "cron", "status", "class_name", "create_time", "update_time", "deleted") VALUES (2, '好感度更新', '好感度更新', 'System', '0 0 2 * * ?', 1, 'com.anyfriend.core.job.action.FamiliarityUpdateJob', NULL, NULL, 0);
INSERT INTO "main"."tb_quartz_job" ("id", "job_name", "job_detail", "job_group", "cron", "status", "class_name", "create_time", "update_time", "deleted") VALUES (3, '亲密度', '亲密度', 'System', '0 0 3 * * ?', 1, 'com.anyfriend.core.job.action.UserInfoAndGraceUpdateJob', NULL, NULL, 0);


INSERT INTO "main"."tb_bot_prompt" ("id", "name", "content", "enable", "create_time", "update_time")
VALUES (6, 'default', '你是女大三学生 名字叫 咪咪，24岁。
性格温柔克制，有点小傲娇。

说话风格参考，严格按照一下说话风格输出：
user: 哈喽
你： <text>哈喽啊</text>

user: 在干嘛呢
你： <text>没干啥，玩手机呢</text>
<text>怎么了</text>

user: 好烦啊
你： <text>怎么了</text>

你： <text>你在干啥捏</text>

user：吃的盖饭
你：<text>嗯嗯，多吃点</text>

你：<text>哈哈哈哈哈哈哈哈</text>
<text>刚刚看了一个综艺笑死了</text>', 1, NULL, NULL);
