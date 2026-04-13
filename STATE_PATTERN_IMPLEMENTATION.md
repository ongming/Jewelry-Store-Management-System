# State Pattern Implementation Guide - Jewelry Store Management System

## 📌 Tổng Quan

State Pattern đã được triển khai đầy đủ cho hệ thống quản lý Account trong Jewelry Store. Hệ thống hỗ trợ 4 trạng thái chính:
- **ACTIVE**: Tài khoản hoạt động bình thường
- **SUSPENDED**: Tài khoản bị tạm khóa
- **LOCKED**: Tài khoản bị khóa (sai mật khẩu quá nhiều)
- **INACTIVE**: Tài khoản chưa được kích hoạt

---

## 📁 Cấu Trúc File Tạo Ra

```
src/main/java/com/example/Jewelry/
├── model/
│   ├── entity/
│   │   └── Account.java (đã cập nhật với state object)
│   └── state/
│       ├── AccountState.java (interface)
│       ├── ActiveState.java
│       ├── SuspendedState.java
│       ├── LockedOutState.java
│       └── InactiveState.java
├── service/
│   ├── AccountService.java (interface - đã cập nhật)
│   └── impl/
│       └── AccountServiceImpl.java (implementation - đã cập nhật)
├── controller/
│   ├── AuthController.java (đã cập nhật login method)
│   └── AccountStateApiController.java (REST API - MỚI)
├── repository/
│   └── AccountRepository.java (đã cập nhật)
├── util/
│   └── AccountStateFactory.java (MỚI - Factory pattern)
└── demo/
    └── AccountStatePatternDemo.java (Demo class)

src/test/java/com/example/Jewelry/
└── service/
    └── AccountStatePatternTest.java (Unit tests - MỚI)
```

---

## 🎯 Các Thay Đổi Chi Tiết

### 1. **Account Entity** (src/main/java/.../model/entity/Account.java)

**Thêm:**
- `@Transient private AccountState state` - Object state (không lưu DB)
- `setState(AccountState state)` - Setter cho state
- `getState()` - Getter cho state
- `canLogin()`, `canAccessSystem()`, `canModifyData()` - Delegates đến state
- `suspendAccount()`, `activateAccount()`, `lockAccount()` - State transitions
- `getAccountStateName()` - Lấy tên state hiện tại
- `initializeState()` - Khởi tạo state từ status string

### 2. **AccountState Interface** (src/main/java/.../model/state/AccountState.java)

Định nghĩa các hành vi cần thiết:
```java
boolean canLogin();
boolean canAccessSystem();
boolean canModifyData();
void suspend(Account account);
void activate(Account account);
void lock(Account account);
String getStateName();
```

### 3. **State Implementations**

#### ActiveState
- `canLogin()` → **true**
- `canAccessSystem()` → **true**
- `canModifyData()` → **true**
- `suspend()` → ACTIVE → SUSPENDED
- `lock()` → ACTIVE → LOCKED

#### SuspendedState
- `canLogin()` → **false**
- `canAccessSystem()` → **false**
- `canModifyData()` → **false**
- `activate()` → SUSPENDED → ACTIVE
- `lock()` → SUSPENDED → LOCKED

#### LockedOutState
- `canLogin()` → **false**
- `canAccessSystem()` → **false**
- `canModifyData()` → **false**
- `activate()` → LOCKED → ACTIVE

#### InactiveState
- `canLogin()` → **false**
- `canAccessSystem()` → **false**
- `canModifyData()` → **false**
- `activate()` → INACTIVE → ACTIVE

### 4. **AccountService Interface** (service/AccountService.java)

**Thêm:**
```java
void suspendAccount(Integer accountId);
void activateAccount(Integer accountId);
void lockAccount(Integer accountId);
List<Account> findByStatus(String status);
```

### 5. **AccountServiceImpl** (service/impl/AccountServiceImpl.java)

**Cập nhật:**
- `findById()`, `findByUsername()`, `findAll()` - Tự động khởi tạo state
- `login()` - Kiểm tra `account.canLogin()` trước khi cho login
- Thêm các method cho suspend/activate/lock account
- Sử dụng `AccountStateFactory.initializeStateFromStatus()` để khởi tạo state

### 6. **AccountRepository** (repository/AccountRepository.java)

**Thêm:**
```java
List<Account> findByStatus(String status);
```

### 7. **AuthController** (controller/AuthController.java)

**Cập nhật `login()` method:**
- Kiểm tra `account.canLogin()` thay vì chỉ check username/password
- Hiển thị thông báo lỗi với tên state: "Tài khoản đang ở trạng thái: SUSPENDED"
- Lưu `accountState` vào session

### 8. **AccountStateApiController** (controller/AccountStateApiController.java) - MỚI

REST API endpoints:
```
POST /api/accounts/{accountId}/suspend    - Tạm khóa tài khoản
POST /api/accounts/{accountId}/activate   - Kích hoạt tài khoản
POST /api/accounts/{accountId}/lock       - Khóa tài khoản
POST /api/accounts/{accountId}/state      - Lấy thông tin state
```

### 9. **AccountStateFactory** (util/AccountStateFactory.java) - MỚI

