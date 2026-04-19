# 📋 THAY ĐỔI VỀ HỆ THỐNG - CONTEXT MENU

## 🎯 TÍNH NĂNG ĐÃ THÊM

### Context Menu - Menu Chuột Phải (Phòng Thuê/Trả)

Khi nhấn **chuột phải** vào một phòng trong sơ đồ "Sơ đồ phòng – Thuê phòng", một popup menu sẽ hiện ra với các tùy chọn:

```
┌────────────────────────┐
│ 👁️  Xem chi tiết      │
│ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │
│ 🔄  Đổi phòng         │ (Phòng OCCUPIED)
│ ➕  Thêm dịch vụ      │ (Phòng OCCUPIED)
│ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │
│ 💳  Thanh toán        │ (Phòng OCCUPIED)
└────────────────────────┘
```

---

## 📍 CHỨC NĂNG CHI TIẾT

### 1. 👁️ Xem Chi Tiết
- **Khả dụng**: Tất cả phòng (bất kể trạng thái)
- **Thông tin hiển thị**:
  - Số phòng, Tầng
  - Loại phòng, View
  - Sức chứa
  - Giá tiền/đêm
  - Trạng thái hiện tại
  - Thông tin khách (nếu có)
  - Thời gian nhận/trả
  - Số đêm tính toán tự động

**Ví dụ**:
```
═══════════════════
THÔNG TIN PHÒNG
═══════════════════

Phòng: P101
Tầng: 1
Loại: Deluxe
View: City View
Sức chứa: 2 người
Giá/đêm: 500,000 đ
Trạng thái: Đang thuê

[KHÁCH HIỆN TẠI]
Tên: Nguyễn Văn A
Nhận: 2026-03-28T14:00:00
Trả: 2026-03-30T12:00:00
Số đêm: 2
```

---

### 2. 🔄 Đổi Phòng
- **Khả dụng**: Chỉ phòng OCCUPIED
- **Điều kiện**: Phòng mới must be AVAILABLE + cùng loại
- **Kết quả**:
  - Phòng cũ → AVAILABLE (clear tên khách)
  - Phòng mới → OCCUPIED (update tên khách)
  - ChiTietDatPhong → update reference phòng

**Quy trình**:
1. Click phải → "🔄 Đổi phòng"
2. Chọn phòng mới từ danh sách
3. Xác nhận
4. Hệ thống cập nhật tự động

---

### 3. ➕ Thêm Dịch Vụ
- **Khả dụng**: Chỉ phòng OCCUPIED
- **Dịch vụ**: Tất cả dịch vụ trong hệ thống
- **Lưu trữ**: SuDungDichVu table

**Thông tin**:
- Chọn dịch vụ từ dropdown
- Nhập số lượng (1-100)
- Hiển thị giá tự động
- Lưu vào DB

**Entity được tạo**:
```java
SuDungDichVu {
  ctdp: ChiTietDatPhong
  dichVu: DichVu
  soLuong: 2
  donGiaLuu: 50000
  thoiDiem: 2026-03-28T15:30:00
}
```

---

### 4. 💳 Thanh Toán
- **Khả dụng**: Chỉ phòng OCCUPIED  
- **Xử lý**: Gọi HoaDonDialog
- **Kết quả**: 
  - Thanh toán hóa đơn
  - Phòng → CLEANING

---

## 🔧 THAY ĐỔI FILE

### File Chính
**[src/UI/panels/ThuePhongPanel.java](src/UI/panels/ThuePhongPanel.java)**

### Phương Thức Mới
```java
// Context Menu chính
private void showContextMenu(Phong p, MouseEvent e)

// Xem chi tiết
private void showRoomDetails(Phong p)

// Đổi phòng
private void showSwitchRoomDialog(Phong currentPhong)
private void doSwitchRoom(ChiTietDatPhong tp, Phong oldRoom, Phong newRoom)

// Thêm dịch vụ
private void showAddServiceDialog(Phong p)
private void doAddService(ChiTietDatPhong tp, DichVu service, int quantity)
```

