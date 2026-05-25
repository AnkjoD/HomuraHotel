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

## Sửa lỗi hiển thị & Nâng cấp (25/05/2026)

- **Lỗi độ tương phản màu chữ khi chọn hóa đơn (`HoaDonPanel`):**
  - **Mô tả lỗi:** Khi nhân viên click chọn một dòng hóa đơn trong bảng lịch sử, nền dòng chuyển sang màu xanh nhạt (`PRIMARY_LIGHT`), nhưng các cột như *Số ĐT, Tiền phòng, Cộng thêm, Tổng tiền, Hình thức TT* lại có màu chữ chuyển thành trắng/xám sáng, gây khó đọc và mỏi mắt.
  - **Giải pháp:** Thay thế toàn bộ render mặc định của bảng bằng lớp vẽ ô đồng bộ `InvoiceCellRenderer`. Khi dòng được chọn, toàn bộ thông tin văn bản sẽ tự động sử dụng màu chữ xanh đậm (`UIConstants.TEXT_PRIMARY`) có độ tương phản cao trên nền xanh nhạt, giúp giao diện trở nên cực kỳ tinh tế, dễ quan sát và chuyên nghiệp.

- **Chuẩn hóa Quốc tịch (Quốc gia) cho Khách hàng:**
  - **Yêu cầu thực tế & Sửa lỗi:** Trong các hệ thống Quản lý Khách sạn (PMS) thực tế, thông tin quốc tịch của khách nước ngoài bắt buộc phải lưu chính xác tên quốc gia cụ thể (ví dụ: Hàn Quốc, Mỹ, Nhật Bản...) để phục vụ việc khai báo lưu trú với cơ quan chức năng. Việc chỉ để chung chung là "Quốc tế" hoặc để nhập tay tự do rất dễ gây sai lệch dữ liệu và lỗi biên dịch (lỗi thiếu biến `txtQuocTich` trong `KhachHangDialog`).
  - **Giải pháp:**
    - Cập nhật [CountryList.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/util/CountryList.java): Sửa lỗi `UnsupportedOperationException` khi thêm phần tử vào danh sách quốc gia lọc, đồng thời viết thêm hàm tiện ích `getCountriesForInput()` trả về danh sách các nước thông dụng với "Việt Nam" được ưu tiên xếp ở đầu.
    - Sửa lỗi biên dịch trong [KhachHangDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/KhachHangDialog.java): Thay thế trường nhập chữ `txtQuocTich` chưa khai báo bằng hộp chọn `cboQuocTich` sử dụng danh sách quốc gia hoàn chỉnh. Khi chọn quốc gia khác "Việt Nam", panel thông tin Hộ chiếu/Visa bắt buộc sẽ tự động hiển thị.
    - Đồng bộ hóa hộp chọn quốc tịch trong các giao diện [CheckinDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/CheckinDialog.java), [DatPhongDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/DatPhongDialog.java), và [ChangeRepresentativeDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/ChangeRepresentativeDialog.java) để tất cả đều sử dụng chung danh sách quốc gia chuẩn này.

- **Sửa lỗi & Tối ưu hóa Bộ lọc Số khách khi tìm phòng trống (Nghiệp vụ thực tế):**
  - **Mô tả lỗi & Yêu cầu thực tế:** 
    * Trước đó, hệ thống gọi hàm tìm phòng trống với sức chứa mặc định cố định là `1`, khiến bộ lọc "Số khách" trên giao diện hoàn toàn không hoạt động.
    * Khi sửa lại lọc chính xác (`soNguoiToiDa >= soKhach`), hệ thống lại gặp bài toán thực tế: Các khách sạn luôn cho phép ở ghép thêm tối đa 2 người (có tính phí phụ thu giường phụ/người lớn). Nếu lọc cứng bằng sức chứa tiêu chuẩn, khi nhập số khách là `5` người, hệ thống sẽ ẩn đi phòng 4 người (Suite Family) và trả về danh sách trống, gây mất doanh thu. Đồng thời, nếu đi nhóm đông người (ví dụ: 7-8 người), lễ tân sẽ muốn nhìn thấy tất cả phòng trống để đặt nhiều phòng cùng lúc (Gom phòng).
  - **Giải pháp:** Tối ưu hóa hàm `doSearch()` trong [DatPhongPanel.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/panels/DatPhongPanel.java) với bộ lọc thông minh:
    * **Với nhóm khách $\le 6$ người:** Cho phép tìm các phòng có thể ở ghép (sức chứa tiêu chuẩn tối thiểu là `soKhach - 2`). Ví dụ: tìm cho 5 khách sẽ hiển thị phòng 4 người; tìm cho 4 khách sẽ hiển thị cả phòng 2 người (Deluxe Twin) và phòng 4 người.
    * **Với nhóm khách $> 6$ người:** Hệ thống tự động bỏ qua lọc để hiển thị toàn bộ phòng trống, giúp lễ tân dễ dàng lựa chọn và đặt ghép nhiều phòng cho khách đoàn.

