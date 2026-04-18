-- ======================================================
-- MIGRATION: Nâng cấp PMS – Thêm các tính năng thực tế
-- 1. Kênh đặt phòng (Booking Channel)
-- 2. Rate Plan nâng cao
-- 3. No-show handling
-- 4. Waitlist / Overbooking
-- 5. Hộ chiếu / Visa cho khách nước ngoài
-- ======================================================

USE HotelMs;
GO

-- =====================================================
-- 1. KÊNH ĐẶT PHÒNG (Booking Channel)
-- =====================================================

-- Bảng danh mục kênh đặt phòng
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'KenhDatPhong')
BEGIN
    CREATE TABLE KenhDatPhong (
        maKenh VARCHAR(20) PRIMARY KEY,
        tenKenh NVARCHAR(100) NOT NULL,
        loaiKenh VARCHAR(20) CHECK (loaiKenh IN ('DIRECT', 'OTA', 'CORPORATE', 'TRAVEL_AGENT', 'OTHER')),
        heSoHoaHong DECIMAL(5,2) DEFAULT 0, -- % hoa hồng trả cho kênh OTA
        trangThai BIT DEFAULT 1,
        moTa NVARCHAR(MAX)
    );

    -- Seed dữ liệu kênh
    INSERT INTO KenhDatPhong VALUES ('DIRECT',     N'Đặt trực tiếp',      'DIRECT',       0,    1, N'Khách walk-in hoặc gọi điện trực tiếp');
    INSERT INTO KenhDatPhong VALUES ('WEBSITE',    N'Website khách sạn',  'DIRECT',       0,    1, N'Đặt qua website chính thức');
    INSERT INTO KenhDatPhong VALUES ('BOOKING',    N'Booking.com',        'OTA',          15,   1, N'Đặt qua Booking.com');
    INSERT INTO KenhDatPhong VALUES ('AGODA',      N'Agoda',              'OTA',          18,   1, N'Đặt qua Agoda');
    INSERT INTO KenhDatPhong VALUES ('TRAVELOKA',  N'Traveloka',          'OTA',          12,   1, N'Đặt qua Traveloka');
    INSERT INTO KenhDatPhong VALUES ('EXPEDIA',    N'Expedia',            'OTA',          15,   1, N'Đặt qua Expedia');
    INSERT INTO KenhDatPhong VALUES ('CORPORATE',  N'Hợp đồng doanh nghiệp', 'CORPORATE', 0,   1, N'Đặt qua hợp đồng doanh nghiệp');
    INSERT INTO KenhDatPhong VALUES ('TRAVEL_AGT', N'Đại lý du lịch',    'TRAVEL_AGENT', 10,   1, N'Đặt qua công ty du lịch / đại lý');

    PRINT N'✓ Tạo bảng KenhDatPhong thành công';
END
GO

-- Thêm cột kênh đặt phòng vào DatPhong
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'DatPhong' AND COLUMN_NAME = 'maKenh')
BEGIN
    ALTER TABLE DatPhong ADD maKenh VARCHAR(20) DEFAULT 'DIRECT';
    -- Không thêm FK constraint vì dữ liệu cũ có thể chưa có kênh → dùng trigger hoặc app logic
    PRINT N'✓ Thêm cột maKenh vào DatPhong';
END
GO

-- Thêm mã đặt phòng từ kênh OTA (Confirmation Number từ Booking.com, Agoda...)
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'DatPhong' AND COLUMN_NAME = 'maXacNhanKenh')
BEGIN
    ALTER TABLE DatPhong ADD maXacNhanKenh NVARCHAR(100) NULL;
    PRINT N'✓ Thêm cột maXacNhanKenh (OTA Confirmation Number) vào DatPhong';
END
GO

-- =====================================================
-- 2. RATE PLAN NÂNG CAO
-- =====================================================

