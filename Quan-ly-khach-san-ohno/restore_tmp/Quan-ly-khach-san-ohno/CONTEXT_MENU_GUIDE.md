# 🎯 Hướng Dẫn Context Menu (Menu Chuột Phải) - Phòng Thuê/Trả

## 📝 Tổng Quan
Tính năng Context Menu (Menu chuột phải) đã được hoàn thiện cho phần "Sơ đồ phòng – Thuê phòng". Khi nhấn chuột phải vào một phòng, sẽ hiện ra popup menu với 4 lựa chọn nhanh.

---

## 🔧 Các Tính Năng

### 1️⃣ **👁️ Xem Chi Tiết** (Xem tất cả phòng)
- **Mục đích**: Hiển thị thông tin chi tiết về phòng
- **Thông tin hiển thị**:
  - 📍 Số phòng & Tầng
  - 🏷️ Loại phòng & View
  - 🛏️ Sức chứa (số người tối đa)
  - 💰 Giá tiền/đêm
  - 🔴 Trạng thái hiện tại
  - 👤 Tên khách hiện tại (nếu có)
  - 📅 Ngày nhận/trả (nếu có)
  - 🌙 Số đêm lưu trú (tính toán tự động)

---

### 2️⃣ **🔄 Đổi Phòng** (Chỉ áp dụng cho phòng OCCUPIED)
- **Mục đích**: Chuyển khách sang phòng khác cùng loại
- **Điều kiện sử dụng**:
  - Phòng hiện tại: OCCUPIED (đang có khách)
  - Phòng mới: AVAILABLE (trống sẵn sàng) + cùng loại
  
- **Quy trình**:
  1. Nhấn "🔄 Đổi phòng" trên phòng đang thuê
  2. Hệ thống mở dialog chọn phòng mới
  3. Chọn phòng cùng loại từ danh sách
  4. Nhấn "Đổi phòng" để xác nhận
  5. Hệ thống tự động:
     - ✅ Đánh dấu phòng cũ → AVAILABLE
     - ✅ Đánh dấu phòng mới → OCCUPIED
     - ✅ Cập nhật tên khách tương ứng

---

### 3️⃣ **➕ Thêm Dịch Vụ** (Chỉ áp dụng cho phòng OCCUPIED)
- **Mục đích**: Thêm dịch vụ bổ sung cho khách đang thuê
- **Dịch vụ có sẵn**: Ăn uống, Spa, Vận chuyển, Dịch vụ phòng, v.v.
- **Quy trình**:
  1. Nhấn "➕ Thêm dịch vụ" trên phòng đang thuê
  2. Hệ thống mở dialog danh sách dịch vụ
  3. Chọn dịch vụ từ dropdown
  4. Nhập số lượng (mặc định: 1)
  5. Giá sẽ hiển thị tự động
  6. Nhấn "Thêm dịch vụ"
  7. Hệ thống lưu vào cơ sở dữ liệu

**Ví dụ**: 
- Dịch vụ: "Gọi dịch vụ phòng"
- Số lượng: 2
- Giá: 50,000đ/lần
- Tổng: 100,000đ

---

### 4️⃣ **💳 Thanh Toán** (Chỉ áp dụng cho phòng OCCUPIED)
- **Mục đích**: Xử lý checkout & thanh toán hóa đơn
- **Ghi chú**: Sử dụng HoaDonDialog hiện có
- **Quy trình**:
  1. Nhấn "💳 Thanh toán" trên phòng đang thuê
  2. Hệ thống mở dialog Hóa Đơn
  3. Xem chi tiết tiền phòng + dịch vụ
  4. Xử lý thanh toán
  5. In hoặc lưu hóa đơn
  6. Phòng tự động → CLEANING

---

## 🎨 Giao Diện

### Context Menu
```
┌─────────────────────────────┐
│ 👁️  Xem chi tiết           │
├─────────────────────────────┤
│ 🔄  Đổi phòng       (disable)│  ✓ Nếu OCCUPIED
│ ➕  Thêm dịch vụ     (disable)│  ✓ Nếu OCCUPIED
├─────────────────────────────┤
│ 💳  Thanh toán       (disable)│  ✓ Nếu OCCUPIED
└─────────────────────────────┘
```

### Trạng Thái Menu
- ✅ **"Xem chi tiết"**: Luôn available (tất cả trạng thái)
- ⚠️  **"Đổi phòng", "Thêm dịch vụ", "Thanh toán"**: Chỉ available khi phòng OCCUPIED
- 🚫 **Disable**: Nếu điều kiện không đủ

---

## 💻 Triển Khai Kỹ Thuật

### Tệp Chính Được Sửa Đổi
- **[ThuePhongPanel.java](src/UI/panels/ThuePhongPanel.java)**

