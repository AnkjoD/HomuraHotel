# BẢNG ĐÁNH GIÁ & ĐỐI CHIẾU SỰ PHÙ HỢP CỦA HỆ THỐNG HIỆN TẠI VỚI YÊU CẦU ĐỀ TÀI

*Đề tài đối chiếu: **Đề tài số 4 - Quản lý đặt bàn trong nhà hàng***
*Hệ thống hiện tại trong mã nguồn: **Hệ thống Quản lý Khách sạn (Hotel PMS) - Ohno Hotel***

---

## 1. NHẬN XÉT TỔNG QUAN

Có một **sự khác biệt cốt lõi** giữa mã nguồn hiện tại trong thư mục dự án và yêu cầu trong ảnh bạn cung cấp:
*   **Mã nguồn hiện tại:** Là một hệ thống **Quản lý Khách sạn (Hotel Property Management System)** hoàn chỉnh với các thực thể: Phòng (`Phong`), Đặt phòng (`DatPhong`), Khách hàng lưu trú (`KhachHang`), Dịch vụ đi kèm (`DichVu`), Giao ca lễ tân (`GiaoCa`), Hóa đơn phòng (`HoaDon`), và tính năng Gom Bill đoàn (`MasterBill`).
*   **Yêu cầu trong ảnh:** Là đề tài **Quản lý đặt bàn trong nhà hàng (Restaurant Table Booking and Order Management System)** với các nghiệp vụ xoay quanh Bàn ăn (`BanAn`), Gọi món ăn/Thực đơn (`MonAn`/`ThucDon`), Báo bếp, Chuyển bàn, Gộp bàn.

Mặc dù hai hệ thống có sự tương đồng rất lớn về mặt **tư duy logic nghiệp vụ (khoảng 85% - 90%)**, nếu bạn nộp nguyên bản dự án Quản lý Khách sạn này cho giáo viên chấm đề tài Quản lý Nhà hàng, bài làm sẽ bị đánh giá là **sai đề tài** hoặc **không đúng yêu cầu**. 

Dưới đây là bảng phân tích chi tiết mức độ đáp ứng và các khoảng trống (gaps) bạn cần bổ sung hoặc đổi tên (refactor) để chuyển đổi từ hệ thống Khách sạn sang Nhà hàng.

---

## 2. BẢNG ĐỐI CHIẾU CHI TIẾT YÊU CẦU ĐỀ TÀI

### 2.1. Phần Khảo sát, Phân tích, Thiết kế

| Yêu cầu đề tài Nhà hàng | Trạng thái đáp ứng trong dự án Khách sạn hiện tại | Đánh giá & Hướng xử lý |
| :--- | :--- | :--- |
| **Quy trình phục vụ:** Khách đặt bàn trước $\rightarrow$ Check-in $\rightarrow$ Xếp bàn $\rightarrow$ Gọi món (Order) $\rightarrow$ Báo bếp $\rightarrow$ Thanh toán $\rightarrow$ Trả bàn. | Đã có quy trình tương đương: Khách đặt phòng trước $\rightarrow$ Check-in nhận phòng $\rightarrow$ Sử dụng dịch vụ $\rightarrow$ Thanh toán $\rightarrow$ Trả phòng & Dọn dẹp. | **Tương thích 90% về logic.**<br>- *Khoảng trống:* Thiếu bước **Báo bếp** (In phiếu order xuống bếp).<br>- *Hướng xử lý:* Cần đổi thuật ngữ hiển thị trên giao diện từ "Phòng" thành "Bàn", "Khách lưu trú" thành "Khách ăn". |
| **Sơ đồ bàn ăn & Trạng thái:** Phân tích sơ đồ bàn ăn và trạng thái bàn trong giờ hoạt động. | Đã có sơ đồ phòng trực quan chia theo tầng, đổi màu sắc theo trạng thái: Trống (Available), Đang có khách (Checked in), Đã đặt trước (Reserved), Đang dọn dẹp (Cleaning). | **Tương thích 95%.**<br>- Giao diện sơ đồ phòng của bạn rất trực quan và đáp ứng hoàn hảo cấu trúc này. Chỉ cần đổi tên trạng thái "Đang dọn dẹp" (Cleaning) của phòng thành trạng thái "Đang dọn bàn" hoặc chuyển thẳng về "Trống" sau khi thanh toán. |

---

### 2.2. Phần Thực hiện Chương trình (Chức năng cốt lõi)

