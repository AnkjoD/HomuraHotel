package service;

import dao.*;
import entity.*;
import java.util.*;

public class BangGiaService {
    private final BangGiaDAO dao = new BangGiaDAO();
    private final ChiTietBangGiaDAO ctDAO = new ChiTietBangGiaDAO();
    private final LoaiPhongDAO lpDAO = new LoaiPhongDAO();

    public List<BangGia> getAll()                          { return dao.getAll(); }
    public List<BangGia> search(String kw, String tt)      { return dao.search(kw, tt); }
    public BangGia getById(String id)                      { return dao.getById(id); }

    public String them(BangGia bg) {
        // Kiểm tra chồng lấn
        if (bg.isTrangThai()) {
            BangGia overlap = dao.checkOverlap(bg);
            if (overlap != null) {
                return "Trùng thời gian với bảng giá: " + overlap.getTenBangGia() + " (Độ ưu tiên: " + overlap.getMucUuTien() + ")";
            }
        }
        bg.setMaBangGia(dao.generateMaBangGia());
        return dao.insert(bg) ? null : "Lỗi thêm bảng giá!";
    }

    public String sua(BangGia bg) {
        // Kiểm tra chồng lấn
        if (bg.isTrangThai()) {
            BangGia overlap = dao.checkOverlap(bg);
            if (overlap != null) {
                return "Trùng thời gian với bảng giá: " + overlap.getTenBangGia() + " (Độ ưu tiên: " + overlap.getMucUuTien() + ")";
            }
        }
        return dao.update(bg) ? null : "Lỗi cập nhật!";
    }

    public String xoa(String id) {
        return dao.delete(id) ? null : "Lỗi xóa bảng giá!";
    }

    // ---- Chi tiết bảng giá ----
    public List<ChiTietBangGia> getChiTiet(String maBangGia) {
        return ctDAO.getByBangGia(maBangGia);
    }

    /** Save all chi tiet for a bang gia (delete old, insert new) */
    public String saveChiTiet(String maBangGia, List<ChiTietBangGia> dsCT) {
        // Delete old
        ctDAO.deleteByBangGia(maBangGia);
        // Insert new
        BangGia bg = new BangGia();
        bg.setMaBangGia(maBangGia);
        for (ChiTietBangGia ct : dsCT) {
            ct.setBangGia(bg);
            if (!ctDAO.insert(ct)) return "Lỗi lưu chi tiết giá cho " + ct.getLoaiPhong().getTenLoaiPhong();
        }
        return null;
    }

    // ---- Rate Lookup Logic (tra giá hiện hành) ----

    /**
     * Lấy giá hiện hành cho một loại phòng (hôm nay).
     */
    public double layGiaHienHanh(String maLoaiPhong) {
        return layGiaVaoThoiDiem(maLoaiPhong, java.time.LocalDateTime.now());
    }

    /**
     * Lấy giá cho một loại phòng vào một thời điểm cụ thể.
     * Ưu tiên: BangGia active vào thời điểm đó → fallback LoaiPhong.giaTheoNgay
     */
    public double layGiaVaoThoiDiem(String maLoaiPhong, java.time.LocalDateTime thoiDiem) {
        return layGiaVaoThoiDiem(maLoaiPhong, thoiDiem, "ALL");
    }

    /**
     * Lấy giá cho một loại phòng vào một thời điểm cụ thể theo phân loại đối tượng khách hàng.
     * loaiKhach: "Khách lẻ (Cá nhân)" -> "CA_NHAN", "Khách đoàn" -> "DOAN"
     */
    public double layGiaVaoThoiDiem(String maLoaiPhong, java.time.LocalDateTime thoiDiem, String loaiKhach) {
        String segment = "ALL";
        if (loaiKhach != null) {
            String lk = loaiKhach.trim().toLowerCase();
            if (lk.contains("đoàn") || lk.equals("doan")) {
                segment = "DOAN";
            } else if (lk.contains("lẻ") || lk.contains("cá nhân") || lk.equals("ca_nhan")) {
                segment = "CA_NHAN";
            }
        }

        BangGia bgActive = dao.findBestRate(thoiDiem, segment);
        if (bgActive != null) {
            ChiTietBangGia ctbg = ctDAO.findByBangGiaAndLoaiPhong(bgActive.getMaBangGia(), maLoaiPhong);
            if (ctbg != null) {
                // Tự động chọn giữa giá ngày thường và giá cuối tuần
                double gia = ctbg.getGiaTheoNgayTrongTuan(thoiDiem.getDayOfWeek());
                if (gia > 0) return gia;
            }
        }
        // Fallback: lấy giá cơ sở từ LoaiPhong
        LoaiPhong lp = lpDAO.getById(maLoaiPhong);
        return lp != null ? lp.getGiaTheoNgay() : 0;
    }

    /**
     * Lấy giá theo giờ hiện hành cho một loại phòng
     */
    public double layGiaTheoGio(String maLoaiPhong) {
        BangGia bgActive = dao.findActiveToday();
        if (bgActive != null) {
            ChiTietBangGia ctbg = ctDAO.findByBangGiaAndLoaiPhong(bgActive.getMaBangGia(), maLoaiPhong);
            if (ctbg != null && ctbg.getGiaGioDau() > 0) return ctbg.getGiaGioDau();
        }
        return 0;
    }

    /**
     * Lấy phụ phí trả trễ cho loại phòng
     */
    public double layPhuPhiTraTre(String maLoaiPhong) {
        BangGia bgActive = dao.findActiveToday();
        if (bgActive != null) {
            ChiTietBangGia ctbg = ctDAO.findByBangGiaAndLoaiPhong(bgActive.getMaBangGia(), maLoaiPhong);
            if (ctbg != null) return ctbg.getPhuPhiTraTre();
        }
        return 0;
    }

    /**
     * Get tên bảng giá đang active hôm nay (nếu có)
     */
    public String getTenBangGiaActive() {
        BangGia bg = dao.findActiveToday();
        return bg != null ? bg.getTenBangGia() : null;
    }
}
