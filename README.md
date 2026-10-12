# 🏨 Homura Hotel Management System

> Hệ thống quản lý khách sạn cao cấp xây dựng trên nền tảng **Java 21 Swing Desktop UI**, tích hợp bộ đôi theme đặc trưng **Homura (Dark Mode)** & **Madoka (Light Mode)** cùng cơ chế **In-Memory Demo Mode** cho phép trải nghiệm tức thì không phụ thuộc cơ sở dữ liệu.

---

## ✨ Điểm nổi bật

- 🎨 **Hệ thống Theme kép (Dynamic Live Switch):**
  - **Homura Mode (Dark):** Tông tím thẫm `#13111C` huyền bí, lịch lãm, viền bo tròn sắc nét, điểm nhấn tím neon `#8B5CF6`.
  - **Madoka Mode (Light):** Tông hồng phấn dịu ngọt `#FFF5F8`, nền card trắng trang nhã `#FFFFFF`, điểm nhấn hồng `#EC4899`.
  - Chuyển đổi giao diện thời gian thực qua Observer pattern (không cần khởi động lại ứng dụng).

- ⚡ **Chế độ Demo (Offline Fallback):**
  - Tự động phát hiện khi chưa kết nối SQL Server và đề xuất kích hoạt chế độ Demo.
  - Bộ nhớ ảo in-memory (`DemoDataStore`) cung cấp sẵn 12 phòng mẫu đủ trạng thái (Trống, Đang ở, Chờ dọn dẹp, Đang sửa), đầy đủ loại phòng, hướng nhìn, dịch vụ và danh mục giá.
  - Trải nghiệm 100% quy trình: Đặt phòng -> Nhận phòng -> Gọi dịch vụ -> Thanh toán trả phòng -> Thống kê.

- 🛎️ **Phân hệ nghiệp vụ hoàn chỉnh:**
  - **Sơ đồ phòng trực quan:** Hiển thị trạng thái phòng theo mã màu, hỗ trợ thao tác nhanh, lọc theo tầng/loại phòng.
  - **Đặt & Thuê phòng:** Quy trình check-in/check-out linh hoạt, tính tiền giờ/ngày, quản lý tiền cọc, phụ thu.
  - **Dịch vụ & Minibar:** Thêm dịch vụ vào hóa đơn phòng, tra cứu giá tự động.
  - **Hóa đơn & Thanh toán:** Hỗ trợ quét mã QR, chia hóa đơn, xuất PDF / Excel.
  - **Giao ca & Thống kê:** Báo cáo doanh thu, công suất phòng, quản lý tiền mặt đầu/cuối ca.
  - **Phân quyền người dùng:** Phân tách rõ ràng vai trò Quản lý (Manager) và Lễ tân (Receptionist).

---

## 📂 Cấu trúc thư mục

```text
HomuraHotel/
├── .env.example              # Mẫu cấu hình môi trường CSDL
├── .gitignore                # Cấu hình Git ignore chuẩn Java/Swing
├── HomuraHotel.bat           # Script 1-click khởi chạy ứng dụng
├── pack_jar.bat              # Script đóng gói HomuraHotel.jar
├── README.md                 # Tài liệu hướng dẫn sử dụng
│
├── database/                 # Script khởi tạo SQL Server
│   └── complete_database.sql # DDL và dữ liệu ban đầu
├── docs/                     # Tài liệu thiết kế & phân tích hệ thống
├── icon/ & img/              # Tài nguyên hình ảnh, biểu tượng
├── lib/                      # Các thư viện phụ thuộc (.jar)
│   ├── itextpdf-5.5.13.3.jar
│   ├── mssql-jdbc-12.10.0.jre11.jar
│   ├── poi-5.2.3.jar & poi-ooxml-5.2.3.jar
│   └── ...
├── resources/                # Cấu hình db.properties và font BeVietnamPro
└── src/                      # Mã nguồn ứng dụng
    ├── UI/                   # Giao diện Swing (Frames, Panels, Dialogs, Components)
    ├── ui/theme/             # Bộ máy Theme (Theme.java, ThemeManager.java)
    ├── dao/                  # Data Access Object (hỗ trợ SQL Server & Demo Mode)
    ├── database/             # Kết nối DatabaseConnection & DemoDataStore
    ├── entity/               # Mô hình dữ liệu (Phong, KhachHang, HoaDon, ...)
    ├── service/              # Logic nghiệp vụ (Auth, DatPhong, Phong, ThongKe, ...)
    ├── util/                 # Tiện ích xuất PDF, Excel, Password Hash
    └── main/                 # Entry point (Main.java)
```