-- Thêm loại bảng giá (Rate Type): RACK, SEASONAL, CORPORATE, OTA, PROMOTION
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BangGia' AND COLUMN_NAME = 'loaiBangGia')
BEGIN
    ALTER TABLE BangGia ADD loaiBangGia VARCHAR(20) DEFAULT 'RACK'
        CHECK (loaiBangGia IN ('RACK', 'SEASONAL', 'CORPORATE', 'OTA', 'PROMOTION'));
    PRINT N'✓ Thêm cột loaiBangGia';
END
GO

-- Đối tượng áp dụng (target): ALL, CA_NHAN, DOAN, CORPORATE, OTA_CHANNEL
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BangGia' AND COLUMN_NAME = 'doiTuongApDung')
BEGIN
    ALTER TABLE BangGia ADD doiTuongApDung VARCHAR(20) DEFAULT 'ALL'
        CHECK (doiTuongApDung IN ('ALL', 'CA_NHAN', 'DOAN', 'CORPORATE', 'VIP'));
    PRINT N'✓ Thêm cột doiTuongApDung';
END
GO

-- Mức ưu tiên (priority) – số nhỏ hơn = ưu tiên cao hơn
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BangGia' AND COLUMN_NAME = 'mucUuTien')
BEGIN
    ALTER TABLE BangGia ADD mucUuTien INT DEFAULT 100;
    PRINT N'✓ Thêm cột mucUuTien';
END
GO

-- Ghi chú/mô tả rate plan
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'BangGia' AND COLUMN_NAME = 'moTa')
BEGIN
    ALTER TABLE BangGia ADD moTa NVARCHAR(MAX);
    PRINT N'✓ Thêm cột moTa vào BangGia';
END
GO

-- Thêm giá cuối tuần (giá riêng cho T7, CN)
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'ChiTietBangGia' AND COLUMN_NAME = 'giaCuoiTuan')
BEGIN
    ALTER TABLE ChiTietBangGia ADD giaCuoiTuan DECIMAL(18,2) DEFAULT 0;
    PRINT N'✓ Thêm cột giaCuoiTuan vào ChiTietBangGia';
END
GO

-- Cập nhật dữ liệu mẫu BangGia hiện tại
UPDATE BangGia SET loaiBangGia = 'RACK', doiTuongApDung = 'ALL', mucUuTien = 100 WHERE maBangGia = 'BG2024';
GO

-- Thêm thêm Rate Plan mẫu
IF NOT EXISTS (SELECT * FROM BangGia WHERE maBangGia = 'BG_CORP')
BEGIN
    INSERT INTO BangGia (maBangGia, tenBangGia, ngayBatDau, ngayKetThuc, isKichHoat, loaiBangGia, doiTuongApDung, mucUuTien, moTa)
    VALUES ('BG_CORP', N'Giá doanh nghiệp 2024', '2024-01-01', '2024-12-31', 1, 'CORPORATE', 'CORPORATE', 50, N'Giảm 20% cho khách doanh nghiệp có hợp đồng');
    
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_CORP', 'LP001', 320000, 56000, 25000, 320000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_CORP', 'LP002', 480000, 80000, 40000, 480000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_CORP', 'LP003', 720000, 120000, 60000, 720000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_CORP', 'LP004', 960000, 160000, 80000, 960000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_CORP', 'LP005', 1600000, 280000, 150000, 1600000);
    
    PRINT N'✓ Thêm bảng giá doanh nghiệp mẫu';
END
GO

