# Bước 3 — Server Module (Cộng đồng & Phân quyền)

## Tổng quan

Server Module xây dựng khái niệm cốt lõi của BeanChiuling — **Server** (cộng đồng nơi người dùng gặp nhau), **Members** (thành viên), **Roles** (vai trò với quyền hạn), và **Invites** (link mời tham gia).

**Sơ đồ quan hệ:**
```
User ──(owner)──► Server ◄──── Invite (code, expires, maxUses)
                    │
                    ├── Role (@everyone, Admin, Moderator...)
                    │     └── permissions: BIGINT (bitfield)
                    │
                    └── ServerMember (User ↔ Server join)
                              └── member_roles (ServerMember ↔ Role)
```

---

## Entities

### `module/server/entity/Server.java`

**Là gì?** Entity đại diện cho một "server" (cộng đồng) — nơi chứa các channel và thành viên.

**`@ManyToOne(fetch = FetchType.LAZY)` trên `owner`:**

```
FetchType.EAGER (mặc định cho @ManyToOne):
  SELECT * FROM servers WHERE id = ?
  SELECT * FROM users WHERE id = ?  ← Tự động join ngay lập tức

FetchType.LAZY:
  SELECT * FROM servers WHERE id = ?
  -- User chỉ được load khi gọi server.getOwner()
```

LAZY tốt hơn vì không phải lúc nào cũng cần thông tin owner. Chỉ load khi thực sự cần.

**`@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`:**

```
cascade = ALL: Mọi operation trên Server đều cascade xuống members/roles
  save(server)   → tự save tất cả members và roles
  delete(server) → tự delete tất cả members và roles

orphanRemoval = true: Nếu một member bị remove khỏi List → tự delete khỏi DB
  server.getMembers().remove(member) → DELETE FROM server_members WHERE id = ?
```

**`@Builder.Default`:**
Khi dùng Builder pattern, các field có giá trị default phải dùng `@Builder.Default`:
```java
// Nếu không có @Builder.Default:
Server.builder().name("Test").build()
→ members = null (không phải ArrayList!)

// Với @Builder.Default:
Server.builder().name("Test").build()
→ members = new ArrayList<>()  ✓
```

---

### `module/server/entity/ServerMember.java`

**Là gì?** Bảng trung gian (join table) giữa User và Server — thể hiện việc user tham gia server.

**`@UniqueConstraint`:**
```java
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "server_id"}))
```
Đảm bảo mỗi user chỉ là thành viên của server 1 lần — không thể join 2 lần.

**`@ManyToMany` với `@JoinTable`:**
```
server_members (ServerMember)
    ↓ @ManyToMany
member_roles (bảng trung gian)
    ↓
roles (Role)
```
Bảng `member_roles` do JPA tự tạo, chứa `member_id` và `role_id`.

---

### `module/server/entity/Role.java`

**Là gì?** Vai trò trong server, quyết định quyền hạn của thành viên.

**Bitfield Permission System — tại sao dùng BIGINT thay vì nhiều boolean columns?**

❌ Cách tệ:
```sql
can_view_channels BOOLEAN,
can_send_messages BOOLEAN,
can_manage_messages BOOLEAN,
... (15 columns cho 15 quyền)
```

✅ Bitfield (BeanChiuling):
```sql
permissions BIGINT  -- 1 column cho TẤT CẢ quyền
```

```java
// Mỗi quyền = 1 bit:
VIEW_CHANNELS  = 1L        = 0000...0001
SEND_MESSAGES  = 1L << 1   = 0000...0010
READ_HISTORY   = 1L << 2   = 0000...0100

// Role có VIEW + SEND + READ:
permissions = 0001 | 0010 | 0100 = 0111 = 7

// Check quyền:
hasPermission(7, SEND_MESSAGES) = (7 & 2) == 2 = true  ✓
hasPermission(7, KICK_MEMBERS)  = (7 & 64) == 64 = false ✓
```

**Ưu điểm:**
- 1 số BIGINT = 64 quyền → tiết kiệm storage
- Bitwise operation cực nhanh (CPU level)
- Thêm quyền mới: chỉ thêm constant mới, không cần ALTER TABLE

---

### `module/server/entity/Invite.java`

**Là gì?** Invite link cho server — có code ngắn gọn, tùy chọn giới hạn sử dụng và thời gian hết hạn.

**Code là Primary Key — tại sao?**
Invite code vừa là identifier vừa là lookup key. Dùng nó làm PK tránh thêm cột `id` thừa.

**`maxUses = null` vs `maxUses = 0`:**
- `null` → không giới hạn số lần dùng
- `5` → tối đa 5 lần, sau đó bị reject

---

## Repositories

### `ServerRepository.java`

**`@Query` với JPQL:**
Khi naming convention không đủ để diễn đạt query phức tạp:

```java
@Query("SELECT s FROM Server s JOIN s.members m WHERE m.user.id = :userId")
List<Server> findAllByMemberUserId(@Param("userId") String userId);
```

Đây là **JPQL** (Java Persistence Query Language) — SQL nhưng viết theo object/field thay vì table/column:
- `Server s` → entity, không phải table name
- `s.members m` → navigate qua relationship `@OneToMany members`
- `m.user.id` → navigate qua `@ManyToOne user` rồi lấy `id`

Hibernate tự dịch JPQL → SQL:
```sql
SELECT s.* FROM servers s
  INNER JOIN server_members sm ON sm.server_id = s.id
  WHERE sm.user_id = ?
```

