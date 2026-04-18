-- ======================================================
-- HỆ THỐNG QUẢN LÝ KHÁCH SẠN (HOTEL MS)
-- Database Redesign based on UML Diagram
-- ======================================================

/* 
-- UNCOMMENT TO RESET ENTIRE DATABASE:
USE master;
GO
IF EXISTS (SELECT * FROM sys.databases WHERE name = 'HotelMs')
BEGIN
    ALTER DATABASE HotelMs SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE HotelMs;
END
GO
*/

IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'HotelMs')
BEGIN
    CREATE DATABASE HotelMs;
END
GO

USE HotelMs;
GO

-- 0. Xóa các bảng cũ theo thứ tự ngược lại của ràng buộc (FK)
-- Đảm bảo xóa sạch để tránh lỗi ràng buộc khi tạo lại
IF OBJECT_ID('ChiTietHoaDon', 'U') IS NOT NULL DROP TABLE ChiTietHoaDon;
IF OBJECT_ID('HoaDon', 'U') IS NOT NULL DROP TABLE HoaDon;
IF OBJECT_ID('KhuyenMai', 'U') IS NOT NULL DROP TABLE KhuyenMai;
IF OBJECT_ID('SuDungDichVu', 'U') IS NOT NULL DROP TABLE SuDungDichVu;
IF OBJECT_ID('DichVu', 'U') IS NOT NULL DROP TABLE DichVu;
IF OBJECT_ID('ChiTietDatPhong', 'U') IS NOT NULL DROP TABLE ChiTietDatPhong;
IF OBJECT_ID('DatPhong', 'U') IS NOT NULL DROP TABLE DatPhong;
IF OBJECT_ID('KhachHang', 'U') IS NOT NULL DROP TABLE KhachHang;
IF OBJECT_ID('LichSuTrangThaiPhong', 'U') IS NOT NULL DROP TABLE LichSuTrangThaiPhong;
IF OBJECT_ID('Phong', 'U') IS NOT NULL DROP TABLE Phong;
IF OBJECT_ID('TienNghi_LoaiPhong', 'U') IS NOT NULL DROP TABLE TienNghi_LoaiPhong;
IF OBJECT_ID('TienNghi', 'U') IS NOT NULL DROP TABLE TienNghi;
IF OBJECT_ID('ChiTietBangGia', 'U') IS NOT NULL DROP TABLE ChiTietBangGia;
IF OBJECT_ID('LoaiPhong', 'U') IS NOT NULL DROP TABLE LoaiPhong;
IF OBJECT_ID('HuongNhin', 'U') IS NOT NULL DROP TABLE HuongNhin;
IF OBJECT_ID('BangGia', 'U') IS NOT NULL DROP TABLE BangGia;
IF OBJECT_ID('TaiKhoan', 'U') IS NOT NULL DROP TABLE TaiKhoan;
IF OBJECT_ID('NhanVien', 'U') IS NOT NULL DROP TABLE NhanVien;
GO

-- 1. Nhân viên & Tài khoản
CREATE TABLE NhanVien (
    maNhanVien VARCHAR(20) PRIMARY KEY,
    hoTen NVARCHAR(100) NOT NULL,
    sdt VARCHAR(20),
    chucVu NVARCHAR(50),
    email NVARCHAR(100),
    cccd NVARCHAR(20),
    ngaySinh DATE,
    gioiTinh NVARCHAR(10),
    diaChi NVARCHAR(255),
    ngayVaoLam DATE,
    dangLamViec BIT DEFAULT 1,
    luongCoBan DECIMAL(18, 2) DEFAULT 0
);

CREATE TABLE TaiKhoan (
    tenDangNhap VARCHAR(50) PRIMARY KEY,
    maNhanVien VARCHAR(20) UNIQUE REFERENCES NhanVien(maNhanVien),
    matKhau VARCHAR(100) NOT NULL,
    vaiTro VARCHAR(20) CHECK (vaiTro IN ('ADMIN', 'MANAGER', 'RECEPTIONIST')),
    trangThai BIT DEFAULT 1, -- 1: Active, 0: Inactive
    lanDangNhapCuoi DATETIME
);

