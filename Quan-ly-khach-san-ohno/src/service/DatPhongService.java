package service;

import dao.*;
import entity.*;
import entity.enums.TrangThaiDatPhong;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

public class DatPhongService {

    private static final Logger LOGGER = Logger.getLogger(DatPhongService.class.getName());

    private final DatPhongDAO        datPhongDAO = new DatPhongDAO();
    private final PhongDAO           phongDAO    = new PhongDAO();
    private final ChiTietDatPhongDAO ctDAO       = new ChiTietDatPhongDAO();

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
        // Validation
        if (dp == null) return "Dữ liệu đặt phòng không hợp lệ!";
        if (dp.getKhachHang() == null || dp.getKhachHang().getMaKhachHang() == null) 
            return "Chưa chọn khách hàng!";
        if (dp.getNgayNhan() == null || dp.getNgayTra() == null) 
            return "Ngày không hợp lệ!";
        if (!dp.getNgayTra().isAfter(dp.getNgayNhan())) 
            return "Ngày trả phải sau ngày nhận!";
        if (dp.getSoLuongKhach() <= 0)
            return "Số lượng khách phải lớn hơn 0!";
        
        // Validate sức chứa phòng
        if (dp.getDsChiTiet() != null) {
            for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                if (ct.getPhong() != null) {
                    Phong p = phongDAO.getById(ct.getPhong().getMaPhong());
                    if (p != null && p.getLoaiPhong() != null) {
                        if (dp.getSoLuongKhach() > p.getLoaiPhong().getSucChua()) {
                            return "Phòng " + p.getMaPhong() + " chỉ chứa tối đa " 
                                   + p.getLoaiPhong().getSucChua() + " người!";
                        }
                    }
                }
            }
        }
        
        if (dp.getTrangThai() == null) dp.setTrangThai(TrangThaiDatPhong.CONFIRMED);
        
        // Generate maDatPhong if not set
        if (dp.getMaDatPhong() == null || dp.getMaDatPhong().isEmpty()) {
            dp.setMaDatPhong(datPhongDAO.generateMaDatPhong());
        }
        if (dp.getNgayDat() == null) {
            dp.setNgayDat(LocalDateTime.now());
        }
        
        boolean ok = datPhongDAO.insert(dp);
        if (!ok) return "Lỗi đặt phòng!";
        
        // Insert ChiTietDatPhong for each room in the booking
        if (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty()) {
            for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                if (ct.getMaChiTiet() == null || ct.getMaChiTiet().isEmpty()) {
                    ct.setMaChiTiet(ctDAO.generateMaChiTiet());
                }
                ct.setDatPhong(dp);
                if (!ctDAO.insert(ct)) {
                    return "Lỗi thêm chi tiết đặt phòng!";
                }
            }
        }
        
        LOGGER.info("Đặt phòng thành công: " + dp.getMaDatPhong());
        return null;
    }

    public String huyDatPhong(String maDatPhong) {
        DatPhong dp = datPhongDAO.getById(maDatPhong);
        if (dp == null) return "Không tìm thấy đặt phòng!";
        if (dp.getTrangThai() == TrangThaiDatPhong.CHECKED_IN) 
            return "Không thể hủy đặt phòng đang ở!";
        if (dp.getTrangThai() == TrangThaiDatPhong.CHECKED_OUT) 
            return "Không thể hủy đặt phòng đã check-out!";
        dp.setTrangThai(TrangThaiDatPhong.CANCELLED);
        boolean ok = datPhongDAO.update(dp);
        return ok ? null : "Lỗi hủy đặt phòng!";
    }

    public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThai) {
        DatPhong dp = datPhongDAO.getById(maDatPhong);
        if (dp == null) return false;
        dp.setTrangThai(trangThai);
        return datPhongDAO.update(dp);
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
    public String sua(DatPhong dp) { return datPhongDAO.update(dp) ? null : "Lỗi cập nhật!"; }

    /**
     * Tìm các đơn đặt phòng quá hạn check-in (no-show).
     */
    public List<DatPhong> getNoShowBookings(int hoursOverdue) {
        List<DatPhong> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (DatPhong dp : datPhongDAO.getAll()) {
            if (dp.getTrangThai() == TrangThaiDatPhong.CONFIRMED
                && dp.getNgayNhanDuKien() != null
                && dp.getNgayNhanDuKien().plusHours(hoursOverdue).isBefore(now)) {
                result.add(dp);
            }
        }
        return result;
    }

    /**
     * Đánh dấu đơn đặt phòng là No-show.
     */
    public String markNoShow(String maDatPhong) {
        DatPhong dp = datPhongDAO.getById(maDatPhong);
        if (dp == null) return "Không tìm thấy đặt phòng!";
        if (dp.getTrangThai() != TrangThaiDatPhong.CONFIRMED) return "Chỉ hủy đơn đã xác nhận!";
        dp.setTrangThai(TrangThaiDatPhong.CANCELLED);
        dp.setGhiChu((dp.getGhiChu() != null ? dp.getGhiChu() + " | " : "") + "No-show: KH không đến");
        return datPhongDAO.update(dp) ? null : "Lỗi cập nhật!";
    }
}
