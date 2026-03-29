package service;

import dao.*;
import entity.*;
import entity.enums.*;

import java.time.LocalDateTime;
import java.util.*;

public class ThuePhongService {

    private final ChiTietDatPhongDAO ctdpDAO    = new ChiTietDatPhongDAO();
    private final PhongDAO            phongDAO   = new PhongDAO();
    private final HoaDonDAO           hoaDonDAO  = new HoaDonDAO();
    private final ChiTietHoaDonDAO    cthdDAO    = new ChiTietHoaDonDAO();
    private final SuDungDichVuDAO     sddvDAO    = new SuDungDichVuDAO();

    // ---- Check-in ----
    public String checkIn(ChiTietDatPhong ct) {
        Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
        if (phong == null) return "Phòng không tồn tại!";
        if (phong.getTrangThai() != TrangThaiPhong.AVAILABLE) 
            return "Phòng " + ct.getPhong().getMaPhong() + " không sẵn sàng!";

        ct.setMaChiTiet(ctdpDAO.generateMaChiTiet());
        ct.setNgayNhanThucTe(LocalDateTime.now());
        
        boolean ok = ctdpDAO.insert(ct);
        if (!ok) return "Lỗi tạo chi tiết thuê phòng!";

        phong.setTrangThai(TrangThaiPhong.OCCUPIED);
        phongDAO.update(phong);
        return null;
    }

    // ---- Thêm dịch vụ ----
    public String themDichVu(SuDungDichVu sddv) {
        ChiTietDatPhong ct = ctdpDAO.getById(sddv.getCtdp().getMaChiTiet());
        if (ct == null) return "Không tìm thấy chi tiết thuê phòng!";
        
        return sddvDAO.insert(sddv) ? null : "Lỗi thêm dịch vụ!";
    }

    // Overload for legacy UI
    public String themDichVu(String maChiTiet, String maDV, int sl, double gia) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        if (ct == null) return "Không tìm thấy chi tiết!";
        DichVu dv = new DichVuDAO().getById(maDV);
        if (dv == null) return "Dịch vụ không tồn tại!";
        
