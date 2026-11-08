# 🏠 校园宿舍管理系统 - 后端

基于 Spring Boot 3.2.5 + MyBatis Plus + MySQL + Redis 的宿舍管理系统后端服务。

---

## 📦 技术栈

- **核心框架**: Spring Boot 3.2.5
- **安全认证**: JWT (jsonwebtoken 0.11.5)
- **ORM 框架**: MyBatis Plus 3.5.5
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.x
- **文档**: Swagger/OpenAPI 3 (springdoc-openapi)
- **工具库**: Hutool 5.8.25
- **JSON**: FastJSON 2.0.43
- **开发工具**: Lombok, Spring Boot DevTools

---

## 🚀 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.x+ (可选)

### 2. 数据库初始化

```bash
# 进入项目根目录
cd D:/campus-housing-admin

# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source sql/init/01_create_database.sql
source sql/init/02_create_tables.sql
```

### 3. 修改配置文件

编辑 `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/dorm_system
    username: root      # 修改为你的数据库用户名
    password: 123456    # 修改为你的数据库密码
```

### 4. 启动项目

#### 方式 1: 使用 Maven 命令

```bash
cd dorm-system-backend
mvn clean install
mvn spring-boot:run
```

#### 方式 2: 使用 IDE

- IDEA: 直接运行 `DormSystemApplication.java` 主类
- Eclipse: 右键项目 → Run As → Spring Boot App

### 5. 访问服务

启动成功后，访问以下地址：

- **API 文档**: http://localhost:8080/swagger-ui.html
- **接口地址**: http://localhost:8080/api
- **测试接口**: http://localhost:8080/api/auth/test

---

## 📁 项目结构

```
dorm-system-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/campus/dorm/
│   │   │       ├── DormSystemApplication.java  # 主启动类
│   │   │       ├── common/                     # 通用类
│   │   │       │   ├── Result.java             # 统一返回结果
│   │   │       │   └── ResultCode.java         # 返回码枚举
│   │   │       ├── config/                     # 配置类
│   │   │       │   ├── CorsConfig.java         # 跨域配置
│   │   │       │   ├── MyBatisPlusConfig.java  # MyBatis Plus配置
│   │   │       │   ├── MyBatisMetaObjectHandler.java  # 字段自动填充
│   │   │       │   ├── RedisConfig.java        # Redis配置
│   │   │       │   ├── SwaggerConfig.java      # Swagger配置
│   │   │       │   └── WebMvcConfig.java       # Web MVC配置
│   │   │       ├── controller/                 # 控制器
│   │   │       │   └── AuthController.java     # 认证控制器
│   │   │       ├── service/                    # 服务接口
│   │   │       │   └── impl/                   # 服务实现
│   │   │       ├── mapper/                     # MyBatis Mapper
│   │   │       ├── entity/                     # 实体类
│   │   │       │   └── BaseEntity.java         # 基础实体类
│   │   │       ├── dto/                        # 数据传输对象
│   │   │       ├── vo/                         # 视图对象
│   │   │       ├── exception/                  # 异常类
│   │   │       │   ├── BusinessException.java  # 业务异常
│   │   │       │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │   │       ├── interceptor/                # 拦截器
│   │   │       │   └── JwtInterceptor.java     # JWT拦截器
│   │   │       └── utils/                      # 工具类
│   │   │           ├── JwtUtil.java            # JWT工具
│   │   │           └── RedisUtil.java          # Redis工具
│   │   └── resources/
│   │       ├── application.yml                 # 主配置文件
│   │       ├── application-dev.yml             # 开发环境配置
│   │       ├── application-prod.yml            # 生产环境配置
│   │       └── mapper/                         # MyBatis XML文件
│   └── test/                                   # 测试代码
├── pom.xml                                     # Maven配置
└── README.md
```

---

## 🔧 主要功能模块

### 1. 认证授权模块
- JWT Token 生成与验证
- 用户登录/登出
- 权限拦截

### 2. 用户管理模块
- 学生管理
- 管理员管理
- 超级管理员管理

### 3. 宿舍管理模块
- 楼栋管理
- 房间管理
- 床位管理
- 宿舍分配

### 4. 申请审核模块
- 调宿申请
- 维修申请
- 物资申请

### 5. 公告管理模块
- 公告发布
- 公告查询

---

## 🛠️ 开发指南

### 1. 新增接口步骤

1. **创建实体类** (`entity` 包)
   - 继承 `BaseEntity` 基类
   - 使用 Lombok 注解简化代码

2. **创建 Mapper** (`mapper` 包)
   - 继承 `BaseMapper<T>`
   - 编写自定义 SQL (如需要)

3. **创建 Service** (`service` 包)
   - 创建接口和实现类
   - 使用 `@Service` 注解

4. **创建 Controller** (`controller` 包)
   - 使用 `@RestController` 注解
   - 添加 Swagger 文档注解

### 2. 统一返回格式

所有接口使用 `Result<T>` 统一返回：

```java
// 成功
return Result.success(data);

// 失败
return Result.error(ResultCode.USER_NOT_FOUND);
```

### 3. 异常处理

使用 `BusinessException` 抛出业务异常：

```java
if (user == null) {
    throw new BusinessException(ResultCode.USER_NOT_FOUND);
}
```

### 4. JWT 使用

在需要认证的接口中，可以从请求属性获取用户信息：

```java
Long userId = (Long) request.getAttribute("userId");
String username = (String) request.getAttribute("username");
String userType = (String) request.getAttribute("userType");
```

---

## 📝 API 文档

启动项目后访问 Swagger UI：http://localhost:8080/swagger-ui.html

可以在线测试所有接口，查看接口文档。

---

## 🔐 安全配置

### JWT 配置

在 `application.yml` 中配置：

```yaml
app:
  jwt:
    secret: your-secret-key      # JWT密钥（生产环境请修改）
    expiration: 86400000          # 过期时间（毫秒）
    header: Authorization         # Token请求头
    prefix: Bearer               # Token前缀
```

### 跨域配置

已配置允许所有域名跨域，生产环境建议修改为具体域名。

---

## 📊 数据库说明

- 使用 MyBatis Plus 作为 ORM 框架
- 支持逻辑删除（`deleted` 字段）
- 自动填充创建时间和更新时间
- 支持分页查询
- 防止全表更新和删除

---

## 🧪 测试

### 运行测试

```bash
mvn test
```

### 测试接口

使用 Postman 或 Swagger UI 测试接口。

示例：测试健康检查接口

```bash
curl http://localhost:8080/api/auth/test
```

---

## 📦 打包部署

### 1. 打包

```bash
mvn clean package
```

生成的 jar 文件位于 `target/dorm-system.jar`

### 2. 运行

```bash
# 开发环境
java -jar target/dorm-system.jar --spring.profiles.active=dev

# 生产环境
java -jar target/dorm-system.jar --spring.profiles.active=prod
```

---

## ⚠️ 注意事项

1. **数据库连接**: 启动前确保 MySQL 服务已启动并创建了数据库
2. **Redis 配置**: 如果不使用 Redis，可以注释掉相关依赖
3. **JWT 密钥**: 生产环境务必修改 JWT 密钥
4. **邮件配置**: 使用邮件功能前需配置 SMTP 信息
5. **文件上传**: 确保上传目录有写入权限

---

## 📞 联系我们

如有问题，请联系开发团队。

---

## 📄 许可证

MIT License