-- 2. Hướng nhìn (Room View) – bảng danh mục
CREATE TABLE HuongNhin (
    maHuongNhin VARCHAR(20) PRIMARY KEY,
    tenHuongNhin NVARCHAR(100) NOT NULL,
    moTa NVARCHAR(MAX),
    heSoGia DECIMAL(18,2) DEFAULT 1.0,  -- hệ số nhân giá (1.0 = giá gốc, 1.3 = +30%)
    thuTu INT DEFAULT 0         -- sắp xếp hiển thị
);

-- 3. Loại phòng & Phòng
CREATE TABLE LoaiPhong (
    maLoaiPhong VARCHAR(20) PRIMARY KEY,
    tenLoaiPhong NVARCHAR(100) NOT NULL,
    soNguoiToiDa INT,
    moTa NVARCHAR(MAX),
    giaTheoNgay DECIMAL(18,2)
);

-- 4. Tiện nghi (Amenities) – bảng danh mục
CREATE TABLE TienNghi (
    maTienNghi VARCHAR(20) PRIMARY KEY,
    tenTienNghi NVARCHAR(100) NOT NULL,
    nhomTienNghi NVARCHAR(50),     -- Cơ bản, Giải trí, Phòng tắm, Nội thất...
    icon NVARCHAR(10),              -- emoji icon
    thuTu INT DEFAULT 0
);

-- 5. Bảng trung gian: Tiện nghi ↔ Loại phòng (Many-to-Many)
CREATE TABLE TienNghi_LoaiPhong (
    maLoaiPhong VARCHAR(20) REFERENCES LoaiPhong(maLoaiPhong),
    maTienNghi VARCHAR(20) REFERENCES TienNghi(maTienNghi),
    PRIMARY KEY (maLoaiPhong, maTienNghi)
);

CREATE TABLE Phong (
    maPhong VARCHAR(20) PRIMARY KEY,
    tang INT,
    maHuongNhin VARCHAR(20) REFERENCES HuongNhin(maHuongNhin),
    maLoaiPhong VARCHAR(20) REFERENCES LoaiPhong(maLoaiPhong),
    trangThai VARCHAR(20) DEFAULT 'AVAILABLE' CHECK (trangThai IN ('AVAILABLE', 'OCCUPIED', 'MAINTENANCE', 'CLEANING'))
);

CREATE TABLE LichSuTrangThaiPhong (
    maLichSu INT IDENTITY(1,1) PRIMARY KEY,
    maPhong VARCHAR(20) REFERENCES Phong(maPhong),
    trangThaiCu VARCHAR(20),
    trangThaiMoi VARCHAR(20),
    thoiGianChuyen DATETIME DEFAULT GETDATE(),
    lyDo NVARCHAR(MAX)
);

-- 6. Bảng giá & Chi tiết bảng giá
CREATE TABLE BangGia (
    maBangGia VARCHAR(20) PRIMARY KEY,
    tenBangGia NVARCHAR(100) NOT NULL,
    ngayBatDau DATETIME,
    ngayKetThuc DATETIME,
    isKichHoat BIT DEFAULT 1
);

CREATE TABLE ChiTietBangGia (
    maChiTietBangGia INT IDENTITY(1,1) PRIMARY KEY,
    maBangGia VARCHAR(20) REFERENCES BangGia(maBangGia),
    maLoaiPhong VARCHAR(20) REFERENCES LoaiPhong(maLoaiPhong),
    giaTheoNgay DECIMAL(18,2),
    giaTheoGio DECIMAL(18,2),
    phuPhiTraTre DECIMAL(18,2)
);

-- 7. Khách hàng & Đặt phòng
CREATE TABLE KhachHang (
    maKhachHang VARCHAR(20) PRIMARY KEY,
    hoTen NVARCHAR(100) NOT NULL,
    sdt VARCHAR(20),
    cccd VARCHAR(20),
    ngaySinh DATE,
    gioiTinh NVARCHAR(10),
    quocTich NVARCHAR(50) DEFAULT N'Việt Nam'
);

