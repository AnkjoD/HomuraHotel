package service;

import dao.KhachHangDAO;
import entity.KhachHang;
import java.util.*;

public class KhachHangService {
    private final KhachHangDAO dao = new KhachHangDAO();

    public List<KhachHang> getAll() { return dao.getAll(); }
    public List<KhachHang> getAllWithInactive() { return dao.getAllWithInactive(); }

    public List<KhachHang> search(String keyword) { return dao.timKiem(keyword); }

    public List<KhachHang> search(String keyword, String dummy) { return search(keyword); }

    public KhachHang getById(String maKhachHang) { return dao.getById(maKhachHang); }

    public KhachHang getByCCCD(String cccd) { return dao.getByCCCD(cccd); }

    public KhachHang getByPhone(String sdt) { return dao.getByPhone(sdt); }

    public String them(KhachHang kh) {
        if (kh.getHoTen() == null || kh.getHoTen().isBlank()) return "Họ tên không được trống!";
        if (kh.getSdt() == null || kh.getSdt().isBlank()) return "Số điện thoại không được trống!";

        // Double check uniqueness of phone
        KhachHang existingByPhone = dao.getByPhone(kh.getSdt().trim());
        if (existingByPhone != null && !"Ngừng".equals(existingByPhone.getTrangThai())) {
            return "Số điện thoại này đã được sử dụng bởi khách hàng đang hoạt động!";
        }

        // Double check uniqueness of CCCD
        if (kh.getCccd() != null && !kh.getCccd().trim().isEmpty()) {
            KhachHang existingByCCCD = dao.getByCCCD(kh.getCccd().trim());
            if (existingByCCCD != null && !"Ngừng".equals(existingByCCCD.getTrangThai())) {
                return "Số CCCD này đã được sử dụng bởi khách hàng đang hoạt động!";
            }
        }

        if (kh.getMaKhachHang() == null || kh.getMaKhachHang().isBlank()) {
            kh.setMaKhachHang(dao.generateMaKH());
        }
        return dao.insert(kh) ? null : "Lỗi thêm khách hàng!";
    }

    public String sua(KhachHang kh) {
        if (kh.getHoTen() == null || kh.getHoTen().isBlank()) return "Họ tên không được trống!";

        // Double check uniqueness of phone
        if (kh.getSdt() != null && !kh.getSdt().trim().isEmpty()) {
            KhachHang existingByPhone = dao.getByPhone(kh.getSdt().trim());
            if (existingByPhone != null && !existingByPhone.getMaKhachHang().equals(kh.getMaKhachHang()) && !"Ngừng".equals(existingByPhone.getTrangThai())) {
                return "Số điện thoại này đã được sử dụng bởi khách hàng đang hoạt động!";
            }
        }

        // Double check uniqueness of CCCD
        if (kh.getCccd() != null && !kh.getCccd().trim().isEmpty()) {
            KhachHang existingByCCCD = dao.getByCCCD(kh.getCccd().trim());
            if (existingByCCCD != null && !existingByCCCD.getMaKhachHang().equals(kh.getMaKhachHang()) && !"Ngừng".equals(existingByCCCD.getTrangThai())) {
                return "Số CCCD này đã được sử dụng bởi khách hàng đang hoạt động!";
            }
        }

        return dao.update(kh) ? null : "Lỗi cập nhật!";
    }

    public String xoa(String maKhachHang) {
        return dao.delete(maKhachHang) ? null : "Không thể xóa!";
    }

    public String activate(String maKhachHang) {
        KhachHang kh = dao.getById(maKhachHang);
        if (kh == null) return "Khách hàng không tồn tại!";

        // Kiểm tra trùng SĐT với các khách hàng đang hoạt động khác
        if (kh.getSdt() != null && !kh.getSdt().trim().isEmpty()) {
            KhachHang duplicate = dao.getByPhone(kh.getSdt().trim());
            if (duplicate != null && !duplicate.getMaKhachHang().equals(maKhachHang) && !"Ngừng".equals(duplicate.getTrangThai())) {
                return "Không thể kích hoạt lại! Số điện thoại này đang được sử dụng bởi khách hàng đang hoạt động: " + duplicate.getHoTen() + " (" + duplicate.getMaKhachHang() + ")";
            }
        }

        // Kiểm tra trùng CCCD với các khách hàng đang hoạt động khác
        if (kh.getCccd() != null && !kh.getCccd().trim().isEmpty()) {
            KhachHang duplicate = dao.getByCCCD(kh.getCccd().trim());
            if (duplicate != null && !duplicate.getMaKhachHang().equals(maKhachHang) && !"Ngừng".equals(duplicate.getTrangThai())) {
                return "Không thể kích hoạt lại! Số CCCD này đang được sử dụng bởi khách hàng đang hoạt động: " + duplicate.getHoTen() + " (" + duplicate.getMaKhachHang() + ")";
            }
        }

        return dao.activate(maKhachHang) ? null : "Không thể kích hoạt lại!";
    }

    /**
     * Cập nhật số lần ở và doanh thu đóng góp của khách hàng.
     * Thường gọi sau khi thanh toán hóa đơn.
     */
    public boolean capNhatThongKe(String maKh, double chiTieuThem, boolean tangSoLanO) {
        if (maKh == null || maKh.isEmpty()) return false;
        return dao.updateCRMStats(maKh, chiTieuThem, tangSoLanO);
    }
}
