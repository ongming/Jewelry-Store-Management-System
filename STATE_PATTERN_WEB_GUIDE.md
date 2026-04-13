# STATE PATTERN - Hướng Dẫn Sử Dụng Trên Web

## 🚀 Các Tính Năng Đã Áp Dụng

### 1. **Login Page Enhancement** ✅
- **File**: `login.html`
- **Cải thiện**: Hiển thị lỗi chi tiết với thông báo state (SUSPENDED, LOCKED)
- **Hành vi**: Khi user login với tài khoản ở trạng thái SUSPENDED/LOCKED, hệ thống sẽ hiển thị thông báo rõ ràng

### 2. **Staff Management - State Badges** ✅
- **File**: `staff-management.html`
- **Tính năng**:
  - Hiển thị badge với màu sắc khác nhau cho mỗi state:
    - 🟢 **ACTIVE** (Xanh lá) - Hoạt động bình thường
    - 🟡 **SUSPENDED** (Vàng cam) - Bị tạm khóa
    - 🔴 **LOCKED** (Đỏ) - Bị khóa
    - ⚫ **INACTIVE** (Xám) - Chưa kích hoạt
  - Các nút hành động động (Tạm khóa / Kích hoạt / Mở khóa)
  - Hiển thị mô tả state trong view card

### 3. **Account State Dashboard** ✅
- **File**: `account-state-dashboard.html`
- **Tính năng**:
  - Thống kê số lượng accounts theo state
  - Bộ lọc để xem accounts theo state
  - Grid hiển thị accounts với card design
  - Các nút hành động nhanh cho mỗi account

### 4. **Account State List** ✅
- **File**: `account-state-list.html`
- **Tính năng**:
  - Danh sách accounts theo trạng thái cụ thể
  - Table view với state badge
  - Nút "Quản lý" để vào chi tiết

### 5. **Account State Manage** ✅
- **File**: `account-state-manage.html`
- **Tính năng**:
  - Hiển thị thông tin chi tiết tài khoản
  - Hiển thị quyền hạn theo state (có thể login, truy cập, sửa đổi)
  - Mô tả chi tiết của mỗi state
  - Timeline quy trình chuyển đổi state
  - Các nút hành động quản lý state

---

## 🎨 CSS & Styling

### **File**: `state-pattern.css`
Bao gồm:
- State badge styles (với icon và màu sắc)
- State action buttons
- State descriptions (alerts)
- State history & timeline
- Modal styles
- Toast notifications
- Responsive design

---

## 🔧 JavaScript

### **File**: `account-state-manager.js`
Cung cấp:
- `AccountStateManager` class để quản lý state
- Hàm gọi API state actions
- Modal confirm dialogs
- Toast notifications
- Loading indicators

**Các Method**:
- `executeStateAction(action, accountId)` - Thực hiện hành động state
- `getAccountState(accountId)` - Lấy thông tin state tài khoản
- `showStateActionModal()` - Hiển thị modal xác nhận
- `showToast()` - Hiển thị notification

---

## 🌐 REST API Endpoints

### **Controller**: `AccountStateApiController`

| Endpoint | Method | Mô tả |
|----------|--------|-------|
| `/api/accounts/{id}/suspend` | POST | Tạm khóa tài khoản |
| `/api/accounts/{id}/activate` | POST | Kích hoạt tài khoản |
| `/api/accounts/{id}/lock` | POST | Khóa tài khoản |
| `/api/accounts/{id}/state` | POST | Lấy thông tin state |
| `/api/accounts/all` | GET | Lấy danh sách tất cả accounts |
| `/api/accounts/by-status/{status}` | GET | Lấy accounts theo status |

**Response Format**:
```json
{
  "success": true,
  "accountId": 1,
  "username": "john.admin",
  "status": "ACTIVE",
  "stateName": "ACTIVE",
  "canLogin": true,
  "canAccessSystem": true,
  "canModifyData": true
}
```

