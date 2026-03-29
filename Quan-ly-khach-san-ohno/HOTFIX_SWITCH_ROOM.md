# 🔧 HOTFIX: Context Menu - Đổi Phòng

## 🐛 VẤNĐỀ PHÁT HIỆN

### Vấn Đề 1: Chỉ Chọn Được 1-2 Phòng ❌
**Nguyên nhân**: Logic filter phòng không chính xác
- Khi `loaiPhong` object không load đầy đủ, `getTenLoaiPhong()` trả về empty string ""
- Filter `p.getTenLoaiPhong().equals(currentPhong.getTenLoaiPhong())` sai kết quả
- Vàng hoặc null pointer exception

**Giải pháp**:
```java
// ❌ CŨ (sai)
.filter(p -> p.getTrangThai() == TrangThaiPhong.AVAILABLE 
        && p.getTenLoaiPhong().equals(currentPhong.getTenLoaiPhong()))

// ✅ MỚI (sửa)
.filter(p -> p.getTrangThai() == TrangThaiPhong.AVAILABLE 
        && p.getLoaiPhong() != null
        && p.getLoaiPhong().getTenLoaiPhong() != null
        && p.getLoaiPhong().getTenLoaiPhong().equals(loaiPhongHienTai))
```

**Bổ sung**: Thêm log debug để kiểm tra
```java
String loaiPhongHienTai = currentPhong.getTenLoaiPhong();
System.out.println("🔍 Filter phòng có loại: " + loaiPhongHienTai);
System.out.println("📊 Tìm được " + availableRooms.size() + " phòng cùng loại");
```

---

### Vấn Đề 2: Lỗi "Không Có Thông Tin Phòng" Sau Đổi ❌
**Nguyên nhân**: **Không lưu ChiTietDatPhong vào Database**
- Sau đổi phòng, chỉ update object trong memory: `tp.setPhong(newRoom)`
- Comment cũ: "Note: You may need to save this to DB using a DAO if not already saved"
- Khi gọi `refresh()`, hệ thống tải lại từ DB → không tìm thấy ChiTietDatPhong liên kết với phòng mới

**Kết quả**:
- `thuePhongService.getActiveByPhong(newRoom.getSoPhong())` không tìm được → null
- → "Không tìm thấy thông tin thuê phòng"

**Giải pháp**:

1. **Thêm method `update()` vào ChiTietDatPhongDAO** [src/dao/ChiTietDatPhongDAO.java](src/dao/ChiTietDatPhongDAO.java)
```java
public boolean update(ChiTietDatPhong ct) {
    String sql = "UPDATE ChiTietDatPhong SET maDatPhong=?, maPhong=?, ngayNhanThucTe=?, ngayTraThucTe=?, giaThucTeChot=?, phuPhiPhatSinh=? WHERE maChiTiet=?";
    try (PreparedStatement ps = getConn().prepareStatement(sql)) {
        ps.setString(1, ct.getDatPhong() != null ? ct.getDatPhong().getMaDatPhong() : null);
        ps.setString(2, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
        ps.setTimestamp(3, ct.getNgayNhanThucTe() != null ? Timestamp.valueOf(ct.getNgayNhanThucTe()) : null);
        ps.setTimestamp(4, ct.getNgayTraThucTe() != null ? Timestamp.valueOf(ct.getNgayTraThucTe()) : null);
        ps.setDouble(5, ct.getGiaThucTeChot());
        ps.setDouble(6, ct.getPhuPhiPhatSinh());
        ps.setString(7, ct.getMaChiTiet());
        return ps.executeUpdate() > 0;
    } catch (SQLException e) { e.printStackTrace(); return false; }
}
```

2. **Gọi update() trong `doSwitchRoom()`** [src/UI/panels/ThuePhongPanel.java](src/UI/panels/ThuePhongPanel.java)
```java
private void doSwitchRoom(ChiTietDatPhong tp, Phong oldRoom, Phong newRoom) {
    try {
        // ... update phòng cũ và mới ...
        
        // Update ChiTietDatPhong với phòng mới
        tp.setPhong(newRoom);
        
        // **QUAN TRỌNG**: Lưu vào DB
        ChiTietDatPhongDAO ctdpDAO = new ChiTietDatPhongDAO();
        boolean saved = ctdpDAO.update(tp);
        
        if (!saved) {
            JOptionPane.showMessageDialog(mainFrame,
                "⚠️ Cảnh báo: Đổi phòng thành công nhưng lưu DB không hoàn toàn!",
                "Cảnh báo", JOptionPane.WARNING_MESSAGE);
        }
        
        System.out.println("✅ Đổi phòng: " + oldRoom.getSoPhong() + " → " + newRoom.getSoPhong() + " | DB Saved: " + saved);
        
        // ...
        refresh();
    } catch (Exception e) {
        // ...
    }
}
```

