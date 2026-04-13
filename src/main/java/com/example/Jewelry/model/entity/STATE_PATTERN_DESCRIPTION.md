# State Pattern - Quy Trình Login Hệ Thống Jewelry Store

## 📋 Mô Tả Tổng Quan

Áp dụng **State Pattern** để quản lý trạng thái tài khoản (Account) trong quy trình login. Thay vì lưu trạng thái dưới dạng string (`status = "ACTIVE"` hoặc `"SUSPENDED"`), chúng ta sẽ sử dụng các **State Objects** để định nghĩa hành vi khác nhau.

---

## 🏗️ Cấu Trúc State Pattern

### **Sơ Đồ Class (UML)**

```
                    ┌─────────────────────┐
                    │  <<interface>>      │
                    │   AccountState      │
                    ├─────────────────────┤
                    │ + canLogin()        │
                    │ + canAccessSystem() │
                    │ + canModifyData()   │
                    │ + suspend()         │
                    │ + activate()        │
                    └──────────┬──────────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
        ┌───────▼────────┐ ┌───▼──────────┐ ┌──▼──────────────┐
        │ ActiveState    │ │SuspendedState│ │ LockedOutState  │
        ├────────────────┤ ├──────────────┤ ├─────────────────┤
        │ + canLogin()   │ │+ canLogin()  │ │ + canLogin()    │
        │   → true       │ │  → false     │ │   → false       │
        │ + suspend()    │ │+ activate()  │ │ + canUnlock()   │
        │   → Suspended  │ │  → Active    │ │   → unlock()    │
        └────────────────┘ └──────────────┘ └─────────────────┘


                    ┌──────────────────┐
                    │     Account      │
                    ├──────────────────┤
                    │ - state: State   │
                    │ - username       │
                    │ - password_hash  │
                    ├──────────────────┤
                    │ + login()        │
                    │ + suspend()      │
                    │ + activate()     │
                    └──────────────────┘
```

---

## 📝 Chi Tiết Từng State

### **1️⃣ AccountState (Interface)**

```java
public interface AccountState {
    // Kiểm tra xem có thể login không
    boolean canLogin();
    
    // Kiểm tra có thể truy cập hệ thống không
    boolean canAccessSystem();
    
    // Kiểm tra có thể sửa đổi dữ liệu không
    boolean canModifyData();
    
    // Chuyển sang trạng thái suspended
    void suspend(Account account);
    
    // Chuyển sang trạng thái active
    void activate(Account account);
    
    // Chuyển sang trạng thái locked
    void lock(Account account);
    
    // Lấy tên trạng thái
    String getStateName();
}
```

---

### **2️⃣ ActiveState (State Cụ Thể)**

```java
public class ActiveState implements AccountState {
    
    @Override
    public boolean canLogin() {
        // Tài khoản ACTIVE có thể login
        return true;
    }
    
    @Override
    public boolean canAccessSystem() {
        return true;
    }
    
    @Override
    public boolean canModifyData() {
        return true;
    }
    
    @Override
    public void suspend(Account account) {
        // Chuyển từ ACTIVE → SUSPENDED
        account.setState(new SuspendedState());
        account.setStatus("SUSPENDED");
        System.out.println("✅ Tài khoản đã bị tạm khóa (SUSPENDED)");
    }
    
    @Override
    public void activate(Account account) {
        // Đã ở trạng thái ACTIVE, không cần làm gì
        System.out.println("⚠️ Tài khoản đã ở trạng thái ACTIVE");
    }
    
    @Override
    public void lock(Account account) {
        // Chuyển từ ACTIVE → LOCKED
        account.setState(new LockedOutState());
        account.setStatus("LOCKED");
    }
    
    @Override
    public String getStateName() {
        return "ACTIVE";
    }
}
```

---

### **3️⃣ SuspendedState (State Cụ Thể)**

```java
public class SuspendedState implements AccountState {
    
    @Override
    public boolean canLogin() {
        // Tài khoản SUSPENDED KHÔNG thể login
        return false;
    }
    
    @Override
    public boolean canAccessSystem() {
        return false;
    }
    
    @Override
    public boolean canModifyData() {
        return false;
    }
    
    @Override
    public void suspend(Account account) {
        // Đã ở SUSPENDED, không cần làm gì
        System.out.println("⚠️ Tài khoản đã ở trạng thái SUSPENDED");
    }
    
    @Override
    public void activate(Account account) {
        // Chuyển từ SUSPENDED → ACTIVE
        account.setState(new ActiveState());
        account.setStatus("ACTIVE");
        System.out.println("✅ Tài khoản đã được kích hoạt lại (ACTIVE)");
    }
    
    @Override
    public void lock(Account account) {
        // Chuyển từ SUSPENDED → LOCKED
        account.setState(new LockedOutState());
        account.setStatus("LOCKED");
    }
    
    @Override
    public String getStateName() {
        return "SUSPENDED";
    }
}
```

