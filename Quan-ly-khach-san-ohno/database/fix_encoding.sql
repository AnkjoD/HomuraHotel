USE HotelMs;
GO

UPDATE KenhDatPhong SET tenKenh = N'Đặt trực tiếp', moTa = N'Khách walk-in hoặc gọi điện trực tiếp' WHERE maKenh = 'DIRECT';
UPDATE KenhDatPhong SET tenKenh = N'Website khách sạn', moTa = N'Đặt qua website chính thức' WHERE maKenh = 'WEBSITE';
UPDATE KenhDatPhong SET tenKenh = N'Hợp đồng doanh nghiệp', moTa = N'Đặt qua hợp đồng doanh nghiệp' WHERE maKenh = 'CORPORATE';
UPDATE KenhDatPhong SET tenKenh = N'Đại lý du lịch', moTa = N'Đặt qua công ty du lịch / đại lý' WHERE maKenh = 'TRAVEL_AGT';

UPDATE BangGia SET tenBangGia = N'Giá doanh nghiệp 2024', moTa = N'Giảm 20% cho khách doanh nghiệp có hợp đồng' WHERE maBangGia = 'BG_CORP';
UPDATE BangGia SET tenBangGia = N'Giá OTA 2024', moTa = N'Giá niêm yết trên các kênh OTA (tăng 10% để bù hoa hồng)' WHERE maBangGia = 'BG_OTA';
UPDATE BangGia SET tenBangGia = N'Giá mùa cao điểm (Tết 2025)', moTa = N'Giá cao điểm Tết Nguyên Đán - tăng 50%' WHERE maBangGia = 'BG_PEAK';

UPDATE KhachHang SET loaiGiayTo = 'PASSPORT', soHoChieu = 'US1234567', soVisa = 'VN-VISA-001', ngayHetHanVisa = '2025-12-31', noiCapHoChieu = N'USA', ngayNhapCanh = '2024-11-01'
WHERE maKhachHang = 'KH004' AND quocTich = N'Mỹ';

UPDATE KhachHang SET loaiGiayTo = 'PASSPORT', soHoChieu = 'KR9876543', soVisa = 'VN-VISA-002', ngayHetHanVisa = '2025-06-30', noiCapHoChieu = N'South Korea', ngayNhapCanh = '2024-10-15'
WHERE maKhachHang = 'KH005' AND quocTich = N'Hàn Quốc';

UPDATE KhachHang SET loaiGiayTo = 'CCCD' WHERE loaiGiayTo IS NULL AND quocTich = N'Việt Nam';
GO