---

## 📊 BEFORE vs AFTER

### BEFORE ❌
```
1. Click "Đổi phòng" → Dialog hiển thị 1-2 phòng
2. Chọn phòng đổi → Thành công
3. Xem chi tiết phòng mới → ❌ "Không có thông tin"
4. Nhấn "Thanh toán" → ❌ "Không tìm thấy"
```

### AFTER ✅
```
1. Click "Đổi phòng" → Dialog hiển thị TẤT CẢ phòng cùng loại
2. Chọn phòng đổi → Cập nhật DB ✅
3. Xem chi tiết phòng mới → ✅ Hiển thị đầy đủ thông tin
4. Nhấn "Thanh toán" → ✅ Mở dialog hóa đơn đúng
```

---

## 🧪 TEST VERIFY

### Test Case 1: Filter Phòng ✓
1. Có ít nhất 3 phòng loại "Deluxe" + AVAILABLE
2. Click phải phòng OCCUPIED loại "Deluxe"
3. Chọn "🔄 Đổi phòng"
4. **Kết quả**: Dialog hiển thị TẤT CẢ 3 phòng (không phải 1-2)
5. Console: `📊 Tìm được 3 phòng cùng loại`

### Test Case 2: Lưu DB ✓
1. Chọn phòng từ dialog
2. Click "Đổi phòng"
3. **Kết quả**: Message "Đã đổi phòng thành công"
4. Console: `✅ Đổi phòng: P101 → P102 | DB Saved: true`
5. DB: `SELECT * FROM ChiTietDatPhong WHERE maChiTiet='...'` → `maPhong` = P102

### Test Case 3: Xem Chi Tiết Sau Đổi ✓
1. Sau đổi phòng thành công
2. Click phải phòng mới
3. Chọn "👁️ Xem chi tiết"
4. **Kết quả**: ✅ Dialog hiển thị đầy đủ thông tin
   - Phòng mới (P102)
   - Tên khách (Nguyễn Văn A)
   - Ngày nhận/trả
   - Số đêm

### Test Case 4: Thanh Toán Sau Đổi ✓
1. Sau đổi phòng thành công
2. Click phải phòng mới
3. Chọn "💳 Thanh toán"
4. **Kết quả**: ✅ Dialog HoaDon mở
   - Mã hóa đơn
   - Phòng mới (P102)
   - Tên khách
   - Chi tiết tiền phòng
   - Danh sách dịch vụ

---

## 📝 THAY ĐỔI FILE

### File 1: ChiTietDatPhongDAO.java
**Thêm**: Method `update(ChiTietDatPhong ct)`
```
Location: src/dao/ChiTietDatPhongDAO.java (sau method insert)
```

### File 2: ThuePhongPanel.java
**Sửa**: Method `showSwitchRoomDialog(Phong currentPhong)`
- Filter phòng: Thêm null checks
- Thêm debug log

**Sửa**: Method `doSwitchRoom(ChiTietDatPhong tp, Phong oldRoom, Phong newRoom)`
- Thêm code lưu DB: `ctdpDAO.update(tp)`
- Thêm alert nếu update thất bại
- Thêm log debug

---

## 🔍 DEBUGGING

Nếu còn lỗi, kiểm tra:

### Console Logs
```
🔍 Filter phòng có loại: Deluxe
📊 Tìm được N phòng cùng loại
✅ Đổi phòng: P101 → P102 | DB Saved: true/false
```

### Database Check
```sql
-- Kiểm tra ChiTietDatPhong sau đổi phòng
SELECT maChiTiet, maDatPhong, maPhong, ngayNhanThucTe, ngayTraThucTe 
FROM ChiTietDatPhong 
WHERE ngayTraThucTe IS NULL
ORDER BY ngayNhanThucTe DESC;
```

Cột `maPhong` phải là **phòng mới** (P102, không phải P101)

### ThuePhongService Check
```java
ChiTietDatPhong tp = thuePhongService.getActiveByPhong("P102");
// Phải NOT NULL
```

---

## ✅ HOÀN THIỆN

- ✓ Thêm method `update()` vào ChiTietDatPhongDAO
- ✓ Sửa filter phòng (thêm null checks)
- ✓ Lưu ChiTietDatPhong vào DB sau đổi
- ✓ Thêm debug logs
- ✓ Thêm error handling
- ✓ Compilation thành công

**Status**: 🟢 READY TO TEST

---

**Ngày sửa**: 28/03/2026