---

## 🚀 Hướng dẫn cài đặt & Khởi chạy

### 1. Yêu cầu hệ thống
- **Java Development Kit (JDK):** Phiên bản **21** trở lên.
- **Hệ điều hành:** Windows 10/11 (hỗ trợ cả macOS/Linux với script tương ứng).

### 2. Khởi chạy nhanh (1-Click trên Windows)
Chỉ cần nhấp đúp vào file hoặc chạy qua CMD:
```cmd
HomuraHotel.bat
```
> Script sẽ tự động kiểm tra, biên dịch mã nguồn (nếu chưa có thư mục `bin/`) và khởi động ứng dụng ngay lập tức.

### 3. Biên dịch và chạy thủ công

```cmd
:: 1. Tạo danh sách file mã nguồn
dir /s /b src\*.java > sources.txt

:: 2. Biên dịch
javac -encoding UTF-8 -d bin -cp "lib/*" @sources.txt

:: 3. Copy thư mục resources vào bin
xcopy /E /I /Y resources bin\resources

:: 4. Khởi chạy
java -cp "bin;lib/*" main.Main
```

### 4. Đóng gói file JAR thực thi
Chạy file batch đóng gói:
```cmd
pack_jar.bat
```
Sau đó khởi chạy file JAR độc lập:
```cmd
java -jar HomuraHotel.jar
```

---

## 🔑 Tài khoản mẫu (Chế độ Demo)

Khi mở ứng dụng, chọn **Chế độ Demo** nếu không kết nối với SQL Server. Giao diện Đăng nhập có sẵn các nút bấm nhanh:

| Tên đăng nhập | Mật khẩu | Quyền hạn | Mô tả |
|---|---|---|---|
| `manager` | `123` | **Quản lý (Manager)** | Toàn quyền quản trị, thống kê doanh thu, quản lý phòng & nhân viên |
| `recept1` | `123` | **Lễ tân (Receptionist)** | Thực hiện check-in/out, đặt phòng, gọi dịch vụ, xem sơ đồ phòng |
| `admin` | `admin` | **Quản trị hệ thống** | Quản trị tài khoản, cài đặt hệ thống |

---

## 🗄️ Cấu hình SQL Server (Chế độ Production)

Nếu muốn sử dụng cùng Microsoft SQL Server thực tế:
1. Mở SQL Server Management Studio (SSMS) và chạy toàn bộ nội dung file `database/complete_database.sql`.
2. Chỉnh sửa thông tin kết nối trong `resources/db.properties` hoặc thiết lập biến môi trường `.env`:
   ```properties
   db.url=jdbc:sqlserver://localhost:1433;databaseName=QuanLyKhachSan;encrypt=true;trustServerCertificate=true
   db.user=sa
   db.password=MatKhauCuaBan123
   ```
3. Khởi động ứng dụng, hệ thống sẽ tự động nhận diện kết nối SQL Server thật.

---

## 🎨 Tùy chỉnh Theme

Ứng dụng hỗ trợ chuyển theme tức thì tại thanh Header hoặc màn hình Login:
- **Nút Toggle Theme:** Biểu tượng 🌙 / ☀️ ở góc trên giao diện.
- **Phím tắt chuyển theme:** Tự động lưu lựa chọn theme vào `theme.properties` cho lần mở tiếp theo.

---

## 📄 Bản quyền
Dự án được xây dựng và phát triển bởi **AnkjoD**. Mọi đóng góp và mã nguồn được chia sẻ theo giấy phép mã nguồn mở.
