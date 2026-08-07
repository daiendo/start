# Muriox Backend

Muriox 的 Java 后端服务，提供验证码登录、RS256 JWT 认证、Redis 登录会话、统一异常响应和当前用户信息接口。

## 技术栈

- Java 26
- Spring Boot 4.1.0
- Spring Security 7.1.0
- PostgreSQL
- Redis
- MyBatis-Plus 3.5.17
- Jackson 3

## 环境要求

- JDK 26
- Maven 3.9+
- PostgreSQL
- Redis

默认本地配置位于 `src/main/resources/application.yaml`。生产或共享环境请使用环境变量或外部配置覆盖数据库、Redis 和 JWT 密钥配置，不要提交真实凭据。

## JWT 密钥

项目使用 RS256 签发和验证 JWT，需要以下文件：

```text
src/main/resources/keys/jwt-private.pem
src/main/resources/keys/jwt-public.pem
```

可以使用 OpenSSL 生成：

```bash
openssl genpkey -algorithm RSA -out jwt-private.pem -pkeyopt rsa_keygen_bits:2048
openssl rsa -pubout -in jwt-private.pem -out jwt-public.pem
```

将文件放入 `src/main/resources/keys/`。私钥已被 `.gitignore` 排除，不应提交到版本库。

## 数据库

应用连接 PostgreSQL，并通过 MyBatis-Plus 访问 `sys_user`。当前登录和 Profile 功能依赖以下用户字段：

```text
id
account
username
password_hash
enabled
created_at
```

`password_hash` 应保存 BCrypt 哈希，不能保存明文密码。

## 启动

确认 PostgreSQL 和 Redis 已启动并完成配置，然后运行：

```bash
mvn spring-boot:run
```

默认服务地址：

```text
http://localhost:8080
```

## API

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| GET | `/api/authority/auth/captcha` | 否 | 获取一次性验证码 |
| POST | `/api/authority/auth/login` | 否 | 验证账号、密码和验证码并签发 JWT |
| POST | `/api/authority/auth/logout` | Bearer Token | 删除当前 Redis 登录会话 |
| GET | `/api/authority/profile` | Bearer Token | 获取当前用户、权限列表和菜单树 |

受保护接口使用标准请求头：

```http
Authorization: Bearer <access-token>
```

登录请求示例：

```json
{
  "account": "muriox",
  "password": "your-password",
  "captchaUuid": "captcha-uuid",
  "captchaCode": "ABCD"
}
```

## 认证流程

1. 登录成功后签发包含 `sub`、`sid`、`jti`、`iat`、`exp` 和 `iss` 的 RS256 JWT。
2. Redis 保存 `auth:session:{sid} = userId`，TTL 与 Access Token 有效期一致。
3. Spring Security 校验 JWT 签名、有效期和 issuer。
4. `ActiveSessionJwtValidator` 检查 Redis session 是否存在，并确认其用户 ID 与 JWT `sub` 一致。
5. logout 删除当前 session；旧 Token 再次访问受保护接口时返回 HTTP 401。

当前 Access Token 有效期为 30 分钟，尚未实现 Refresh Token。

## 响应格式

接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "errors": []
}
```

认证失败返回真实 HTTP 401，权限不足返回真实 HTTP 403，并保持相同的 JSON 响应结构。

## 当前进度

- [x] Redis 一次性验证码
- [x] BCrypt 密码验证
- [x] RS256 JWT 签发和校验
- [x] Redis 登录会话与主动注销
- [x] 统一 401/403 JSON 响应
- [x] 当前用户 Profile
- [ ] 用户角色和权限查询
- [ ] 菜单树查询
- [ ] Refresh Token
- [ ] 多设备会话管理与全部退出
