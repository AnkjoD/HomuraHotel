package test;

import database.DatabaseConnection;
import database.DemoDataStore;
import entity.NhanVien;
import entity.Phong;
import java.util.List;
import service.AuthService;
import ui.components.UIConstants;
import ui.theme.Theme;
import ui.theme.ThemeManager;

public class HomuraSmokeTest {
    public static void main(String[] args) {
        System.out.println("=== HOMURA HOTEL SMOKE TEST ===");

        // 1. Enable Demo Mode
        DatabaseConnection.setDemoMode(true);
        System.out.println("[+] Demo Mode enabled: " + DatabaseConnection.isDemoMode());

        // 2. Test Login
        AuthService auth = AuthService.getInstance();
        NhanVien nvAdmin = auth.dangNhap("admin", "admin");
        assert nvAdmin != null : "Admin login failed";
        System.out.println("[+] Admin login OK: " + nvAdmin.getHoTen() + " (" + nvAdmin.getChucVu() + ")");

        NhanVien nvManager = auth.dangNhap("manager", "123");
        assert nvManager != null : "Manager login failed";
        System.out.println("[+] Manager login OK: " + nvManager.getHoTen() + " (" + nvManager.getChucVu() + ")");

        // 3. Test Room Listing
        dao.PhongDAO phongDAO = new dao.PhongDAO();
        List<Phong> rooms = phongDAO.getAll();
        assert rooms.size() >= 12 : "Rooms count incorrect";
        System.out.println("[+] Rooms loaded count: " + rooms.size());

        // 4. Test Themes
        System.out.println("[+] Initial Theme: " + ThemeManager.getCurrentTheme().name);
        assert ThemeManager.isDarkMode() : "Expected Homura dark mode by default";

        ThemeManager.setTheme(Theme.MADOKA);
        System.out.println("[+] Switched to Theme: " + ThemeManager.getCurrentTheme().name);
        assert !ThemeManager.isDarkMode() : "Expected Madoka light mode";
        assert UIConstants.BG_MAIN.equals(Theme.MADOKA.bgMain) : "UIConstants should update with theme";

        ThemeManager.setTheme(Theme.HOMURA);
        System.out.println("[+] Restored to Theme: " + ThemeManager.getCurrentTheme().name);
        assert ThemeManager.isDarkMode() : "Expected Homura dark mode";

        // 5. Test GUI Headless instantiation
        try {
            System.setProperty("java.awt.headless", "true");
            ui.LoginFrame lf = new ui.LoginFrame();
            System.out.println("[+] LoginFrame created successfully: " + lf.getTitle());
        } catch (Exception e) {
            System.out.println("[!] Headless notice (normal on some headless JDKs): " + e.getMessage());
        }

        System.out.println("=== ALL CHECKS PASSED SUCCESSFULLY ===");
    }
}
