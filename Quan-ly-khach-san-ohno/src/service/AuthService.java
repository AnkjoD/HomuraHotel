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
    private entity.GiaoCa currentShift;
    private final dao.TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();
    private final dao.GiaoCaDAO giaoCaDAO = new dao.GiaoCaDAO();

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public NhanVien dangNhap(String tenDN, String matKhau) {
        TaiKhoan tk = taiKhoanDAO.dangNhap(tenDN, matKhau);
        if (tk != null) {
            currentAccount = tk;
            // Ghi nhật ký đăng nhập
            LogService.addLog("Đăng nhập", "Hệ thống", "Nhân viên " + tk.getNhanVien().getHoTen() + " (" + tenDN + ") đăng nhập thành công");
            
            // Khôi phục ca làm việc cũ (nếu có)
            this.currentShift = giaoCaDAO.findCurrentShift(tk.getNhanVien().getMaNhanVien());

            // Cập nhật lần đăng nhập cuối
            try {
                taiKhoanDAO.updateLanDangNhapCuoi(tenDN);
            } catch (Exception ignored) { /* không ảnh hưởng login */ }
            return tk.getNhanVien();
        }
        if (tk == null) {
            LogService.addLog("Đăng nhập thất bại", "Hệ thống", "Thử đăng nhập với tài khoản: " + tenDN);
        }
        return null;
    }

    /** Kiểm tra tài khoản bị khóa (credentials đúng nhưng trangThai = 0) */
    public boolean isAccountDisabled(String tenDN, String matKhau) {
        return taiKhoanDAO.isAccountDisabled(tenDN, matKhau);
    }

    public void dangXuat() { 
        if (currentAccount != null) {
            LogService.addLog("Đăng xuất", "Hệ thống", "Tài khoản " + currentAccount.getTenDangNhap() + " đăng xuất");
        }
        currentAccount = null; 
        currentShift = null;
    }

    public void setCurrentShift(entity.GiaoCa shift) { this.currentShift = shift; }
    public entity.GiaoCa getCurrentShift() { return currentShift; }

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
        return role == VaiTro.MANAGER;
    }

    public String getCurrentMaNV() {
        NhanVien nv = getCurrentUser();
        return nv != null ? nv.getMaNhanVien() : null;
    }

    /** Lấy vai trò hiện tại (null nếu chưa đăng nhập) */
    public VaiTro getCurrentRole() {
        return currentAccount != null ? currentAccount.getVaiTro() : null;
    }

    /**
     * Kiểm tra vai trò hiện tại có quyền truy cập (xem) module không.
     * MANAGER: xem tất cả.
     * ADMIN: chỉ xem các module kỹ thuật.
     * RECEPTIONIST: chỉ nghiệp vụ cơ bản.
     */
    public boolean hasAccess(String moduleKey) {
        if (currentAccount == null) return false;
        VaiTro role = currentAccount.getVaiTro();

        if (role == VaiTro.MANAGER) return true;

        if (role == VaiTro.ADMIN) {
            switch (moduleKey) {
                case "nhanvien":
                case "qlhethong":
                case "nhatky":
                    return true;
                default:
                    return false;
            }
        }

        // RECEPTIONIST
        switch (moduleKey) {
            case "tongquan":
            case "kehoach":
            case "datphong":
            case "thuephong":
            case "hoadon":
            case "khachhang":
                return true;
            default:
                return false;
        }
    }

    /**
     * Kiểm tra có quyền chỉnh sửa (Thêm/Sửa/Xóa) trong module.
     * - MANAGER: full permissions (except reports/statistics)
     * - ADMIN: only accounts/permissions ('nhanvien')
     */
    public boolean canEdit(String moduleKey) {
        if (currentAccount == null) return false;
        VaiTro role = currentAccount.getVaiTro();

        if (role == VaiTro.MANAGER) {
            if ("thongke".equals(moduleKey)) return false;
            return true;
        }

        if (role == VaiTro.ADMIN) {
            return "nhanvien".equals(moduleKey);
        }

        switch (moduleKey) {
            case "nhanvien":
            case "thongke":
            case "qlhethong":
                return false;
            default:
                return hasAccess(moduleKey);
        }
    }
}
