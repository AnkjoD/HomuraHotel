# 🧪 HƯỚNG DẪN TEST CONTEXT MENU

## ⚡ Quick Start Test

### Điều Kiện Tiên Quyết
- ✓ Ứng dụng đã compile thành công
- ✓ Database đã khởi động
- ✓ Có dữ liệu phòng (ít nhất 2 phòng cùng loại)
- ✓ Có dữ liệu dịch vụ

---

## 🧪 TEST CASES

### Test 1: Context Menu Hiển Thị ✓
**Mục đích**: Kiểm tra popup menu xuất hiện

**Bước**:
1. Mở ứng dụng → Đăng nhập
2. Chuyển sang tab "Sơ đồ phòng – Thuê phòng"
3. Nhấn **chuột phải** vào một phòng bất kỳ

**Kết quả mong đợi**:
- Popup menu xuất hiện gần con trỏ
- Menu có 4 item: Xem chi tiết, Đổi phòng, Thêm dịch vụ, Thanh toán

---

### Test 2: Xem Chi Tiết ✓
**Mục đích**: Hiển thị thông tin phòng

**Bước**:
1. Click phải trên **phòng AVAILABLE** (trống)
2. Chọn "👁️ Xem chi tiết"

**Kết quả mong đợi**:
- Dialog/Popup hiển thị thông tin:
  - Phòng: (số phòng)
  - Tầng: (số tầng)
  - Loại: (tên loại)
  - View: (view name)
  - Sức chứa: X người
  - Giá/đêm: xxxxxx đ
  - Trạng thái: Có sẵn

---

### Test 3: Xem Chi Tiết (Phòng Occupied) ✓
**Mục đích**: Hiển thị thêm thông tin khách

**Bước**:
1. Click phải trên **phòng OCCUPIED** (đang thuê)
2. Chọn "👁️ Xem chi tiết"

**Kết quả mong đợi**:
- Dialog hiển thị:
  - Thông tin phòng (như test 2)
  - **[KHÁCH HIỆN TẠI]**
    - Tên: (tên khách)
    - Nhận: (ngày/giờ)
    - Trả: (ngày/giờ)
    - Số đêm: (số ngày)

---

### Test 4: Menu Item Status ✓
**Mục đích**: Kiểm tra item enable/disable đúng

**Bước**:
1. Click phải **phòng AVAILABLE**

**Kết quả mong đợi**:
```
✓ Xem chi tiết    → ENABLE (bình thường)
✗ Đổi phòng       → DISABLE (mờ/không được click)
✗ Thêm dịch vụ    → DISABLE (mờ/không được click)
✗ Thanh toán      → DISABLE (mờ/không được click)
```

**Bước**:
2. Click phải **phòng OCCUPIED**

**Kết quả mong đợi**:
```
✓ Xem chi tiết    → ENABLE
✓ Đổi phòng       → ENABLE (được click)
✓ Thêm dịch vụ    → ENABLE (được click)
✓ Thanh toán      → ENABLE (được click)
```

---

### Test 5: Đổi Phòng ✓
**Prerequisites**: 
- Có 1 phòng **OCCUPIED** (phòng A)
- Có ít nhất 2 phòng **AVAILABLE** cùng loại (phòng B, C)

**Bước**:
1. Click phải **phòng A (OCCUPIED)**
2. Chọn "🔄 Đổi phòng"
3. Dialog mở với danh sách phòng trống cùng loại
4. Chọn **phòng B**
5. Nhấn "Đổi phòng"

**Kết quả mong đợi**:
- ✓ Message "Đã đổi phòng thành công"
- ✓ Phòng A: chuyển AVAILABLE (không còn tên khách)
- ✓ Phòng B: chuyển OCCUPIED (cập nhật tên khách)
- ✓ UI tự động reload

**Xác minh**:
1. Xem chi tiết phòng A → trống
2. Xem chi tiết phòng B → có tên khách

---

### Test 6: Thêm Dịch Vụ ✓
**Prerequisites**:
- Có 1 phòng **OCCUPIED**
- Có ít nhất 1 dịch vụ trong DB

**Bước**:
1. Click phải **phòng OCCUPIED**
2. Chọn "➕ Thêm dịch vụ"
3. Dialog mở danh sách dịch vụ
4. Chọn dịch vụ (vd: "Massage")
5. Giá hiển thị tự động
6. Thay đổi số lượng: 1 → 2
7. Nhấn "Thêm dịch vụ"

