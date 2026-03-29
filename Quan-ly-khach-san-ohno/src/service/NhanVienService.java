package service;

import dao.NhanVienDAO;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;
import java.util.List;

public class NhanVienService {
    private final NhanVienDAO dao = new NhanVienDAO();

    public List<NhanVien> getAll() { return dao.getAll(); }

    public List<NhanVien> search(String kw, String chucVu) { return dao.search(kw, chucVu); }

    public NhanVien getById(String maNhanVien) { return dao.getById(maNhanVien); }

    public String them(NhanVien nv) {
        if (nv.getHoTen() == null || nv.getHoTen().isBlank()) return "Họ tên không được trống!";
        if (nv.getSdt() == null || nv.getSdt().isBlank()) return "Số điện thoại không được trống!";
        if (nv.getMaNhanVien() == null || nv.getMaNhanVien().isBlank()) {
            nv.setMaNhanVien(dao.generateMaNV());
        }
        
        if (nv.getTaiKhoan() == null) {
            TaiKhoan tk = new TaiKhoan();
            tk.setTenDangNhap(nv.getMaNhanVien());
            tk.setMatKhau("123456");
            tk.setVaiTro(VaiTro.RECEPTIONIST);
            nv.setTaiKhoan(tk);
            tk.setNhanVien(nv);
        } else {
            // Đảm bảo tenDangNhap luôn được set
            if (nv.getTaiKhoan().getTenDangNhap() == null || nv.getTaiKhoan().getTenDangNhap().isBlank()) {
                nv.getTaiKhoan().setTenDangNhap(nv.getMaNhanVien());
            }
            nv.getTaiKhoan().setNhanVien(nv);
        }
        
        return dao.insert(nv) ? null : "Lỗi thêm nhân viên!";
    }

    public String sua(NhanVien nv) {
        if (nv.getHoTen() == null || nv.getHoTen().isBlank()) return "Họ tên không được trống!";
        return dao.update(nv) ? null : "Lỗi cập nhật!";
    }

    public String xoa(String maNhanVien) {
        return dao.delete(maNhanVien) ? null : "Không thể xóa nhân viên này!";
    }
}
