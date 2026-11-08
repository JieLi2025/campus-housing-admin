# 🚀 快速开始指南

## 一、前端代码移动（5分钟）

### Windows 系统

```bash
# 方法1：使用自动化脚本（推荐）
restructure.bat

# 方法2：手动操作
# 1. 创建新的项目根目录
cd ..
mkdir dorm-management-system
cd dorm-management-system

# 2. 移动前端代码
move ..\dorm-system-vue frontend

# 3. 清理并重装依赖
cd frontend
rmdir /s /q node_modules
rmdir /s /q dist
npm install

# 4. 启动测试
npm run dev
```

### Linux/Mac 系统

```bash
# 1. 创建新的项目根目录
cd ..
mkdir dorm-management-system
cd dorm-management-system

# 2. 移动前端代码
mv ../dorm-system-vue ./frontend

# 3. 清理并重装依赖
cd frontend
rm -rf node_modules dist .vite
npm install

# 4. 启动测试
npm run dev
```

### ⚠️ 移动注意事项

✅ **不会影响的：**
- 所有相对路径引用（`@/`、`./`、`../`）
- Vite 配置
- 路由配置
- 组件引用

⚠️ **需要检查的：**
- 浏览器访问地址保持 `http://localhost:5173`
- 所有功能正常运行
- 开发服务器正常启动

---

## 二、数据库初始化（10分钟）

### 1. 安装 MySQL 8.0+

**Windows:**
- 下载：https://dev.mysql.com/downloads/installer/
- 安装时记住 root 密码
- 默认端口：3306

**Mac:**
```bash
brew install mysql@8.0
brew services start mysql@8.0
```

**Linux (Ubuntu):**
```bash
sudo apt update
sudo apt install mysql-server
sudo systemctl start mysql
```

### 2. 执行数据库初始化脚本

```bash
# 登录 MySQL
mysql -u root -p

# 在 MySQL 中执行
source sql/init/01_create_database.sql
source sql/init/02_create_tables.sql

# 或者使用命令行直接执行
mysql -u root -p < sql/init/01_create_database.sql
mysql -u root -p < sql/init/02_create_tables.sql
```

### 3. 验证数据库

```sql
-- 查看数据库
SHOW DATABASES;

-- 使用数据库
USE dorm_system;

-- 查看表
SHOW TABLES;

-- 查看表结构（示例）
DESC sys_user;
```

预期结果：
```
+----------------+
| Tables         |
+----------------+
| admin          |
| announcement   |
| application    |
| bed            |
| building       |
| material       |
| notification   |
| operation_log  |
| room           |
| student        |
| sys_user       |
| visitor        |
+----------------+
12 rows in set
```

---

## 三、后端项目创建（15分钟）

### 方法1：使用 IDEA 创建（推荐）

1. **打开 IDEA → New Project**

2. **选择 Spring Initializr**
   - Type: Maven
   - Language: Java
   - Spring Boot: 3.2.0
   - Java: 17
   - Packaging: Jar

3. **项目信息**
   - Group: com.dorm
   - Artifact: dorm-management-system
   - Package name: com.dorm

4. **选择依赖**
   - Spring Web
   - Spring Security
   - MySQL Driver
   - MyBatis Framework
   - Redis
   - Validation
   - Lombok

5. **项目位置**
   - Location: `.../dorm-management-system/backend`

6. **完成创建**

### 方法2：使用 Spring Initializr 网站

1. 访问 https://start.spring.io/

2. 配置项目：
   - Project: Maven
   - Language: Java
   - Spring Boot: 3.2.0
   - Packaging: Jar
   - Java: 17

3. 添加依赖：
   - Spring Web
   - Spring Security
   - MySQL Driver
   - MyBatis Framework
   - Spring Data Redis
   - Validation
   - Lombok

4. 点击 Generate，下载并解压到 `backend/` 目录

### 方法3：手动创建

参考 `PROJECT_RESTRUCTURE_GUIDE.md` 中的 `pom.xml` 配置。

---

## 四、配置后端（10分钟）

### 1. 修改 application.yml

在 `backend/src/main/resources/` 目录下：

**application.yml:**
```yaml
spring:
  application:
    name: dorm-management-system
  profiles:
    active: dev

server:
  port: 8080
  servlet:
    context-path: /api
```

**application-dev.yml:**
```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/dorm_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为你的密码
    
  redis:
    host: localhost
    port: 6379
```

### 2. 创建启动类

`src/main/java/com/dorm/DormApplication.java`:
```java
package com.dorm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DormApplication {
    public static void main(String[] args) {
        SpringApplication.run(DormApplication.class, args);
    }
}
```

### 3. 测试运行

```bash
# 在 backend 目录下
mvn spring-boot:run

# 或在 IDEA 中直接运行 DormApplication
```

访问：http://localhost:8080/api

预期看到 Spring Boot 的欢迎页或错误页（因为还没有配置路由）。

---

## 五、前后端联调准备（5分钟）

### 1. 配置前端代理

修改 `frontend/vite.config.js`:

```javascript
export default defineConfig({
  plugins: [vue(), vueDevTools()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### 2. 创建前端 API 模块

创建 `frontend/src/api/request.js`:
```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res.data
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
```

### 3. 测试连接

创建一个简单的后端测试接口，验证前后端是否能正常通信。

---

## 六、开发流程

### 前端开发

```bash
cd frontend
npm run dev
```

访问：http://localhost:5173

### 后端开发

```bash
cd backend
mvn spring-boot:run
```

API：http://localhost:8080/api

### 同时开发

**终端1（前端）：**
```bash
cd frontend && npm run dev
```

**终端2（后端）：**
```bash
cd backend && mvn spring-boot:run
```

---

## 七、项目结构检查清单

完成上述步骤后，你的项目结构应该是：

```
dorm-management-system/
├── frontend/                 ✓ 前端项目
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── vite.config.js
│   └── index.html
├── backend/                  ✓ 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/dorm/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   └── target/
├── sql/                      ✓ 数据库脚本
│   └── init/
│       ├── 01_create_database.sql
│       └── 02_create_tables.sql
├── docs/                     ✓ 文档目录
├── PROJECT_RESTRUCTURE_GUIDE.md  ✓ 详细指南
├── QUICK_START.md           ✓ 快速开始
└── README.md                ✓ 项目说明
```

---

## 八、下一步

1. ✅ 完成项目重构
2. ✅ 数据库初始化
3. ✅ 后端项目创建
4. 🚀 **开始开发认证模块**
   - 用户登录
   - JWT Token
   - 权限控制

5. 🚀 **逐步迁移功能模块**
   - 学生管理
   - 宿舍管理
   - 申请审核
   - ... 其他模块

---

## 九、常见问题

### Q1: 前端启动失败

**A:** 
```bash
cd frontend
rm -rf node_modules package-lock.json
npm install
npm run dev
```

### Q2: 数据库连接失败

**A:** 
- 检查 MySQL 是否启动
- 检查用户名和密码
- 检查端口（默认3306）
- 检查数据库是否创建

### Q3: 后端启动失败

**A:**
- 检查 JDK 版本（需要 Java 17+）
- 检查 Maven 配置
- 查看错误日志
- 确认数据库配置正确

### Q4: 跨域问题

**A:**
- 后端添加 CORS 配置
- 或使用 Vite 代理（已配置）

---

## 十、获取帮助

- 📖 详细指南：`PROJECT_RESTRUCTURE_GUIDE.md`
- 🗄️ 数据库设计：`docs/database/`
- 🔌 API文档：开发中使用 Swagger
- 💬 技术支持：提交 Issue

---

**准备完成后，开始愉快地开发吧！🎉**

