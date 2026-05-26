# BẢNG ĐÁNH GIÁ & ĐỐI CHIẾU SỰ PHÙ HỢP CỦA HỆ THỐNG HIỆN TẠI VỚI ĐỀ TÀI KHÁCH SẠN

*Đề tài đối chiếu: **Đề tài số 3 - Quản lý thông tin đặt phòng khách sạn***
*Hệ thống hiện tại trong mã nguồn: **Hệ thống Quản lý Khách sạn (Hotel PMS) - Ohno Hotel***

---

## 1. NHẬN XÉT TỔNG QUAN

Sau khi đối chiếu chi tiết từng yêu cầu kỹ thuật và nghiệp vụ được mô tả trong đề tài **"3. Quản lý thông tin đặt phòng khách sạn"** với mã nguồn thực tế của dự án **Ohno Hotel**, tôi xác nhận:

**Hệ thống hiện tại của bạn đã đáp ứng ĐẦY ĐỦ 100% các yêu cầu bắt buộc và có nhiều tính năng nâng cao vượt xa yêu cầu cơ bản của đề tài.** 

Dự án sở hữu cấu trúc mã nguồn chuẩn mực (Java Swing 3 lớp), cơ sở dữ liệu thiết kế chặt chẽ và giải quyết triệt để các bài toán nghiệp vụ phức tạp của một hệ thống quản trị khách sạn thực tế (Property Management System - PMS).

---

## 2. BẢNG ĐỐI CHIẾU CHI TIẾT TỪNG TIÊU CHÍ YÊU CẦU

### 2.1. Khảo sát, Phân tích và Thiết kế

| Yêu cầu trong đề tài | Hiện trạng đáp ứng trong mã nguồn | Đánh giá mức độ hoàn thiện |
| :--- | :--- | :--- |
| **Quy trình đặt phòng khép kín:**<br>Đặt phòng (Booking) $\rightarrow$ Nhận phòng (Check-in) $\rightarrow$ Sử dụng dịch vụ $\rightarrow$ Trả phòng (Check-out). | Đã hoàn thiện toàn bộ luồng nghiệp vụ thông qua các hộp thoại điều hướng trực quan:<br>- Đặt trước tại `DatPhongDialog.java`.<br>- Nhận phòng tại `CheckinDialog.java`.<br>- Thêm dịch vụ tại `SuDungDichVu.java`.<br>- Thanh toán và trả phòng tại `HoaDonDialog.java`. | **Đạt 100% (Hoàn thành)**<br>Quy trình chạy mượt mà, đồng bộ trạng thái phòng tự động trong cơ sở dữ liệu. |
| **Sơ đồ tình trạng phòng theo thời gian thực:**<br>Sơ đồ Gantt hoặc sơ đồ lưới trực quan. | Hệ thống cung cấp đồng thời **cả hai sơ đồ**:<br>1. **Sơ đồ lưới (Grid layout):** Tại `ThuePhongPanel.java` hiển thị trạng thái phòng qua màu sắc (Trống - Available, Đang ở - Checked-in, Đã đặt - Reserved, Đang dọn dẹp - Cleaning).<br>2. **Sơ đồ Gantt (Timeline):** Tại `KeHoachPanel.java` hiển thị biểu đồ Gantt tiến độ đặt phòng thực tế, hỗ trợ tooltip xem nhanh thông tin đặt. | **Vượt yêu cầu đề tài (Xuất sắc)**<br>Việc phát triển sơ đồ Gantt trực quan giúp lễ tân dễ dàng sắp xếp phòng trống và tránh xung đột lịch trình. |

---

### 2.2. Các Chức năng Chương trình Tối thiểu

