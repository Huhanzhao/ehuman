# 1.技术架构
| 类别    | 技术选型及版本                                             |
|-------|-----------------------------------------------------|
| 框架    | （springcloud）StringBoot3.5.6 langchain4j1.1.0-beta7 |
| jdk   | 21                                                  |
| 关系持久层 | mysql9.5.0                                          |
| 矢量库   | chroma latest                                       |
| 大模型   | openai:dall-e-3,text-embedding-3-large,gpt-4o-mini  |
| 部署    | docker compose                                      |
|缓存    | redis 版本待定                                          |
|oss| minio                                               |

# 2.整体需求
## 2.1 基础需求：
* 登录、注册功能（邮箱验证或其他免费验证方式）、人机校验功能（可选）
* 角色权限管理功能,用户档案管理（增删查改 大文本 支持RAG检索）
* 创建知识库，支持上传文件，删除文件，查看文件列表，文件下载，模糊搜索
* 
