package service;

import dao.TaiKhoanDAO;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;

/**
 * Quản lý phiên đăng nhập (Session).
 * Dùng Singleton để giữ account đang đăng nhập trong suốt phiên.
 */
public class AuthService {

    private static AuthService instance;
    private TaiKhoan currentAccount;
    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public NhanVien dangNhap(String tenDN, String matKhau) {
        TaiKhoan tk = taiKhoanDAO.dangNhap(tenDN, matKhau);
        if (tk != null) {
            currentAccount = tk;
            return tk.getNhanVien();
        }
        return null;
    }

    public void dangXuat() { currentAccount = null; }

    public TaiKhoan getCurrentAccount() { return currentAccount; }

    public NhanVien getCurrentUser() { 
        return currentAccount != null ? currentAccount.getNhanVien() : null; 
    }

    public boolean isLoggedIn() { return currentAccount != null; }

    public boolean isAdmin() {
        return currentAccount != null && currentAccount.getVaiTro() == VaiTro.ADMIN;
    }

    public boolean isManager() {
        if (currentAccount == null) return false;
        VaiTro role = currentAccount.getVaiTro();
        return role == VaiTro.ADMIN || role == VaiTro.MANAGER;
    }

    public String getCurrentMaNV() {
        NhanVien nv = getCurrentUser();
        return nv != null ? nv.getMaNhanVien() : null;
    }
}
