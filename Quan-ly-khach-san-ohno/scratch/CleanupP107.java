import java.sql.*;
import java.util.Properties;
import java.io.*;

/**
 * Script dọn dẹp toàn diện các đơn đặt phòng của P107 (đã DELETED):
 *  1) Hủy đơn PENDING/CONFIRMED → CANCELLED
 *  2) Force-checkout đơn CHECKED_IN/PARTIALLY_CHECKED_IN đã quá hạn:
 *     - Đánh dấu ngayTraThucTe trong ChiTietDatPhong
 *     - Chuyển trạng thái DatPhong → CHECKED_OUT
 */
public class CleanupP107 {
    static final String ROOM_ID = "P107";

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        String[] paths = {"resources/db.properties", "../resources/db.properties", "db.properties"};
        for (String path : paths) {
            File f = new File(path);
            if (f.exists()) { props.load(new FileInputStream(f)); break; }
        }
        String url  = props.getProperty("db.url",      "jdbc:sqlserver://localhost:1433;databaseName=HotelMs;encrypt=false;trustServerCertificate=true");
        String user = props.getProperty("db.user",     "sa");
        String pass = props.getProperty("db.password", "ILoveHomura@123!");

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("=== DON DEP DON DAT PHONG CUA " + ROOM_ID + " ===\n");

            // ---- BUOC 1: Huy don PENDING/CONFIRMED ----
            System.out.println("[BUOC 1] Huy don PENDING/CONFIRMED...");
            int cancelled;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE DatPhong SET trangThai = 'CANCELLED' " +
                    "WHERE trangThai IN ('PENDING', 'CONFIRMED') " +
                    "AND maDatPhong IN (SELECT maDatPhong FROM ChiTietDatPhong WHERE maPhong = ?)")) {
                ps.setString(1, ROOM_ID);
                cancelled = ps.executeUpdate();
                System.out.println("  => Da huy " + cancelled + " don (PENDING/CONFIRMED -> CANCELLED)");
            }

            // ---- BUOC 2: Force-checkout cac don CHECKED_IN / PARTIALLY_CHECKED_IN qua han ----
            System.out.println("\n[BUOC 2] Force-checkout don CHECKED_IN/PARTIALLY_CHECKED_IN qua han...");

            // 2a. Danh dau ngayTraThucTe cho cac ChiTietDatPhong cua P107 con chua tra
            int ctUpdated;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ChiTietDatPhong " +
                    "SET ngayTraThucTe = GETDATE() " +
                    "WHERE maPhong = ? " +
                    "AND ngayTraThucTe IS NULL " +
                    "AND ngayNhanThucTe IS NOT NULL " +
                    "AND maDatPhong IN (" +
                    "  SELECT maDatPhong FROM DatPhong " +
                    "  WHERE trangThai IN ('CHECKED_IN', 'PARTIALLY_CHECKED_IN')" +
                    ")")) {
                ps.setString(1, ROOM_ID);
                ctUpdated = ps.executeUpdate();
                System.out.println("  => Da cap nhat ngayTraThucTe cho " + ctUpdated + " ChiTietDatPhong");
            }

            // 2b. Chuyen DatPhong sang CHECKED_OUT neu khong con phong nao chua tra
            //     (tuc la tat ca ChiTietDatPhong cua don do deu co ngayTraThucTe)
            int checkedOut;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE DatPhong SET trangThai = 'CHECKED_OUT' " +
                    "WHERE trangThai IN ('CHECKED_IN', 'PARTIALLY_CHECKED_IN') " +
                    "AND maDatPhong IN (SELECT maDatPhong FROM ChiTietDatPhong WHERE maPhong = ?) " +
                    "AND maDatPhong NOT IN (" +
                    "  SELECT maDatPhong FROM ChiTietDatPhong " +
                    "  WHERE ngayTraThucTe IS NULL AND ngayNhanThucTe IS NOT NULL" +
                    ")")) {
                ps.setString(1, ROOM_ID);
                checkedOut = ps.executeUpdate();
                System.out.println("  => Da chuyen " + checkedOut + " don sang CHECKED_OUT");
            }

            // ---- BUOC 3: Ghi log ----
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO NhatKyHeThong (tenDangNhap, hanhDong, doiTuong, chiTiet) VALUES (?,?,?,?)")) {
                ps.setString(1, "SYSTEM_CLEANUP");
                ps.setString(2, "Don dep phong da xoa");
                ps.setString(3, "Phong " + ROOM_ID);
                ps.setString(4, "Huy " + cancelled + " don PENDING/CONFIRMED; Force-checkout " 
                               + checkedOut + " don CHECKED_IN/PARTIALLY_CHECKED_IN cua phong " + ROOM_ID);
                ps.executeUpdate();
                System.out.println("\n  => Da ghi log he thong");
            }

            // ---- KIEM TRA SAU DON DEP ----
            System.out.println("\n=== KIEM TRA SAU DON DEP ===");
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ct.maDatPhong, dp.trangThai, ct.ngayTraThucTe " +
                    "FROM ChiTietDatPhong ct " +
                    "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
                    "WHERE ct.maPhong = ?")) {
                ps.setString(1, ROOM_ID);
                ResultSet rs = ps.executeQuery();
                int remaining = 0;
                while (rs.next()) {
                    String status = rs.getString("trangThai");
                    String ngayTra = rs.getTimestamp("ngayTraThucTe") != null ? rs.getTimestamp("ngayTraThucTe").toString() : "NULL";
                    System.out.printf("  Don: %-20s | Trang thai: %-20s | ngayTraThucTe: %s%n",
                        rs.getString("maDatPhong"), status, ngayTra);
                    if (!status.equals("CANCELLED") && !status.equals("CHECKED_OUT")) remaining++;
                }
                if (remaining == 0) {
                    System.out.println("\n  => THANH CONG! Khong con don nao dang hoat dong cho phong " + ROOM_ID);
                } else {
                    System.out.println("\n  => Con " + remaining + " don can xu ly thu cong!");
                }
            }

            System.out.println("\n=== HOAN TAT ===");
        }
    }
}