### Phương Thức Chính
1. `showContextMenu(Phong p, MouseEvent e)` - Hiển thị popup menu
2. `showRoomDetails(Phong p)` - Dialog xem chi tiết
3. `showSwitchRoomDialog(Phong currentPhong)` - Dialog chọn phòng đổi
4. `doSwitchRoom(...)` - Xử lý logic đổi phòng
5. `showAddServiceDialog(Phong p)` - Dialog thêm dịch vụ
6. `doAddService(...)` - Xử lý logic thêm dịch vụ

### Xử Lý Chuột
```java
card.addMouseListener(new MouseAdapter() {
    @Override public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) handleRoomClick(p); // Clic trái
    }
    @Override public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) showContextMenu(p, e); // Chuột phải (Windows)
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) showContextMenu(p, e); // Chuột phải (Mac)
    }
});
```

---

## 📊 Trạng Thái Phòng

| Trạng Thái | Màu | "Xem Chi Tiết" | "Đổi Phòng" | "Thêm DV" | "Thanh Toán" |
|----------|------|:--------------:|:----------:|:--------:|:----------:|
| AVAILABLE | 🟢 Xanh | ✓ | ✗ | ✗ | ✗ |
| OCCUPIED | 🔴 Đỏ | ✓ | ✓ | ✓ | ✓ |
| CLEANING | 🟡 Vàng | ✓ | ✗ | ✗ | ✗ |
| MAINTENANCE | 🟣 Tím | ✓ | ✗ | ✗ | ✗ |

---

## 🔄 Quy Trình Đối Tượng

### Quy Trình Đổi Phòng
```
[Nhấn Chuột Phải] → [Context Menu] → [Chọn "Đổi Phòng"]
    ↓
[Dialog chọn phòng mới] → [Chọn phòng] → [Xác nhận]
    ↓
[Update Phòng Cũ: AVAILABLE] 
[Update Phòng Mới: OCCUPIED + tên khách]
[Cập nhật ChiTietDatPhong]
    ↓
[Refresh UI] → [Reload sơ đồ phòng]
```

### Quy Trình Thêm Dịch Vụ
```
[Nhấn Chuột Phải] → [Context Menu] → [Chọn "Thêm Dịch Vụ"]
    ↓
[Dialog danh sách dịch vụ]
[Chọn dịch vụ] → [Nhập số lượng] → [Xác nhận]
    ↓
[Tạo SuDungDichVu object]
[Lưu vào DB via SuDungDichVuDAO.insert()]
    ↓
[Refresh UI] → [Hiển thị success message]
```

---

## ⚙️ Cấu Hình

### Import Bổ Sung
```java
import dao.*;  // Cho SuDungDichVuDAO
```

### Entity Sử Dụng
- `Phong` - Thông tin phòng
- `ChiTietDatPhong` - Thông tin đặt/thuê phòng
- `DichVu` - Thông tin dịch vụ
- `SuDungDichVu` - Sử dụng dịch vụ
- `TrangThaiPhong` - Enum trạng thái

### DAO Sử Dụng
- `PhongDAO` - Thông qua PhongService
- `SuDungDichVuDAO` - Lưu dịch vụ sử dụng

### Service Sử Dụng
- `PhongService` - Cập nhật thông tin phòng
- `ThuePhongService` - Lấy thông tin thuê phòng
- `DichVuService` - Lấy danh sách dịch vụ

---

## 🧪 Kiểm Thử

### Test Case 1: Xem Chi Tiết
- ✓ Click phải trên phòng bất kỳ
- ✓ Dialog hiển thị đúng thông tin
- ✓ Đóng dialog → Quay lại

### Test Case 2: Đổi Phòng
- ✓ Click phải trên phòng OCCUPIED
- ✓ "Đổi Phòng" enable (không dim)
- ✓ Chọn phòng → Xác nhận
- ✓ Phòng cũ chuyển AVAILABLE
- ✓ Phòng mới chuyển OCCUPIED với tên khách

### Test Case 3: Thêm Dịch Vụ
- ✓ Click phải trên phòng OCCUPIED
- ✓ "Thêm Dịch Vụ" enable
- ✓ Chọn dịch vụ → Giá hiển thị
- ✓ Nhập số lượng → Xác nhận
- ✓ Kiểm tra DB: SuDungDichVu được lưu

### Test Case 4: Thanh Toán
- ✓ Click phải trên phòng OCCUPIED
- ✓ "Thanh Toán" enable
- ✓ Dialog HoaDon mở
- ✓ Xử lý thanh toán thành công

---

## 🚀 Tính Năng Tương Lai

- [ ] Thêm tính năng "In phòng"
- [ ] Thêm "Lịch sử hoạt động" cho từng phòng
- [ ] "Ghi chú" cho phòng
- [ ] "Hình ảnh" phòng
- [ ] "Rating" phòng từ khách

---

## 📞 Hỗ Trợ

Nếu có lỗi hoặc câu hỏi, vui lòng:
1. Kiểm tra log console
2. Đảm bảo phòng có trạng thái OCCUPIED
3. Kiểm tra kết nối DB

---

**🎉 Hoàn Thành: 28/03/2026**