CREATE TABLE DatPhong (
    maDatPhong VARCHAR(20) PRIMARY KEY,
    maKhachHang VARCHAR(20) REFERENCES KhachHang(maKhachHang),
    maNhanVien VARCHAR(20) REFERENCES NhanVien(maNhanVien), -- Người tạo
    ngayDat DATETIME DEFAULT GETDATE(),
    ngayNhanDuKien DATETIME,
    ngayTraDuKien DATETIME,
    soNguoi INT,
    tienDatCoc DECIMAL(18,2) DEFAULT 0,
    tongTienTamTinh DECIMAL(18,2) DEFAULT 0,
    trangThai VARCHAR(20) DEFAULT 'PENDING' CHECK (trangThai IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')),
    ghiChu NVARCHAR(MAX),
    loaiKhach VARCHAR(10) DEFAULT 'CA_NHAN' CHECK (loaiKhach IN ('CA_NHAN', 'DOAN')),
    tenDoan NVARCHAR(200) -- Tên đoàn / công ty (NULL nếu khách lẻ)
);

CREATE TABLE ChiTietDatPhong (
    maChiTiet VARCHAR(20) PRIMARY KEY,
    maDatPhong VARCHAR(20) REFERENCES DatPhong(maDatPhong),
    maPhong VARCHAR(20) REFERENCES Phong(maPhong),
    maKhachHang VARCHAR(20) REFERENCES KhachHang(maKhachHang), -- Người lưu trú thực tế (NULL = dùng khách của DatPhong)
    ngayNhanThucTe DATETIME,
    ngayTraThucTe DATETIME,
    giaThucTeChot DECIMAL(18,2),
    phuPhiPhatSinh DECIMAL(18,2) DEFAULT 0,
    daThanhToan BIT DEFAULT 0
);

-- 8. Dịch vụ & Sử dụng dịch vụ
CREATE TABLE DichVu (
    maDichVu VARCHAR(20) PRIMARY KEY,
    tenDichVu NVARCHAR(100) NOT NULL,
    loai NVARCHAR(50),
    donGiaHienTai DECIMAL(18,2) NOT NULL,
    donViTinh NVARCHAR(20),
    soLuongMin INT DEFAULT 1,
    moTa NVARCHAR(MAX),
    trangThai BIT DEFAULT 1
);

CREATE TABLE SuDungDichVu (
    maSuDung VARCHAR(20) PRIMARY KEY,
    maChiTiet VARCHAR(20) REFERENCES ChiTietDatPhong(maChiTiet), -- Dịch vụ gán vào từng phòng để dễ tách bill
    maDichVu VARCHAR(20) REFERENCES DichVu(maDichVu),
    soLuong INT NOT NULL,
    donGiaLucDung DECIMAL(18,2),
    thoiGianDung DATETIME DEFAULT GETDATE()
);

-- 9. Khuyến mãi, Hóa đơn & Chi tiết hóa đơn
CREATE TABLE KhuyenMai (
    maKhuyenMai VARCHAR(20) PRIMARY KEY,
    loaiGiam VARCHAR(10) CHECK (loaiGiam IN ('PERCENT', 'FIXED')),
    giaTriGiam DECIMAL(18,2),
    ngayBatDau DATETIME,
    ngayKetThuc DATETIME,
    dieuKienApDung NVARCHAR(MAX)
);

CREATE TABLE HoaDon (
    maHoaDon VARCHAR(20) PRIMARY KEY,
    maDatPhong VARCHAR(20) REFERENCES DatPhong(maDatPhong),
    maNhanVien VARCHAR(20) REFERENCES NhanVien(maNhanVien), -- Người lập
    maKhuyenMai VARCHAR(20) REFERENCES KhuyenMai(maKhuyenMai),
    ngayLap DATETIME DEFAULT GETDATE(),
    tongTienPhong DECIMAL(18,2) DEFAULT 0,
    tongTienDichVu DECIMAL(18,2) DEFAULT 0,
    tienGiamKhuyenMai DECIMAL(18,2) DEFAULT 0,
    tongThanhToan DECIMAL(18,2) DEFAULT 0,
    trangThai VARCHAR(20) DEFAULT 'UNPAID' CHECK (trangThai IN ('UNPAID', 'PARTIALLY_PAID', 'PAID', 'REFUNDED'))
);

CREATE TABLE ChiTietHoaDon (
    maChiTiet INT IDENTITY(1,1) PRIMARY KEY,
    maHoaDon VARCHAR(20) REFERENCES HoaDon(maHoaDon),
    loai VARCHAR(10) CHECK (loai IN ('PHONG', 'DICH_VU')),
    moTa NVARCHAR(MAX),
    soLuong INT,
    donGia DECIMAL(18,2),
    thanhTien DECIMAL(18,2),
    maNguonNoiBo VARCHAR(20) -- maChiTietDatPhong hoặc maSuDungDichVu
);

-- ======================================================
-- INDEXES
-- ======================================================
CREATE INDEX IX_PHONG_LOAI         ON Phong(maLoaiPhong);
CREATE INDEX IX_PHONG_TRANGTHAI    ON Phong(trangThai);
CREATE INDEX IX_KHACH_SDT          ON KhachHang(sdt);
CREATE INDEX IX_KHACH_CCCD         ON KhachHang(cccd);
CREATE INDEX IX_DATPHONG_KHACH     ON DatPhong(maKhachHang);
CREATE INDEX IX_DATPHONG_TRANGTHAI ON DatPhong(trangThai);
CREATE INDEX IX_CTDP_DATPHONG      ON ChiTietDatPhong(maDatPhong);
CREATE INDEX IX_CTDP_PHONG         ON ChiTietDatPhong(maPhong);
CREATE INDEX IX_SDDV_CHITIET       ON SuDungDichVu(maChiTiet);
CREATE INDEX IX_HOADON_DATPHONG    ON HoaDon(maDatPhong);
CREATE INDEX IX_CTHD_HOADON        ON ChiTietHoaDon(maHoaDon);

-- ======================================================
-- DATA SEEDING (Dữ liệu mẫu để test) -- MỞ RỘNG
-- ======================================================

-- 1. Nhân viên & Tài khoản
INSERT INTO NhanVien (maNhanVien, hoTen, sdt, chucVu, email, cccd, ngaySinh, gioiTinh, diaChi, ngayVaoLam, dangLamViec, luongCoBan) 
VALUES ('admin_nv', N'Lê Quản Trị', '0901234567', N'Quản lý hệ thống', 'admin@hotel.com', '123456789', '1985-01-01', N'Nam', N'Hà Nội', '2020-01-01', 1, 20000000);
INSERT INTO NhanVien (maNhanVien, hoTen, sdt, chucVu, email, cccd, ngaySinh, gioiTinh, diaChi, ngayVaoLam, dangLamViec, luongCoBan)
VALUES ('manager_nv', N'Trần Giám Đốc', '0912345678', N'Quản lý khách sạn', 'manager@hotel.com', '987654321', '1980-05-10', N'Nam', N'TP HCM', '2021-06-15', 1, 15000000);
INSERT INTO NhanVien (maNhanVien, hoTen, sdt, chucVu, email, cccd, ngaySinh, gioiTinh, diaChi, ngayVaoLam, dangLamViec, luongCoBan)
VALUES ('recept_01', N'Nguyễn Lễ Tân 1', '0987654321', N'Lễ tân', 'recept1@hotel.com', '456789123', '1995-03-20', N'Nữ', N'Đà Nẵng', '2022-09-01', 1, 8000000);
INSERT INTO NhanVien (maNhanVien, hoTen, sdt, chucVu, email, cccd, ngaySinh, gioiTinh, diaChi, ngayVaoLam, dangLamViec, luongCoBan)
VALUES ('recept_02', N'Phạm Lễ Tân 2', '0977888999', N'Lễ tân', 'recept2@hotel.com', '321654987', '1998-11-12', N'Nữ', N'Cần Thơ', '2023-01-10', 1, 8000000);

INSERT INTO TaiKhoan (tenDangNhap, maNhanVien, matKhau, vaiTro, trangThai) VALUES ('admin', 'admin_nv', 'admin', 'ADMIN', 1);
INSERT INTO TaiKhoan (tenDangNhap, maNhanVien, matKhau, vaiTro, trangThai) VALUES ('manager', 'manager_nv', '123', 'MANAGER', 1);
INSERT INTO TaiKhoan (tenDangNhap, maNhanVien, matKhau, vaiTro, trangThai) VALUES ('recept1', 'recept_01', '123', 'RECEPTIONIST', 1);
INSERT INTO TaiKhoan (tenDangNhap, maNhanVien, matKhau, vaiTro, trangThai) VALUES ('recept2', 'recept_02', '123', 'RECEPTIONIST', 1);

-- 2. Hướng nhìn (seed) — heSoGia: 1.0 = giá gốc, 1.15 = +15%, 1.3 = +30%...
INSERT INTO HuongNhin VALUES ('HN01', N'Hướng phố',    N'Nhìn ra đường phố sầm uất',       1.0,  1);
INSERT INTO HuongNhin VALUES ('HN02', N'Hướng vườn',   N'Nhìn ra khu vườn xanh mát',       1.0,  2);
INSERT INTO HuongNhin VALUES ('HN03', N'Hướng biển',   N'Nhìn ra bãi biển và đại dương',    1.3,  3);
INSERT INTO HuongNhin VALUES ('HN04', N'Hướng hồ bơi', N'Nhìn ra hồ bơi resort',            1.15, 4);
INSERT INTO HuongNhin VALUES ('HN05', N'Hướng núi',    N'Nhìn ra cảnh núi non',             1.1,  5);
INSERT INTO HuongNhin VALUES ('HN06', N'VIP Panorama', N'View toàn cảnh 180° tầng cao',     1.5,  6);

-- 3. Loại phòng (bỏ cột tienNghi – giờ dùng bảng trung gian)
INSERT INTO LoaiPhong VALUES ('LP001', N'Standard Single', 1, N'Phòng tiêu chuẩn cho 1 người', 400000);
INSERT INTO LoaiPhong VALUES ('LP002', N'Standard Double', 2, N'Phòng tiêu chuẩn cho 2 người', 600000);
INSERT INTO LoaiPhong VALUES ('LP003', N'Deluxe Twin', 2, N'Phòng hạng sang 2 giường đơn', 900000);
INSERT INTO LoaiPhong VALUES ('LP004', N'Deluxe King', 2, N'Phòng hạng sang giường lớn view biển', 1200000);
INSERT INTO LoaiPhong VALUES ('LP005', N'Suite Family', 4, N'Phòng cao cấp cho gia đình', 2000000);

-- 4. Tiện nghi (seed)
INSERT INTO TienNghi VALUES ('TN01', N'Wifi miễn phí',     N'Cơ bản',      N'📶', 1);
INSERT INTO TienNghi VALUES ('TN02', N'Điều hòa',          N'Cơ bản',      N'❄️', 2);
INSERT INTO TienNghi VALUES ('TN03', N'Tivi',              N'Giải trí',    N'📺', 3);
INSERT INTO TienNghi VALUES ('TN04', N'Tivi 55 inch',      N'Giải trí',    N'📺', 4);
INSERT INTO TienNghi VALUES ('TN05', N'Minibar',           N'Giải trí',    N'🍷', 5);
INSERT INTO TienNghi VALUES ('TN06', N'Giường đơn',        N'Nội thất',    N'🛏️', 6);
INSERT INTO TienNghi VALUES ('TN07', N'Giường đôi',        N'Nội thất',    N'🛏️', 7);
INSERT INTO TienNghi VALUES ('TN08', N'Giường King',       N'Nội thất',    N'🛏️', 8);
INSERT INTO TienNghi VALUES ('TN09', N'2 Giường đơn',      N'Nội thất',    N'🛏️', 9);
INSERT INTO TienNghi VALUES ('TN10', N'2 Giường đôi',      N'Nội thất',    N'🛏️', 10);
INSERT INTO TienNghi VALUES ('TN11', N'Bồn tắm',          N'Phòng tắm',   N'🛁', 11);
INSERT INTO TienNghi VALUES ('TN12', N'Vòi sen đứng',     N'Phòng tắm',   N'🚿', 12);
INSERT INTO TienNghi VALUES ('TN13', N'Máy sấy tóc',      N'Phòng tắm',   N'💇', 13);
INSERT INTO TienNghi VALUES ('TN14', N'Két an toàn',       N'Tiện ích',    N'🔒', 14);
INSERT INTO TienNghi VALUES ('TN15', N'Sofa',              N'Nội thất',    N'🛋️', 15);
INSERT INTO TienNghi VALUES ('TN16', N'Bàn làm việc',      N'Nội thất',    N'🪑', 16);
INSERT INTO TienNghi VALUES ('TN17', N'Ban công',          N'Tiện ích',    N'🌅', 17);
INSERT INTO TienNghi VALUES ('TN18', N'Máy pha cà phê',   N'Tiện ích',    N'☕', 18);

-- 5. Tiện nghi ↔ Loại phòng (Many-to-Many)
-- Standard Single: Wifi, Điều hòa, Tivi, Giường đơn, Vòi sen
INSERT INTO TienNghi_LoaiPhong VALUES ('LP001', 'TN01');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP001', 'TN02');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP001', 'TN03');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP001', 'TN06');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP001', 'TN12');
-- Standard Double: Wifi, Điều hòa, Tivi, Giường đôi, Vòi sen, Máy sấy
INSERT INTO TienNghi_LoaiPhong VALUES ('LP002', 'TN01');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP002', 'TN02');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP002', 'TN03');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP002', 'TN07');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP002', 'TN12');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP002', 'TN13');
-- Deluxe Twin: Wifi, Điều hòa, Tivi, 2 Giường đơn, Vòi sen, Máy sấy, Két, Bàn
INSERT INTO TienNghi_LoaiPhong VALUES ('LP003', 'TN01');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP003', 'TN02');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP003', 'TN03');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP003', 'TN09');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP003', 'TN12');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP003', 'TN13');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP003', 'TN14');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP003', 'TN16');
-- Deluxe King: Wifi, Điều hòa, Tivi 55", Giường King, Bồn tắm, Minibar, Két, Ban công, Cà phê
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN01');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN02');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN04');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN05');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN08');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN11');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN14');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN17');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP004', 'TN18');
-- Suite Family: Wifi, Điều hòa, Tivi 55", 2 Giường đôi, Bồn tắm, Minibar, Sofa, Két, Ban công, Cà phê, Bàn
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN01');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN02');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN04');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN05');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN10');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN11');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN14');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN15');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN16');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN17');
INSERT INTO TienNghi_LoaiPhong VALUES ('LP005', 'TN18');