| Chức năng yêu cầu (Nhà hàng) | Chức năng hiện tại (Khách sạn) | Mức độ đáp ứng & Khoảng trống cần hoàn thiện |
| :--- | :--- | :--- |
| **1. Quản lý Thực đơn (Menu):**<br>- Quản lý danh mục món ăn phân loại: Khai vị, Món chính, Tráng miệng, Đồ uống.<br>- Cập nhật giá bán và hình ảnh. | **Quản lý Dịch vụ (`DichVu`):**<br>- Đã có quản lý danh mục dịch vụ (giặt ủi, nước uống, mì gói...).<br>- Có cập nhật đơn giá, số lượng tồn kho. | **Đáp ứng 60%.**<br>- *Khoảng trống:* Chưa phân loại danh mục thành 4 nhóm (Khai vị, Món chính, Tráng miệng, Đồ uống) và chưa có trường lưu trữ hình ảnh món ăn.<br>- *Hướng xử lý:* Thêm thuộc tính `loaiMonAn` (Enum: KHAI_VI, MON_CHINH, TRANG_MIENG, DO_UONG) và `hinhAnh` vào thực thể `DichVu` (hoặc đổi tên thực thể thành `MonAn`). |
| **2. Quản lý Sơ đồ bàn ăn:**<br>- Hiển thị trực quan danh sách bàn theo khu vực: Tầng 1, Tầng 2, Phòng VIP.<br>- Màu sắc thay đổi theo trạng thái: Bàn trống, Đang có khách, Đã đặt trước. | **Quản lý sơ đồ phòng (`ThuePhongPanel`):**<br>- Hiển thị phòng chia theo các Tab Tầng (Tầng 1 $\rightarrow$ Tầng 10).<br>- Màu sắc trực quan theo trạng thái. | **Đáp ứng 95%.**<br>- Cấu trúc tab chia tầng của bạn hoàn toàn khớp.<br>- *Hướng xử lý:* Đổi tên nhãn trên các Tab từ "Tầng 1", "Tầng 2"... thành "Tầng 1", "Tầng 2", "Phòng VIP" (VIP Room của khách sạn đổi tên thành Phòng VIP ăn uống). |
| **3. Quản lý giá bán:**<br>- Định nghĩa giá bán cho từng loại thức ăn/dịch vụ theo từng khoảng thời gian. | **Quản lý bảng giá phòng (`BangGiaPanel`):**<br>- Đã cấu hình giá phòng động theo giờ, ngày thường, ngày lễ. | **Đáp ứng 90%.**<br>- Cơ chế quản lý bảng giá động của bạn rất mạnh mẽ.<br>- *Hướng xử lý:* Áp dụng logic bảng giá này cho danh mục món ăn/dịch vụ nếu nhà hàng có chính sách tăng giá giờ cao điểm hoặc ngày lễ. |
| **4. Nghiệp vụ Gọi món (Order):**<br>- Chọn bàn $\rightarrow$ Chọn món từ thực đơn $\rightarrow$ Nhập số lượng và **ghi chú cho bếp** (Ví dụ: Không cay, ít hành). | **Nghiệp vụ gọi dịch vụ phòng (`SuDungDichVu`):**<br>- Cho phép chọn phòng $\rightarrow$ Chọn dịch vụ $\rightarrow$ Nhập số lượng sử dụng. | **Đáp ứng 70%.**<br>- *Khoảng trống:* Hệ thống khách sạn hiện tại hoàn toàn **không có trường "Ghi chú cho bếp"** (ví dụ: không cay, ít hành, chín tái...) khi gọi dịch vụ.<br>- *Hướng xử lý:* Bổ sung thuộc tính `ghiChu` vào thực thể `SuDungDichVu` và thiết kế thêm một ô nhập liệu `txtGhiChu` trên giao diện gọi dịch vụ. |
| **5. Nghiệp vụ Thanh toán:**<br>- Tự động tính tổng tiền bàn ăn.<br>- Hỗ trợ tính năng phụ thu (VAT, phí phục vụ) hoặc giảm giá trên tổng hóa đơn. | **Nghiệp vụ thanh toán hóa đơn (`HoaDonDialog`):**<br>- Tự động tính tổng tiền phòng + dịch vụ.<br>- Hỗ trợ cấn trừ cọc, giảm giá, phụ thu sớm/muộn, phí phạt trả sớm và thuế VAT. | **Đáp ứng 95%.**<br>- Hệ thống tính toán hóa đơn hiện tại của bạn cực kỳ chi tiết.<br>- *Hướng xử lý:* Ẩn bớt các mục đặc thù của khách sạn như "Tiền đặt cọc", "Phí trả phòng sớm", "Phụ thu check-in sớm / check-out trễ". Thay vào đó là giữ lại VAT, Giảm giá và bổ sung Phí phục vụ (Service Charge nếu có). |

---

### 2.3. Xử lý Nghiệp vụ Thực tế đặc thù