Factory class để tạo state object từ status string:
```java
static void initializeStateFromStatus(Account account)
static AccountState createState(String stateName)
```

### 10. **AccountStatePatternDemo** (demo/AccountStatePatternDemo.java) - MỚI

Demo class hiển thị cách sử dụng State Pattern với 6 test cases.

### 11. **AccountStatePatternTest** (src/test/.../AccountStatePatternTest.java) - MỚI

Unit tests kiểm tra:
- Mỗi state có hành vi đúng (canLogin, canAccessSystem, v.v.)
- State transitions hoạt động đúng
- Complex transitions (ACTIVE → SUSPENDED → LOCKED → ACTIVE)

---

## 🚀 Cách Sử Dụng

### **Ví dụ 1: Tạm khóa tài khoản**

```java
// Thông qua service
accountService.suspendAccount(accountId);

// Hoặc thông qua REST API
POST /api/accounts/1/suspend
```

### **Ví dụ 2: Kiểm tra xem có thể login không**

```java
// Trong AuthController
if (!account.canLogin()) {
    model.addAttribute("error", 
        "Tài khoản đang ở trạng thái: " + account.getAccountStateName());
    return "login";
}
```

### **Ví dụ 3: Lấy thông tin state**

```java
// Thông qua REST API
POST /api/accounts/1/state
// Response:
{
    "accountId": 1,
    "username": "john.admin",
    "status": "ACTIVE",
    "stateName": "ACTIVE",
    "canLogin": true,
    "canAccessSystem": true,
    "canModifyData": true
}
```

### **Ví dụ 4: Demo class**

```bash
java -cp target/classes com.example.Jewelry.demo.AccountStatePatternDemo
```

Hoặc chạy thông qua IDE bằng cách click chuột phải → Run.

---

## 📊 State Transition Diagram

```
                    ┌─────────────┐
                    │   INACTIVE  │
                    └──────┬──────┘
                           │ activate()
                           ▼
            ┌──────────────────────────────┐
            │                              │
      suspend()                    activate()
            │                              │
            ▼                              │
    ┌──────────────┐                       │
    │  SUSPENDED   │                       │
    ├──────────────┤                       │
    │              │                       │
    │ lock()       │                       │
    │   │          │                       │
    │   ▼          │                       │
    │┌────────────┐│                       │
    ││  LOCKED    ││◄──────────────────────┤
    │└────────────┘│                       │
    │              │                       │
    │ activate()   │                       │
    │   │          │                       │
    │   └─────────►│◄──────────────────────┘
    │              │
    │              │ activate()
    │              ├─────────────►┌───────────┐
    │              │              │  ACTIVE   │
    └──────────────┼──────────────┤           │
                   │              │ suspend() │
                   └──────────────┤           │
                   activate()     └───────────┘
```

---

## ✅ Checklist Implementation

- ✅ AccountState interface tạo xong
- ✅ 4 State classes (ActiveState, SuspendedState, LockedOutState, InactiveState)
- ✅ Account entity cập nhật với state object
- ✅ AccountService interface thêm các method
- ✅ AccountServiceImpl implement đầy đủ
- ✅ AccountRepository thêm findByStatus()
- ✅ AuthController cập nhật login logic
- ✅ AccountStateApiController tạo 4 REST endpoints
- ✅ AccountStateFactory utility class
- ✅ AccountStatePatternDemo class
- ✅ AccountStatePatternTest unit tests

---

## 🔗 Sự Liên Kết Giữa Các Thành Phần

```
AuthController.login()
    ↓
AccountService.login()
    ↓
Khởi tạo state từ database status
    ↓
account.canLogin() [gọi state.canLogin()]
    ↓
✨ State Pattern quyết định cho login hay không
    ↓
Tạo session hoặc hiển thị lỗi
```

---

## 📝 Lợi Ích Của Triển Khai Này

1. **Encapsulation**: Hành vi state được đóng gói trong class riêng
2. **Single Responsibility**: Mỗi state class chỉ có 1 trách nhiệm
3. **Open/Closed Principle**: Dễ thêm state mới mà không ảnh hưởng code cũ
4. **Type Safety**: Dùng state object thay vì string (compile-time check)
5. **Easy Testing**: Có thể test từng state độc lập
6. **Centralized Logic**: Tất cả state transitions đều rõ ràng
7. **Polymorphism**: Hành vi thay đổi dựa trên state object, không string

---

## 🐛 Debugging Tips

1. **Kiểm tra state hiện tại:**
   ```java
   System.out.println("Current state: " + account.getAccountStateName());
   System.out.println("Can login: " + account.canLogin());
   ```

2. **Kiểm tra database:**
   ```sql
   SELECT account_id, username, status FROM account WHERE account_id = 1;
   ```

3. **Test REST API:**
   ```bash
   curl -X POST http://localhost:8080/api/accounts/1/state
   ```

---

## 📞 Contact & Support

Nếu có câu hỏi về State Pattern implementation, hãy tham khảo:
- Các State class files
- AccountStatePatternTest.java để xem cách sử dụng
- AccountStatePatternDemo.java để xem demo

---

**Created**: April 13, 2026
**Version**: 1.0
**Status**: ✅ Implementation Complete