-- 6. Phòng (Tạo 25 phòng mẫu) – dùng maHuongNhin thay vì text
-- Tầng 1 (Standard)
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P101', 1, 'HN01', 'LP001', 'OCCUPIED');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P102', 1, 'HN01', 'LP001', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P103', 1, 'HN02', 'LP002', 'CLEANING');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P104', 1, 'HN02', 'LP002', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P105', 1, 'HN02', 'LP002', 'AVAILABLE');

-- Tầng 2 (Standard & Deluxe)
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P201', 2, 'HN02', 'LP001', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P202', 2, 'HN02', 'LP002', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P203', 2, 'HN02', 'LP003', 'OCCUPIED');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P204', 2, 'HN02', 'LP003', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P205', 2, 'HN02', 'LP003', 'AVAILABLE');

-- Tầng 3 (Deluxe)
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P301', 3, 'HN03', 'LP003', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P302', 3, 'HN03', 'LP003', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P303', 3, 'HN03', 'LP004', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P304', 3, 'HN03', 'LP004', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P305', 3, 'HN03', 'LP004', 'AVAILABLE');

-- Tầng 4 (Deluxe & Suite)
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P401', 4, 'HN03', 'LP004', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P402', 4, 'HN03', 'LP004', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P403', 4, 'HN03', 'LP005', 'OCCUPIED');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P404', 4, 'HN03', 'LP005', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P405', 4, 'HN03', 'LP005', 'AVAILABLE');

