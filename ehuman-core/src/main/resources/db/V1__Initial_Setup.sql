/*
 Navicat Premium Data Transfer

 Source Server         : bot_local
 Source Server Type    : SQLite
 Source Server Version : 3035005
 Source Schema         : main

 Target Server Type    : SQLite
 Target Server Version : 3035005
 File Encoding         : 65001

 Date: 23/08/2026 20:08:33
*/

-- ----------------------------
-- Table structure for tb_ai_chat_memory
-- ----------------------------
CREATE TABLE "tb_ai_chat_memory" (
  "ID" INTEGER NOT NULL,
  "MEMORY_ID" INTEGER NOT NULL,
  "MESSAGE" TEXT NOT NULL,
  "VECTORED" integer NOT NULL DEFAULT 0,
  "ROLE" TEXT NOT NULL,
  "CREATE_TIME" date NOT NULL,
  "UPDATE_TIME" date NOT NULL,
  "DELETE_FLAG" INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY ("ID")
);

-- ----------------------------
-- Table structure for tb_bot_life_detail
-- ----------------------------
CREATE TABLE "tb_bot_life_detail" (
  "id" INTEGER NOT NULL,
  "bot_id" INTEGER NOT NULL,
  "start_time" date NOT NULL,
  "end_time" date NOT NULL,
  "detail" TEXT NOT NULL,
  "create_time" date,
  "update_time" date,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_bot_prompt
-- ----------------------------
CREATE TABLE "tb_bot_prompt" (
  "id" integer NOT NULL,
  "name" TEXT,
  "content" TEXT,
  "enable" integer NOT NULL,
  "create_time" DATE,
  "update_time" DATE,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_chat_config
-- ----------------------------
CREATE TABLE "tb_chat_config" (
  "id" INTEGER NOT NULL,
  "temperature" REAL,
  "user_id" INTEGER,
  "top_p" REAL,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_graph_log
-- ----------------------------
CREATE TABLE "tb_graph_log" (
  "id" TEXT NOT NULL,
  "thread_id" text,
  "node_id" text,
  "state" TEXT,
  "error_message" TEXT,
  "create_time" DATE,
  "update_time" DATE,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_quartz_job
-- ----------------------------
CREATE TABLE "tb_quartz_job" (
  "id" INTEGER NOT NULL,
  "job_name" TEXT,
  "job_detail" TEXT,
  "job_group" TEXT,
  "cron" TEXT,
  "status" INTEGER,
  "class_name" TEXT,
  "create_time" date,
  "update_time" date,
  "deleted" INTEGER DEFAULT 0,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_update_grace_history
-- ----------------------------
CREATE TABLE "tb_update_grace_history" (
  "id" INTEGER NOT NULL,
  "user_id" INTEGER NOT NULL,
  "increase" INTEGER,
  "decrease" INTEGER,
  "reason" TEXT,
  "create_time" date,
  "update_time" date,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_user_bot_status
-- ----------------------------
CREATE TABLE "tb_user_bot_status" (
  "id" INTEGER NOT NULL,
  "user_id" INTEGER,
  "grace" INTEGER,
  "familiarity" INTEGER NOT NULL DEFAULT 0,
  "mood" INTEGER NOT NULL DEFAULT 0,
  "mood_reason" TEXT,
  "create_by" TEXT,
  "create_time" date,
  "update_by" TEXT,
  "update_time" date,
  "delete_flag" INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_user_feature_button
-- ----------------------------
CREATE TABLE "tb_user_feature_button" (
  "id" text NOT NULL,
  "user_id" INTEGER,
  "feature_type" integer NOT NULL DEFAULT 0,
  "enable" integer,
  "create_time" DATE,
  "update_time" DATE,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_user_info
-- ----------------------------
CREATE TABLE "tb_user_info" (
  "id" INTEGER NOT NULL,
  "user_id" INTEGER NOT NULL,
  "user_info" TEXT,
  "create_time" date,
  "update_time" date,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_user_model_config
-- ----------------------------
CREATE TABLE "tb_user_model_config" (
  "id" INTEGER NOT NULL,
  "model" TEXT,
  "node_num" INTEGER NOT NULL DEFAULT 0,
  "user_id" INTEGER,
  "base_url" TEXT,
  "api_key" TEXT,
  "update_time" date,
  "create_time" date,
  "delete_flag" INTEGER NOT NULL DEFAULT 0,
  "category" TEXT,
  "need_proxy" INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Table structure for tb_user_refer_audio
-- ----------------------------
CREATE TABLE "tb_user_refer_audio" (
  "id" INTEGER NOT NULL,
  "user_id" INTEGER,
  "refer_audio_url" TEXT,
  "refer_audio_name" TEXT,
  "type" TEXT,
  "enable" INTEGER NOT NULL DEFAULT 0,
  "create_time" date,
  "update_time" date,
  "delete_flag" INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY ("id")
);

-- ----------------------------
-- Indexes structure for table tb_ai_chat_memory
-- ----------------------------
CREATE INDEX "idx_tb_ai_chat_memory_delete_flag_memory_id"
ON "tb_ai_chat_memory" (
  "DELETE_FLAG" ASC,
  "MEMORY_ID" ASC
);
CREATE INDEX "idx_tb_ai_chat_memory_memory_id"
ON "tb_ai_chat_memory" (
  "MEMORY_ID" ASC
);


-- ----------------------------
-- Indexes structure for table tb_quartz_job
-- ----------------------------
CREATE UNIQUE INDEX "index_job_name"
ON "tb_quartz_job" (
  "job_name" ASC
);

-- ----------------------------
-- Indexes structure for table tb_user_bot_status
-- ----------------------------
CREATE UNIQUE INDEX "idx_tb_user_bot_status_user_id"
ON "tb_user_bot_status" (
  "user_id" ASC
);

-- ----------------------------
-- Indexes structure for table tb_user_info
-- ----------------------------
CREATE UNIQUE INDEX "idx_tb_user_info_user_id"
ON "tb_user_info" (
  "user_id" ASC
);