---

### **4️⃣ LockedOutState (State Cụ Thể)**

```java
public class LockedOutState implements AccountState {
    
    @Override
    public boolean canLogin() {
        // Tài khoản LOCKED KHÔNG thể login
        return false;
    }
    
    @Override
    public boolean canAccessSystem() {
        return false;
    }
    
    @Override
    public boolean canModifyData() {
        return false;
    }
    
    @Override
    public void suspend(Account account) {
        // Tài khoản đã locked, không thể suspend thêm
        System.out.println("⚠️ Tài khoản đã ở trạng thái LOCKED");
    }
    
    @Override
    public void activate(Account account) {
        // Chuyển từ LOCKED → ACTIVE (cần xác nhận từ admin)
        account.setState(new ActiveState());
        account.setStatus("ACTIVE");
        System.out.println("✅ Tài khoản đã được mở khóa (ACTIVE)");
    }
    
    @Override
    public void lock(Account account) {
        // Đã ở LOCKED
        System.out.println("⚠️ Tài khoản đã ở trạng thái LOCKED");
    }
    
    @Override
    public String getStateName() {
        return "LOCKED";
    }
}
```

---

## 🔄 Cập Nhật Account Entity

### **Trước (Hiện tại):**
```java
@Column(name = "status", nullable = false, length = 20)
private String status = "ACTIVE";

// Kiểm tra bằng string
if ("ACTIVE".equals(account.getStatus())) {
    // Cho phép login
}
```

### **Sau (Với State Pattern):**
```java
@Transient  // Không lưu vào DB
private AccountState state = new ActiveState();

@Column(name = "status", nullable = false, length = 20)
private String status = "ACTIVE";  // Vẫn giữ để query DB

// Kiểm tra bằng state object
public boolean canLogin() {
    return this.state.canLogin();
}

public void suspendAccount() {
    this.state.suspend(this);
}

public void activateAccount() {
    this.state.activate(this);
}
```

---

## 🔐 Cập Nhật AccountRepository

```java
public interface AccountRepository extends JpaRepository<Account, Integer> {

    // Tìm tài khoản có thể login (status = ACTIVE)
    Optional<Account> findByUsernameAndPasswordHashAndStatusIgnoreCase(
        String username, 
        String passwordHash, 
        String status
    );

    // Tìm tài khoản bằng username
    Optional<Account> findByUsername(String username);
    
    // NEW: Tìm tài khoản trong trạng thái SUSPENDED
    List<Account> findByStatus(String status);
}
```

---

## 💼 Cập Nhật AccountService

### **Interface:**
```java
public interface AccountService {
    // ... các method khác ...
    
    // Login - giờ sẽ check state.canLogin()
    Optional<Account> login(String username, String password);
    
    // NEW: Tạm khóa tài khoản
    void suspendAccount(Integer accountId);
    
    // NEW: Kích hoạt lại tài khoản
    void activateAccount(Integer accountId);
    
    // NEW: Khóa tài khoản vì vào sai mật khẩu quá nhiều
    void lockAccount(Integer accountId);
}
```

### **Implementation:**
```java
@Service
public class AccountServiceImpl implements AccountService {
    
    private final AccountRepository accountRepository;
    
    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    @Override
    public Optional<Account> login(String username, String password) {
        Optional<Account> accountOpt = accountRepository.findByUsername(username);
        
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }
        
        Account account = accountOpt.get();
        
        // ✨ SỬ DỤNG STATE PATTERN
        if (!account.canLogin()) {
            // Nếu state là SUSPENDED hoặc LOCKED, không cho login
            return Optional.empty();
        }
        
        // Kiểm tra mật khẩu
        if (!account.getPasswordHash().equals(password)) {
            // Nếu sai mật khẩu > 5 lần → lock account
            // account.lockAccount();
            return Optional.empty();
        }
        
        return Optional.of(account);
    }
    
    @Override
    public void suspendAccount(Integer accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.suspendAccount();  // Sử dụng state pattern
        accountRepository.save(account);
    }
    
    @Override
    public void activateAccount(Integer accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.activateAccount();  // Sử dụng state pattern
        accountRepository.save(account);
    }
    
    @Override
    public void lockAccount(Integer accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.lockAccount();  // Sử dụng state pattern
        accountRepository.save(account);
    }
}
```