-- Tầng 5 (VIP Suite)
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P501', 5, 'HN06', 'LP005', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P502', 5, 'HN06', 'LP005', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P503', 5, 'HN06', 'LP005', 'AVAILABLE');
INSERT INTO Phong (maPhong, tang, maHuongNhin, maLoaiPhong, trangThai) VALUES ('P505', 5, 'HN06', 'LP005', 'AVAILABLE');

-- 4. Khách hàng
INSERT INTO KhachHang (maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich) VALUES ('KH001', N'Nguyễn Văn An', '0911222333', '012345678901', '1990-05-15', N'Nam', N'Việt Nam');
INSERT INTO KhachHang (maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich) VALUES ('KH002', N'Trần Thị Bình', '0922333444', '098765432102', '1992-10-20', N'Nữ', N'Việt Nam');
INSERT INTO KhachHang (maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich) VALUES ('KH003', N'Lê Văn Cường', '0933444555', '045678912303', '1988-02-28', N'Nam', N'Việt Nam');
INSERT INTO KhachHang (maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich) VALUES ('KH004', N'Phạm Thị Dung', '0944555666', '032165498704', '1995-07-10', N'Nữ', N'Mỹ');
INSERT INTO KhachHang (maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich) VALUES ('KH005', N'Hoàng Văn Em', '0955666777', '078912345605', '2000-12-05', N'Nam', N'Hàn Quốc');