---

## Utilities

### `common/utils/PermissionUtils.java`

**Là gì?** Utility class chứa tất cả permission constants và helper methods.

**`final class` + `private constructor`:**
Pattern để tạo utility class thuần túy — không thể instantiate, không thể extend.

**Bit shift operators:**
```java
VIEW_CHANNELS  = 1L        // binary: ...00001
SEND_MESSAGES  = 1L << 1   // binary: ...00010  (shift 1 bit sang trái)
READ_HISTORY   = 1L << 2   // binary: ...00100  (shift 2 bit sang trái)
KICK_MEMBERS   = 1L << 6   // binary: ...1000000
ADMINISTRATOR  = 1L << 8   // binary: ...100000000
```

**Tại sao `ADMINISTRATOR` bypass tất cả:**
```java
public static boolean has(long permissions, long flag) {
    if ((permissions & ADMINISTRATOR) == ADMINISTRATOR) return true; // Bypass!
    return (permissions & flag) == flag;
}
```
Giống Discord: ADMINISTRATOR có quyền làm mọi thứ bất kể các bit khác.

**`DEFAULT_PERMISSIONS` cho `@everyone` role:**
```java
DEFAULT_PERMISSIONS = VIEW_CHANNELS | SEND_MESSAGES | READ_HISTORY
                    | EMBED_LINKS | ATTACH_FILES | ADD_REACTIONS
```
Mọi thành viên mới đều có những quyền cơ bản này.

---

## Service

### `ServerService.java`

**`createServer()` — tự động tạo `@everyone` role và owner membership:**
```
1. Save Server entity
2. Tạo Role "@everyone" với DEFAULT_PERMISSIONS
3. Tạo ServerMember cho owner, gán role @everyone
4. Return ServerDetailDto
```

Discord cũng làm vậy — khi tạo server, owner tự động là member với role @everyone.

**`joinByInviteCode()` — validation pipeline:**
```
Invite tồn tại? → Chưa expired? → Chưa đạt maxUses?
    → User chưa là member? → Server chưa đầy?
    → Tạo ServerMember → Tăng currentUses
```

**`resolvePermissions()` — tính toán effective permissions:**
```java
// 1. Owner luôn có ADMINISTRATOR
if (server.getOwner().getId().equals(userId))
    return ADMINISTRATOR;

// 2. OR tất cả permissions của các roles user có
member.getRoles().stream()
    .mapToLong(Role::getPermissions)
    .reduce(0L, (a, b) -> a | b)

// VD: role1.perms = 0111, role2.perms = 1000
// Effective = 0111 | 1000 = 1111 (có tất cả quyền của cả 2 role)
```

**`SecureRandom` thay vì `Random`:**
```java
private static final SecureRandom RANDOM = new SecureRandom();
```
`Random` dùng thuật toán linear congruential — predictable nếu biết seed. `SecureRandom` dùng entropy từ OS → không đoán được → an toàn cho invite code.

---

## Controller

### `ServerController.java`

**`@AuthenticationPrincipal User currentUser`:**
Thay vì viết:
```java
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
User user = (User) auth.getPrincipal();
```

Dùng annotation:
```java
@AuthenticationPrincipal User currentUser
// Spring tự inject User object từ SecurityContext vào parameter
```

**`@RequestBody(required = false)` cho CreateInviteRequest:**
```java
@PostMapping("/servers/{serverId}/invites")
public ResponseEntity<?> createInvite(
    @RequestBody(required = false) CreateInviteRequest request) {

    if (request == null) request = new CreateInviteRequest(); // Dùng default
```
Client có thể gọi không có body → tạo invite không giới hạn, không hết hạn.

**REST URL design:**
```
POST   /servers                           → Tạo server
GET    /servers/{serverId}                → Lấy chi tiết server (cần là member)
GET    /users/@me/servers                 → Danh sách server của tôi
DELETE /servers/{serverId}                → Xóa server (owner only)
POST   /servers/{serverId}/leave          → Rời server
DELETE /servers/{serverId}/members/{uid}  → Kick member
POST   /servers/{serverId}/invites        → Tạo invite link
GET    /invites/{code}                    → Xem invite (preview trước khi join)
POST   /invites/{code}                    → Dùng invite để join server
```

**Tại sao `GET /invites/{code}` là public (không cần auth)?**
Để hiển thị preview "Bạn được mời vào server X — 150 thành viên" trước khi login/đăng ký. Giống Discord: ai cũng xem được link invite, nhưng cần đăng nhập để join.

---

## API Test nhanh

```powershell
# 1. Đăng nhập lấy token
$loginBody = '{"email":"bean@example.com","password":"secret123"}'
$loginResp = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/auth/login" `
    -Method POST -ContentType "application/json" -Body $loginBody
$token = ($loginResp.Content | ConvertFrom-Json).data.accessToken

# 2. Tạo server
$serverBody = '{"name":"Bean Community","description":"The best place for beans"}'
Invoke-WebRequest -Uri "http://localhost:8080/api/v1/servers" `
    -Method POST -ContentType "application/json" `
    -Headers @{Authorization="Bearer $token"} -Body $serverBody

# 3. Lấy danh sách server của mình
Invoke-WebRequest -Uri "http://localhost:8080/api/v1/users/@me/servers" `
    -Headers @{Authorization="Bearer $token"}
```