---

## 🎯 Cập Nhật AuthController

```java
@Controller
public class AuthController {

    private final AccountService accountService;
    // ... các field khác ...
    
    @PostMapping({"/auth/login", "/login"})
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        Account account;
        try {
            account = accountService.login(username, password).orElse(null);
        } catch (RuntimeException runtimeException) {
            model.addAttribute("error", "Không thể xác thực do lỗi dữ liệu.");
            model.addAttribute("username", username);
            return "login";
        }

        if (account == null) {
            model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng.");
            model.addAttribute("username", username);
            return "login";
        }
        
        // ✨ KIỂM TRA STATE
        if (!account.getState().canLogin()) {
            String stateName = account.getState().getStateName();
            model.addAttribute("error", 
                "Tài khoản đang ở trạng thái: " + stateName + ". Vui lòng liên hệ admin.");
            model.addAttribute("username", username);
            return "login";
        }

        // Tạo session
        String roleName = account.getRoleName() == null ? "STAFF" : 
                         account.getRoleName().toUpperCase(Locale.ROOT);
        session.setAttribute("accountId", account.getAccountId());
        session.setAttribute("username", account.getUsername());
        session.setAttribute("fullName", account.getFullName());
        session.setAttribute("roleName", roleName);
        session.setAttribute("accountState", account.getState().getStateName());

        if ("ADMIN".equals(roleName)) {
            return "redirect:/admin/dashboard";
        }
        if ("STAFF".equals(roleName)) {
            return "redirect:/staff/dashboard";
        }
        return "redirect:/home";
    }
}
```

---

## 📊 So Sánh Trước & Sau

| Khía Cạnh | Trước (String) | Sau (State Pattern) |
|-----------|--------------|-------------------|
| **Kiểm tra trạng thái** | `if ("ACTIVE".equals(status))` | `if (account.canLogin())` |
| **Chuyển trạng thái** | `status = "SUSPENDED"` | `account.suspendAccount()` |
| **Hành vi theo trạng thái** | Phải kiểm tra string ở nhiều chỗ | Logic tập trung trong State class |
| **Dễ mở rộng** | ❌ Phải sửa nhiều chỗ | ✅ Chỉ tạo State class mới |
| **Type-safe** | ❌ String, dễ nhầm lẫn | ✅ Object, compile-time check |
| **Dễ test** | ❌ Mock string | ✅ Mock State object |

---

## 🔄 Quy Trình Login Với State Pattern

```
User nhập username & password
    ↓
AuthController.login() nhận request
    ↓
AccountService.login(username, password)
    ↓
Tìm Account từ DB
    ↓
✨ Kiểm tra: account.getState().canLogin()
    ├─ Nếu ActiveState.canLogin() → true
    │   └─ Kiểm tra mật khẩu
    │       ├─ Đúng → Tạo session → Redirect dashboard
    │       └─ Sai 5+ lần → lockAccount() → Lock state → Lỗi
    │
    ├─ Nếu SuspendedState.canLogin() → false
    │   └─ Hiển thị: "Tài khoản đã bị tạm khóa. Liên hệ admin"
    │
    └─ Nếu LockedOutState.canLogin() → false
        └─ Hiển thị: "Tài khoản bị khóa. Liên hệ admin để mở"
```

---

## ✅ Lợi Ích Của State Pattern

1. **Encapsulation**: Hành vi của mỗi trạng thái được đóng gói trong class riêng
2. **Single Responsibility**: Mỗi State class chỉ có trách nhiệm với một trạng thái
3. **Open/Closed Principle**: Dễ thêm trạng thái mới mà không ảnh hưởng code cũ
4. **Dễ test**: Có thể test từng state độc lập
5. **Code rõ ràng**: Logic trạng thái rõ ràng hơn so với nhiều if/else

---

## 🎬 Tóm Tắt

**State Pattern** cho phép:
- ✅ Quản lý trạng thái tài khoản (ACTIVE, SUSPENDED, LOCKED) thông qua các State objects
- ✅ Mỗi state có hành vi khác nhau (có thể login hay không)
- ✅ Dễ thêm trạng thái mới hoặc hành vi mới
- ✅ Code sạch, dễ maintain và dễ test
- ✅ Tránh lạm dụng if/else kiểm tra string status