-- 8. Bảng giá năm 2024
INSERT INTO BangGia VALUES ('BG2024', N'Bảng giá năm 2024', '2024-01-01', '2024-12-31', 1);
INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre) VALUES ('BG2024', 'LP001', 400000, 70000, 25000);
INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre) VALUES ('BG2024', 'LP002', 600000, 100000, 40000);
INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre) VALUES ('BG2024', 'LP003', 900000, 150000, 60000);
INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre) VALUES ('BG2024', 'LP004', 1200000, 200000, 80000);
INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre) VALUES ('BG2024', 'LP005', 2000000, 350000, 150000);

-- 9. Dịch vụ
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV001', N'Nước suối', N'Ăn uống', 15000, N'chai', 1, N'Nước khoáng thiên nhiên 500ml', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV002', N'Coca Cola', N'Ăn uống', 25000, N'lon', 1, N'Nước giải khát có gas', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV003', N'Bia Tiger', N'Ăn uống', 35000, N'lon', 1, N'Bia Tiger xanh', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV004', N'Mì ly', N'Ăn uống', 20000, N'ly', 1, N'Mì tôm ly ăn liền', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV005', N'Giặt ủi (kg)', N'Dịch vụ phòng', 40000, N'kg', 1, N'Dịch vụ giặt sấy quần áo', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV006', N'Thuê xe máy', N'Vận chuyển', 150000, N'ngày', 1, N'Thuê xe máy số/ga', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV007', N'Ăn sáng tại phòng', N'Ăn uống', 100000, N'suất', 1, N'Bữa sáng giao tận nơi', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV008', N'Massage thư giãn', N'Spa & Làm đẹp', 300000, N'lượt', 1, N'Massage body 60p', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV009', N'Phụ thu người lớn', N'Phụ thu', 150000, N'người/đêm', 1, N'Phụ thu thêm người lớn vượt sức chứa tiêu chuẩn (bao gồm giường phụ/nệm)', 1);
INSERT INTO DichVu (maDichVu, tenDichVu, loai, donGiaHienTai, donViTinh, soLuongMin, moTa, trangThai) VALUES ('DV010', N'Phụ thu trẻ em', N'Phụ thu', 100000, N'người/đêm', 1, N'Phụ thu thêm trẻ em (6-12 tuổi) vượt sức chứa tiêu chuẩn', 1);

