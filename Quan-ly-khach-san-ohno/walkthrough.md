# Tổng Kết: Hoàn Thiện Tính Tiền Khách Lẻ / Khách Đoàn & Gom Bill

Bản cập nhật này đã hoàn thiện luồng thanh toán chuyên nghiệp cho khách sạn, đặc biệt giải quyết bài toán phức tạp khi **khách đoàn trả nhiều phòng cùng lúc** thông qua cơ chế **Master Bill (Gom Bill)**.

## Thay Đổi Chính

### 1. Giao Diện & Trải Nghiệm Ghi Nợ / Thanh Toán Gom
- **Phân loại Luồng Checkout Đoàn:** Xóa bỏ tình trạng "phòng cuối cùng của đoàn cũng phải thanh toán riêng". Giờ đây, khi phòng cuối cùng của một đoàn làm thủ tục checkout, hệ thống sẽ:
  - Hiển thị pop-up thông minh để báo cáo số phòng đã trả nhưng đang "treo nợ".
  - Cho phép chọn "Gom Bill" tất cả các phòng treo nợ cùng với phòng hiện tại vào 1 hóa đơn tổng duy nhất.
- **Menu Tiện Ích (`ThuePhongPanel`):** Thêm tính năng chuột phải `"📋 Gom Bill Đoàn"` vào mục menu của các phòng đang có khách đoàn ở, hỗ trợ nhân viên chủ động gom bill bất kỳ lúc nào nếu khách muốn thanh toán sớm.
- **Bảng Quản Lý Hóa Đơn (`HoaDonPanel`):** 
  - Thêm cột phân loại ("Khách lẻ" / "Đoàn") sử dụng màu sắc thân thiện (xanh dương cho lẻ, tím cho đoàn) để nhân viên dễ dàng phân biệt.
  - Tên nhóm đại diện (Ví dụ: `Công ty ABC (Nguyễn Văn A)`) giờ được hiển thị đầy đủ và rõ ràng.

### 2. Dialog Master Bill Mới
- **`MasterBillDialog`:** Một giao diện hóa đơn thiết kế đặc biệt (chủ đề màu tím) dùng riêng cho đoàn.
  - **Bảng tóm tắt toàn diện:** Liệt kê tự động tiền phòng của *tất cả* phòng trong đoàn, phụ phí check-in/checkout, và danh sách dịch vụ chi tiết của từng phòng.
  - **Cộng dồn tự động:** Tính toán tổng tiền phòng, tổng phụ phí, và tổng tiền dịch vụ theo thời gian thực thay vì thanh toán mù.
  - **Tính năng In:** Cho phép "In hóa đơn đoàn" trực tiếp từ giao diện với form in bill được nâng cấp đẹp mắt.

### 3. Nâng Cấp Logic Giao Dịch
- **`checkOutMasterBill()` trong `ThuePhongService`:**
  - Logic gom tất cả giao dịch con (`ChiTietDatPhong`, `SuDungDichVu`) liên quan tới một `maDatPhong` và tạo ra duy nhất 1 `HoaDon`.
  - Hỗ trợ transaction DB chặt chẽ (`beginTransaction()`, `commitTransaction()`) đảm bảo không bị thất thoát tiền hoặc lỗi status khi có sự cố.
  - Tự động thay đổi trạng thái của tất cả các phòng đã thanh toán thành `CLEANING`.
  - Vá lỗi tương thích cho `LoaiKhach` để nhận diện "DOAN".

## Các Bước Kiểm Tra Bạn Có Thể Thử:

1. **Khách Lẻ:** Đặt phòng cho một khách bình thường $\rightarrow$ Mở menu $\rightarrow$ Chọn *Thanh toán & Trả phòng*. Luồng hiển thị bình thường.
2. **Khách Đoàn (Ghi nợ):** Đặt 1 đơn có 3 phòng (đánh dấu Loại khách: *Khách đoàn*). 
   - Click chuột phải 1 phòng, chọn *Thanh toán*.
   - Chọn "Trả phòng & Treo nợ đoàn". Phòng sẽ vào trạng thái `CLEANING` nhưng tiền vẫn được lưu.
3. **Khách Đoàn (Gom Bill):** Cuối cùng, checkout phòng còn lại của đoàn đó.
   - Hệ thống sẽ hiện popup đề xuất "Gom Bill".
   - Mở cửa sổ **Hóa Đơn Đoàn (Master Bill)**. Kiểm tra tính đúng đắn của phép tính. 
   - Xác nhận thanh toán $\rightarrow$ Xem lịch sử trên mục *Hóa đơn* thấy mã hóa đơn có chữ màu tím "Đoàn".