- **Tự động gợi ý & Cảnh báo số phòng theo Tầng:**
  - **Mô tả lỗi & Yêu cầu thực tế:** Trong các hệ thống quản lý khách sạn thực tế, số phòng luôn được đặt theo một quy chuẩn nhất quán (ví dụ: Tầng 3 thì số phòng là 301, 302, 303...). Việc để người dùng nhập tay tự do từ đầu mà không có gợi ý/kiểm tra rất dễ dẫn đến sai lệch dữ liệu (nhập số phòng sai tầng hoặc nhập số phòng trùng lặp).
  - **Giải pháp:** Cập nhật [PhongDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/PhongDialog.java):
    * **Chuyển ô nhập Tầng thành ComboBox (Lựa chọn):** Thay thế ô nhập chữ tự do `txtTang` bằng hộp chọn `cboTang` hiển thị danh sách từ Tầng 1 đến Tầng 10. Điều này loại bỏ hoàn toàn khả năng người dùng nhập sai định dạng (như chữ cái, số âm, số quá lớn).
    * **Tự động gợi ý tức thời khi thay đổi lựa chọn:** Gán sự kiện `ActionListener` vào `cboTang`. Mỗi khi người dùng bấm chọn tầng mới, hệ thống lập tức tính toán và điền số phòng gợi ý tiếp theo mà không cần đợi người dùng chuyển tiêu điểm (Focus Lost) như trước.
    * **Cảnh báo sai quy chuẩn:** Khi lưu phòng mới, nếu người dùng nhập số phòng không bắt đầu bằng số tầng (ví dụ: Tầng 3 nhưng nhập phòng `105`), hệ thống sẽ hiển thị hộp thoại cảnh báo và yêu cầu xác nhận. Điều này giúp ngăn ngừa gõ nhầm thông tin nhưng vẫn giữ tính linh hoạt nếu khách sạn có phòng VIP hoặc tên phòng đặc biệt.

- **Đồng bộ hóa định dạng hiển thị Số phòng (getSoPhongDisplay()):**
  - **Vấn đề thực tế:** Số phòng trong cơ sở dữ liệu được lưu kèm tiền tố "P" (ví dụ: `P101`). Khi hiển thị trên UI, một số chỗ tự động ghép thêm chữ "P." thành "P.P101", gây mất mỹ quan và thiếu chuyên nghiệp.
  - **Giải pháp:**
    - Bổ sung `getSoPhongDisplay()` vào [ChiTietDatPhong.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/entity/ChiTietDatPhong.java) để trả về số phòng đã được bóc tách tiền tố (ví dụ: `101`).
    - Đồng bộ hóa toàn bộ các màn hình hiển thị, hộp thoại xác nhận, cảnh báo nghiệp vụ và lịch trình để sử dụng `getSoPhongDisplay()` một cách nhất quán:
      * [CheckinDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/CheckinDialog.java) (Thông tin nhận phòng, checklist, thông báo thành công).
      * [DatPhongDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/DatPhongDialog.java) (Tiêu đề và danh sách phòng đặt).
      * [ExtendStayDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/ExtendStayDialog.java) (Thông tin gia hạn lưu trú).
      * [HoaDonDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/HoaDonDialog.java) (Tiêu đề hóa đơn, cảnh báo người đại diện rời đi, xác nhận trả phòng).
      * [RoomTransferDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/RoomTransferDialog.java) (Thông tin phòng cũ, danh sách lựa chọn phòng mới).
      * [KeHoachPanel.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/panels/KeHoachPanel.java) (Hiển thị số phòng trên sơ đồ lịch trình/timeline).
      * [ThuePhongPanel.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/panels/ThuePhongPanel.java) (Thông báo trạng thái dọn dẹp, bảo trì, thông tin chi tiết phòng).

