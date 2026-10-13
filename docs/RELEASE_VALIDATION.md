# Kiểm tra bản sửa nghiệp vụ và giao diện — 13/10/2026

## Tự động

Chạy `scripts/build.ps1 -Test` và `scripts/package.ps1`.

- Biên dịch Java 21.
- Đăng nhập, liệt kê dữ liệu Demo.
- Chuyển theme tối/sáng nhiều lần; giữ nội dung đăng nhập, mật khẩu, trạng thái khóa nút và tìm kiếm.
- Đặt phòng và từ chối trùng lịch, bao gồm hai yêu cầu đồng thời.
- Nhận phòng, từ chối nhận lại, gọi dịch vụ.
- Đổi phòng và chuyển dịch vụ một lần; giữ giá chốt.
- Tính chặng lưu trú liên kết; không đổi trạng thái phòng cũ đã tái sử dụng.
- Thanh toán riêng và hóa đơn khách đoàn; từ chối tạo hóa đơn lặp.
- Tiền cọc được khấu trừ một lần; đơn hoàn tất không giữ lại cọc đã sử dụng.
- Hủy đơn giải phóng lịch.
- Lỗi khi ghi dịch vụ đi kèm rollback cả đơn và chi tiết đã ghi trước đó.
- Rollback phục hồi cả dữ liệu mutable.
- Giá chốt được ưu tiên so với bảng giá hiện hành; phụ phí trả trễ không vừa cộng thêm đêm vừa tính giờ trễ.
- Đổi mật khẩu, từ chối mật khẩu cũ và xác minh mật khẩu hash.
- Doanh thu Demo khớp hóa đơn đã thanh toán.

## Kiểm thử tích hợp cần thực hiện trước vận hành

Máy kiểm tra hiện không có SQL Server lắng nghe ở cổng 1433. Không ghi thay đổi lên database thật trong đợt này.

Trên database thử nghiệm đã áp dụng migration:

1. Kiểm tra đăng nhập bằng mật khẩu cũ và sau khi đổi mật khẩu.
2. Chạy các luồng ở trên với SQL Server, đối chiếu Phong, DatPhong, ChiTietDatPhong, SuDungDichVu, HoaDon và ChiTietHoaDon.
3. Dùng hai phiên ứng dụng đặt cùng phòng và trả cùng lượt lưu trú; chỉ một phiên được thành công.
4. Giả lập lỗi ghi từng bảng trong nhận/đổi/trả phòng, xác nhận rollback không để lại trạng thái lệch.
5. Kiểm tra soft delete phòng và hủy hóa đơn chưa thanh toán; hóa đơn đã thanh toán được bảo toàn.
6. Đối chiếu tiền cọc, hoàn tiền nếu có số dư âm, phụ phí, dịch vụ đã thu và báo cáo ca theo chính sách vận hành thực tế.
7. Kiểm tra xuất PDF/Excel và in hóa đơn trên máy triển khai.

Demo chỉ lưu trong bộ nhớ, không dùng thay database sản xuất. Các chính sách tiền phòng/giờ trễ hiện được ghi trong README và cần được đơn vị vận hành xác nhận.
