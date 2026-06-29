# 🫘 BeanChiuling

> A Discord-like real-time chat application built with **Spring Boot + WebSocket + MongoDB + Redis**

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-green?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-green?logo=mongodb)
![Redis](https://img.shields.io/badge/Redis-7.0-red?logo=redis)

## ✨ Features

- 🔐 **JWT Authentication** — Secure login with Access & Refresh tokens
- 🏠 **Server & Channels** — Create communities with categorized text channels
- 💬 **Real-time Chat** — WebSocket (STOMP) messaging with typing indicators
- 👥 **Role & Permission System** — Discord-like bitfield permission management
- 🟢 **Presence System** — Real-time Online/Offline/Idle status via Redis
- 📎 **File Attachments** — Image & file upload via Cloudinary CDN
- 🤝 **Friend System** — Send/accept friend requests and direct messages

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Framework** | Spring Boot 3.5.3 (Java 21) |
| **REST API** | Spring Web + Spring Security |
| **Real-time** | Spring WebSocket (STOMP) |
| **SQL Database** | MySQL 8.0 + Spring Data JPA |
| **Message Store** | MongoDB 7.0 + Spring Data MongoDB |
| **Cache / Presence** | Redis 7.0 + Spring Data Redis |
| **Message Broker** | RabbitMQ |
| **Auth** | JWT (jjwt 0.12.6) |
| **File Storage** | Cloudinary |

## 🚀 Getting Started

### Prerequisites
- Java 21+
- MySQL 8.0
- MongoDB 7.0
- Redis 7.0
- RabbitMQ 3.x

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/qibuuu/BeanChiuling.git
   cd BeanChiuling/beanchiuling-server
   ```

2. **Create the database**
   ```sql
   CREATE DATABASE beanchiuling CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

3. **Configure local credentials**
   
   Create `src/main/resources/application-local.yml`:
   ```yaml
   spring:
     datasource:
       password: "your_mysql_password"
   jwt:
     secret: "your-256-bit-secret-key"
   ```

4. **Run the application**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

5. **Access the API**
   - Base URL: `http://localhost:8080/api/v1`
   - Health check: `http://localhost:8080/actuator/health`

## 📁 Project Structure

```
beanchiuling-server/
├── src/main/java/com/beanchiuling/
│   ├── BeanChiulingApplication.java    # Entry point
│   ├── common/                         # Shared utilities
│   │   ├── exception/                  # Global error handling
│   │   └── response/                   # API response wrapper
│   ├── config/                         # Spring configurations
│   ├── security/                       # JWT & Spring Security
│   └── module/                         # Feature modules
│       ├── auth/                       # Authentication
│       ├── user/                       # User profiles
│       ├── server/                     # Discord-like servers
│       ├── channel/                    # Text channels
│       ├── message/                    # Chat messages (MongoDB)
│       ├── role/                       # Permission system
│       ├── presence/                   # Online status (Redis)
│       └── file/                       # File upload
└── src/main/resources/
    ├── application.yml                 # Config template (no secrets)
    └── application-local.yml           # Local secrets (gitignored)
```

## 📖 API Documentation

Coming soon...

## 🤝 Contributing

This is a learning project for Java Web development. PRs and suggestions are welcome!

## 📄 License

MIT License