IF NOT EXISTS (SELECT * FROM BangGia WHERE maBangGia = 'BG_OTA')
BEGIN
    INSERT INTO BangGia (maBangGia, tenBangGia, ngayBatDau, ngayKetThuc, isKichHoat, loaiBangGia, doiTuongApDung, mucUuTien, moTa)
    VALUES ('BG_OTA', N'Giá OTA 2024', '2024-01-01', '2024-12-31', 1, 'OTA', 'ALL', 80, N'Giá niêm yết trên các kênh OTA (tăng 10% để bù hoa hồng)');
    
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_OTA', 'LP001', 440000, 77000, 25000, 480000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_OTA', 'LP002', 660000, 110000, 40000, 720000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_OTA', 'LP003', 990000, 165000, 60000, 1080000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_OTA', 'LP004', 1320000, 220000, 80000, 1440000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_OTA', 'LP005', 2200000, 385000, 150000, 2400000);
    
    PRINT N'✓ Thêm bảng giá OTA mẫu';
END
GO

IF NOT EXISTS (SELECT * FROM BangGia WHERE maBangGia = 'BG_PEAK')
BEGIN
    INSERT INTO BangGia (maBangGia, tenBangGia, ngayBatDau, ngayKetThuc, isKichHoat, loaiBangGia, doiTuongApDung, mucUuTien, moTa)
    VALUES ('BG_PEAK', N'Giá mùa cao điểm (Tết 2025)', '2025-01-25', '2025-02-10', 1, 'SEASONAL', 'ALL', 30, N'Giá cao điểm Tết Nguyên Đán - tăng 50%');
    
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_PEAK', 'LP001', 600000, 100000, 40000, 600000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_PEAK', 'LP002', 900000, 150000, 60000, 900000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_PEAK', 'LP003', 1350000, 225000, 90000, 1350000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_PEAK', 'LP004', 1800000, 300000, 120000, 1800000);
    INSERT INTO ChiTietBangGia (maBangGia, maLoaiPhong, giaTheoNgay, giaTheoGio, phuPhiTraTre, giaCuoiTuan) VALUES ('BG_PEAK', 'LP005', 3000000, 500000, 200000, 3000000);
    
    PRINT N'✓ Thêm bảng giá mùa cao điểm mẫu';
END
GO

-- =====================================================
-- 3. NO-SHOW HANDLING
-- =====================================================

-- Mở rộng CHECK constraint cho DatPhong.trangThai để thêm NO_SHOW và WAITLIST
-- SQL Server: phải drop constraint cũ rồi tạo lại
DECLARE @constraintName NVARCHAR(200);
SELECT @constraintName = name FROM sys.check_constraints 
WHERE parent_object_id = OBJECT_ID('DatPhong') AND definition LIKE '%trangThai%';

IF @constraintName IS NOT NULL
BEGIN
    EXEC('ALTER TABLE DatPhong DROP CONSTRAINT ' + @constraintName);
END

ALTER TABLE DatPhong ADD CONSTRAINT CK_DatPhong_TrangThai 
    CHECK (trangThai IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'NO_SHOW', 'WAITLIST'));

PRINT N'✓ Cập nhật CHECK constraint DatPhong.trangThai (thêm NO_SHOW, WAITLIST)';
GO

-- Thêm cột phí no-show (số tiền phạt khi khách không đến)
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'DatPhong' AND COLUMN_NAME = 'phiNoShow')
BEGIN
    ALTER TABLE DatPhong ADD phiNoShow DECIMAL(18,2) DEFAULT 0;
    PRINT N'✓ Thêm cột phiNoShow';
END
GO

-- Thêm cột deadline check-in (nếu quá giờ này → tự động chuyển no-show)
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'DatPhong' AND COLUMN_NAME = 'hanCheckIn')
BEGIN
    ALTER TABLE DatPhong ADD hanCheckIn DATETIME NULL;
    PRINT N'✓ Thêm cột hanCheckIn (deadline check-in)';
END
GO

-- =====================================================
-- 4. WAITLIST / OVERBOOKING
-- =====================================================
-- (Trạng thái WAITLIST đã thêm ở bước 3)
-- Thêm cột độ ưu tiên waitlist
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'DatPhong' AND COLUMN_NAME = 'thuTuWaitlist')
BEGIN
    ALTER TABLE DatPhong ADD thuTuWaitlist INT DEFAULT 0;
    PRINT N'✓ Thêm cột thuTuWaitlist';
