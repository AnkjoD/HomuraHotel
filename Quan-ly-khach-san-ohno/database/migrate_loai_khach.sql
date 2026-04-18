-- ======================================================
-- MIGRATION: Thêm phân loại Khách lẻ / Khách đoàn
-- Chạy script này trên database HotelMs hiện có
-- ======================================================

USE HotelMs;
GO

-- 1. Thêm cột loaiKhach (nếu chưa có)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DatPhong') AND name = 'loaiKhach')
BEGIN
    ALTER TABLE DatPhong ADD loaiKhach VARCHAR(10) DEFAULT 'CA_NHAN';
    ALTER TABLE DatPhong ADD CONSTRAINT CK_DatPhong_LoaiKhach CHECK (loaiKhach IN ('CA_NHAN', 'DOAN'));
    PRINT N'✓ Đã thêm cột loaiKhach vào bảng DatPhong';
END
ELSE
    PRINT N'⏭ Cột loaiKhach đã tồn tại, bỏ qua.';
GO

-- 2. Thêm cột tenDoan (nếu chưa có)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('DatPhong') AND name = 'tenDoan')
BEGIN
    ALTER TABLE DatPhong ADD tenDoan NVARCHAR(200);
    PRINT N'✓ Đã thêm cột tenDoan vào bảng DatPhong';
END
ELSE
    PRINT N'⏭ Cột tenDoan đã tồn tại, bỏ qua.';
GO

-- 3. Cập nhật các đơn đặt phòng cũ: mặc định là khách lẻ
UPDATE DatPhong SET loaiKhach = 'CA_NHAN' WHERE loaiKhach IS NULL;
GO

PRINT N'✓ Migration hoàn tất: Hệ thống đã hỗ trợ phân loại Khách lẻ / Khách đoàn';
GO