---

## 🎯 Web Controller Routes

### **Controller**: `AccountStateWebController`

| Route | Method | Mô tả |
|-------|--------|-------|
| `/admin/account-state/dashboard` | GET | Dashboard quản lý state |
| `/admin/account-state/by-status` | GET | Danh sách theo status |
| `/admin/account-state/manage/{id}` | GET | Trang quản lý state chi tiết |
| `/admin/account-state/suspend` | POST | Tạm khóa (form) |
| `/admin/account-state/activate` | POST | Kích hoạt (form) |
| `/admin/account-state/lock` | POST | Khóa (form) |

---

## 📱 Cách Sử Dụng Trên Web

### **1. Từ Staff Management Page**
```
Admin Dashboard
  ↓
Quản lý nhân sự
  ↓
Chọn tài khoản
  ↓
Nhấn nút "Tạm khóa" / "Kích hoạt" / "Mở khóa"
  ↓
Xác nhận trong modal
  ↓
Tài khoản được cập nhật + Refresh trang
```

### **2. Từ Account State Dashboard**
```
Admin Dashboard
  ↓
Quản lý trạng thái tài khoản
  ↓
Xem thống kê và filter theo state
  ↓
Chọn account từ grid
  ↓
Nhấn nút hành động
  ↓
State được cập nhật
```

### **3. Từ Account State Manage Page**
```
Account State Dashboard
  ↓
Nhấn "Quản lý" hoặc "Chi tiết"
  ↓
Xem thông tin chi tiết state
  ↓
Xem timeline chuyển đổi state
  ↓
Nhấn nút hành động (Tạm khóa / Kích hoạt)
  ↓
Form submit → State cập nhật
```

---

## 🔄 State Transition Flow

```
INACTIVE
    ↓ (activate)
ACTIVE ←→ SUSPENDED
    ↓       ↓
  LOCKED ←─┘
    ↓
 (activate) → ACTIVE
```

### State Transitions Chi Tiết:
- **ACTIVE → SUSPENDED**: Nhấn "Tạm khóa"
- **SUSPENDED → ACTIVE**: Nhấn "Kích hoạt"
- **ACTIVE → LOCKED**: Tự động (sai password 5+ lần) hoặc admin khóa
- **LOCKED → ACTIVE**: Nhấn "Mở khóa"
- **INACTIVE → ACTIVE**: Nhấn "Kích hoạt"

---

## ✨ Features Trong Mỗi Trang

### **Login Page**
```
❌ Lỗi đăng nhập với thông báo state
   - "Tài khoản đang ở trạng thái: SUSPENDED"
   - "Tài khoản đang ở trạng thái: LOCKED"
   - "Vui lòng liên hệ quản trị viên"
```

### **Staff Management**
```
✅ Hiển thị state badges
✅ Nút "Tạm khóa" / "Kích hoạt" cho từng tài khoản
✅ View card với mô tả state
✅ Xác nhận hành động trong modal
✅ Toast notification khi thành công
```

### **Account State Dashboard**
```
📊 Thống kê:
   - Số lượng ACTIVE
   - Số lượng SUSPENDED
   - Số lượng LOCKED
   - Số lượng INACTIVE

🔍 Bộ lọc theo state

📋 Grid hiển thị accounts:
   - Thông tin cơ bản
   - State badge với icon
   - Nút hành động nhanh
```

### **Account State Manage**
```
📌 Thông tin tài khoản

🔐 Quyền hạn theo state:
   - ✅ Có thể login?
   - ✅ Có thể truy cập?
   - ✅ Có thể sửa đổi?

📝 Mô tả chi tiết state

⏳ Timeline chuyển đổi state

🎯 Nút hành động:
   - Tạm khóa (nếu ACTIVE)
   - Kích hoạt (nếu SUSPENDED/LOCKED/INACTIVE)
```

---

## 🧪 Test State Pattern Trên Web

