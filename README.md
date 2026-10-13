# Homura Hotel

Ứng dụng quản lý khách sạn dùng Java 21 và Swing, với SQL Server cho dữ liệu thật và Demo trong bộ nhớ cho trải nghiệm/kiểm tra offline.

## Chạy ứng dụng

Cần JDK 21 trở lên. Trên Windows, mở `HomuraHotel.bat`. Script biên dịch lại nguồn trước khi chạy, nên các sửa đổi luôn được áp dụng. Khi không kết nối được SQL Server, có thể chọn Demo.

Tài khoản Demo: `manager / 123`, `recept1 / 123`, `admin / admin`.
Demo là dữ liệu tạm trong bộ nhớ, được tạo lại khi khởi động; dùng SQL Server để lưu dữ liệu kinh doanh lâu dài. Thay đổi mật khẩu Demo có hiệu lực trong phiên hiện tại và mật khẩu cũ không được chấp nhận lại.

## Cấu trúc

```text
HomuraHotel/
├── src/
│   ├── main/        # Khởi động ứng dụng
│   ├── ui/          # Giao diện, components, dialogs, panels, theme
│   ├── service/     # Quy tắc và giao dịch nghiệp vụ
│   ├── dao/         # Truy vấn SQL và adapter Demo
│   ├── database/    # Kết nối, dữ liệu Demo và thống kê Demo
│   ├── entity/      # Mô hình dữ liệu và enums
│   ├── util/        # Xuất file, mật khẩu, mã định danh
│   └── test/        # Kiểm tra nghiệp vụ và giao diện
├── database/
│   ├── complete_database.sql
│   └── migrations/ # Nâng cấp database hiện có, không xóa dữ liệu
├── scripts/        # Build, test và đóng gói
├── resources/      # Font, icon và mẫu cấu hình
├── icon/           # Ảnh được các màn hình hiện có đọc từ filesystem
├── img/            # Ảnh phòng/tài nguyên giao diện
├── lib/            # Thư viện JAR
├── docs/           # Đặc tả và hướng dẫn
├── .github/workflows/build.yml
├── HomuraHotel.bat
└── pack_jar.bat
```

`bin/` là kết quả biên dịch và được Git bỏ qua. `.env`, `resources/db.properties`, lựa chọn theme và trạng thái đăng nhập là file cá nhân, không đưa lên GitHub. Tên thư mục `src/ui` khớp package `ui`.

Cấu trúc hiện tại giữ các lớp giao diện, nghiệp vụ và dữ liệu riêng, phù hợp với quy mô ứng dụng này. Việc chuyển sang cấu trúc Maven/Gradle có thể làm ở một đợt riêng; không cần đổi framework để sửa các lỗi hiện tại.

## SQL Server

**Database mới:** chạy `database/complete_database.sql`, tạo database `HotelMs`. Script khởi tạo có phần xóa bảng để tạo lại; chỉ dùng cho database mới hoặc dữ liệu có thể bỏ đi.

**Database đang sử dụng:** sao lưu trước, rồi chạy lần lượt `database/migrations/001_business_integrity.sql` và `database/migrations/002_dao_schema_alignment.sql`. Migration bổ sung trạng thái `DELETED`, trạng thái hóa đơn `CANCELLED` và cột liên kết chặng đổi phòng `maChiTietTruoc`. Không chạy lại script khởi tạo trên dữ liệu kinh doanh hiện có.

Copy `resources/db.properties.example` thành `resources/db.properties`, điền thông tin kết nối. Có thể dùng biến môi trường hoặc file `.env`:

```dotenv
DB_URL=jdbc:sqlserver://localhost:1433;databaseName=HotelMs;encrypt=true;trustServerCertificate=false
DB_USER=homura_app
DB_PASSWORD=your-password
```

Biến môi trường ghi đè `.env`; `.env` ghi đè properties. `MSSQL_SA_PASSWORD` vẫn được hỗ trợ cho cấu hình Docker hiện có. Với SQL Server cục bộ dùng chứng chỉ tự ký, có thể đặt `trustServerCertificate=true` trong môi trường phát triển. Dùng tài khoản ứng dụng phù hợp và cấu hình chứng chỉ khi triển khai.

## Quy tắc đã thống nhất

- Mỗi lượt lưu trú dùng giá đã chốt; đổi bảng giá không thay giá của lượt đang ở.
- Số đêm dựa trên ngày nhận/trả. Giờ trả trễ được xử lý bởi phụ phí, không đồng thời làm tròn thêm một đêm.
- Nhận sớm: trước 05:00 tính 100%, trước 09:00 tính 50%, trước 12:00 tính 30%. Trả trễ sau khi ở qua đêm: từ 13:00 tính 30%, từ 15:00 tính 50%, từ 18:00 tính 100%.
- Đổi phòng nối các chặng bằng mã chi tiết trước. Chặng cùng ngày không tự cộng thêm một đêm tối thiểu nếu chặng trước đã ghi nhận qua đêm. Thanh toán lịch sử không đổi trạng thái phòng đã tái sử dụng.
- Không cho nhận phòng hoặc đặt thêm khi trùng lịch; không tạo lại hóa đơn cho lượt đã lập hóa đơn.
- Giao dịch thất bại phải rollback dữ liệu. Kho Demo dùng bản sao độc lập, tránh để sửa object trước khi lưu làm thay đổi dữ liệu gốc.
- Mật khẩu mới dùng PBKDF2; xác minh được cả định dạng cũ để tương thích khi nâng cấp.

## Build, test và đóng gói

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1 -Test
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package.ps1
```

Hoặc chạy `pack_jar.bat`. Không cần tạo `sources.txt` thủ công.

JAR dùng các thư viện trong `lib/`; khi chuyển ứng dụng sang máy khác, giữ `HomuraHotel.jar`, `lib/`, `resources/`, `icon/` và `img/` cùng thư mục. Chạy:

```text
java -jar HomuraHotel.jar
```

GitHub Actions chạy biên dịch, kiểm tra và đóng gói trên Windows với Java 21.

## Phạm vi kiểm tra

Các bài kiểm tra tự động xác nhận luồng Demo: đặt/nhận phòng, dịch vụ, đổi phòng, thanh toán riêng và khách đoàn, hủy đơn, chặn trùng lịch, thao tác đồng thời, chặn hóa đơn lặp, rollback, giá chốt và thay mật khẩu. Kiểm tra giao diện xác nhận Tổng quan tải được, chuyển theme nhiều lần và giữ nội dung nhập/bộ lọc.

Đã kiểm thử thêm với SQL Server 2022 trong Docker. Chạy `scripts/test-sql.ps1` sau khi container `sql_server_container` sẵn sàng; script chỉ ghi vào database kiểm thử mới có tiền tố `HomuraHotel_Test_`. Có thể dùng `database/sample_data.sql` để thêm dữ liệu giả lập vào bản cài đặt mới chưa có đơn đặt phòng. Xem `docs/RELEASE_VALIDATION.md` trước khi đưa bản này vào vận hành.
