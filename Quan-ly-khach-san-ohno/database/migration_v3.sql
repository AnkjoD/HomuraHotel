-- migration_v3.sql
-- Nâng cấp chức năng Giao Ca chuyên nghiệp và Quản lý Chi phí

USE HotelMs;
GO

-- 1. Bổ sung phương thức thanh toán cho Hóa đơn
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'phuongThucThanhToan')
BEGIN
    ALTER TABLE HoaDon ADD phuongThucThanhToan VARCHAR(20) DEFAULT 'CASH' 
    CHECK (phuongThucThanhToan IN ('CASH', 'CARD', 'TRANSFER'));
END
GO

-- 2. Bổ sung cột chênh lệch cho bảng GiaoCa
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('GiaoCa') AND name = 'tienMatChenhLech')
BEGIN
    ALTER TABLE GiaoCa ADD tienMatChenhLech DECIMAL(18,2) DEFAULT 0;
END
GO

-- 3. Tạo bảng Quản lý Chi phí (Petty Cash/Expenditure)
IF OBJECT_ID('ChiPhi', 'U') IS NOT NULL DROP TABLE ChiPhi;
CREATE TABLE ChiPhi (
    maChiPhi INT IDENTITY(1,1) PRIMARY KEY,
    maNhanVien VARCHAR(20) REFERENCES NhanVien(maNhanVien),
    soTien DECIMAL(18,2) NOT NULL,
    lyDo NVARCHAR(MAX) NOT NULL,
    thoiGian DATETIME DEFAULT GETDATE(),
    maGiaoCa VARCHAR(20) REFERENCES GiaoCa(maGiaoCa) -- Ghi nhận chi phí diễn ra trong ca nào
);
GO

-- 4. Bảng chi tiết đếm tiền (Audit trail cho số lượng tờ tiền)
IF OBJECT_ID('ChiTietKiemTien', 'U') IS NOT NULL DROP TABLE ChiTietKiemTien;
CREATE TABLE ChiTietKiemTien (
    maGiaoCa VARCHAR(20) REFERENCES GiaoCa(maGiaoCa),
    menhGia INT NOT NULL, -- 500000, 200000, ...
    soLuong INT NOT NULL,
    PRIMARY KEY (maGiaoCa, menhGia)
);
GO

PRINT '✓ Migration v3 created successfully.';