### **Test 1: Login với tài khoản SUSPENDED**
1. Vào page `/admin/account-state/dashboard`
2. Chọn một tài khoản ACTIVE
3. Nhấn "Tạm khóa"
4. Logout
5. Thử login với tài khoản đó
6. ❌ Nhận lỗi: "Tài khoản đang ở trạng thái: SUSPENDED"

### **Test 2: Tạo State Transition**
1. Vào Staff Management (`/admin/staff-management`)
2. Nhấn "Tạm khóa" → SUSPENDED
3. Nhấn "Kích hoạt" → ACTIVE
4. Nhấn "Tạm khóa" → SUSPENDED
5. Vào Account State Manage
6. Xem timeline hiển thị các transition

### **Test 3: API Testing**
```bash
# Lấy danh sách tất cả accounts
curl -X GET http://localhost:8080/api/accounts/all

# Lấy thông tin state
curl -X POST http://localhost:8080/api/accounts/1/state

# Tạm khóa tài khoản
curl -X POST http://localhost:8080/api/accounts/1/suspend

# Kích hoạt tài khoản
curl -X POST http://localhost:8080/api/accounts/1/activate
```

---

## 📁 Danh sách Files Tạo Ra

### **Templates** (3 files):
- ✅ `login.html` - Cập nhật thông báo lỗi
- ✅ `admin/staff-management.html` - Cập nhật state badges & buttons
- ✅ `admin/account-state-dashboard.html` - Dashboard quản lý state
- ✅ `admin/account-state-list.html` - Danh sách theo status
- ✅ `admin/account-state-manage.html` - Quản lý chi tiết

### **Static Files** (2 files):
- ✅ `css/state-pattern.css` - Styling cho state badges, buttons, modals
- ✅ `js/account-state-manager.js` - JavaScript class xử lý state

### **Controllers** (2 files):
- ✅ `AccountStateApiController.java` - REST API endpoints (4 endpoints)
- ✅ `AccountStateWebController.java` - Web routes & views

---

## 🎁 Tổng Kết

| Thành Phần | Status | Mô Tả |
|-----------|--------|-------|
| State Classes | ✅ | ActiveState, SuspendedState, LockedOutState, InactiveState |
| Account Entity | ✅ | Cập nhật với state object |
| AccountService | ✅ | Thêm suspend, activate, lock methods |
| REST APIs | ✅ | 6 endpoints quản lý state |
| Web Templates | ✅ | 5 templates hiển thị state |
| CSS Styling | ✅ | Đầy đủ styles cho state badges & actions |
| JavaScript | ✅ | AccountStateManager class |
| Web Controllers | ✅ | Routes & views cho state management |

---

## 🚀 Cách Chạy & Test

### **1. Build & Run**
```bash
cd C:\Users\acer\Desktop\TKHDT\Jewelry-Store-Management-System
mvn clean package
mvn spring-boot:run
```

### **2. Truy Cập Trang**
```
Login: http://localhost:8080/auth/login
Staff Management: http://localhost:8080/admin/staff-management
Account State Dashboard: http://localhost:8080/admin/account-state/dashboard
```

### **3. Test State Actions**
- Chọn tài khoản
- Nhấn "Tạm khóa" / "Kích hoạt" / "Mở khóa"
- Xác nhận trong modal
- Xem state được cập nhật ngay

---

## 💡 Ưu Điểm Của Implementation

✅ **Type-Safe**: Dùng State objects thay vì strings
✅ **Encapsulation**: Logic state đóng gói trong class
✅ **Easy to Extend**: Thêm state mới mà không ảnh hưởng code cũ
✅ **Testable**: Có thể test từng state độc lập
✅ **User-Friendly**: Web UI với badges, colors, modals
✅ **RESTful**: API endpoints cho integration
✅ **Responsive**: Mobile-friendly design

---

**Đây là một implementation hoàn chỉnh của State Pattern trên web! 🎉**
