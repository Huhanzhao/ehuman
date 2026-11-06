CREATE TABLE "tb_bot_owner" (
  "id" INTEGER NOT NULL,
  "user_id" INTEGER NOT NULL,
  "create_time" DATE,
  "update_time" DATE,
  PRIMARY KEY ("id")
);

CREATE TABLE "tb_bot_whitelist" (
  "id" INTEGER NOT NULL,
  "user_id" INTEGER NOT NULL,
  "nickname" TEXT,
  "create_time" DATE,
  "update_time" DATE,
  "delete_flag" INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

CREATE UNIQUE INDEX "idx_tb_bot_whitelist_user_id_active"
ON "tb_bot_whitelist" ("user_id") WHERE "delete_flag" = 0;

-- 旧版本主动消息任务为全局单任务，格式已不兼容，升级时清理
DELETE FROM "tb_quartz_job"
WHERE "job_name" LIKE 'nextSendMessage_%' OR "job_name" LIKE 'callNextSendMessageJob_%';