-- 10. Khuyến mãi
INSERT INTO KhuyenMai VALUES ('SALE10', 'PERCENT', 10, '2024-01-01', '2024-12-31', N'Giảm 10% tổng hóa đơn');
INSERT INTO KhuyenMai VALUES ('WELCOME', 'FIXED', 100000, '2024-01-01', '2024-12-31', N'Giảm 100k cho khách mới');

-- 11. Đặt phòng & Chi tiết đặt phòng (với hoạt động hôm nay)
INSERT INTO DatPhong (maDatPhong, maKhachHang, maNhanVien, ngayDat, ngayNhanDuKien, ngayTraDuKien, soNguoi, tienDatCoc, tongTienTamTinh, trangThai, ghiChu) 
VALUES ('DP001', 'KH001', 'recept_01', GETDATE(), GETDATE(), DATEADD(DAY, 2, GETDATE()), 1, 200000, 400000, 'CHECKED_IN', N'Check-in hôm nay');
INSERT INTO DatPhong (maDatPhong, maKhachHang, maNhanVien, ngayDat, ngayNhanDuKien, ngayTraDuKien, soNguoi, tienDatCoc, tongTienTamTinh, trangThai, ghiChu) 
VALUES ('DP002', 'KH002', 'recept_01', GETDATE(), GETDATE(), DATEADD(DAY, 1, GETDATE()), 2, 300000, 600000, 'CHECKED_OUT', N'Check-out hôm nay');
INSERT INTO DatPhong (maDatPhong, maKhachHang, maNhanVien, ngayDat, ngayNhanDuKien, ngayTraDuKien, soNguoi, tienDatCoc, tongTienTamTinh, trangThai, ghiChu) 
VALUES ('DP003', 'KH003', 'recept_02', GETDATE(), GETDATE(), DATEADD(DAY, 3, GETDATE()), 2, 450000, 900000, 'CHECKED_IN', N'Khách VIP');
INSERT INTO DatPhong (maDatPhong, maKhachHang, maNhanVien, ngayDat, ngayNhanDuKien, ngayTraDuKien, soNguoi, tienDatCoc, tongTienTamTinh, trangThai, ghiChu) 
VALUES ('DP004', 'KH004', 'recept_02', GETDATE(), GETDATE(), DATEADD(DAY, 1, GETDATE()), 4, 600000, 2000000, 'CHECKED_IN', N'Suite family');

