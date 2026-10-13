package service;

import dao.*;
import entity.*;
import entity.enums.TrangThaiDatPhong;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatPhongService {

    private static final Logger LOGGER = Logger.getLogger(DatPhongService.class.getName());

    private final DatPhongDAO        datPhongDAO = new DatPhongDAO();
    private final PhongDAO           phongDAO    = new PhongDAO();
    private final ChiTietDatPhongDAO ctDAO       = new ChiTietDatPhongDAO();
    private final HoaDonDAO          hoaDonDAO   = new HoaDonDAO();
    private final ChiTietHoaDonDAO   cthdDAO     = new ChiTietHoaDonDAO();
    private final NhanVienDAO        nhanVienDAO = new NhanVienDAO();
    private final SuDungDichVuDAO    sddvDAO     = new SuDungDichVuDAO();
    private final KhuyenMaiDAO       khuyenMaiDAO = new KhuyenMaiDAO();

    public List<DatPhong> getAll()               { return datPhongDAO.getAll(); }
    public DatPhong getById(String id)           { return datPhongDAO.getById(id); }

    public List<DatPhong> search(String kw, String status, String date) {
        return datPhongDAO.search(kw, status, date);
    }

    public List<Phong> timPhongTrong(LocalDateTime start, LocalDateTime end, int sucChua) {
        return phongDAO.findAvailableRooms(start, end, sucChua);
    }

    // Overload for legacy UI using java.util.Date
    public List<Phong> timPhongTrong(Date start, Date end, int sucChua) {
        LocalDateTime s = start.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime e = end.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        return timPhongTrong(s, e, sucChua);
    }

    public String datPhong(DatPhong dp) {
        // ── Validation (trước transaction) ──────────────────────────────────
        if (dp == null) return "Dữ liệu đặt phòng không hợp lệ!";
        if (dp.getKhachHang() == null || dp.getKhachHang().getMaKhachHang() == null)
            return "Chưa chọn khách hàng!";
        if (dp.getNgayNhan() == null || dp.getNgayTra() == null)
            return "Ngày không hợp lệ!";
        if (!dp.getNgayTra().isAfter(dp.getNgayNhan()))
            return "Ngày trả phải sau ngày nhận!";
        if (dp.getSoLuongKhach() <= 0)
            return "Số lượng khách phải lớn hơn 0!";

        if (dp.getTrangThai() == null) dp.setTrangThai(TrangThaiDatPhong.PENDING);
        if (dp.getMaDatPhong() == null || dp.getMaDatPhong().isEmpty())
            dp.setMaDatPhong(datPhongDAO.generateMaDatPhong());
        if (dp.getNgayDat() == null)
            dp.setNgayDat(LocalDateTime.now());

        // ── Transaction bao toàn bộ các bước ghi DB ─────────────────────────
        database.DatabaseConnection db = database.DatabaseConnection.getInstance();
        try {
            db.beginTransaction();

            if (dp.getDsChiTiet()==null || dp.getDsChiTiet().isEmpty()) { db.rollbackTransaction(); return "Chưa chọn phòng!"; }
            java.util.Set<String> roomIds=new java.util.HashSet<>();
            for(ChiTietDatPhong ct:dp.getDsChiTiet()) {
                LocalDateTime from=ct.getNgayNhanDuKien()!=null?ct.getNgayNhanDuKien():dp.getNgayNhanDuKien();
                LocalDateTime to=ct.getNgayTraDuKien()!=null?ct.getNgayTraDuKien():dp.getNgayTraDuKien();
                if(ct.getPhong()==null || !roomIds.add(ct.getPhong().getMaPhong()) || from==null || to==null || !to.isAfter(from)) { db.rollbackTransaction();return "Danh sách phòng hoặc khoảng lưu trú không hợp lệ!"; }
                Phong room=phongDAO.getById(ct.getPhong().getMaPhong());
                if(room==null || room.getTrangThai()==entity.enums.TrangThaiPhong.DELETED || room.getTrangThai()==entity.enums.TrangThaiPhong.MAINTENANCE || !phongDAO.isRoomAvailable(room.getMaPhong(),from,to,dp.getMaDatPhong())) { db.rollbackTransaction();return "Phòng không khả dụng hoặc bị trùng lịch!"; }
            }

            // 1. Tạo đơn đặt phòng chính
            if (!datPhongDAO.insert(dp)) {
                db.rollbackTransaction();
                return "Lỗi tạo đơn đặt phòng!";
            }

            // 2. Cập nhật quota Voucher nếu có
            if (dp.getMaKhuyenMai() != null && !dp.getMaKhuyenMai().isEmpty()) {
                KhuyenMai km = khuyenMaiDAO.getByVoucherCode(dp.getMaKhuyenMai());
                if (km != null) {
                    km.setDaDung(km.getDaDung() + 1);
                    if(!khuyenMaiDAO.update(km)) throw new IllegalStateException("Không cập nhật được voucher");
                    LogService.addLog("Áp dụng Voucher", "Khuyến mãi",
                            "Voucher " + km.getMaKhuyenMai() + " được dành cho ĐP " + dp.getMaDatPhong());
                }
            }

            // 3. Insert ChiTietDatPhong cho từng phòng
            if (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty()) {
                for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                    if (ct.getMaChiTiet() == null || ct.getMaChiTiet().isEmpty())
                        ct.setMaChiTiet(ctDAO.generateMaChiTiet());
                    ct.setDatPhong(dp);
                    if (!ctDAO.insert(ct)) {
                        db.rollbackTransaction();
                        return "Lỗi thêm chi tiết đặt phòng (phòng " +
                               (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + ")!";
                    }
                }
            }

            // 4. Insert dịch vụ đi kèm (nếu có)
            if (dp.getDsDichVu() != null && !dp.getDsDichVu().isEmpty()) {
                SuDungDichVuDAO sddvDAO = new SuDungDichVuDAO();
                for (SuDungDichVu s : dp.getDsDichVu()) {
                    if (s.getCtdp() == null && dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty())
                        s.setCtdp(dp.getDsChiTiet().get(0));
                    if (!sddvDAO.insert(s)) throw new IllegalStateException("Không lưu được dịch vụ đi kèm");
                }
            }

            db.commitTransaction();
            LOGGER.info("Đặt phòng thành công: " + dp.getMaDatPhong());
            return null;

        } catch (Exception e) {
            db.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi trong datPhong()", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }


    /**
     * Tính toán phí phạt hủy phòng dự kiến dựa trên thời điểm hủy.
     * Policy:
     * - < 24h: Phạt 100% tiền cọc (hoặc 1 đêm)
     * - 24h - 48h: Phạt 50% tiền cọc
     * - > 48h: Miễn phí
     */
    public double tinhPhiHuyDien(String maDatPhong) {
        DatPhong dp = datPhongDAO.getById(maDatPhong);
        if (dp == null || dp.getNgayNhan() == null) return 0;

        // Đơn PENDING (Chờ xác nhận / Chưa cọc) thì hủy luôn miễn phí, không phạt!
        if (dp.getTrangThai() == TrangThaiDatPhong.PENDING) return 0;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime arrival = dp.getNgayNhan();
        java.time.Duration duration = java.time.Duration.between(now, arrival);
        long hours = duration.toHours();

        if (hours < 0) return dp.getTienDatCoc(); // Đã quá ngày nhận
        if (hours < 24) return dp.getTienDatCoc(); // Phạt 100% cọc
        if (hours < 48) return dp.getTienDatCoc() * 0.5; // Phạt 50% cọc
        return 0; // Miễn phí
    }

    public String huyDatPhong(String maDatPhong) {
        return huyDatPhong(maDatPhong, tinhPhiHuyDien(maDatPhong), "Yêu cầu khách hàng");
    }

    /**
     * Hủy đặt phòng có tính phí phạt và xử lý hóa đơn.
     */
    public String huyDatPhong(String maDatPhong, double phiPhat, String lyDo) {
        database.DatabaseConnection db = database.DatabaseConnection.getInstance();
        try {
            db.beginTransaction();
            DatPhong dp = datPhongDAO.getById(maDatPhong);
            if (dp == null) {
                db.rollbackTransaction();
                return "Không tìm thấy đặt phòng!";
            }

            // Chỉ cho phép hủy đơn PENDING, CONFIRMED, WAITLIST
            if (dp.getTrangThai() != TrangThaiDatPhong.PENDING &&
                dp.getTrangThai() != TrangThaiDatPhong.CONFIRMED &&
                dp.getTrangThai() != TrangThaiDatPhong.WAITLIST) {
                db.rollbackTransaction();
                return "Không thể hủy đơn đặt ở trạng thái: " + dp.getTrangThai();
            }

            // 1. Tạo hóa đơn phạt nếu có phí
            if (phiPhat > 0) {
                HoaDon hd = new HoaDon();
                hd.setMaHoaDon(hoaDonDAO.generateMaHD());
                hd.setDatPhong(dp);
                hd.setNgayLap(LocalDateTime.now());
                hd.setTongThanhToan(phiPhat);
                hd.setTrangThai(entity.enums.TrangThaiThanhToan.PAID);
                hd.setPhuongThucThanhToan("DEPOSIT");
                hd.setNhanVien(nhanVienDAO.getById(AuthService.getInstance().getCurrentMaNV() != null ? AuthService.getInstance().getCurrentMaNV() : "admin_nv"));

                if (!hoaDonDAO.insert(hd)) {
                    db.rollbackTransaction();
                    return "Lỗi tạo hóa đơn phạt hủy!";
                }

                ChiTietHoaDon cthd = new ChiTietHoaDon();
                cthd.setHoaDon(hd);
                cthd.setLoaiChiTiet(entity.enums.LoaiChiTietHoaDon.PHU_PHI);
                cthd.setNoiDung("Phí phạt hủy phòng (Late Cancellation Penalty)");
                cthd.setSoLuong(1);
                cthd.setDonGia(phiPhat);
                cthd.setThanhTien(phiPhat);
                cthdDAO.insert(cthd);
            }

            // 2. Cập nhật trạng thái
            dp.setTrangThai(TrangThaiDatPhong.CANCELLED);
            dp.setPhiHuyPhong(phiPhat);
            dp.setGhiChu((dp.getGhiChu() != null ? dp.getGhiChu() + " | " : "") + "Hủy: " + lyDo + " (Phạt: " + String.format("%,.0fđ", phiPhat) + ")");

            if (datPhongDAO.update(dp)) {
                // Release Voucher Quota if present
                if (dp.getMaKhuyenMai() != null && !dp.getMaKhuyenMai().isEmpty()) {
                    KhuyenMai km = khuyenMaiDAO.getByVoucherCode(dp.getMaKhuyenMai());
                    if (km != null && km.getDaDung() > 0) {
                        km.setDaDung(km.getDaDung() - 1);
                        if(!khuyenMaiDAO.update(km)) throw new IllegalStateException("Không cập nhật được voucher");
                        LogService.addLog("Hoàn voucher", "Khuyến mãi", "Hoàn lại 1 lượt dùng mã " + km.getMaKhuyenMai() + " do hủy đơn " + maDatPhong);
                    }
                }
                db.commitTransaction();
                LogService.addLog("Hủy đặt phòng", "Nghiệp vụ", "Hủy đơn " + maDatPhong + " kèm phạt " + phiPhat);
                return null;
            } else {
                db.rollbackTransaction();
                return "Lỗi cập nhật CSDL khi hủy!";
            }
        } catch (Exception e) {
            db.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi khi hủy đơn", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThai) {
        DatPhong dp = datPhongDAO.getById(maDatPhong);
        if (dp == null) return false;

        if (trangThai == TrangThaiDatPhong.CONFIRMED || trangThai == TrangThaiDatPhong.CHECKED_IN || trangThai == TrangThaiDatPhong.PARTIALLY_CHECKED_IN) {
            List<ChiTietDatPhong> rooms = ctDAO.getByDatPhong(maDatPhong);
            if (rooms != null) {
                for (ChiTietDatPhong ct : rooms) {
                    if (ct.getPhong() != null) {
                        // Ưu tiên dùng ngày riêng của phòng; fallback về ngày cha của đơn
                        LocalDateTime from = ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : dp.getNgayNhanDuKien();
                        LocalDateTime to   = ct.getNgayTraDuKien()  != null ? ct.getNgayTraDuKien()  : dp.getNgayTraDuKien();
                        if (!phongDAO.isRoomAvailable(ct.getPhong().getMaPhong(), from, to, dp.getMaDatPhong())) {
                            return false;
                        }
                    }
                }
            }
        }

        dp.setTrangThai(trangThai);
        return datPhongDAO.update(dp);
    }

    public String moveToWaitlist(String maDatPhong) {
        DatPhong dp = datPhongDAO.getById(maDatPhong);
        if (dp == null) return "Không tìm thấy đặt phòng!";
        if (dp.getTrangThai() != TrangThaiDatPhong.PENDING && dp.getTrangThai() != TrangThaiDatPhong.CONFIRMED) {
            return "Chỉ có thể đưa đơn ở trạng thái Chờ xác nhận hoặc Đã xác nhận vào danh sách chờ!";
        }
        int nextOrder = datPhongDAO.getNextWaitlistOrder();
        dp.setTrangThai(TrangThaiDatPhong.WAITLIST);
        dp.setThuTuWaitlist(nextOrder);
        if (datPhongDAO.update(dp)) {
            LogService.addLog("Đưa vào danh sách chờ", "Nghiệp vụ", "Đơn " + maDatPhong + " xếp thứ " + nextOrder);
            return null;
        }
        return "Lỗi cập nhật CSDL!";
    }

    // Overload for String status
    public boolean updateTrangThai(String maDatPhong, String trangThai) {
        try {
            return updateTrangThai(maDatPhong, TrangThaiDatPhong.valueOf(trangThai));
        } catch (Exception e) {
            if ("Đã checkin".equalsIgnoreCase(trangThai)) return updateTrangThai(maDatPhong, TrangThaiDatPhong.CHECKED_IN);
            return false;
        }
    }

    public String generateMaDatPhong() { return datPhongDAO.generateMaDatPhong(); }
    public String them(DatPhong dp) { return datPhong(dp); }
    public String sua(DatPhong dp) { return suaDatPhong(dp); }
    /**
     * Xóa hoàn toàn đơn đặt phòng khỏi hệ thống (Hard delete).
     * Chỉ dùng cho các đơn nháp (PENDING), đơn đã hủy (CANCELLED) không có ràng buộc thanh toán.
     */
    public String xoaDatPhong(String maDatPhong) {
        DatPhong dp = getById(maDatPhong);
        if (dp == null) return "Không tìm thấy đặt phòng để xóa!";

        // 1. Kiểm tra trạng thái an toàn
        if (dp.getTrangThai() != TrangThaiDatPhong.PENDING &&
            dp.getTrangThai() != TrangThaiDatPhong.CANCELLED &&
            dp.getTrangThai() != TrangThaiDatPhong.WAITLIST) {
            return "Chỉ có thể xóa hoàn toàn các đơn PENDING hoặc CANCELLED. Đơn này đang ở trạng thái: " + dp.getTrangThai();
        }

        // 2. Kiểm tra ràng buộc hóa đơn (Cực kỳ quan trọng để giữ lịch sử Checkout/No-show)
        if (hoaDonDAO.countByDatPhong(maDatPhong) > 0) {
            return "Đơn đặt này đã có hóa đơn liên quan (có thể là phí No-show hoặc Tiền phòng). Không thể xóa để bảo toàn dữ liệu tài chính!";
        }

        database.DatabaseConnection db = database.DatabaseConnection.getInstance();
        try {
            db.beginTransaction();

            // Xóa dữ liệu rác liên quan (dùng cho đơn PENDING/CANCELLED)
            if (!sddvDAO.deleteByDatPhong(maDatPhong) || !ctDAO.deleteByDatPhong(maDatPhong))
                throw new IllegalStateException("Không xóa được dữ liệu liên quan");

            if (datPhongDAO.deleteHard(maDatPhong)) {
                db.commitTransaction();
                LogService.addLog("Xóa đặt phòng", "Nghiệp vụ", "Xóa vĩnh viễn đơn " + maDatPhong);
                return null;
            } else {
                db.rollbackTransaction();
                return "Lỗi khi thực thi xóa dữ liệu!";
            }
        } catch (Exception e) {
            db.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi xoaDatPhong: " + maDatPhong, e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    /**
     * Tìm các đơn đặt phòng quá hạn check-in (No-show) bằng SQL.
     */
    public List<DatPhong> getNoShowBookings(int hoursOverdue) {
        return datPhongDAO.getPotentialNoShows(hoursOverdue);
    }

    /**
     * Đánh dấu đơn đặt phòng là No-show và xử lý tài chính (phạt tiền cọc).
     * @param maDatPhong Mã đơn đặt phòng
     * @param maNhanVien Mã nhân viên thực hiện (để gán vào hóa đơn phạt)
     * @param penalty Phí phạt (thường là tiền cọc, hoặc số tiền nhập từ UI)
     */
    public String markNoShow(String maDatPhong, String maNhanVien, double penalty) {
        database.DatabaseConnection db = database.DatabaseConnection.getInstance();
        try {
            db.beginTransaction();

            DatPhong dp = datPhongDAO.getById(maDatPhong);
            if (dp == null) {
                db.rollbackTransaction();
                return "Không tìm thấy đặt phòng!";
            }
            if (dp.getTrangThai() != entity.enums.TrangThaiDatPhong.CONFIRMED &&
                dp.getTrangThai() != entity.enums.TrangThaiDatPhong.PENDING) {
                db.rollbackTransaction();
                return "Chỉ có thể đánh dấu No-show cho đơn PENDING hoặc CONFIRMED!";
            }

            // Nếu không truyền phí phạt (<=0), mặc định lấy tiền cọc
            if (penalty < 0) penalty = dp.getTienDatCoc();

            // 1. Phạt tiền cọc / phí No-show (nếu có)
            if (penalty > 0) {
                HoaDon hd = new HoaDon();
                hd.setMaHoaDon(hoaDonDAO.generateMaHD());
                hd.setDatPhong(dp);
                hd.setNhanVien(nhanVienDAO.getById(maNhanVien));
                hd.setNgayLap(LocalDateTime.now());
                hd.setTongThanhToan(penalty);
                hd.setTrangThai(entity.enums.TrangThaiThanhToan.PAID);
                hd.setPhuongThucThanhToan("DEPOSIT"); // Khấu trừ từ tiền cọc

                if (!hoaDonDAO.insert(hd)) {
                    db.rollbackTransaction();
                    return "Lỗi tạo hóa đơn phạt No-show!";
                }

                // Chi tiết hóa đơn
                ChiTietHoaDon cthd = new ChiTietHoaDon();
                cthd.setHoaDon(hd);
                cthd.setLoaiChiTiet(entity.enums.LoaiChiTietHoaDon.DICH_VU);
                cthd.setNoiDung("Phí phạt Khách không đến (Khấu trừ từ tiền cọc)");
                cthd.setSoLuong(1);
                cthd.setDonGia(penalty);
                cthd.setThanhTien(penalty);

                if (!cthdDAO.insert(cthd)) {
                    db.rollbackTransaction();
                    return "Lỗi tạo chi tiết hóa đơn phạt!";
                }
            }

            // 2. Chuyển trạng thái đặt phòng
            dp.setTrangThai(entity.enums.TrangThaiDatPhong.NO_SHOW);
            dp.setPhiNoShow(penalty);
            dp.setGhiChu((dp.getGhiChu() != null ? dp.getGhiChu() + " | " : "") +
                         "Khách không đến (No-show). Phạt: " + String.format("%,.0f đ", penalty));

            if (!datPhongDAO.update(dp)) {
                db.rollbackTransaction();
                return "Lỗi cập nhật trạng thái đặt phòng!";
            }

            db.commitTransaction();
            LOGGER.info("Mark No-show success: " + maDatPhong + ", Penalty: " + penalty);
            return null;

        } catch (Exception e) {
            db.rollbackTransaction();
            LOGGER.log(java.util.logging.Level.SEVERE, "Lỗi khi xử lý No-show", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    /**
     * Legacy method for simple status update (no penalty logic)
     */
    public String markNoShow(String maDatPhong) {
        return markNoShow(maDatPhong, "NV001", -1); // -1 triggers default to deposit logic
    }

    /**
     * Tìm các đơn CONFIRMED có ngày nhận trong vòng hoursAhead tiếng tới (dùng SQL).
     */
    public List<DatPhong> getUpcomingArrivals(int hoursAhead) {
        return datPhongDAO.getUpcomingArrivals(hoursAhead);
    }

    /**
     * Tìm các đơn PENDING lâu hơn hoursOld tiếng mà chưa được xác nhận (dùng SQL).
     */
    public List<DatPhong> getLongPendingBookings(int hoursOld) {
        return datPhongDAO.getLongPending(hoursOld);
    }

    /**
     * Lấy danh sách các đơn đặt phòng PENDING đã quá hạn nộp cọc.
     */
    public List<DatPhong> getOverdueDepositBookings() {
        List<DatPhong> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (DatPhong dp : datPhongDAO.getAll()) {
            boolean isPendingOrWaitlist = dp.getTrangThai() == TrangThaiDatPhong.PENDING || dp.getTrangThai() == TrangThaiDatPhong.WAITLIST;
            if (isPendingOrWaitlist &&
                dp.getHanNopCoc() != null &&
                dp.getHanNopCoc().isBefore(now)) {
                result.add(dp);
            }
        }
        return result;
    }

    /**
     * Lấy danh sách các đơn đặt phòng PENDING có hạn nộp cọc trong vòng hoursAhead tiếng tới.
     */
    public List<DatPhong> getUpcomingDepositDeadlines(int hoursAhead) {
        List<DatPhong> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = now.plusHours(hoursAhead);
        for (DatPhong dp : datPhongDAO.getAll()) {
            boolean isPendingOrWaitlist = dp.getTrangThai() == TrangThaiDatPhong.PENDING || dp.getTrangThai() == TrangThaiDatPhong.WAITLIST;
            if (isPendingOrWaitlist &&
                dp.getHanNopCoc() != null &&
                dp.getHanNopCoc().isAfter(now) &&
                dp.getHanNopCoc().isBefore(limit)) {
                result.add(dp);
            }
        }
        return result;
    }

    /**
     * Cập nhật thông tin đơn đặt phòng (Sửa đơn).
     */
    public String suaDatPhong(DatPhong dp) {
        if (dp == null || dp.getMaDatPhong() == null) return "Dữ liệu không hợp lệ!";

        // Kiểm tra xem đơn có còn ở trạng thái cho phép sửa không (PENDING/CONFIRMED)
        DatPhong old = datPhongDAO.getById(dp.getMaDatPhong());
        if (old == null) return "Không tìm thấy đơn đặt phòng!";
        if (old.getTrangThai() != TrangThaiDatPhong.PENDING && old.getTrangThai() != TrangThaiDatPhong.CONFIRMED) {
            return "Chỉ có thể sửa đơn ở trạng thái Chờ xác nhận hoặc Đã xác nhận!";
        }

        // Tính khoảng lệch ngày nhận/trả để dịch chuyển ngày của từng phòng tương ứng
        long checkinShiftSeconds = java.time.Duration.between(old.getNgayNhanDuKien(), dp.getNgayNhanDuKien()).getSeconds();
        long checkoutShiftSeconds = java.time.Duration.between(old.getNgayTraDuKien(), dp.getNgayTraDuKien()).getSeconds();

        // Kiểm tra xung đột phòng khi chuyển sang CONFIRMED hoặc CHECKED_IN
        if (dp.getTrangThai() == TrangThaiDatPhong.CONFIRMED || dp.getTrangThai() == TrangThaiDatPhong.CHECKED_IN || dp.getTrangThai() == TrangThaiDatPhong.PARTIALLY_CHECKED_IN) {
            List<ChiTietDatPhong> rooms = ctDAO.getByDatPhong(dp.getMaDatPhong());
            if (rooms != null) {
                for (ChiTietDatPhong ct : rooms) {
                    if (ct.getPhong() != null) {
                        LocalDateTime rIn = ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : old.getNgayNhanDuKien();
                        LocalDateTime rOut = ct.getNgayTraDuKien() != null ? ct.getNgayTraDuKien() : old.getNgayTraDuKien();

                        LocalDateTime newIn = rIn.plusSeconds(checkinShiftSeconds);
                        LocalDateTime newOut = rOut.plusSeconds(checkoutShiftSeconds);

                        if (!phongDAO.isRoomAvailable(ct.getPhong().getMaPhong(), newIn, newOut, dp.getMaDatPhong())) {
                            return "Phòng " + ct.getPhong().getSoPhongDisplay() + " đã có lịch đặt hoặc đang sử dụng trong khoảng thời gian này!";
                        }
                    }
                }
            }
        }

        // Thực hiện cập nhật trong transaction để đảm bảo tính nhất quán
        database.DatabaseConnection db = database.DatabaseConnection.getInstance();
        try {
            db.beginTransaction();
            if (!datPhongDAO.update(dp)) {
                db.rollbackTransaction();
                return "Lỗi cập nhật đơn đặt phòng!";
            }

            List<ChiTietDatPhong> rooms = ctDAO.getByDatPhong(dp.getMaDatPhong());
            if (rooms != null) {
                for (ChiTietDatPhong ct : rooms) {
                    LocalDateTime rIn = ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : old.getNgayNhanDuKien();
                    LocalDateTime rOut = ct.getNgayTraDuKien() != null ? ct.getNgayTraDuKien() : old.getNgayTraDuKien();

                    ct.setNgayNhanDuKien(rIn.plusSeconds(checkinShiftSeconds));
                    ct.setNgayTraDuKien(rOut.plusSeconds(checkoutShiftSeconds));

                    if (!ctDAO.update(ct)) {
                        db.rollbackTransaction();
                        return "Lỗi cập nhật lịch phòng " + (ct.getPhong() != null ? ct.getPhong().getSoPhongDisplay() : "") + "!";
                    }
                }
            }

            db.commitTransaction();
            return null;
        } catch (Exception e) {
            db.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi trong suaDatPhong", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    /**
     * Cập nhật toàn bộ thông tin đơn đặt phòng và các phòng được chọn (Chỉnh sửa phòng/dates toàn diện).
     */
    public String capNhatDatPhongToanDien(DatPhong dp) {
        if (dp == null || dp.getMaDatPhong() == null) return "Dữ liệu không hợp lệ!";

        DatPhong old = datPhongDAO.getById(dp.getMaDatPhong());
        if (old == null) return "Không tìm thấy đơn đặt phòng!";
        if (old.getTrangThai() != TrangThaiDatPhong.PENDING && old.getTrangThai() != TrangThaiDatPhong.CONFIRMED && old.getTrangThai() != TrangThaiDatPhong.WAITLIST) {
            return "Chỉ có thể sửa đơn ở trạng thái Chờ xác nhận, Đã xác nhận hoặc Chờ xếp phòng!";
        }

        // Kiểm tra tính sẵn sàng của các phòng mới chọn
        if (dp.getDsChiTiet() != null) {
            for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                if (ct.getPhong() != null) {
                    LocalDateTime rIn = ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : dp.getNgayNhanDuKien();
                    LocalDateTime rOut = ct.getNgayTraDuKien() != null ? ct.getNgayTraDuKien() : dp.getNgayTraDuKien();

                    if (!phongDAO.isRoomAvailable(ct.getPhong().getMaPhong(), rIn, rOut, dp.getMaDatPhong())) {
                        return "Phòng " + ct.getPhong().getSoPhongDisplay() + " đã có lịch đặt hoặc đang sử dụng trong khoảng thời gian này!";
                    }
                }
            }
        }

        database.DatabaseConnection db = database.DatabaseConnection.getInstance();
        try {
            db.beginTransaction();

            // 1. Cập nhật DatPhong header
            if (!datPhongDAO.update(dp)) {
                db.rollbackTransaction();
                return "Lỗi cập nhật đơn đặt phòng!";
            }

            // 2. Xóa các ChiTietDatPhong cũ
            if (!ctDAO.deleteByDatPhong(dp.getMaDatPhong())) {
                db.rollbackTransaction();
                return "Lỗi xóa chi tiết phòng cũ!";
            }

            // 3. Thêm các ChiTietDatPhong mới
            if (dp.getDsChiTiet() != null) {
                for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                    if (ct.getMaChiTiet() == null || ct.getMaChiTiet().isEmpty()) {
                        ct.setMaChiTiet(ctDAO.generateMaChiTiet());
                    }
                    ct.setDatPhong(dp);
                    if (!ctDAO.insert(ct)) {
                        db.rollbackTransaction();
                        return "Lỗi thêm chi tiết phòng mới (" + (ct.getPhong() != null ? ct.getPhong().getSoPhongDisplay() : "?") + ")!";
                    }
                }
            }

            db.commitTransaction();
            return null;
        } catch (Exception e) {
            db.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi trong capNhatDatPhongToanDien", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    /**
     * Tính phí hủy phòng dựa trên chính sách khách sạn.
     */
    public double tinhPhiHuy(DatPhong dp) {
        if (dp == null || dp.getNgayNhanDuKien() == null) return 0;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadlineFree = dp.getNgayNhanDuKien().minusHours(24);

        if (now.isBefore(deadlineFree)) {
            return 0; // Hủy sớm miễn phí
        } else if (now.isBefore(dp.getNgayNhanDuKien())) {
            return dp.getTienDatCoc() * 0.5; // Hủy trong vòng 24h phạt 50%
        } else {
            return dp.getTienDatCoc(); // Hủy sau giờ check-in phạt toàn bộ cọc
        }
    }
}