END
GO

-- =====================================================
-- 5. HỘ CHIẾU / VISA CHO KHÁCH NƯỚC NGOÀI
-- =====================================================

-- Loại giấy tờ: CCCD, PASSPORT, CMND
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'KhachHang' AND COLUMN_NAME = 'loaiGiayTo')
BEGIN
    ALTER TABLE KhachHang ADD loaiGiayTo VARCHAR(20) DEFAULT 'CCCD'
        CHECK (loaiGiayTo IN ('CCCD', 'CMND', 'PASSPORT'));
    PRINT N'✓ Thêm cột loaiGiayTo';
END
GO

-- Số hộ chiếu (passport number) — riêng biệt với CCCD
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'KhachHang' AND COLUMN_NAME = 'soHoChieu')
BEGIN
    ALTER TABLE KhachHang ADD soHoChieu VARCHAR(30) NULL;
    PRINT N'✓ Thêm cột soHoChieu';
END
GO

-- Số visa
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'KhachHang' AND COLUMN_NAME = 'soVisa')
BEGIN
    ALTER TABLE KhachHang ADD soVisa VARCHAR(30) NULL;
    PRINT N'✓ Thêm cột soVisa';
END
GO

-- Ngày hết hạn visa
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'KhachHang' AND COLUMN_NAME = 'ngayHetHanVisa')
BEGIN
    ALTER TABLE KhachHang ADD ngayHetHanVisa DATE NULL;
    PRINT N'✓ Thêm cột ngayHetHanVisa';
END
GO

-- Nơi cấp hộ chiếu
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'KhachHang' AND COLUMN_NAME = 'noiCapHoChieu')
BEGIN
    ALTER TABLE KhachHang ADD noiCapHoChieu NVARCHAR(100) NULL;
    PRINT N'✓ Thêm cột noiCapHoChieu';
END
GO

-- Ngày nhập cảnh
IF NOT EXISTS (SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'KhachHang' AND COLUMN_NAME = 'ngayNhapCanh')
BEGIN
    ALTER TABLE KhachHang ADD ngayNhapCanh DATE NULL;
    PRINT N'✓ Thêm cột ngayNhapCanh';
END
GO

-- Cập nhật khách nước ngoài mẫu
UPDATE KhachHang SET loaiGiayTo = 'PASSPORT', soHoChieu = 'US1234567', soVisa = 'VN-VISA-001', ngayHetHanVisa = '2025-12-31', noiCapHoChieu = N'USA', ngayNhapCanh = '2024-11-01'
WHERE maKhachHang = 'KH004' AND quocTich = N'Mỹ';

UPDATE KhachHang SET loaiGiayTo = 'PASSPORT', soHoChieu = 'KR9876543', soVisa = 'VN-VISA-002', ngayHetHanVisa = '2025-06-30', noiCapHoChieu = N'South Korea', ngayNhapCanh = '2024-10-15'
WHERE maKhachHang = 'KH005' AND quocTich = N'Hàn Quốc';

UPDATE KhachHang SET loaiGiayTo = 'CCCD' WHERE loaiGiayTo IS NULL AND quocTich = N'Việt Nam';
GO

-- =====================================================
-- INDEXES bổ sung
-- =====================================================
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_DATPHONG_KENH')
    CREATE INDEX IX_DATPHONG_KENH ON DatPhong(maKenh);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_BANGIA_LOAI')
    CREATE INDEX IX_BANGIA_LOAI ON BangGia(loaiBangGia);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_KHACH_PASSPORT')
    CREATE INDEX IX_KHACH_PASSPORT ON KhachHang(soHoChieu);

PRINT N'✓ MIGRATION HOÀN TẤT – Tất cả tính năng PMS mới đã được thêm!';
GO
