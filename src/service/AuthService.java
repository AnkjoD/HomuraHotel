package service;

import dao.GiaoCaDAO;
import dao.TaiKhoanDAO;
import database.DatabaseConnection;
import database.DemoDataStore;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;

public class AuthService {

    private static AuthService instance;
    private TaiKhoan currentAccount;
    private entity.GiaoCa currentShift;
    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAO();
    private final GiaoCaDAO giaoCaDAO = new GiaoCaDAO();

    private AuthService() {}

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    public NhanVien dangNhap(String tenDN, String matKhau) {
        if (DatabaseConnection.isDemoMode()) {
            for (TaiKhoan tk : DemoDataStore.getInstance().getAccounts()) {
                if (tk.getTenDangNhap().equalsIgnoreCase(tenDN) && tk.getMatKhau().equals(matKhau)) {
                    currentAccount = tk;
                    return tk.getNhanVien();
                }
            }
            // Tiện ích cho chế độ demo: nếu nhập đúng user mẫu thì chấp nhận
            if ("admin".equalsIgnoreCase(tenDN) && "admin".equals(matKhau)) {
                currentAccount = DemoDataStore.getInstance().getAccounts().get(0);
                return currentAccount.getNhanVien();
            }
            if ("manager".equalsIgnoreCase(tenDN) && ("123".equals(matKhau) || "manager".equals(matKhau))) {
                currentAccount = DemoDataStore.getInstance().getAccounts().get(1);
                return currentAccount.getNhanVien();
            }
            if ("recept1".equalsIgnoreCase(tenDN) && ("123".equals(matKhau) || "recept1".equals(matKhau))) {
                currentAccount = DemoDataStore.getInstance().getAccounts().get(2);
                return currentAccount.getNhanVien();
            }
            return null;
        }

        TaiKhoan tk = taiKhoanDAO.dangNhap(tenDN, matKhau);
        if (tk != null) {
            currentAccount = tk;
            LogService.addLog("Đăng nhập", "Hệ thống", "Nhân viên " + tk.getNhanVien().getHoTen() + " (" + tenDN + ") đăng nhập thành công");
            this.currentShift = giaoCaDAO.findCurrentShift(tk.getNhanVien().getMaNhanVien());
            try {
                taiKhoanDAO.updateLanDangNhapCuoi(tenDN);
            } catch (Exception ignored) { }
            return tk.getNhanVien();
        }

        LogService.addLog("Đăng nhập thất bại", "Hệ thống", "Thử đăng nhập tài khoản: " + tenDN);
        return null;
    }

    public boolean isAccountDisabled(String tenDN, String matKhau) {
        if (DatabaseConnection.isDemoMode()) return false;
        return taiKhoanDAO.isAccountDisabled(tenDN, matKhau);
    }

    public void dangXuat() { 
        if (currentAccount != null && !DatabaseConnection.isDemoMode()) {
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
        return currentAccount.getVaiTro() == VaiTro.MANAGER;
    }

    public String getCurrentMaNV() {
        NhanVien nv = getCurrentUser();
        return nv != null ? nv.getMaNhanVien() : null;
    }

    public VaiTro getCurrentRole() {
        return currentAccount != null ? currentAccount.getVaiTro() : null;
    }

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

    public boolean canEdit(String moduleKey) {
        if (currentAccount == null) return false;
        VaiTro role = currentAccount.getVaiTro();

        if (role == VaiTro.MANAGER) {
            return !"thongke".equals(moduleKey);
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