- **Tối ưu hóa hiển thị Icon trên Báo cáo & Thống kê:**
  - **Vấn đề thực tế:** Vòng tròn chứa icon của các thẻ KPI có kích thước quá nhỏ (`32x32`), vùng vẽ vector thực tế chật hẹp (`12x12` pixels) kết hợp với nét bút dày (`1.5f`) khiến các chi tiết bị nhòe và biến dạng nghiêm trọng (ví dụ: icon *Occupancy Rate* trông giống như một chuỗi xích bị lỗi).
  - **Giải pháp:**
    - Nâng kích thước `iconCircle` từ `32x32` lên `44x44` trong [ThongKePanel.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/panels/ThongKePanel.java) để tăng không gian hiển thị và độ thông thoáng.
    - Mở rộng vùng vẽ vector lên `20x20` pixels (tăng gần gấp 3 lần diện tích vẽ).
    - **Phong cách Linear SVG cao cấp:** 
      * Áp dụng `BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)` cho nét bút, giúp tất cả các đường vẽ vector mềm mại, bo tròn góc cực kỳ mượt mà, giống hệt các thư viện icon SVG hiện đại như Lucide / Feather.
      * Loại bỏ viền nét mỏng cứng nhắc xung quanh đĩa tròn (`iconCircle`), thay bằng phong cách đĩa pastel phẳng không viền với sắc màu bán trong suốt dịu mắt (`alpha = 35`).
    - **Typography Vector hoàn mỹ:**
      * Sử dụng các ký tự vector gốc từ bộ font Segoe UI Bold (ví dụ: chữ `đ` cho Tổng doanh thu, ký tự `%` cho Tỷ suất lợi nhuận ròng) để vẽ trực tiếp bằng Font-hinting hệ thống. Việc này đảm bảo tỷ lệ hoàn hảo tuyệt đối và độ sắc nét 100% không tì vết.
    - **Bộ nhận diện chủ đề khách sạn (Hotel-themed Icons):**
      * *Occupancy Rate*: Biểu tượng giường ngủ (Bed) bo góc mượt mà.
      * *Lượt check-in*: Biểu tượng chuông lễ tân khách sạn (Hotel Desk Bell) sang trọng thay cho vé giấy cũ.
      * *Lợi nhuận ròng*: Biểu tượng túi tiền/túi trữ vàng (Money Pouch) bo tròn mang tính biểu trưng cao.
      * *Phòng đang thuê*: Ngôi nhà lưu trú tinh gọn với cửa ra vào.
      * *ADR*: Nhãn giá treo (Price Tag) có lỗ luồn dây.
      * *ALOS*: Quyển lịch lò xo (Calendar) ghi nhận ngày lưu trú.
- **Đổi vị trí & Ưu tiên tiêu điểm trường SĐT và Họ tên (`DatPhongDialog`):**
  - **Vấn đề thực tế:** Khi nhập số điện thoại, hệ thống sẽ tự động tìm kiếm thông tin và điền sẵn (auto-fill) thông tin họ tên, CCCD, quốc tịch của khách hàng. Tuy nhiên, ô *Họ và tên* lại được đặt trước (bên trái) và tự động nhận tiêu điểm (Focus) đầu tiên khi mở hộp thoại, buộc lễ tân phải bấm chuyển sang ô *Số điện thoại* trước khi gõ. Điều này làm giảm trải nghiệm người dùng và tính mượt mà của luồng thao tác.
  - **Giải pháp:**
    - Cập nhật [DatPhongDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/DatPhongDialog.java) đổi vị trí hiển thị: đưa ô *Số điện thoại** sang bên trái (đầu tiên) và ô *Họ và tên** sang bên phải (thứ hai).
    - Cập nhật tiêu điểm ban đầu: tự động tập trung (Focus) vào ô nhập *Số điện thoại** ngay khi hộp thoại mở ra, giúp lễ tân có thể gõ ngay lập tức số điện thoại để hệ thống tự điền dữ liệu mà không cần click chuột.

- **Vá lỗi Nghiệp vụ Overlap & Xung đột nhận phòng / Gia hạn lưu trú:**
  - **Vấn đề thực tế:** 
    1. Khi gia hạn một phòng đang thuê, hệ thống bỏ qua kiểm tra các đơn đặt phòng ở trạng thái `PENDING` (chờ xác nhận), dẫn đến việc cho phép gia hạn đè lên khoảng thời gian đã được đặt trước bởi khách khác.
    2. Khi có sự xung đột này xảy ra, nếu cố tình tiến hành check-in cho đơn đặt mới, hệ thống vẫn cập nhật trạng thái đơn đặt thành `CHECKED_IN` trong cơ sở dữ liệu trước khi thực hiện nhận phòng thực tế, gây ra hiện tượng lệch pha dữ liệu (đơn đã nhận phòng nhưng thực tế phòng vẫn đang bị chiếm dụng bởi khách cũ, luồng check-in thực tế bị rollback nhưng DB đã lưu).
  - **Giải pháp xử lý triệt để theo chuẩn PMS khách sạn thực tế:**
    - **Nâng cấp kiểm tra trống phòng (`isRoomAvailable`):** Cập nhật hàm trong [PhongDAO.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/dao/PhongDAO.java) để đưa cả các đơn đặt phòng trạng thái `PENDING` vào danh sách kiểm tra xung đột lịch. Nhờ đó, việc gia hạn lưu trú sẽ bị chặn ngay từ đầu nếu đè lên lịch của bất kỳ đơn đặt phòng nào đang hoạt động.
    - **Chặn xung đột khi Xác nhận/Sửa đơn đặt:** Thêm logic kiểm tra tính sẵn sàng của phòng trong [DatPhongService.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/service/DatPhongService.java) tại các phương thức `suaDatPhong` và `updateTrangThai` khi đơn chuyển sang `CONFIRMED`/`CHECKED_IN`, đảm bảo không thể lách luật xác nhận đè phòng.
    - **Kiểm soát trạng thái chặt chẽ tại UI Check-in:** Bổ sung bước kiểm tra trạng thái phòng thực tế tức thời (Real-time vacancy status check) tại [CheckinDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/CheckinDialog.java) ngay trước khi thực hiện bất kỳ giao dịch ghi DB nào. Nếu phòng không ở trạng thái `AVAILABLE` (Có sẵn), giao dịch check-in và cập nhật trạng thái đơn đặt sẽ bị từ chối ngay lập tức, ngăn ngừa hoàn toàn tình trạng lệch pha dữ liệu.

