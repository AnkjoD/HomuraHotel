-- migration_v2.sql
-- Thêm chức năng Giao Ca và Nhật ký hệ thống

USE HotelMs;
GO

-- 1. Bảng Nhật ký hệ thống
IF OBJECT_ID('NhatKyHeThong', 'U') IS NOT NULL DROP TABLE NhatKyHeThong;
CREATE TABLE NhatKyHeThong (
    maLog INT IDENTITY(1,1) PRIMARY KEY,
    thoiGian DATETIME DEFAULT GETDATE(),
    tenDangNhap VARCHAR(50), -- Tài khoản thực hiện
    hanhDong NVARCHAR(50),    -- LOGIN, LOGOUT, CREATE, UPDATE, DELETE, ...
    doiTuong NVARCHAR(100),   -- Tên thực thể tác động (Phòng P101, Hóa đơn HD001, ...)
    chiTiet NVARCHAR(MAX)     -- Mô tả chi tiết thay đổi (ví dụ: Thay đổi giá từ 500k -> 400k)
);

-- 2. Bảng Giao ca
IF OBJECT_ID('GiaoCa', 'U') IS NOT NULL DROP TABLE GiaoCa;
CREATE TABLE GiaoCa (
    maGiaoCa VARCHAR(20) PRIMARY KEY,
    maNhanVien VARCHAR(20) REFERENCES NhanVien(maNhanVien), -- Nhân viên bàn giao
    thoiGianBatDau DATETIME NOT NULL,
    thoiGianKetThuc DATETIME, -- NULL nếu ca đang diễn ra
    tienMatDauCa DECIMAL(18,2) DEFAULT 0,
    tienMatThuTrongCa DECIMAL(18,2) DEFAULT 0, -- Tổng tiền từ các hóa đơn PAID trong ca
    tienMatBanGiao DECIMAL(18,2) DEFAULT 0,     -- Số tiền thực tế đếm được và bàn giao
    maNhanVienNhan VARCHAR(20) REFERENCES NhanVien(maNhanVien), -- Nhân viên nhận ca (optional)
    ghiChu NVARCHAR(MAX),
    trangThai VARCHAR(20) DEFAULT 'OPEN' CHECK (trangThai IN ('OPEN', 'CLOSED'))
);

GO
PRINT '✓ Migration v2 completed successfully.';
