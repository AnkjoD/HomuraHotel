SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
GO
-- Optional synthetic sample data, only for a fresh local HotelMs installation.
USE HotelMs;
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
IF EXISTS(SELECT 1 FROM DatPhong) THROW 50000, 'Sample seeding requires an empty booking database', 1;
DECLARE @now DATETIME = DATEADD(HOUR, 7, SYSUTCDATETIME());

INSERT INTO Phong(maPhong,tang,maHuongNhin,maLoaiPhong,trangThai) VALUES
('P102',1,'HN01','LP002','OCCUPIED'),('P103',1,'HN01','LP001','CLEANING'),('P104',1,'HN01','LP002','AVAILABLE'),
('P201',2,'HN03','LP002','AVAILABLE'),('P202',2,'HN03','LP003','OCCUPIED'),('P203',2,'HN03','LP003','MAINTENANCE'),
('P204',2,'HN03','LP002','AVAILABLE'),('P302',3,'HN06','LP004','AVAILABLE');
INSERT INTO KhachHang(maKhachHang,hoTen,sdt,cccd,quocTich) VALUES
('KH005',N'Trần Minh Anh - dữ liệu mẫu','0919900005','099000000005',N'Việt Nam'),
('KH006',N'Lê Hoàng Nam - dữ liệu mẫu','0919900006','099000000006',N'Việt Nam'),
('KH007',N'Phạm Thu Hà - dữ liệu mẫu','0919900007','099000000007',N'Việt Nam');
INSERT INTO DatPhong(maDatPhong,maKhachHang,maNhanVien,ngayDat,ngayNhanDuKien,ngayTraDuKien,soNguoi,tienDatCoc,trangThai,ghiChu) VALUES
('SEED_ACTIVE_1','KH001','recept_01',DATEADD(day,-2,@now),DATEADD(day,-1,@now),DATEADD(day,2,@now),2,200000,'CHECKED_IN',N'Dữ liệu mẫu kiểm tra giao diện'),
('SEED_ACTIVE_2','KH005','recept_01',DATEADD(day,-1,@now),@now,DATEADD(day,1,@now),2,0,'CHECKED_IN',N'Dữ liệu mẫu kiểm tra giao diện'),
('SEED_BOOK_1','KH006','recept_01',@now,DATEADD(day,1,@now),DATEADD(day,3,@now),2,300000,'CONFIRMED',N'Dữ liệu mẫu đặt trước'),
('SEED_PAID_1','KH007','recept_01',DATEADD(day,-4,@now),DATEADD(day,-3,@now),DATEADD(day,-1,@now),1,0,'CHECKED_OUT',N'Dữ liệu mẫu đã thanh toán');
INSERT INTO ChiTietDatPhong(maChiTiet,maDatPhong,maPhong,maKhachHang,ngayNhanDuKien,ngayTraDuKien,ngayNhanThucTe,ngayTraThucTe,giaThucTeChot,daThanhToan) VALUES
('SEED_CT1','SEED_ACTIVE_1','P102','KH001',DATEADD(day,-1,@now),DATEADD(day,2,@now),DATEADD(day,-1,@now),NULL,600000,0),
('SEED_CT2','SEED_ACTIVE_2','P202','KH005',@now,DATEADD(day,1,@now),@now,NULL,900000,0),
('SEED_CT3','SEED_BOOK_1','P104','KH006',DATEADD(day,1,@now),DATEADD(day,3,@now),NULL,NULL,600000,0),
('SEED_CT4','SEED_PAID_1','P301','KH007',DATEADD(day,-3,@now),DATEADD(day,-1,@now),DATEADD(day,-3,@now),DATEADD(day,-1,@now),900000,1);
INSERT INTO SuDungDichVu(maSuDung,maChiTiet,maDichVu,soLuong,donGiaLucDung,thoiGianDung) VALUES('SEED_SERVICE_1','SEED_CT1','DV001',2,15000,@now);
INSERT INTO HoaDon(maHoaDon,maDatPhong,maNhanVien,ngayLap,tongTienPhong,tongThanhToan,trangThai,phuongThucThanhToan) VALUES('SEED_BILL_1','SEED_PAID_1','recept_01',DATEADD(day,-1,@now),1800000,1800000,'PAID','CASH');
INSERT INTO ChiTietHoaDon(maHoaDon,loai,moTa,soLuong,donGia,thanhTien) VALUES('SEED_BILL_1','PHONG',N'Phòng P301 (2 đêm)',2,900000,1800000);
COMMIT TRANSACTION;
GO
SELECT (SELECT COUNT(*) FROM Phong) AS rooms,(SELECT COUNT(*) FROM KhachHang) AS customers,(SELECT COUNT(*) FROM DatPhong) AS bookings,(SELECT COUNT(*) FROM HoaDon) AS invoices;
