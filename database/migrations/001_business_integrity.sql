-- Run once against an existing HotelMs database; preserves existing data.
USE HotelMs;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @constraint sysname;
SELECT @constraint = name FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.Phong') AND definition LIKE '%trangThai%';
IF @constraint IS NOT NULL EXEC('ALTER TABLE dbo.Phong DROP CONSTRAINT ' + QUOTENAME(@constraint));
ALTER TABLE dbo.Phong WITH CHECK ADD CONSTRAINT CK_Phong_TrangThai CHECK (trangThai IN ('AVAILABLE','OCCUPIED','MAINTENANCE','CLEANING','DELETED'));
SET @constraint = NULL;
SELECT @constraint = name FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID('dbo.HoaDon') AND definition LIKE '%trangThai%';
IF @constraint IS NOT NULL EXEC('ALTER TABLE dbo.HoaDon DROP CONSTRAINT ' + QUOTENAME(@constraint));
ALTER TABLE dbo.HoaDon WITH CHECK ADD CONSTRAINT CK_HoaDon_TrangThai CHECK (trangThai IN ('UNPAID','PARTIALLY_PAID','PAID','REFUNDED','CANCELLED'));
IF COL_LENGTH('dbo.ChiTietDatPhong','maChiTietTruoc') IS NULL
    ALTER TABLE dbo.ChiTietDatPhong ADD maChiTietTruoc VARCHAR(20) NULL;
COMMIT TRANSACTION;
GO