- **Nâng cấp Đặt nhiều phòng khác ngày trong một lần thao tác (Batch Booking lệch ngày):**
  - **Lựa chọn thiết kế:** Hệ thống đã lựa chọn triển khai **Phương án B** (được người dùng phê duyệt trực tiếp). Phương án này tạo nhanh nhiều đơn đặt phòng riêng lẻ trong một lần xác nhận trên UI, vừa đáp ứng trọn vẹn nghiệp vụ lệch ngày thực tế, vừa an toàn tuyệt đối 100% cho cấu trúc CSDL và không làm xáo trộn các module thanh toán, sơ đồ Gantt.
  - **Giải pháp chi tiết:**
    - **Thiết kế UI trực quan tại [DatPhongDialog.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/dialogs/DatPhongDialog.java):** Khi lễ tân đặt nhiều phòng cùng lúc, hộp thoại hiển thị danh sách tất cả phòng đã chọn kèm theo các hộp chọn ngày nhận/trả phòng (`DateTimePicker`) độc lập cho từng phòng.
    - **Tích hợp Nhận diện Xung đột Thời gian thực (Real-time Instant Validation Feedback):** Để giải quyết triệt để rủi ro nhân viên lễ tân vô tình chọn lệch sang khoảng ngày đã bị trùng lịch đặt bởi khách khác, tôi đã phát triển bộ nhận diện xung đột thời gian thực:
      * Mỗi phòng trong danh sách có một nhãn trạng thái trực quan (`lblStatus`) đi kèm ngay dưới trường chọn ngày.
      * Mỗi khi lễ tân thay đổi ngày nhận hoặc ngày trả của một phòng, hệ thống lập tức thực hiện gọi ngầm hàm kiểm tra xung đột `isRoomAvailable`.
      * Nếu không có xung đột, nhãn lập tức hiển thị màu xanh lá cây dịu mát: `✔️ Phòng trống sẵn sàng`.
      * Nếu phát hiện ngày trả sớm hơn ngày nhận hoặc phòng đã bị đặt bởi người khác trong khoảng thời gian đó, nhãn lập tức chuyển sang màu đỏ cảnh báo nổi bật: `❌ Ngày trả phải sau ngày nhận` hoặc `❌ Bị trùng lịch đặt phòng!`.
      * Điều này giúp nhân viên lễ tân luôn kiểm soát hoàn toàn tính khả dụng của phòng tức thời mà không cần đợi đến lúc nhấn nút Lưu.
    - **Tính toán tiền phòng & Cọc động thông minh:** Khi người dùng thay đổi ngày nhận/trả của bất kỳ phòng nào, hệ thống lập tức tự động tính toán lại số đêm và giá phòng tại thời điểm đó, cập nhật tổng tiền phòng dự kiến theo thời gian thực. Ô chọn đặt cọc (30%, 50%, 100%) cũng tự động cập nhật số tiền cọc tương ứng theo tổng tiền mới.
    - **Phân bổ tiền cọc công bằng:** Tiền cọc được tự động phân bổ tỷ lệ thuận (Proportionally) theo giá trị của từng phòng trong đơn đặt.
    - **Lưu hàng loạt (Batch Save) an toàn:** Khi nhấn nút xác nhận đặt phòng, hệ thống tự động kiểm tra tính sẵn sàng của tất cả các phòng trước. Nếu không có xung đột, hệ thống tiến hành lưu từng phòng thành một đơn đặt phòng riêng biệt độc lập (`DatPhong` riêng).
    - **Cơ chế Tự động Rollback (Safe Rollback on Failure):** Trong trường hợp hy hữu xảy ra lỗi ghi DB ở bất kỳ phòng nào trong loạt đặt, hệ thống sẽ tự động xóa sạch (rollback) tất cả các đơn đặt phòng vừa tạo trước đó trong đợt để trả dữ liệu về trạng thái sạch sẽ và nhất quán.
