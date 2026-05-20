package scratch;

import dao.ThongKeDAO;
import java.util.Map;

public class TestThongKe {
    public static void main(String[] args) {
        System.out.println("Starting ThongKe test...");
        try {
            ThongKeDAO dao = new ThongKeDAO();
            
            System.out.println("\n--- Testing getDashboardStats ---");
            Map<String, Object> dash = dao.getDashboardStats();
            System.out.println("Result: " + dash);
            
            System.out.println("\n--- Testing getThongKeKy('thang') ---");
            Map<String, Object> ky = dao.getThongKeKy("thang");
            System.out.println("Result: " + ky);
            
            System.out.println("\n--- Testing getDoanhThuTheoNgay('thang') ---");
            java.util.List<long[]> daily = dao.getDoanhThuTheoNgay("thang");
            System.out.println("Result count: " + daily.size());
            
            System.out.println("\n--- Testing getTopPhongDoanhThu ---");
            System.out.println("Result: " + dao.getTopPhongDoanhThu(5, "thang"));
            
            System.out.println("\n--- Testing getTopDichVu ---");
            System.out.println("Result: " + dao.getTopDichVu(5, "thang"));
            
            System.out.println("\n--- Testing getAlerts ---");
            System.out.println("Result: " + dao.getAlerts());

            System.out.println("\n--- Testing getChiTietPhongStats ---");
            System.out.println("Result: " + dao.getChiTietPhongStats("thang"));

            System.out.println("\n--- Testing getChiTietDichVuStats ---");
            System.out.println("Result: " + dao.getChiTietDichVuStats("thang"));

            System.out.println("\n--- Testing getPhanTichKenhStats ---");
            System.out.println("Result: " + dao.getPhanTichKenhStats("thang"));

            System.out.println("\n--- Testing getPhanTichKhachStats ---");
            System.out.println("Result: " + dao.getPhanTichKhachStats("thang"));
            
            System.out.println("Test finished successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