**Kết quả mong đợi**:
- ✓ Message "Đã thêm dịch vụ: Massage x2"
- ✓ Dialog đóng
- ✓ UI reload

**Xác minh DB**:
```sql
SELECT * FROM SuDungDichVu 
WHERE maThue = (ChiTietDatPhong ID)
ORDER BY thoiDiem DESC LIMIT 1;
```

**Kết quả**:
- soLuong: 2
- donGiaLuu: (giá dịch vụ)
- thoiDiem: (hiện tại)

---

### Test 7: Thanh Toán ✓
**Prerequisites**:
- Có 1 phòng **OCCUPIED**

**Bước**:
1. Click phải **phòng OCCUPIED**
2. Chọn "💳 Thanh toán"

**Kết quả mong đợi**:
- ✓ Dialog HoaDon mở
- ✓ Hiển thị:
  - Mã hóa đơn
  - Thông tin khách
  - Chi tiết tiền phòng
  - Danh sách dịch vụ
  - Tổng cộng

---

### Test 8: Left Click vs Right Click ✓
**Mục đích**: Kiểm tra left click vẫn hoạt động

**Bước**:
1. **Left click** (chuột trái) trên phòng AVAILABLE
   → `showCheckinDialog()` được gọi

2. **Left click** trên phòng OCCUPIED
   → `showCheckoutDialog()` được gọi

3. **Right click** (chuột phải) trên bất kỳ phòng
   → `showContextMenu()` được gọi

**Kết quả mong đợi**:
- Chuột trái: Mở dialog Nhận/Trả phòng (hoạt động như trước)
- Chuột phải: Mở context menu (chức năng mới)

---

## 🔍 Kiểm Tra Chi Tiết

### Encoding Check
```bash
# Tiếng Việt phải hiển thị đúng
- "Xem chi tiết" → ✓ (không phải "Xem chi tiWhat")
- "Đổi phòng" → ✓ (không phải "─?├ú ─æß║╖t")
- "Thêm dịch vụ" → ✓
- "Thanh toán" → ✓
```

### Performance Check
```
- Menu xuất hiện: < 100ms
- Dialog mở: < 500ms
- DB insert (dịch vụ): < 1s
- UI refresh: < 500ms
```

### Error Handling Check
```
1. Right click → No active rentals → "Không tìm thấy thông tin thuê phòng"
2. Switch room → No available → "Không có phòng cùng loại trống"
3. Add service → No services → "Không có dịch vụ nào"
4. DB error → Stack trace in console + user message
```

---

## 🐛 Debug Tips

### Nếu Menu Không Xuất Hiện
```
1. Kiểm tra: MouseEvent.isPopupTrigger() có true không
2. Kiểm tra console: có exception không
3. Thử: Right click ở vị trí khác trên card
4. Thử: Sử dụng máy khác (đôi khi là vấn đề hệ thống)
```

### Nếu Item Bị Disable Sai
```
1. Kiểm tra giá trị tt (TrangThaiPhong)
2. Kiểm tra: == TrangThaiPhong.OCCUPIED
3. Debug: Print giá trị trangThai ra console
```

### Nếu Dialog Theo Nguồng
```
1. Kiểm tra: Có data trong dropdown không
2. Kiểm tra: DichVuService.getAll() trả về list hay null
3. Debug: Add log statements
```

### Nếu DB Không Lưu
```
1. Kiểm tra: SuDungDichVuDAO.insert() return true/false
2. Kiểm tra: DB connection active
3. Kiểm tra: Table schema có đúng không
4. Debug: Check exception message
```

---

## 📋 Test Report Template

```
TEST CASE: [Tên test]
Date: [Ngày]
Status: [PASS/FAIL]

Steps:
1. ...
2. ...

Expected Result: ...

Actual Result: ...

Notes: ...
```

---

## ✅ FINAL CHECKLIST

- [ ] Test 1: Context Menu Hiển Thị
- [ ] Test 2: Xem Chi Tiết (AVAILABLE)
- [ ] Test 3: Xem Chi Tiết (OCCUPIED)
- [ ] Test 4: Menu Item Status
- [ ] Test 5: Đổi Phòng
- [ ] Test 6: Thêm Dịch Vụ
- [ ] Test 7: Thanh Toán
- [ ] Test 8: Left vs Right Click
- [ ] Encoding Check
- [ ] Performance Check
- [ ] Error Handling Check

---

**Hoàn chỉnh: 28/03/2026**