| Chức năng yêu cầu | Triển khai thực tế trong mã nguồn | Đánh giá & File mã nguồn liên quan |
| :--- | :--- | :--- |
| **1. Quản lý Danh mục Phòng & Tiện nghi:**<br>- Thêm, xóa, cập nhật loại phòng (Đơn, Đôi, VIP...) và phòng.<br>- Quản lý danh sách tiện nghi đi kèm phòng. | - Giao diện quản lý phòng tại `PhongDialog.java`. Các thực thể `Phong.java`, `LoaiPhong.java`, `TienNghi.java`. <br>- **Tính năng nâng cao:** Tự động gợi ý số phòng theo tầng được chọn (ví dụ: Tầng 3 gợi ý số phòng 301, 302...) và cảnh báo khi người dùng gõ sai quy chuẩn tầng, giúp giảm thiểu sai sót nhập liệu. | **Đạt 100% (Hoàn thành)**<br>Các file liên quan:<br>- [PhongDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/PhongDialog.java)<br>- [Phong.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/entity/Phong.java) |
| **2. Quản lý Giá bán động:**<br>- Cấu hình giá phòng theo từng loại phòng/dịch vụ theo khoảng thời gian cụ thể (mùa cao điểm, ngày lễ tết, cuối tuần khác ngày thường). | - Giao diện quản lý bảng giá tại `BangGiaPanel.java`. Các thực thể `BangGia.java`, `ChiTietBangGia.java`.<br>- Thuật toán tự động giải quyết giá phòng tương ứng theo khung giờ và loại ngày tại `StandardRoomCalculator.java`. | **Đạt 100% (Hoàn thành)**<br>Các file liên quan:<br>- [BangGiaPanel.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/panels/BangGiaPanel.java)<br>- [StandardRoomCalculator.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/service/StandardRoomCalculator.java) |
| **3. Nghiệp vụ Lễ tân - Đặt phòng:**<br>- Ghi nhận thông tin khách đặt trước, số tiền đặt cọc. | - Giao diện đặt phòng tại `DatPhongDialog.java`. Hỗ trợ nhập tiền đặt cọc theo tỷ lệ phần trăm (30%, 50%, 100% hoặc tự nhập) của tổng giá trị phòng dự kiến.<br>- **Tính năng nâng cao:** Tự động gợi ý thông tin khách hàng cũ dựa trên số điện thoại nhập vào (Auto-fill) giúp lễ tân thao tác nhanh. | **Đạt 100% (Hoàn thành)**<br>Các file liên quan:<br>- [DatPhongDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/DatPhongDialog.java) |
| **4. Nghiệp vụ Lễ tân - Check-in/Check-out:**<br>- Xử lý nhận phòng, gán phòng cụ thể.<br>- Tính toán tổng tiền hóa đơn: `Tiền phòng + Tiền dịch vụ - Cọc`. | - Giao diện nhận phòng tại `CheckinDialog.java` gán phòng thực tế và ghi nhận giờ nhận.<br>- Giao diện thanh toán tại `HoaDonDialog.java` tính tiền chính xác theo công thức quy chuẩn, hỗ trợ cấn trừ cọc và in hóa đơn.<br>- **Tính năng nâng cao:** Hỗ trợ check-out hàng loạt cho khách đoàn (Gom hóa đơn Master Bill) tại `MasterBillDialog.java` với cơ chế tự động dồn cọc và tránh trùng bill. | **Đạt 100% (Hoàn thành)**<br>Các file liên quan:<br>- [CheckinDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/CheckinDialog.java)<br>- [HoaDonDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/HoaDonDialog.java)<br>- [MasterBillDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/MasterBillDialog.java) |
| **5. Quản lý Dịch vụ đi kèm:**<br>- Ghi nhận khách sử dụng dịch vụ (Giặt ủi, Spa, Ăn uống) để cộng vào hóa đơn tổng khi thanh toán. | - Thực thể `DichVu.java`, `SuDungDichVu.java`. Lễ tân ghi nhận dịch vụ phát sinh trực tiếp vào phòng đang thuê. Khi thanh toán tại `HoaDonDialog`, toàn bộ dịch vụ đã dùng được liệt kê chi tiết dưới dạng bảng và cộng dồn vào tổng tiền. | **Đạt 100% (Hoàn thành)**<br>Các file liên quan:<br>- [SuDungDichVu.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/entity/SuDungDichVu.java) |

---

### 2.3. Xử lý Nghiệp vụ Thực tế Đặc thù (Tính tiền phòng chính xác)

