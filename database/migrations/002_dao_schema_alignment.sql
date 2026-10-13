-- Non-destructive schema alignment for the Java DAO contracts.
USE HotelMs;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF COL_LENGTH('dbo.LoaiPhong','trangThai') IS NULL ALTER TABLE dbo.LoaiPhong ADD trangThai BIT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.BangGia','ngayCuoiTuan') IS NULL ALTER TABLE dbo.BangGia ADD ngayCuoiTuan VARCHAR(100) NULL DEFAULT 'SATURDAY,SUNDAY';
IF COL_LENGTH('dbo.KhachHang','trangThai') IS NULL ALTER TABLE dbo.KhachHang ADD trangThai BIT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.KhachHang','soLanO') IS NULL ALTER TABLE dbo.KhachHang ADD soLanO INT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.KhachHang','tongChiTieu') IS NULL ALTER TABLE dbo.KhachHang ADD tongChiTieu DECIMAL(18,2) NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.KhachHang','diemTichLuy') IS NULL ALTER TABLE dbo.KhachHang ADD diemTichLuy INT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.KhachHang','hangKhachHang') IS NULL ALTER TABLE dbo.KhachHang ADD hangKhachHang NVARCHAR(100) NULL DEFAULT N'Hạng Thường (Bronze)';
IF COL_LENGTH('dbo.DatPhong','maKhuyenMai') IS NULL ALTER TABLE dbo.DatPhong ADD maKhuyenMai VARCHAR(20) NULL;
IF COL_LENGTH('dbo.ChiTietDatPhong','ngayNhanDuKien') IS NULL ALTER TABLE dbo.ChiTietDatPhong ADD ngayNhanDuKien DATETIME NULL;
IF COL_LENGTH('dbo.ChiTietDatPhong','ngayTraDuKien') IS NULL ALTER TABLE dbo.ChiTietDatPhong ADD ngayTraDuKien DATETIME NULL;
IF COL_LENGTH('dbo.KhuyenMai','tenKhuyenMai') IS NULL ALTER TABLE dbo.KhuyenMai ADD tenKhuyenMai NVARCHAR(100) NULL;
IF COL_LENGTH('dbo.KhuyenMai','trangThai') IS NULL ALTER TABLE dbo.KhuyenMai ADD trangThai BIT NOT NULL DEFAULT 1;
IF COL_LENGTH('dbo.KhuyenMai','soLuong') IS NULL ALTER TABLE dbo.KhuyenMai ADD soLuong INT NOT NULL DEFAULT 999;
IF COL_LENGTH('dbo.KhuyenMai','daDung') IS NULL ALTER TABLE dbo.KhuyenMai ADD daDung INT NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.KhuyenMai','dieuKienToiThieu') IS NULL ALTER TABLE dbo.KhuyenMai ADD dieuKienToiThieu DECIMAL(18,2) NOT NULL DEFAULT 0;
IF COL_LENGTH('dbo.KhuyenMai','giaTriGiamToiDa') IS NULL ALTER TABLE dbo.KhuyenMai ADD giaTriGiamToiDa DECIMAL(18,2) NOT NULL DEFAULT 0;
COMMIT TRANSACTION;
GO
