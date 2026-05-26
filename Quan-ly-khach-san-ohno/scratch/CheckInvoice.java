package scratch;

import database.DatabaseConnection;
import java.sql.*;

public class CheckInvoice {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT hd.maHoaDon, hd.tongTienPhong, hd.tongTienDichVu, hd.tienGiamKhuyenMai, hd.tongThanhToan, ct.loai, ct.moTa, ct.thanhTien " +
                         "FROM HoaDon hd " +
                         "LEFT JOIN ChiTietHoaDon ct ON hd.maHoaDon = ct.maHoaDon " +
                         "WHERE hd.maDatPhong = 'DP-20260526-003'";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                System.out.println("=== INVOICES AND DETAILS ===");
                while (rs.next()) {
                    System.out.println("Invoice: " + rs.getString("maHoaDon") +
                                       ", TongPhong: " + rs.getDouble("tongTienPhong") +
                                       ", TongDV: " + rs.getDouble("tongTienDichVu") +
                                       ", GiamKM: " + rs.getDouble("tienGiamKhuyenMai") +
                                       ", TongThanhToan: " + rs.getDouble("tongThanhToan") +
                                       " | Detail: [" + rs.getString("loai") + "] " + rs.getString("moTa") +
                                       " (" + rs.getDouble("thanhTien") + ")");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