### Phương Thức Sửa Đổi
```java
// Sửa buildRoomCard() để thêm mouseListener
// Hỗ trợ mousePressed/mouseReleased cho context menu
```

### Import Bổ Sung
```java
import dao.*;  // Cho SuDungDichVuDAO
```

---

## 🎨 STYLE & UX

### Men u Item Icons
- 👁️  Xem chi tiết
- 🔄  Đổi phòng
- ➕  Thêm dịch vụ
- 💳  Thanh toán

### Trạng Thái Menu
- ✓ Item available → Normal
- ✗ Item not available → Dimmed/Disabled

### Dialog Style
Tất cả dialog tuân theo style UIConstants:
- Font: Segoe UI
- Primary Color: UIConstants.PRIMARY
- Background: Color.WHITE
- Rounded corners: 10px

---

## 📊 BẢNG TRẠNG THÁI

| Item | AVAILABLE | OCCUPIED | CLEANING | MAINTENANCE |
|------|:---------:|:--------:|:--------:|:-----------:|
| Xem chi tiết | ✓ | ✓ | ✓ | ✓ |
| Đổi phòng | ✗ | ✓ | ✗ | ✗ |
| Thêm dịch vụ | ✗ | ✓ | ✗ | ✗ |
| Thanh toán | ✗ | ✓ | ✗ | ✗ |

---

## 🔗 LIÊN KẾT ENTITY & SERVICE

```
Phong
  ├─ PhongService (updateTrangThai, suaPhong)
  └─ UX: buildRoomCard()

ChiTietDatPhong
  ├─ ThuePhongService (getActiveByPhong)
  ├─ DatPhong → KhachHang
  └─ SuDungDichVu (list)

DichVu
  └─ DichVuService (getAll)

SuDungDichVu
  ├─ ChiTietDatPhong
  ├─ DichVu
  └─ SuDungDichVuDAO (insert)
```

---

## 🧪 TEST CHECKLIST

- [ ] **Xem chi tiết**: Click phải phòng → Hiển thị popup/dialog
- [ ] **Thông tin**: Tất cả field hiển thị đúng
- [ ] **Menu items**: Xem chi tiết luôn enable, others Occupied only
- [ ] **Đổi phòng**: Chọn phòng → Update UI thành công
- [ ] **Thêm dịch vụ**: Giá tự động, lưu DB thành công
- [ ] **Thanh toán**: Dialog HoaDon mở
- [ ] **Refresh**: UI tự động reload sau mỗi action
- [ ] **Encoding**: Tiếng Việt hiển thị đúng

---

## 🚀 HƯỚNG PHÁT TRIỂN

- [ ] Thêm "Xem hóa đơn"
- [ ] Thêm "Lịch sử phòng"
- [ ] Thêm "Ghi chú"
- [ ] Batch actions (multi-select)
- [ ] Drag & drop để đổi phòng
- [ ] Phím tắt: Shift+Right click = menu nhanh

---

## 📝 GHI CHÚ

1. **Mouse Events**: Hỗ trợ Windows (mouseReleased) và Mac (mousePressed)
2. **Encoding**: Dùng `-encoding UTF-8` khi compile
3. **Database**: SuDungDichVu auto-insert via DAO
4. **UI Refresh**: Tự động gọi `refresh()` sau updates
5. **Error Handling**: Try-catch với user messages

---

## ✅ HOÀN THÀNH

- ✓ Context Menu implementation
- ✓ Xem chi tiết
- ✓ Đổi phòng
- ✓ Thêm dịch vụ
- ✓ Thanh toán (tái sử dụng)
- ✓ UI/UX styling
- ✓ Database integration
- ✓ Compilation & Testing
- ✓ Documentation

**Ngày hoàn thiện: 28/03/2026**