| Nghiệp vụ Nhà hàng yêu cầu | Giải pháp tương đương trong code Khách sạn hiện tại | Đánh giá & Hướng xử lý |
| :--- | :--- | :--- |
| **Chuyển bàn** (Khách muốn đổi sang bàn khác) | **Chuyển phòng (`RoomTransferDialog`):** Đã có tính năng chuyển phòng thời gian thực, tự động cập nhật trạng thái phòng cũ về trống và chuyển toàn bộ thông tin/dịch vụ sang phòng mới. | **Đáp ứng 100% về mặt logic.**<br>- Chỉ cần đổi tên giao diện từ "Chuyển phòng" thành "Chuyển bàn". |
| **Gộp bàn** (Nhóm khách đông ngồi 2 bàn ghép lại) | **Gom Bill đoàn (`MasterBillDialog` / Gom Bill):** Cho phép gom nhiều hóa đơn phòng đang nợ thành một hóa đơn tổng duy nhất để trưởng đoàn thanh toán. | **Đáp ứng 80% về mặt logic thanh toán.**<br>- *Sự khác biệt:* Trong nhà hàng, "Gộp bàn" nghĩa là gộp chung thực đơn của bàn A và bàn B vào làm một bàn duy nhất trong suốt quá trình ngồi ăn (chứ không chỉ gom bill lúc cuối).<br>- *Hướng xử lý:* Bạn có thể sử dụng giao diện Gom Bill hiện tại để giải thích đây là tính năng "Gom hóa đơn các bàn đi cùng nhóm để thanh toán chung". |
| **Tự động quay về "Trống"** (Khi thanh toán xong, trạng thái bàn tự động quay về Trống) | **Cập nhật trạng thái phòng:** Khi thanh toán hóa đơn, hệ thống khách sạn cập nhật trạng thái phòng về `CLEANING` (Đang dọn dẹp) hoặc `AVAILABLE` (Có sẵn). | **Đáp ứng 100%.**<br>- *Hướng xử lý:* Cập nhật logic để sau khi thanh toán xong, trạng thái bàn ăn lập tức chuyển thẳng về `AVAILABLE` (Trống). |

---

### 2.4. Giao diện & Kỹ thuật

*   **Giao diện sơ đồ trực quan (Sử dụng Tab):** Dự án khách sạn của bạn đã sử dụng cấu trúc `JTabbedPane` để phân chia phòng theo các tầng rất trực quan, dễ quan sát, hoàn toàn thỏa mãn yêu cầu đề bài.
*   **Coding Convention:** Mã nguồn Java hiện tại của bạn viết rất sạch sẽ, biến và hàm được đặt tên lạc đà (`camelCase`), các lớp được phân chia theo mô hình 3 lớp chuẩn (UI - Service/DAO - Entity), hoàn toàn đáp ứng tốt tiêu chuẩn kỹ thuật.

---

## 3. KHUYẾN NGHỊ VÀ CÁC BƯỚC CẦN THỰC HIỆN TIẾP THEO

Nếu mục tiêu của bạn là hoàn thành đúng **Đề tài số 4 - Quản lý đặt bàn trong nhà hàng**, bạn không nên nộp nguyên bản dự án này mà hãy tiến hành **chuyển đổi (refactor)** theo các bước sau:

1.  **Đổi tên các lớp Entity (Thực thể dữ liệu):**
    *   Đổi `Phong.java` thành `BanAn.java` (Số phòng $\rightarrow$ Số bàn, Loại phòng $\rightarrow$ Loại bàn/Khu vực).
    *   Đổi `DichVu.java` thành `MonAn.java` hoặc `ThucDon.java`.
    *   Đổi `SuDungDichVu.java` thành `ChiTietOrder.java` hoặc `GoiMon.java`.
    *   Đổi `DatPhong.java` thành `DatBan.java`.
2.  **Cập nhật các bảng trong Cơ sở dữ liệu (SQL Server):**
    *   Đổi tên các bảng tương ứng để đồng bộ với mã nguồn Java.
3.  **Refactor giao diện người dùng (UI):**
    *   Tìm kiếm và thay thế toàn bộ từ khóa hiển thị trên giao diện (Labels, Buttons, Dialog Titles):
        *   "Phòng" $\rightarrow$ "Bàn"
        *   "Đặt phòng" $\rightarrow$ "Đặt bàn"
        *   "Nhận phòng / Check-in" $\rightarrow$ "Mở bàn / Nhận bàn"
        *   "Trả phòng / Checkout" $\rightarrow$ "Thanh toán & Trả bàn"
        *   "Dịch vụ" $\rightarrow$ "Món ăn / Thực đơn"
4.  **Bổ sung thuộc tính Ghi chú cho bếp:**
    *   Thêm cột `ghiChu` trong bảng chi tiết gọi món để lễ tân nhập thông tin (ví dụ: "ít cay", "không hành") truyền xuống bếp.

*Nếu giáo viên của bạn cho phép tự chọn đề tài và bạn được phép nộp đề tài **Quản lý Khách sạn**, thì hệ thống Ohno Hotel hiện tại của bạn đã **cực kỳ đầy đủ, hoàn thiện, chuyên nghiệp** và vượt xa yêu cầu thông thường của một bài tập lớn.*
