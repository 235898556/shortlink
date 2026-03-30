
主要模块：
- **short-link-gateway**：API 网关，负责 Bloom Filter 拦截、限流、路由
- **short-link-core**：核心服务，短链生成 + 跳转查询（带 Sharding + 多级缓存）
- **short-link-stats**：统计服务，消费 RocketMQ 消息，记录点击数据
- **short-link-monitor**：监控模块

## 🛠️ 技术栈

| 类别          | 技术                              |
|---------------|-----------------------------------|
| 基础框架      | Spring Boot 3.x、Spring Cloud     |
| 网关          | Spring Cloud Gateway              |
| 缓存          | Caffeine（本地） + Redis          |
| 布隆过滤器    | Guava Bloom Filter                |
| 分布式锁      | Redisson                          |
| 数据库        | MySQL + ShardingSphere-JDBC（分库分表） |
| ORM           | MyBatis-Plus                      |
| 消息队列      | RocketMQ（异步统计）              |
| 熔断降级      | Sentinel                          |
| 监控          | Micrometer + CustomMetrics        |
| 构建工具      | Maven                             |

##  快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.x（创建 short_link_db_0 和 short_link_db_1）
- Redis 7.x
- RocketMQ（可选，统计使用）

### 2. 数据库初始化

执行项目中对应的 SQL 脚本，创建分库分表结构（ShardingSphere 配置已内置）。

### 3. 配置修改

修改各模块 `application.yml` 中的数据库、Redis、RocketMQ 等连接信息。

### 4. 启动顺序

```bash
# 1. 启动 Redis、RocketMQ、MySQL
# 2. 启动 short-link-core
mvn spring-boot:run -pl short-link-core

# 3. 启动 short-link-gateway
mvn spring-boot:run -pl short-link-gateway

# 4. 启动 short-link-stats（统计服务）
mvn spring-boot:run -pl short-link-stats

## 📊 性能表现

- **压测环境**：ApacheBench（ab -n 100000 -c 5000）
- **最高吞吐量**：**10万 QPS**
- **跳转延迟**：p99 < 10ms（命中缓存时），整体响应极快
- **架构优势**：Gateway 层纯内存 Bloom Filter + 本地 Caffeine 缓存，几乎无网络 IO，轻松承载高并发跳转场景
####压测
采用nginx,三个网关实例+五个short-link-core实例,进行压测，采用ab压测跳转接口结果可达到10万不出现错误请求
压测结果
http://106.12.149.130:8090/p.jpg
