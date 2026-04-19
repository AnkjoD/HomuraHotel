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
    private final BangGiaService     bangGiaService = new BangGiaService();

    // ---- Check-in ----
    public String checkIn(ChiTietDatPhong ct) {
        if (ct == null || ct.getPhong() == null) return "Thông tin phòng không hợp lệ!";
        
        Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
        if (phong == null) return "Phòng không tồn tại!";
        if (phong.getTrangThai() != TrangThaiPhong.AVAILABLE && phong.getTrangThai() != TrangThaiPhong.OCCUPIED) 
            return "Phòng " + ct.getPhong().getMaPhong() + " không sẵn sàng (trạng thái: " + phong.getTrangThaiString() + ")!";

        // Validate sức chứa (Cho phép vượt mức tối đa 2 người - đã xử lý cảnh báo ở giao diện UI)
        if (ct.getDatPhong() != null && phong.getLoaiPhong() != null) {
            int sucChua = phong.getLoaiPhong().getSucChua();
            int maxPax = sucChua + 2; // Cho phép nhồi thêm tối đa 2 người
            if (ct.getDatPhong().getSoLuongKhach() > maxPax && sucChua > 0) {
                return "Số khách (" + ct.getDatPhong().getSoLuongKhach() + ") vượt mức tối đa tuyệt đối cho phép (" + maxPax + ")!";
            }
        }

        // Handle existing bookings to avoid duplicate ChiTietDatPhong
        ChiTietDatPhong existingCt = null;
        if (ct.getDatPhong() != null && ct.getDatPhong().getMaDatPhong() != null) {
            for (ChiTietDatPhong act : ctdpDAO.getByDatPhong(ct.getDatPhong().getMaDatPhong())) {
                if (act.getPhong() != null && act.getPhong().getMaPhong().equals(phong.getMaPhong()) && act.getNgayTraThucTe() == null) {
                    existingCt = act;
                    break;
                }
            }
        }

        if (existingCt != null) {
            existingCt.setNgayNhanThucTe(LocalDateTime.now());
            if (ct.getGiaThucTeChot() > 0) existingCt.setGiaThucTeChot(ct.getGiaThucTeChot());
            ct.setMaChiTiet(existingCt.getMaChiTiet()); // Pass ID back to caller for service mapping
            if (!ctdpDAO.update(existingCt)) return "Lỗi cập nhật chi tiết đặt phòng!";
        } else {
            ct.setMaChiTiet(ctdpDAO.generateMaChiTiet());
            ct.setNgayNhanThucTe(LocalDateTime.now());
            if (ct.getGiaThucTeChot() <= 0) {
                ct.setGiaThucTeChot(bangGiaService.layGiaHienHanh(phong.getLoaiPhong().getMaLoaiPhong()));
            }
            if (!ctdpDAO.insert(ct)) return "Lỗi tạo chi tiết thuê phòng!";
        }

        phong.setTrangThai(TrangThaiPhong.OCCUPIED);
        phongDAO.update(phong);
        return null;
    }

    // ---- Group Check-in ----
    public String checkInGroup(DatPhong dp) {
        if (dp == null || dp.getDsChiTiet() == null || dp.getDsChiTiet().isEmpty()) return "Dữ liệu đoàn không hợp lệ!";
        
        // Update DatPhong status
        dp.setTrangThai(TrangThaiDatPhong.CHECKED_IN);
        if (!datPhongDAO.update(dp)) return "Lỗi cập nhật trạng thái Master Booking!";

        for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
            if (ct.getPhong() == null) continue;
            
            Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
            if (phong == null) continue;
            
            // Allow checkin if AVAILABLE or already OCCUPIED (idempotent)
            if (phong.getTrangThai() != TrangThaiPhong.AVAILABLE && phong.getTrangThai() != TrangThaiPhong.OCCUPIED) {
               continue; // Skip rooms that are cleaning, out of order, etc.
            }
            
            ct.setNgayNhanThucTe(LocalDateTime.now());
            if (ct.getGiaThucTeChot() <= 0) {
                ct.setGiaThucTeChot(bangGiaService.layGiaHienHanh(phong.getLoaiPhong().getMaLoaiPhong()));
            }
            
            boolean ok;
            if (ct.getMaChiTiet() == null || ct.getMaChiTiet().isEmpty()) {
                ct.setMaChiTiet(ctdpDAO.generateMaChiTiet());
                ok = ctdpDAO.insert(ct);
            } else {
                ok = ctdpDAO.update(ct);
            }
            
            if (ok) {
                phong.setTrangThai(TrangThaiPhong.OCCUPIED);
                phongDAO.update(phong);
            }
        }
        
        return null; // Return null on success
    }

    // ---- Thêm dịch vụ ----
    public String themDichVu(SuDungDichVu sddv) {
        if (sddv == null) return "Dữ liệu dịch vụ trống!";
        
        // DAO now handles maDatPhong fallback, so we just need a basic sanity check
        if (sddv.getDichVu() == null || sddv.getSoLuong() <= 0) 
            return "Thông tin dịch vụ hoặc số lượng không hợp lệ!";
        
        return sddvDAO.insert(sddv) ? null : "Lỗi lưu dịch vụ vào cơ sở dữ liệu!";
    }

    // Overload for legacy UI
    public String themDichVu(String maChiTiet, String maDV, int sl, double gia) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        DichVu dv = dichVuDAO.getById(maDV);
        if (dv == null) return "Dịch vụ không tồn tại!";
        
        SuDungDichVu s = new SuDungDichVu();
        // Gán ct ngay cả khi null (DAO sẽ dùng maDatPhong dự phòng nếu ct rỗng)
        if (ct == null) {
            ct = new ChiTietDatPhong();
            ct.setMaChiTiet(maChiTiet);
        }
        s.setCtdp(ct);
        s.setDichVu(dv);
        s.setSoLuong(sl);
        s.setDonGiaLuu(gia > 0 ? gia : dv.getDonGia());
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
            
            // Resolve ngày nhận: ưu tiên thực tế, fallback sang dự kiến
            LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
            if (ngayNhanResolved == null && ct.getDatPhong() != null) {
                ngayNhanResolved = ct.getDatPhong().getNgayNhanDuKien();
            }
            
            // Tính tiền phòng theo số ngày thực tế
            long soNgay = Math.max(1, tinhSoNgay(ngayNhanResolved, ct.getNgayTraThucTe()));
            double donGiaThang = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
            double tienPhong = donGiaThang * soNgay;
            
            // Tính phí phát sinh (check-in sớm/check-out muộn) — lấy từ bảng giá
            double phuPhiTraTre = 50000; // Giá mặc định fallback
            try {
                BangGia bgActive = new BangGiaDAO().findActiveToday();
                if (bgActive != null && ct.getPhong() != null && ct.getPhong().getLoaiPhong() != null) {
                    ChiTietBangGia ctbg = new ChiTietBangGiaDAO().findByBangGiaAndLoaiPhong(
                        bgActive.getMaBangGia(), ct.getPhong().getLoaiPhong().getMaLoaiPhong());
                    if (ctbg != null && ctbg.getPhuPhiTraTre() > 0) {
                        phuPhiTraTre = ctbg.getPhuPhiTraTre();
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(java.util.logging.Level.WARNING, "Không đọc được phụ phí từ bảng giá, dùng mặc định", ex);
            }

            double phuPhi = 0;
            // Check-in trước 14:00 → phụ thu nhận sớm
            if (ngayNhanResolved != null) {
                int gioNhan = ngayNhanResolved.getHour();
                if (gioNhan < 14) phuPhi += phuPhiTraTre;
            }
            // Check-out sau 12:00 → phụ thu trả muộn
            if (ct.getNgayTraThucTe() != null) {
                int gioTra = ct.getNgayTraThucTe().getHour();
                if (gioTra > 12) phuPhi += phuPhiTraTre;
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
            hd.setTongTienDichVu(tongDichVu + phuPhi);
            
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
            hd.setTrangThai(entity.enums.TrangThaiThanhToan.PAID); // Update status to PAID since checkout is completed
            
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

            // Lưu ngày trả thực tế và đánh dấu đã thanh toán (Split Bill / FIT Payment)
            ct.setDaThanhToan(true);
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

    public String checkOutWithoutBilling(String maChiTiet) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();
            ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
            if (ct == null) {
                dbConn.rollbackTransaction();
                return "Không tìm thấy chi tiết thuê phòng!";
            }
            if (ct.getNgayTraThucTe() != null) {
                dbConn.rollbackTransaction();
                return "Phòng này đã được trả!";
            }

            ct.setNgayTraThucTe(LocalDateTime.now());
            ct.setDaThanhToan(false); // Đánh dấu là chưa thanh toán để Master Bill thu
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

            dbConn.commitTransaction();
            LOGGER.info("Treo nợ Master Folio thành công: " + maChiTiet);
            return null;

        } catch (Exception e) {
            dbConn.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi treo gộp bill", e);
            return "Lỗi hệ thống khi treo gộp bill: " + e.getMessage();
        }
    }

    /**
     * Gom Bill (Master Bill Settlement) — Thanh toán tổng hợp toàn bộ đoàn.
     * Tạo 1 HoaDon duy nhất chứa tiền phòng + dịch vụ + phụ phí của TẤT CẢ phòng trong đoàn.
     * Đánh dấu tất cả ChiTietDatPhong.daThanhToan = true.
     */
    public String checkOutMasterBill(String maDatPhong, String maNV) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();

            DatPhong dp = datPhongDAO.getById(maDatPhong);
            if (dp == null) {
                dbConn.rollbackTransaction();
                return "Không tìm thấy đơn đặt phòng!";
            }

            List<ChiTietDatPhong> allRooms = ctdpDAO.getByDatPhong(maDatPhong);
            if (allRooms.isEmpty()) {
                dbConn.rollbackTransaction();
                return "Không có phòng nào trong đơn đặt này!";
            }

            // Tạo hóa đơn tổng
            HoaDon hd = new HoaDon();
            hd.setMaHoaDon(hoaDonDAO.generateMaHD());
            hd.setDatPhong(dp);
            hd.setNhanVien(nhanVienDAO.getById(maNV));
            hd.setNgayLap(LocalDateTime.now());

            double tongTienPhong = 0;
            double tongTienDichVu = 0;
            double tongPhuPhi = 0;

            // Lấy phụ phí từ bảng giá
            double phuPhiTraTre = 50000;
            try {
                BangGia bgActive = new BangGiaDAO().findActiveToday();
                if (bgActive != null) {
                    // Lấy phụ phí trung bình (dùng phòng đầu tiên làm fallback)
                    for (ChiTietDatPhong ct : allRooms) {
                        if (ct.getPhong() != null && ct.getPhong().getLoaiPhong() != null) {
                            ChiTietBangGia ctbg = new ChiTietBangGiaDAO().findByBangGiaAndLoaiPhong(
                                bgActive.getMaBangGia(), ct.getPhong().getLoaiPhong().getMaLoaiPhong());
                            if (ctbg != null && ctbg.getPhuPhiTraTre() > 0) {
                                phuPhiTraTre = ctbg.getPhuPhiTraTre();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.log(java.util.logging.Level.WARNING, "Không đọc được phụ phí từ bảng giá", ex);
            }

            List<ChiTietHoaDon> pendingCTHD = new ArrayList<>();

            // Xử lý từng phòng
            for (ChiTietDatPhong ct : allRooms) {
                // Set ngày trả nếu chưa có (phòng cuối cùng chưa checkout)
                if (ct.getNgayTraThucTe() == null) {
                    ct.setNgayTraThucTe(LocalDateTime.now());
                }

                // Tính tiền phòng
                LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
                if (ngayNhanResolved == null && dp.getNgayNhanDuKien() != null) {
                    ngayNhanResolved = dp.getNgayNhanDuKien();
                }
                long soNgay = Math.max(1, tinhSoNgay(ngayNhanResolved, ct.getNgayTraThucTe()));
                double donGia = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
                double tienPhong = donGia * soNgay;
                tongTienPhong += tienPhong;

                // Tính phụ phí
                double phuPhi = 0;
                if (ngayNhanResolved != null && ngayNhanResolved.getHour() < 14) phuPhi += phuPhiTraTre;
                if (ct.getNgayTraThucTe() != null && ct.getNgayTraThucTe().getHour() > 12) phuPhi += phuPhiTraTre;
                phuPhi += ct.getPhuPhiPhatSinh();
                tongPhuPhi += phuPhi;
                
                String tenPhong = ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?";

                // Chi tiết hóa đơn — Tiền phòng
                ChiTietHoaDon cthdPhong = new ChiTietHoaDon();
                cthdPhong.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdPhong.setHoaDon(hd);
                cthdPhong.setLoaiChiTiet(entity.enums.LoaiChiTietHoaDon.PHONG);
                cthdPhong.setSoLuong((int) soNgay);
                cthdPhong.setDonGia(donGia);
                cthdPhong.setNoiDung("Phòng " + tenPhong + " (" + soNgay + " đêm)");
                cthdPhong.setThanhTien(tienPhong);
                pendingCTHD.add(cthdPhong);

                // Chi tiết hóa đơn — Phụ phí
                if (phuPhi > 0) {
                    ChiTietHoaDon cthdPhuPhi = new ChiTietHoaDon();
                    cthdPhuPhi.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdPhuPhi.setHoaDon(hd);
                    cthdPhuPhi.setLoaiChiTiet(entity.enums.LoaiChiTietHoaDon.PHU_PHI);
                    cthdPhuPhi.setSoLuong(1);
                    cthdPhuPhi.setDonGia(phuPhi);
                    cthdPhuPhi.setNoiDung("Phụ phí phòng " + tenPhong);
                    cthdPhuPhi.setThanhTien(phuPhi);
                    pendingCTHD.add(cthdPhuPhi);
                }

                // Chi tiết hóa đơn — Dịch vụ từng phòng
                List<SuDungDichVu> dsDv = sddvDAO.getByChiTietDatPhong(ct.getMaChiTiet());
                for (SuDungDichVu sddv : dsDv) {
                    double tienDV = sddv.getDonGiaLuu() * sddv.getSoLuong();
                    tongTienDichVu += tienDV;

                    ChiTietHoaDon cthdDV = new ChiTietHoaDon();
                    cthdDV.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdDV.setHoaDon(hd);
                    cthdDV.setLoaiChiTiet(entity.enums.LoaiChiTietHoaDon.DICH_VU);
                    cthdDV.setSoLuong(sddv.getSoLuong());
                    cthdDV.setDonGia(sddv.getDonGiaLuu());
                    cthdDV.setNoiDung("[P." + tenPhong + "] " + (sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dịch vụ"));
                    cthdDV.setThanhTien(tienDV);
                    pendingCTHD.add(cthdDV);
                }

                // Đánh dấu phòng đã thanh toán
                ct.setDaThanhToan(true);
                if (!ctdpDAO.update(ct)) {
                    dbConn.rollbackTransaction();
                    return "Lỗi cập nhật phòng " + tenPhong + "!";
                }

                // Cập nhật trạng thái phòng
                Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
                if (phong != null && phong.getTrangThai() == entity.enums.TrangThaiPhong.OCCUPIED) {
                    phong.setTrangThai(entity.enums.TrangThaiPhong.CLEANING);
                    phongDAO.update(phong);
                }
            }

            // Lưu tổng tiền vào hóa đơn
            hd.setTongTienPhong(tongTienPhong);
            hd.setTongTienDichVu(tongTienDichVu + tongPhuPhi);
            hd.setTienGiamKhuyenMai(0);
            hd.setTongThanhToan(tongTienPhong + tongTienDichVu + tongPhuPhi);
            hd.setTrangThai(entity.enums.TrangThaiThanhToan.PAID);

            // Bắt buộc insert HoaDon trước khi insert ChiTietHoaDon để tránh lỗi Foreign Key
            if (!hoaDonDAO.insert(hd)) {
                dbConn.rollbackTransaction();
                return "Lỗi tạo hóa đơn tổng đoàn!";
            }
            
            // Insert các Chi tiết hóa đơn
            for (ChiTietHoaDon pendingItem : pendingCTHD) {
                if (!cthdDAO.insert(pendingItem)) {
                    dbConn.rollbackTransaction();
                    return "Lỗi lưu chi tiết hóa đơn: " + pendingItem.getNoiDung() + "!";
                }
            }

            // Cập nhật trạng thái đặt phòng
            dp.setTrangThai(entity.enums.TrangThaiDatPhong.CHECKED_OUT);
            datPhongDAO.update(dp);

            dbConn.commitTransaction();
            LOGGER.info("Master Bill thanh toán thành công, mã HĐ: " + hd.getMaHoaDon());
            return null;

        } catch (Exception e) {
            dbConn.rollbackTransaction();
            LOGGER.log(java.util.logging.Level.SEVERE, "Lỗi Master Bill, đã rollback", e);
            return "Lỗi hệ thống khi gom bill: " + e.getMessage();
        }
    }

    /**
     * Lấy danh sách ChiTietDatPhong chưa thanh toán của một đơn đặt phòng.
     */
    public List<ChiTietDatPhong> getUnpaidByDatPhong(String maDatPhong) {
        List<ChiTietDatPhong> result = new ArrayList<>();
        for (ChiTietDatPhong ct : ctdpDAO.getByDatPhong(maDatPhong)) {
            if (!ct.isDaThanhToan()) {
                result.add(ct);
            }
        }
        return result;
    }

    public ChiTietDatPhong getById(String maChiTiet) { return ctdpDAO.getById(maChiTiet); }

    public List<ChiTietDatPhong> getChiTietByDatPhong(String maDatPhong) {
        return ctdpDAO.getByDatPhong(maDatPhong);
    }

    public List<SuDungDichVu> getDichVuByChiTiet(String maChiTiet) {
        return sddvDAO.getByChiTietDatPhong(maChiTiet);
    }

    public List<SuDungDichVu> getDichVuByThue(String maChiTiet) {
        return getDichVuByChiTiet(maChiTiet);
    }

    public ChiTietDatPhong getActiveByPhong(String soPhong) {
        ChiTietDatPhong booking = null;
        for (ChiTietDatPhong c : ctdpDAO.getAll()) {
            if (c.getPhong() != null && c.getPhong().getMaPhong().equals(soPhong) && c.getNgayTraThucTe() == null) {
                if (c.getNgayNhanThucTe() != null) return c; // Active checkin - prioritize this!
                booking = c; // Fallback to pending booking
            }
        }
        return booking;
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