        SuDungDichVu s = new SuDungDichVu();
        s.setCtdp(ct);
        s.setDichVu(dv);
        s.setSoLuong(sl);
        s.setDonGiaLuu(gia);
        s.setThoiDiem(LocalDateTime.now());
        return themDichVu(s);
    }

    // ---- Check-out ----
    public String checkOut(String maChiTiet, String maNV) {
        return checkOut(maChiTiet, maNV, "PAID");
    }

    public String checkOut(String maChiTiet, String maNV, String status) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        if (ct == null) return "Không tìm thấy chi tiết thuê phòng!";

        ct.setNgayTraThucTe(LocalDateTime.now());
        ctdpDAO.update(ct);
        
        // Tính tiền phòng theo số ngày thực tế
        long soNgay = Math.max(1, ct.tinhSoNgayO());
        double donGiaThang = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
        double tienPhong = donGiaThang * soNgay;
        
        // Tính phí phát sinh (check-in sớm/check-out muộn)
        double phuPhi = 0;
        if (ct.getNgayNhanThucTe() != null) {
            int gioNhan = ct.getNgayNhanThucTe().getHour();
            if (gioNhan < 12) phuPhi += 50000;
        }
        if (ct.getNgayTraThucTe() != null) {
            int gioTra = ct.getNgayTraThucTe().getHour();
            if (gioTra > 13) phuPhi += 50000;
        }
        phuPhi += ct.getPhuPhiPhatSinh();
        
        // Tạo hóa đơn
        HoaDon hd = new HoaDon();
        hd.setMaHoaDon(hoaDonDAO.generateMaHD());
        if (ct.getDatPhong() != null) hd.setDatPhong(ct.getDatPhong());
        hd.setNhanVien(new NhanVienDAO().getById(maNV));
        hd.setNgayLap(LocalDateTime.now());
        hd.setTrangThai(TrangThaiThanhToan.PAID);
        hd.setTongTienPhong(tienPhong + phuPhi);
        
        // Tạo chi tiết hóa đơn cho phòng
        ChiTietHoaDon cthdPhong = new ChiTietHoaDon();
        cthdPhong.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
        cthdPhong.setHoaDon(hd);
        cthdPhong.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
        cthdPhong.setSoLuong((int)soNgay);
        cthdPhong.setDonGia(donGiaThang);
        cthdPhong.setNoiDung("Thuê phòng " + ct.getPhong().getMaPhong() + " (" + soNgay + " ngày)");
        cthdPhong.setThanhTien(tienPhong);

        // Tạo chi tiết hóa đơn cho dịch vụ
        List<SuDungDichVu> dsDichVu = sddvDAO.getByChiTietDatPhong(maChiTiet);
        double tongDichVu = 0;
        for (SuDungDichVu sddv : dsDichVu) {
            tongDichVu += sddv.getDonGiaLuu() * sddv.getSoLuong();
        }
        
        hd.setTongTienDichVu(tongDichVu);
        
        // Tính khuyến mãi (nếu có)
        double tienGiam = 0;
        if (hd.getKhuyenMai() != null) {
            double tongTienTruocGiam = hd.getTongTienPhong() + hd.getTongTienDichVu();
            tienGiam = hd.getKhuyenMai().tinhSoTienGiam(tongTienTruocGiam);
        }
        hd.setTienGiamKhuyenMai(tienGiam);
        
        // ★ Trừ đặt cọc khi tính tổng thanh toán
        double tienDatCoc = 0;
        if (ct.getDatPhong() != null) {
            tienDatCoc = ct.getDatPhong().getTienDatCoc();
        }
        double tongThanhToan = hd.getTongTienPhong() + hd.getTongTienDichVu() - tienGiam - tienDatCoc;
        hd.setTongThanhToan(Math.max(0, tongThanhToan));
        
        hoaDonDAO.insert(hd);

        // ★ Cập nhật trạng thái DatPhong → CHECKED_OUT
        if (ct.getDatPhong() != null) {
            DatPhong dp = ct.getDatPhong();
            dp.setTrangThai(TrangThaiDatPhong.CHECKED_OUT);
            new DatPhongDAO().update(dp);
        }

        // Cập nhật phòng → CLEANING
        Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
        if (phong != null) {
            phong.setTrangThai(TrangThaiPhong.CLEANING);
            phongDAO.update(phong);
        }

        return null;
    }

    public ChiTietDatPhong getById(String maChiTiet) { return ctdpDAO.getById(maChiTiet); }

    public List<SuDungDichVu> getDichVuByChiTiet(String maChiTiet) {
        return sddvDAO.getByChiTietDatPhong(maChiTiet);
    }

    public List<SuDungDichVu> getDichVuByThue(String maChiTiet) {
        return getDichVuByChiTiet(maChiTiet);
    }

    public ChiTietDatPhong getActiveByPhong(String soPhong) {
        for (ChiTietDatPhong c : ctdpDAO.getAll()) {
            if (c.getPhong() != null && c.getPhong().getMaPhong().equals(soPhong) && c.getNgayTraThucTe() == null) {
                return c;
            }
        }
        return null;
    }

    public long tinhSoNgay(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        java.time.Duration duration = java.time.Duration.between(start, end);
        long days = duration.toDays();
        return Math.max(1, days + (duration.toHoursPart() > 2 ? 1 : 0)); // Simple rounding: >2h counts as extra day
    }

    public long tinhSoNgay(Date start, Date end) {
        if (start == null || end == null) return 0;
        return tinhSoNgay(
            start.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
            end.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        );
    }

    // ---- Gia hạn lưu trú ----
    public String extendStay(String maChiTiet, LocalDateTime newCheckout) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        if (ct == null) return "Không tìm thấy chi tiết thuê phòng!";
        if (ct.getDatPhong() == null) return "Không tìm thấy đơn đặt phòng!";
        
        DatPhong dp = ct.getDatPhong();
        if (newCheckout.isBefore(LocalDateTime.now())) return "Ngày trả mới phải sau hiện tại!";
        
        // Check xung đột với đặt phòng khác
        PhongDAO pDAO = new PhongDAO();
        boolean available = pDAO.isRoomAvailable(ct.getPhong().getMaPhong(), 
            dp.getNgayTraDuKien() != null ? dp.getNgayTraDuKien() : LocalDateTime.now(), 
            newCheckout);
        if (!available) return "Phòng đã có đặt trước trong khoảng thời gian gia hạn!";
        
        dp.setNgayTraDuKien(newCheckout);
        // Tính lại tổng tiền tạm tính
        long soNgay = tinhSoNgay(dp.getNgayNhanDuKien(), newCheckout);
        double giaPhong = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
        dp.setTongTienTamTinh(giaPhong * soNgay);
        
        return new DatPhongDAO().update(dp) ? null : "Lỗi cập nhật gia hạn!";
    }

    // ---- Lấy tiền đặt cọc cho phòng đang thuê ----
    public double getDepositForRoom(String soPhong) {
        ChiTietDatPhong ct = getActiveByPhong(soPhong);
        if (ct != null && ct.getDatPhong() != null) {
            return ct.getDatPhong().getTienDatCoc();
        }
        return 0;
    }
}