-- Chi tiết đặt phòng (check-in/check-out hôm nay) — mỗi phòng gán người lưu trú riêng
INSERT INTO ChiTietDatPhong (maChiTiet, maDatPhong, maPhong, maKhachHang, ngayNhanThucTe, ngayTraThucTe, giaThucTeChot, phuPhiPhatSinh)
VALUES ('CT001', 'DP001', 'P101', 'KH001', GETDATE(), NULL, 400000, 0);
INSERT INTO ChiTietDatPhong (maChiTiet, maDatPhong, maPhong, maKhachHang, ngayNhanThucTe, ngayTraThucTe, giaThucTeChot, phuPhiPhatSinh)
VALUES ('CT002', 'DP002', 'P103', 'KH002', DATEADD(DAY, -1, GETDATE()), GETDATE(), 600000, 0);
INSERT INTO ChiTietDatPhong (maChiTiet, maDatPhong, maPhong, maKhachHang, ngayNhanThucTe, ngayTraThucTe, giaThucTeChot, phuPhiPhatSinh)
VALUES ('CT003', 'DP003', 'P203', 'KH003', GETDATE(), NULL, 900000, 0);
INSERT INTO ChiTietDatPhong (maChiTiet, maDatPhong, maPhong, maKhachHang, ngayNhanThucTe, ngayTraThucTe, giaThucTeChot, phuPhiPhatSinh)
VALUES ('CT004', 'DP004', 'P403', 'KH004', GETDATE(), NULL, 2000000, 50000);

-- 12. Hóa đơn
INSERT INTO HoaDon (maHoaDon, maDatPhong, maNhanVien, maKhuyenMai, ngayLap, tongTienPhong, tongTienDichVu, tienGiamKhuyenMai, tongThanhToan, trangThai)
VALUES ('HD001', 'DP002', 'recept_01', NULL, GETDATE(), 600000, 50000, 0, 650000, 'PAID');

PRINT '✓ Da mo rong du lieu mau (25 phong, 5 loai phong, 5 khach hang, 8 dich vu, hoat dong hom nay) thanh cong!';
GO
