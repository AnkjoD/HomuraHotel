# Tài Liệu Hướng Dẫn Phân Quyền Hệ Thống (RBAC) - Hotel Ohno

Hệ thống quản lý khách sạn **Hotel Ohno** áp dụng mô hình phân quyền dựa trên vai trò (**Role-Based Access Control - RBAC**). Phân quyền được thiết kế nhằm cân bằng giữa sự linh hoạt trong vận hành hàng ngày của nhân viên lễ tân và tính bảo mật, toàn vẹn dữ liệu tài chính, nhân sự của doanh nghiệp.

---

## 1. Các Vai Trò Trong Hệ Thống (Roles)

Hệ thống định nghĩa 3 vai trò chính trong enum `entity.enums.VaiTro`:

1.  **`RECEPTIONIST` (Lễ tân)**: Nhân viên vận hành trực tiếp tại quầy lễ tân (Front-Office). Tập trung vào các nghiệp vụ đón khách, đặt phòng, trả phòng và dịch vụ khách hàng hàng ngày.
2.  **`MANAGER` (Quản lý)**: Người giám sát vận hành khách sạn. Có quyền cấu hình hệ thống, quản lý doanh thu, duyệt chi phí, và xử lý các vấn đề nghiệp vụ đặc biệt.
3.  **`ADMIN` (Quản trị viên)**: Tài khoản kỹ thuật hoặc bộ phận nhân sự cấp cao. Quản lý toàn bộ hệ thống, phân quyền, cấu hình tài khoản nhân viên và kiểm soát nhật ký hệ thống (Audit Logs).

---

## 2. Ma Trận Quyền Truy Cập Chức Năng (Menu Access Matrix)

Bảng dưới đây thể hiện quyền truy cập (Xem/Mở tab) của từng vai trò đối với các module trong Sidebar:

| Mã chức năng (`moduleKey`) | Tên chức năng                                        | Lễ tân (`RECEPTIONIST`) | Quản lý (`MANAGER`) | Admin (`ADMIN`) |
| :------------------------- | :--------------------------------------------------- | :---------------------: | :-----------------: | :-------------: |
| `tongquan`                 | Tổng quan (Dashboard)                                |          ✅ Có          |        ✅ Có        |      ✅ Có      |
| `kehoach`                  | Kế hoạch sử dụng phòng                               |          ✅ Có          |        ✅ Có        |      ✅ Có      |
| `datphong`                 | Danh sách đặt phòng                                  |          ✅ Có          |        ✅ Có        |      ✅ Có      |
| `thuephong`                | Sơ đồ phòng (Check-in/out)                           |          ✅ Có          |        ✅ Có        |      ✅ Có      |
| `hoadon`                   | Lịch sử Hóa đơn                                      |          ✅ Có          |        ✅ Có        |      ✅ Có      |
| `khachhang`                | Hồ sơ khách hàng (CRM)                               |          ✅ Có          |        ✅ Có        |      ✅ Có      |
| `qlhethong`                | Cấu hình hệ thống (Loại phòng, Dịch vụ, Bảng giá...) |        ❌ Không         |        ✅ Có        |      ✅ Có      |
| `thongke`                  | Thống kê báo cáo doanh thu                           |        ❌ Không         |        ✅ Có        |      ✅ Có      |
| `nhanvien`                 | Tài khoản nhân sự                                    |        ❌ Không         |        ✅ Có        |      ✅ Có      |

> [!NOTE]  
> Màn hình **Tài khoản nhân sự (`nhanvien`)** cho phép vai trò `MANAGER` truy cập để xem danh sách nhân sự liên lạc, nhưng bị giới hạn quyền thêm/sửa/xóa tài khoản (chỉ `ADMIN` mới được sửa).

---

## 3. Quyền Thao Tác Chi Tiết (Data Mutations)

Quyền chỉnh sửa (Thêm/Sửa/Xóa) dữ liệu được ràng buộc nghiêm ngặt trong hệ thống thông qua phương thức `canEdit` của `AuthService`:

### 3.1. Nghiệp vụ Khách hàng (CRM)

- **Thêm mới & Cập nhật thông tin khách hàng**: Cho phép cả **Lễ tân (`RECEPTIONIST`)**, **Quản lý (`MANAGER`)**, và **Admin (`ADMIN`)**.
- **Xóa / Vô hiệu hóa hồ sơ khách hàng**: **Chỉ cho phép Quản lý hoặc Admin**. Lễ tân bị ẩn nút xóa và bị hệ thống từ chối thao tác nếu cố tình thực hiện. Điều này nhằm bảo vệ tính toàn vẹn dữ liệu điểm thưởng và lịch sử hóa đơn.

### 3.2. Cấu hình Hệ thống (`qlhethong`)

_(Bao gồm: Thêm/Sửa/Xóa Loại phòng, Phòng, Dịch vụ, Bảng giá, Khuyến mãi...)_

- **Chỉ cho phép**: **Quản lý (`MANAGER`)** và **Admin (`ADMIN`)** thực hiện cấu hình bảng giá và dịch vụ để tránh nhân viên tự ý thay đổi giá phòng.
- **Lễ tân**: Bị ẩn hoàn toàn chức năng này.

### 3.3. Tài khoản Nhân sự (`nhanvien`)

- **Chỉ cho phép**: **Admin (`ADMIN`)** được quyền thêm nhân viên mới, cấp tài khoản, đổi mật khẩu nhân viên hoặc khóa tài khoản.
- **Quản lý**: Chỉ được xem danh sách, không có quyền chỉnh sửa.
- **Lễ tân**: Bị ẩn hoàn toàn chức năng này.

### 3.4. Thống kê & Báo cáo (`thongke`)

- **Chỉ cho phép**: **Quản lý (`MANAGER`)** và **Admin (`ADMIN`)** xem và xuất báo cáo tài chính/biểu đồ doanh thu.
- **Lễ tân**: Bị ẩn hoàn toàn chức năng này nhằm bảo mật thông tin doanh thu của khách sạn.

---

## 4. Cấu Trúc Kỹ Thuật (Technical Code Locations)

Việc kiểm tra phân quyền được thực thi ở cả **Giao diện (Frontend - Swing)** và **Logic nghiệp vụ (Backend - Services)**:

1.  **Lớp xác thực & phân quyền chính**:
    - [AuthService.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/service/AuthService.java):
      - `hasAccess(String moduleKey)`: Quyết định quyền được xem/mở màn hình.
      - `canEdit(String moduleKey)`: Quyết định quyền chỉnh sửa nghiệp vụ.
2.  **Định nghĩa Vai trò**:
    - [VaiTro.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/entity/enums/VaiTro.java): Enum chứa các hằng số vai trò (`ADMIN`, `MANAGER`, `RECEPTIONIST`).
3.  **Điều khiển hiển thị Menu**:
    - [MainFrame.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/MainFrame.java): Kiểm tra `hasAccess(key)` để hiển thị hoặc ẩn các nút trên thanh Sidebar khi nhân viên đăng nhập.
4.  **Bảo mật thao tác dữ liệu Khách hàng**:
    - [KhachHangPanel.java](file:///d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/src/ui/panels/KhachHangPanel.java): Sử dụng `AuthService.getInstance().isManager()` để thiết lập trạng thái ẩn/hiện nút xóa và kiểm tra nghiệp vụ trước khi vô hiệu hóa hồ sơ.