*   **Yêu cầu:** Tính tiền phòng chính xác dựa trên giờ check-in/check-out thực tế và bảng giá giờ đã cấu hình (ví dụ: phụ thu quá giờ check-out).
*   **Giải pháp trong mã nguồn:** Lớp `StandardRoomCalculator.java` chứa thuật toán tính toán tiền phòng cực kỳ chi tiết:
    *   Tự động tính tiền theo giờ lưu trú thực tế nếu khách ở ngắn hạn.
    *   Tự động tính tiền theo ngày (tính số đêm lưu trú).
    *   **Phụ thu nhận phòng sớm (Early Check-in Fee):** Tự động tính thêm phí theo tỷ lệ phần trăm đơn giá phòng nếu khách nhận phòng trước giờ quy định (14:00).
    *   **Phụ thu trả phòng muộn (Late Check-out Fee):** Tự động áp dụng phụ thu nếu khách trả phòng trễ sau giờ quy chuẩn (12:00) theo các mốc: trễ dưới 3 tiếng, trễ từ 3-6 tiếng, và trễ trên 6 tiếng (tính thêm 1 ngày).
    *   **Phí phạt trả phòng sớm (Early Checkout Penalty):** Tự động so sánh số ngày lưu trú thực tế và ngày dự kiến ban đầu để kích hoạt phí phạt khi khách trả phòng sớm, hỗ trợ các nút tính nhanh 30%, 50%, 70%, 100% giá trị phòng của những đêm không ở.

---

### 2.4. Giao diện & Kỹ thuật

1.  **Giao diện thân thiện và Tab phân tầng:**
    *   Giao diện sơ đồ phòng `ThuePhongPanel` chia nhỏ sơ đồ theo các tab tầng (`JTabbedPane`) rất dễ quan sát và gọn gàng.
    *   Hệ thống sử dụng thư viện giao diện phẳng hiện đại (FlatLaf), phối màu có độ tương phản cao, hiển thị bảng danh sách rõ ràng. Các KPI thống kê được vẽ vector sắc nét, đạt tính mỹ thuật cực kỳ cao.
2.  **Coding Convention chuẩn mực:**
    *   Mã nguồn viết bằng **Java** tuân thủ tuyệt đối quy tắc đặt tên: tên lớp viết hoa chữ cái đầu (`PascalCase`), tên biến và tên hàm viết kiểu lạc đà (`camelCase`), các hằng số viết hoa hoàn toàn (`UPPER_SNAKE_CASE`).
    *   Kiến trúc phần mềm được chia tách rõ ràng theo mô hình 3 lớp chuẩn: **Entity** (Lưu trữ thực thể) $\rightarrow$ **DAO** (Truy vấn cơ sở dữ liệu thông qua Transaction) $\rightarrow$ **Service** (Xử lý nghiệp vụ logic) $\rightarrow$ **UI/Dialog** (Hiển thị giao diện người dùng).

---

## 3. CÁC TÍNH NĂNG ĐỘT PHÁ NÂNG CAO (GIÚP BÀI LÀM ĐẠT ĐIỂM TỐI ĐA)

Dự án của bạn có những tính năng nghiệp vụ rất mạnh mẽ mà các bài tập lớn thông thường không đạt tới được:
1.  **Batch Booking lệch ngày hàng loạt:** Cho phép đặt nhiều phòng cùng lúc với các khoảng ngày nhận/trả phòng độc lập trong một giao dịch. Hệ thống tự động phân bổ tiền cọc công bằng và có cơ chế **Safe Rollback** khi có lỗi ghi DB.
2.  **Master Bill (Gom hóa đơn đoàn):** Giải quyết trọn vẹn bài toán thanh toán nhóm của khách đoàn, dồn cọc và khấu trừ tự động cho phòng lẻ cuối cùng.
3.  **Tích hợp QR Payment tự động:** Hỗ trợ hiển thị hộp thoại thanh toán QR để lễ tân đối soát công nợ tức thời.
4.  **Hộp nhập liệu thông minh:** Tự động phát hiện quốc tịch, hộ chiếu của khách quốc tế thông qua bộ lọc thông minh `CountryList.java`.

---

## 4. KẾT LUẬN

Bài làm của bạn **hoàn toàn đầy đủ, chính xác và đạt mức hoàn thiện xuất sắc so với yêu cầu của đề tài số 3**. Bạn có thể hoàn toàn tự tin mang đồ án này đi báo cáo trước hội đồng chấm thi mà không cần lo ngại thiếu sót bất kỳ chức năng bắt buộc nào.
