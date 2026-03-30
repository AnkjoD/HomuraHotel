package service;

import dao.*;
import database.DatabaseConnection;
import entity.*;
import entity.enums.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThuePhongService {

    private static final Logger LOGGER = Logger.getLogger(ThuePhongService.class.getName());

    private final ChiTietDatPhongDAO ctdpDAO  = new ChiTietDatPhongDAO();
    private final PhongDAO           phongDAO = new PhongDAO();
    private final HoaDonDAO          hoaDonDAO = new HoaDonDAO();
    private final ChiTietHoaDonDAO   cthdDAO  = new ChiTietHoaDonDAO();
    private final SuDungDichVuDAO    sddvDAO  = new SuDungDichVuDAO();
    private final DatPhongDAO        datPhongDAO = new DatPhongDAO();
    private final NhanVienDAO        nhanVienDAO = new NhanVienDAO();
    private final DichVuDAO          dichVuDAO   = new DichVuDAO();

    // ---- Check-in ----
    public String checkIn(ChiTietDatPhong ct) {
        if (ct == null || ct.getPhong() == null) return "Thông tin phòng không hợp lệ!";
        
        Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
        if (phong == null) return "Phòng không tồn tại!";
        if (phong.getTrangThai() != TrangThaiPhong.AVAILABLE) 
            return "Phòng " + ct.getPhong().getMaPhong() + " không sẵn sàng (trạng thái: " + phong.getTrangThaiString() + ")!";

        // Validate sức chứa
        if (ct.getDatPhong() != null && phong.getLoaiPhong() != null) {
            int sucChua = phong.getLoaiPhong().getSucChua();
            if (ct.getDatPhong().getSoLuongKhach() > sucChua) {
                return "Số khách (" + ct.getDatPhong().getSoLuongKhach() + ") vượt sức chứa phòng (" + sucChua + ")!";
            }
        }

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
        if (sddv == null || sddv.getCtdp() == null) return "Thông tin dịch vụ không hợp lệ!";
        
        ChiTietDatPhong ct = ctdpDAO.getById(sddv.getCtdp().getMaChiTiet());
        if (ct == null) return "Không tìm thấy chi tiết thuê phòng!";
        
        return sddvDAO.insert(sddv) ? null : "Lỗi thêm dịch vụ!";
    }

    // Overload for legacy UI
    public String themDichVu(String maChiTiet, String maDV, int sl, double gia) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        if (ct == null) return "Không tìm thấy chi tiết!";
        DichVu dv = dichVuDAO.getById(maDV);
        if (dv == null) return "Dịch vụ không tồn tại!";
        
        SuDungDichVu s = new SuDungDichVu();
        s.setCtdp(ct);
        s.setDichVu(dv);
        s.setSoLuong(sl);
        s.setDonGiaLuu(gia);
        s.setThoiDiem(LocalDateTime.now());
        return themDichVu(s);
    }

    // ---- Gia hạn phòng ----
    public String extendStay(String maChiTiet, LocalDateTime newLDT) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        if (ct == null || ct.getDatPhong() == null) return "Không tìm thấy thông tin thuê phòng!";
        DatPhong dp = ct.getDatPhong();
        
        if (newLDT.isBefore(dp.getNgayNhanDuKien())) {
            return "Ngày trả mới không thể trước ngày nhận!";
        }
        
        dp.setNgayTraDuKien(newLDT);
        boolean ok = datPhongDAO.update(dp);
        return ok ? null : "Lỗi cập nhật CSDL!";
    }

    // ---- Check-out (CÓ TRANSACTION) ----
    public String checkOut(String maChiTiet, String maNV) {
        return checkOut(maChiTiet, maNV, "PAID");
    }

    /**
     * Check-out phòng — toàn bộ thao tác được wrap trong Transaction.
     * Nếu bất kỳ bước nào lỗi → rollback tất cả.
     */
    public String checkOut(String maChiTiet, String maNV, String status) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        
        try {
            // ===== BẮT ĐẦU TRANSACTION =====
            dbConn.beginTransaction();

            ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
            if (ct == null) {
                dbConn.rollbackTransaction();
                return "Không tìm thấy chi tiết thuê phòng!";
            }

            ct.setNgayTraThucTe(LocalDateTime.now());
            
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
            hd.setNhanVien(nhanVienDAO.getById(maNV));
            hd.setNgayLap(LocalDateTime.now());
            hd.setTongTienPhong(tienPhong);
            
            // Tính chi tiết dịch vụ
            List<SuDungDichVu> dsDichVu = sddvDAO.getByChiTietDatPhong(maChiTiet);
            double tongDichVu = 0;
            for (SuDungDichVu sddv : dsDichVu) {
                 tongDichVu += sddv.getDonGiaLuu() * sddv.getSoLuong();
            }
            hd.setTongTienDichVu(tongDichVu);
            
            // Tính khuyến mãi
            double tienGiam = 0;
            if (hd.getKhuyenMai() != null) {
                double tongTienTruocGiam = hd.getTongTienPhong() + hd.getTongTienDichVu();
                tienGiam = hd.getKhuyenMai().tinhSoTienGiam(tongTienTruocGiam);
            }
            hd.setTienGiamKhuyenMai(tienGiam);
            
            // Tính tổng tiền
            double tongThanhToan = tienPhong + tongDichVu + phuPhi - tienGiam;
            hd.setTongThanhToan(tongThanhToan);
            
            // Lưu hóa đơn
            if (!hoaDonDAO.insert(hd)) {
                dbConn.rollbackTransaction();
                return "Lỗi tạo hóa đơn!";
            }

            // Lưu chi tiết hóa đơn — tiền phòng
            ChiTietHoaDon cthdPhong = new ChiTietHoaDon();
            cthdPhong.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
            cthdPhong.setHoaDon(hd);
            cthdPhong.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
            cthdPhong.setSoLuong((int) soNgay);
            cthdPhong.setDonGia(donGiaThang);
            cthdPhong.setNoiDung("Thuê phòng " + ct.getPhong().getMaPhong() + " (" + soNgay + " đêm)");
            cthdPhong.setThanhTien(tienPhong);
            if (!cthdDAO.insert(cthdPhong)) {
                dbConn.rollbackTransaction();
                return "Lỗi lưu chi tiết hóa đơn (phòng)!";
            }

            // Chi tiết hóa đơn — phụ phí
            if (phuPhi > 0) {
                ChiTietHoaDon cthdPhuPhi = new ChiTietHoaDon();
                cthdPhuPhi.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdPhuPhi.setHoaDon(hd);
                cthdPhuPhi.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                cthdPhuPhi.setSoLuong(1);
                cthdPhuPhi.setDonGia(phuPhi);
                cthdPhuPhi.setNoiDung("Phí check-in/out");
                cthdPhuPhi.setThanhTien(phuPhi);
                if (!cthdDAO.insert(cthdPhuPhi)) {
                    dbConn.rollbackTransaction();
                    return "Lỗi lưu chi tiết hóa đơn (phụ phí)!";
                }
            }

            // Chi tiết hóa đơn — dịch vụ
            for (SuDungDichVu sddv : dsDichVu) {
                 ChiTietHoaDon cthdDV = new ChiTietHoaDon();
                 cthdDV.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                 cthdDV.setHoaDon(hd);
                 cthdDV.setLoaiChiTiet(LoaiChiTietHoaDon.DICH_VU);
                 cthdDV.setSoLuong(sddv.getSoLuong());
                 cthdDV.setDonGia(sddv.getDonGiaLuu());
                 cthdDV.setNoiDung(sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dịch vụ");
                 cthdDV.setThanhTien(sddv.getDonGiaLuu() * sddv.getSoLuong());
                 if (!cthdDAO.insert(cthdDV)) {
                     dbConn.rollbackTransaction();
                     return "Lỗi lưu chi tiết hóa đơn (dịch vụ)!";
                 }
            }

            // Lưu ngày trả thực tế
            if (!ctdpDAO.update(ct)) {
                dbConn.rollbackTransaction();
                return "Lỗi cập nhật chi tiết đặt phòng!";
            }

            // Cập nhật trạng thái tổng của đặt phòng
            if (ct.getDatPhong() != null) {
                DatPhong dp = datPhongDAO.getById(ct.getDatPhong().getMaDatPhong());
                if (dp != null) {
                    boolean allCheckedOut = true;
                    for (ChiTietDatPhong c : ctdpDAO.getByDatPhong(dp.getMaDatPhong())) {
                        if (c.getNgayTraThucTe() == null) {
                            allCheckedOut = false;
                            break;
                        }
                    }
                    if (allCheckedOut) {
                        dp.setTrangThai(TrangThaiDatPhong.CHECKED_OUT);
                        datPhongDAO.update(dp);
                    }
                }
            }

            // Cập nhật phòng → Đang vệ sinh
            Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
            if (phong != null) {
                phong.setTrangThai(TrangThaiPhong.CLEANING);
                phongDAO.update(phong);
            }

            // ===== COMMIT TRANSACTION =====
            dbConn.commitTransaction();
            LOGGER.info("Check-out thành công: " + maChiTiet);
            return null;

        } catch (Exception e) {
            // ===== ROLLBACK NẾU CÓ BẤT KỲ LỖI NÀO =====
            dbConn.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi check-out, đã rollback", e);
            return "Lỗi hệ thống khi check-out: " + e.getMessage();
        }
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
        return Math.max(1, days + (duration.toHoursPart() > 2 ? 1 : 0));
    }

    public long tinhSoNgay(java.util.Date start, java.util.Date end) {
        if (start == null || end == null) return 0;
        return tinhSoNgay(
            start.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime(),
            end.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        );
    }
}
