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
    private final KhachHangDAO       khachHangDAO = new KhachHangDAO();
    private final KhuyenMaiDAO       khuyenMaiDAO = new KhuyenMaiDAO();

    // ---- Check-in ----
    public String checkIn(ChiTietDatPhong ct) {
        if (ct == null || ct.getPhong() == null) return "Thông tin phòng không hợp lệ!";
        
        Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
        if (phong == null) return "Phòng không tồn tại!";
        
        // Strict availability check - Real-world industrial standard
        if (phong.getTrangThai() != TrangThaiPhong.AVAILABLE) 
            return "Phòng " + ct.getPhong().getMaPhong() + " không sẵn sàng (đang: " + phong.getTrangThaiString() + ")!";

        // Global check: Verify there is no unclosed stay (dangling record)
        if (ctdpDAO.hasActiveStay(phong.getMaPhong())) {
            return "Phòng " + ct.getPhong().getMaPhong() + " hiện đang có một lượt lưu trú chưa được trả phòng trên hệ thống. Vui lòng kiểm tra và trả phòng cũ trước!";
        }

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
        
        // Kiểm tra logic thời gian
        if (newLDT.isBefore(dp.getNgayTraDuKien())) {
            return "Ngày trả mới không thể sớm hơn ngày trả hiện tại!";
        }

        // KIỂM TRA XUNG ĐỘT RESERVATION (THÔNG MINH)
        // Kiểm tra từ thời điểm trả phòng cũ đến thời điểm trả phòng mới
        // Loại trừ chính đơn đặt phòng này để không tự xung đột với mình
        LocalDateTime oldEnd = dp.getNgayTraDuKien();
        if (!phongDAO.isRoomAvailable(ct.getPhong().getMaPhong(), oldEnd, newLDT, dp.getMaDatPhong())) {
            return "Không thể gia hạn! Phòng " + ct.getPhong().getMaPhong() + " đã có khách khác đặt trước trong khoảng thời gian này.";
        }
        
        dp.setNgayTraDuKien(newLDT);
        boolean ok = datPhongDAO.update(dp);
        if (ok) {
            LOGGER.info("Gia hạn phòng " + ct.getPhong().getMaPhong() + " đến " + newLDT);
            return null;
        }
        return "Lỗi cập nhật CSDL!";
    }

    // ---- Check-out (CÓ TRANSACTION) ----
    public String checkOut(String maChiTiet, String maNV) {
        return checkOut(maChiTiet, maNV, "PAID", null);
    }
    
    public String checkOut(String maChiTiet, String maNV, String voucherCode) {
        return checkOut(maChiTiet, maNV, "PAID", voucherCode);
    }

    /**
     * Check-out phòng — toàn bộ thao tác được wrap trong Transaction.
     * Nếu bất kỳ bước nào lỗi → rollback tất cả.
     */
    /**
     * Check-out phòng — toàn bộ thao tác được wrap trong Transaction.
     * NÂNG CẤP: Gom tất cả các lượt lưu trú (segments) chưa thanh toán của cùng 1 đơn đặt phòng.
     */
    public String checkOut(String maChiTiet, String maNV, String status, String voucherCode) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            // ===== BẮT ĐẦU TRANSACTION =====
            dbConn.beginTransaction();

            ChiTietDatPhong mainCt = ctdpDAO.getById(maChiTiet);
            if (mainCt == null) {
                dbConn.rollbackTransaction();
                return "Không tìm thấy chi tiết thuê phòng!";
            }

            // 1. TÌM TẤT CẢ CÁC CHẶNG CHƯA THANH TOÁN CỦA CÙNG KHÁCH HÀNG NÀY (Hỗ trợ Stay Splitting nhưng không lôi cả đoàn)
            List<ChiTietDatPhong> segments = new ArrayList<>();
            if (mainCt.getDatPhong() != null) {
                String mainKhId = mainCt.getKhachHang() != null ? mainCt.getKhachHang().getMaKhachHang() : "";
                
                segments = ctdpDAO.getByDatPhong(mainCt.getDatPhong().getMaDatPhong()).stream()
                        .filter(c -> !c.isDaThanhToan())
                        .filter(c -> {
                            // Chỉ lấy các chặng của đúng khách này (tránh lôi cả phòng khác trong đoàn)
                            String currentKhId = c.getKhachHang() != null ? c.getKhachHang().getMaKhachHang() : "";
                            return currentKhId.equals(mainKhId);
                        })
                        .collect(java.util.stream.Collectors.toList());
            } else {
                segments.add(mainCt);
            }

            // Đảm bảo chặng hiện tại luôn có trong danh sách
            boolean foundMain = false;
            for (ChiTietDatPhong s : segments) if (s.getMaChiTiet().equals(maChiTiet)) foundMain = true;
            if (!foundMain) segments.add(mainCt);

            // 2. TẠO HÓA ĐƠN
            HoaDon hd = new HoaDon();
            hd.setMaHoaDon(hoaDonDAO.generateMaHD());
            hd.setDatPhong(mainCt.getDatPhong());
            hd.setNhanVien(nhanVienDAO.getById(maNV));
            hd.setNgayLap(LocalDateTime.now());
            
            double tongTienPhong = 0;
            double tongDichVu = 0;
            double tongPhuPhi = 0;
            List<ChiTietHoaDon> pendingItems = new ArrayList<>();

            // 3. DUYỆT TỪNG CHẶNG ĐỂ TÍNH TIỀN
            for (ChiTietDatPhong ct : segments) {
                if (ct.getNgayTraThucTe() == null) {
                    ct.setNgayTraThucTe(LocalDateTime.now());
                }
                
                // Resolve ngày nhận
                LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
                if (ngayNhanResolved == null && ct.getDatPhong() != null) {
                    ngayNhanResolved = ct.getDatPhong().getNgayNhanDuKien();
                }
                
                // Tính tiền phòng chặng này
                long soNgay = Math.max(1, tinhSoNgay(ngayNhanResolved, ct.getNgayTraThucTe()));
                double donGiaChot = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
                double tienPhongChang = donGiaChot * soNgay;
                tongTienPhong += tienPhongChang;

                // Tạo item phòng cho hóa đơn
                ChiTietHoaDon cthdPhong = new ChiTietHoaDon();
                cthdPhong.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdPhong.setHoaDon(hd);
                cthdPhong.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
                cthdPhong.setSoLuong((int) soNgay);
                cthdPhong.setDonGia(donGiaChot);
                cthdPhong.setNoiDung("Phòng " + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + " (" + soNgay + " đêm)");
                cthdPhong.setThanhTien(tienPhongChang);
                pendingItems.add(cthdPhong);

                // Tính phí phát sinh & dịch vụ cho chặng này
                List<SuDungDichVu> dsDichVu = sddvDAO.getByChiTietDatPhong(ct.getMaChiTiet());
                for (SuDungDichVu sddv : dsDichVu) {
                    double subTotal = sddv.getDonGiaLuu() * sddv.getSoLuong();
                    tongDichVu += subTotal;
                    
                    ChiTietHoaDon cthdDV = new ChiTietHoaDon();
                    cthdDV.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdDV.setHoaDon(hd);
                    cthdDV.setLoaiChiTiet(LoaiChiTietHoaDon.DICH_VU);
                    cthdDV.setSoLuong(sddv.getSoLuong());
                    cthdDV.setDonGia(sddv.getDonGiaLuu());
                    cthdDV.setNoiDung("[" + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + "] " + (sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dịch vụ"));
                    cthdDV.setThanhTien(subTotal);
                    pendingItems.add(cthdDV);
                }
                tongPhuPhi += ct.getPhuPhiPhatSinh();
                
                // Đánh dấu chặng đã thanh toán
                ct.setDaThanhToan(true);
                if (!ctdpDAO.update(ct)) {
                    dbConn.rollbackTransaction();
                    return "Lỗi cập nhật chặng!";
                }

                // Chuyển trạng thái phòng sang Vệ sinh
                if (ct.getPhong() != null) {
                    Phong p = phongDAO.getById(ct.getPhong().getMaPhong());
                    if (p != null) {
                        p.setTrangThai(TrangThaiPhong.CLEANING);
                        p.setTenKhachHienTai(null);
                        phongDAO.update(p);
                    }
                }
            }
            
            // 4. TỔNG HỢP HÓA ĐƠN
            hd.setTongTienPhong(tongTienPhong);
            hd.setTongTienDichVu(tongDichVu + tongPhuPhi);
            
            // Xử lý khuyến mãi: Ưu tiên voucher nhập tay, nếu không thì lấy Deal tốt nhất
            KhuyenMai chosenKM = null;
            double subTotal = tongTienPhong + hd.getTongTienDichVu();
            
            if (voucherCode != null && !voucherCode.trim().isEmpty()) {
                chosenKM = khuyenMaiDAO.getByVoucherCode(voucherCode.trim());
                if (chosenKM != null && !chosenKM.kiemTraHopLe(subTotal)) {
                    chosenKM = null; // Voucher không hợp lệ
                }
            }
            
            if (chosenKM == null) {
                chosenKM = getBestPromotion(subTotal);
            }
            
            hd.setKhuyenMai(chosenKM);
            double tienGiam = 0;
            if (chosenKM != null) {
                tienGiam = chosenKM.tinhSoTienGiam(subTotal);
                // Cập nhật số lượng đã dùng
                chosenKM.setDaDung(chosenKM.getDaDung() + 1);
                khuyenMaiDAO.update(chosenKM);
            }
            
                        hd.setTienGiamKhuyenMai(tienGiam);
            
            // --- KHAU TRU TIEN COC ---
            double tienCoc = 0;
            if (mainCt.getDatPhong() != null) {
                tienCoc = mainCt.getDatPhong().getTienDatCoc();
                if (tienCoc > 0) {
                    ChiTietHoaDon cthdCoc = new ChiTietHoaDon();
                    cthdCoc.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdCoc.setHoaDon(hd);
                    cthdCoc.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                    cthdCoc.setSoLuong(1);
                    cthdCoc.setDonGia(-tienCoc);
                    cthdCoc.setNoiDung("Khau tru tien coc");
                    cthdCoc.setThanhTien(-tienCoc);
                    pendingItems.add(cthdCoc);
                }
            }
            hd.setTongThanhToan(subTotal - tienGiam - tienCoc);
            
            try {
                hd.setTrangThai(entity.enums.TrangThaiThanhToan.valueOf(status.toUpperCase()));
            } catch (Exception e) {
                hd.setTrangThai(entity.enums.TrangThaiThanhToan.PAID);
            }
            
            // 5. LƯU HÓA ĐƠN & CHI TIẾT
            if (!hoaDonDAO.insert(hd)) {
                dbConn.rollbackTransaction();
                return "Lỗi tạo hóa đơn!";
            }
            for (ChiTietHoaDon item : pendingItems) {
                if (!cthdDAO.insert(item)) {
                    dbConn.rollbackTransaction();
                    return "Lỗi lưu chi tiết hóa đơn!";
                }
            }

            // 6. CẬP NHẬT TRẠNG THÁI ĐẶT PHÒNG
            if (mainCt.getDatPhong() != null) {
                DatPhong dp = datPhongDAO.getById(mainCt.getDatPhong().getMaDatPhong());
                if (dp != null) {
                    boolean allPaid = true;
                    for (ChiTietDatPhong c : ctdpDAO.getByDatPhong(dp.getMaDatPhong())) {
                        if (c.getNgayTraThucTe() == null) { allPaid = false; break; }
                    }
                    if (allPaid) {
                        dp.setTrangThai(TrangThaiDatPhong.CHECKED_OUT);
                        datPhongDAO.update(dp);
                    }
                }
            }

            // 7. CẬP NHẬT CRM CHO KHÁCH HÀNG (Loyalty & History)
            if (hd.getTrangThai() == entity.enums.TrangThaiThanhToan.PAID && mainCt.getKhachHang() != null) {
                // Increment stay by 1 and add total payment amount to spending
                String khId = mainCt.getKhachHang().getMaKhachHang();
                khachHangDAO.updateCRMStats(khId, hd.getTongThanhToan(), true);
            }

            // ===== COMMIT TRANSACTION =====
            dbConn.commitTransaction();
            LOGGER.info("Check-out chặng thành công, HĐ: " + hd.getMaHoaDon());
            return null;

        } catch (Exception e) {
            dbConn.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi check-out", e);
            return "Lỗi hệ thống: " + e.getMessage();
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

            // Lấy bảng giá active 1 lần duy nhất để dùng trong loop
            BangGia bgActive = null;
            try { bgActive = new BangGiaDAO().findActiveToday(); } catch (Exception ignored) {}

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

                // --- TÍNH PHỤ PHÍ THEO TỪNG LOẠI PHÒNG (FIX BUG) ---
                double phuPhiTraTreCuaPhong = 50000; // Mặc định fallback 50k
                if (bgActive != null && ct.getPhong() != null && ct.getPhong().getLoaiPhong() != null) {
                    ChiTietBangGia ctbg = new ChiTietBangGiaDAO().findByBangGiaAndLoaiPhong(
                        bgActive.getMaBangGia(), ct.getPhong().getLoaiPhong().getMaLoaiPhong());
                    if (ctbg != null && ctbg.getPhuPhiTraTre() > 0) {
                        phuPhiTraTreCuaPhong = ctbg.getPhuPhiTraTre();
                    }
                }

                double phuPhi = 0;
                if (ngayNhanResolved != null && ngayNhanResolved.getHour() < 14) phuPhi += phuPhiTraTreCuaPhong;
                if (ct.getNgayTraThucTe() != null && ct.getNgayTraThucTe().getHour() > 12) phuPhi += phuPhiTraTreCuaPhong;
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

                // Đánh dấu phòng đã trả và đã thanh toán (theo đơn giá đã chốt)
                ct.setNgayTraThucTe(LocalDateTime.now());
                ct.setDaThanhToan(true);
                if (!ctdpDAO.update(ct)) {
                    dbConn.rollbackTransaction();
                    return "Lỗi cập nhật phòng " + tenPhong + "!";
                }

                // Cập nhật trạng thái phòng sang Vệ sinh
                Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
                if (phong != null && (phong.getTrangThai() == entity.enums.TrangThaiPhong.OCCUPIED)) {
                    phong.capNhatTrangThai(entity.enums.TrangThaiPhong.CLEANING);
                    phongDAO.update(phong);
                }
            }

            // Lưu tổng tiền vào hóa đơn và trạng thái
            hd.setTongTienPhong(tongTienPhong);
            hd.setTongTienDichVu(tongTienDichVu + tongPhuPhi);
                        hd.setTienGiamKhuyenMai(0);
            double tienCocDoan = dp.getTienDatCoc();
            if (tienCocDoan > 0) {
                ChiTietHoaDon cthdCoc = new ChiTietHoaDon();
                cthdCoc.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdCoc.setHoaDon(hd);
                cthdCoc.setLoaiChiTiet(entity.enums.LoaiChiTietHoaDon.PHU_PHI);
                cthdCoc.setSoLuong(1);
                cthdCoc.setDonGia(-tienCocDoan);
                cthdCoc.setNoiDung("Khau tru tien coc (Doan)");
                cthdCoc.setThanhTien(-tienCocDoan);
                pendingCTHD.add(cthdCoc);
            }
            hd.setTongThanhToan(tongTienPhong + tongTienDichVu + tongPhuPhi - tienCocDoan);
            
            // Đối với Gom Bill Đoàn: Có thể thanh toán ngay (PAID) hoặc treo nợ đoàn (UNPAID)
            // Nếu có chức năng chọn nợ cho đoàn thì truyền vào, ở đây mặc định PAID cho group bill
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

            // 7. CẬP NHẬT CRM CHO TOÀN ĐOÀN
            if (hd.getTrangThai() == entity.enums.TrangThaiThanhToan.PAID) {
                // Người đại diện (Paying Member) nhận Tiền & Điểm
                if (dp.getKhachHang() != null) {
                    khachHangDAO.updateCRMStats(dp.getKhachHang().getMaKhachHang(), hd.getTongThanhToan(), false);
                }
                
                // Tất cả khách trong đoàn được tính +1 Lượt ở (Stay Credit)
                Set<String> processedKhIds = new HashSet<>();
                for (ChiTietDatPhong room : allRooms) {
                    if (room.getKhachHang() != null) {
                        String kId = room.getKhachHang().getMaKhachHang();
                        if (processedKhIds.add(kId)) {
                            khachHangDAO.updateCRMStats(kId, 0, true);
                        }
                    }
                }
            }

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
        // Optimized: Uses direct SQL index-backed query instead of full list scan
        return ctdpDAO.getActiveStayByPhong(soPhong);
    }

    /**
     * Cập nhật người đại diện cho một phòng đang ở/đang đặt.
     * Cập nhật cả trong DatPhong và tenKhachHienTai trong Phong để UI đồng bộ.
     */
    public String updateRepresentative(String maDatPhong, KhachHang newKh, String soPhong) {
        try {
            // 1. Cập nhật DatPhong
            DatPhong dp = datPhongDAO.getById(maDatPhong);
            if (dp == null) return "Không tìm thấy thông tin đặt phòng!";
            
            dp.setKhachHang(newKh);
            if (!datPhongDAO.update(dp)) return "Lỗi cập nhật người đại diện trong CSDL!";

            // 2. Cập nhật tenKhachHienTai trong Phong (Để sơ đồ phòng cập nhật ngay)
            if (soPhong != null) {
                Phong p = phongDAO.getById(soPhong);
                if (p != null) {
                    p.setTenKhachHienTai(newKh.getHoTen());
                }
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật người đại diện", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    public String transferRoom(String maChiTiet, String maPhongMoi, boolean keepOldPrice) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            // ===== BẮT ĐẦU TRANSACTION =====
            dbConn.beginTransaction();

            ChiTietDatPhong ctOld = ctdpDAO.getById(maChiTiet);
            if (ctOld == null) {
                dbConn.rollbackTransaction();
                return "Không tìm thấy thông tin lượt ở cũ!";
            }

            Phong oldRoom = ctOld.getPhong();
            Phong newRoom = phongDAO.getById(maPhongMoi);
            if (newRoom == null) {
                dbConn.rollbackTransaction();
                return "Phòng mới không tồn tại!";
            }

            if (newRoom.getTrangThai() != TrangThaiPhong.AVAILABLE) {
                dbConn.rollbackTransaction();
                return "Phòng " + maPhongMoi + " không sẵn sàng!";
            }

            // Conflict Check (Intelligent: exclude current booking)
            LocalDateTime startCheck = LocalDateTime.now();
            LocalDateTime endCheck = (ctOld.getDatPhong() != null && ctOld.getDatPhong().getNgayTraDuKien() != null)
                    ? ctOld.getDatPhong().getNgayTraDuKien()
                    : LocalDateTime.now().plusHours(24);

            if (!phongDAO.isRoomAvailable(maPhongMoi, startCheck, endCheck, ctOld.getDatPhong().getMaDatPhong())) {
                dbConn.rollbackTransaction();
                return "Phòng " + maPhongMoi + " đã có người đặt trước!";
            }

            // 1. KẾT THÚC CHẶNG CŨ (Splitting)
            ctOld.setNgayTraThucTe(LocalDateTime.now());
            ctOld.setDaThanhToan(false); // Chưa thanh toán, để dành Gom Bill
            if (!ctdpDAO.update(ctOld)) {
                dbConn.rollbackTransaction();
                return "Lỗi cập nhật chặng cũ!";
            }

            // Chuyển phòng cũ sang vệ sinh
            if (oldRoom != null) {
                oldRoom.setTrangThai(TrangThaiPhong.CLEANING);
                oldRoom.setTenKhachHienTai(null);
                phongDAO.update(oldRoom);
            }

            // 2. TẠO CHẶNG MỚI (Splitting)
            ChiTietDatPhong ctNew = new ChiTietDatPhong();
            ctNew.setMaChiTiet(ctdpDAO.generateMaChiTiet());
            ctNew.setDatPhong(ctOld.getDatPhong());
            ctNew.setPhong(newRoom);
            ctNew.setKhachHang(ctOld.getKhachHang());
            ctNew.setNgayNhanThucTe(LocalDateTime.now());
            
            // PRIORITY: Active Price Table > Base Price
            double currentRate = bangGiaService.layGiaHienHanh(newRoom.getLoaiPhong() != null ? newRoom.getLoaiPhong().getMaLoaiPhong() : "");
            ctNew.setGiaThucTeChot(keepOldPrice ? ctOld.getGiaThucTeChot() : currentRate);
            
            ctNew.setDaThanhToan(false);

            if (!ctdpDAO.insert(ctNew)) {
                dbConn.rollbackTransaction();
                return "Lỗi khởi tạo chặng mới!";
            }

            // Cập nhật phòng mới sang Occupied
            newRoom.setTrangThai(TrangThaiPhong.OCCUPIED);
            newRoom.setTenKhachHienTai(ctNew.getKhachHang() != null ? ctNew.getKhachHang().getHoTen() : "Khách");
            phongDAO.update(newRoom);

            // ===== COMMIT TRANSACTION =====
            dbConn.commitTransaction();
            LOGGER.info("Stay Split thành công: " + oldRoom.getMaPhong() + " -> " + maPhongMoi);
            return null;

        } catch (Exception e) {
            dbConn.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi khi tách chặng đổi phòng", e);
            return "Lỗi hệ thống khi đổi phòng: " + e.getMessage();
        }
    }

    /**
     * Tìm khuyến mãi tốt nhất hiện có cho giá trị hóa đơn này.
     */
    public KhuyenMai getBestPromotion(double total) {
        List<KhuyenMai> all = khuyenMaiDAO.getAll();
        KhuyenMai best = null;
        double maxDiscount = 0;
        
        for (KhuyenMai km : all) {
            if (km.kiemTraHopLe(total)) {
                double discount = km.tinhSoTienGiam(total);
                if (discount > maxDiscount) {
                    maxDiscount = discount;
                    best = km;
                }
            }
        }
        return best;
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

    /**
     * Tinh phu phi nhan phong som.
     */
    public double tinhPhuPhiEarlyCheckin(Phong p, java.time.LocalDateTime checkinTime) {
        if (p == null || p.getLoaiPhong() == null || checkinTime == null) return 0;
        if (checkinTime.getHour() >= 14) return 0;
        double giaHienHanh = bangGiaService.layGiaHienHanh(p.getLoaiPhong().getMaLoaiPhong());
        return giaHienHanh * 0.3; 
    }
}
