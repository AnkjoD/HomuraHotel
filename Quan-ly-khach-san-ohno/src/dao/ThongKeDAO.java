package dao;

import database.DatabaseConnection;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * ThongKeDAO – đã sửa:
 *  - Thống nhất tên bảng đúng với schema: Phong, DatPhong, ChiTietDatPhong, KhachHang, HoaDon
 *  - Sửa tên cột: maKhachHang, tongThanhToan, ngayLap, maLoaiPhong, tenLoaiPhong
 *  - getAlerts: thêm cảnh báo quá hạn trả & đặt phòng hôm nay chưa checkin
 */
public class ThongKeDAO {

    private static final String TAG = "[ThongKeDAO]";
    private static final Logger LOGGER = Logger.getLogger(ThongKeDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ---- Dashboard tổng quan ----
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            Connection conn = getConn();

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Phong WHERE trangThai='AVAILABLE'")) {
                ResultSet rs = ps.executeQuery();
                stats.put("phongTrong", rs.next() ? rs.getInt(1) : 0);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM Phong WHERE trangThai='OCCUPIED'")) {
                ResultSet rs = ps.executeQuery();
                stats.put("dangO", rs.next() ? rs.getInt(1) : 0);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM DatPhong WHERE trangThai IN ('PENDING','CONFIRMED')")) {
                ResultSet rs = ps.executeQuery();
                stats.put("daDat", rs.next() ? rs.getInt(1) : 0);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Phong")) {
                stats.put("tongPhong", rs.next() ? rs.getInt(1) : 0);
            }
            // Gộp checkin + checkout count vào đây — không cần gọi riêng nữa
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM ChiTietDatPhong " +
                    "WHERE CAST(ngayNhanThucTe AS DATE)=CAST(GETDATE() AS DATE)")) {
                stats.put("checkinHomNay", rs.next() ? rs.getInt(1) : 0);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM ChiTietDatPhong " +
                    "WHERE CAST(ngayTraThucTe AS DATE)=CAST(GETDATE() AS DATE)")) {
                stats.put("checkoutHomNay", rs.next() ? rs.getInt(1) : 0);
            }
            stats.put("doanhThuHomNay", sumRevenueToday(conn));

        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getDashboardStats: " + e.getMessage());
        }
        return stats;
    }

    // ---- Thống kê theo kỳ ----
    public Map<String, Object> getThongKeKy(String ky) {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            Connection conn = getConn();
            stats.put("doanhThu",     sumRevenueByKy(conn, ky));
            stats.put("doanhThuDV",   sumServiceRevenueByKy(conn, ky));

            String sqlLP = "SELECT COUNT(*) FROM ChiTietDatPhong WHERE ngayNhanThucTe IS NOT NULL AND "
                         + getDateFilter("ngayNhanThucTe", ky);
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlLP)) {
                stats.put("luotDatPhong", rs.next() ? rs.getInt(1) : 0);
            }
            String sqlKH = "SELECT COUNT(DISTINCT maKhachHang) FROM DatPhong WHERE "
                         + getDateFilter("ngayDat", ky);
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlKH)) {
                stats.put("khachMoi", rs.next() ? rs.getInt(1) : 0);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Phong WHERE trangThai='OCCUPIED'")) {
                stats.put("phongDangThue", rs.next() ? rs.getInt(1) : 0);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getThongKeKy: " + e.getMessage());
        }
        return stats;
    }

    // ---- Doanh thu theo ngày trong tháng ----
    public List<long[]> getDoanhThuTheoNgay(String ky) {
        List<long[]> result = new ArrayList<>();
        String sql = "SELECT DAY(ngayLap) AS ngay, ISNULL(SUM(tongThanhToan),0) AS dt " +
                     "FROM HoaDon " +
                     "WHERE " + getDateFilter("ngayLap", ky) + " " +
                     "GROUP BY DAY(ngayLap) ORDER BY ngay";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) result.add(new long[]{rs.getLong("ngay"), rs.getLong("dt")});
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING getDoanhThuTheoNgay: " + e.getMessage());
        }
        return result;
    }

    private String getDateFilter(String dateCol, String ky) {
        if (ky != null && ky.startsWith("custom:")) {
            String[] parts = ky.split(":");
            if (parts.length == 3) {
                String d1 = parts[1];
                String d2 = parts[2];
                if (d1.matches("\\d{4}-\\d{2}-\\d{2}") && d2.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    return dateCol + " >= '" + d1 + " 00:00:00' AND " + dateCol + " <= '" + d2 + " 23:59:59'";
                }
            }
        }
        
        switch (ky != null ? ky : "thang") {
            case "7ngay":
                return dateCol + " >= DATEADD(DAY,-7,GETDATE())";
            case "quy":
                return "DATEPART(QUARTER," + dateCol + ")=DATEPART(QUARTER,GETDATE()) " +
                       "AND YEAR(" + dateCol + ")=YEAR(GETDATE())";
            case "nam":
                return "YEAR(" + dateCol + ")=YEAR(GETDATE())";
            case "thang":
            default:
                return "MONTH(" + dateCol + ")=MONTH(GETDATE()) AND YEAR(" + dateCol + ")=YEAR(GETDATE())";
        }
    }

    private long sumRevenueToday(Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(tongThanhToan),0) FROM HoaDon " +
                "WHERE CAST(ngayLap AS DATE)=CAST(GETDATE() AS DATE)")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumRevenueToday: " + e.getMessage());
        }
        return 0L;
    }

    private long sumRevenueByKy(Connection conn, String ky) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(tongThanhToan),0) FROM HoaDon WHERE " + getDateFilter("ngayLap", ky))) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumRevenueByKy: " + e.getMessage());
        }
        return 0L;
    }

    private long sumServiceRevenueByKy(Connection conn, String ky) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(tongTienDichVu),0) FROM HoaDon WHERE " + getDateFilter("ngayLap", ky))) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumServiceRevenueByKy: " + e.getMessage());
        }
        return 0L;
    }

    // ---- Top phòng doanh thu ----
    public List<Map<String, Object>> getTopPhongDoanhThu(int topN, String ky) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT TOP " + topN + " ct.maPhong, lp.tenLoaiPhong, " +
            "  SUM(hd.tongThanhToan) AS dt, COUNT(DISTINCT hd.maHoaDon) AS luot " +
            "FROM HoaDon hd " +
            "JOIN ChiTietDatPhong ct ON hd.maDatPhong = ct.maDatPhong " +
            "JOIN Phong p ON ct.maPhong = p.maPhong " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "WHERE " + getDateFilter("hd.ngayLap", ky) + " " +
            "GROUP BY ct.maPhong, lp.tenLoaiPhong ORDER BY dt DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("soPhong",  rs.getString("maPhong"));
                row.put("tenLoai",  rs.getString("tenLoaiPhong"));
                row.put("doanhThu", rs.getLong("dt"));
                row.put("luot",     rs.getInt("luot"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getTopPhongDoanhThu: " + e.getMessage());
        }
        return list;
    }

    // ---- Check-in hôm nay ----
    public List<Map<String, Object>> getCheckinHomNay() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT ct.maPhong, kh.hoTen, lp.tenLoaiPhong, ct.ngayNhanThucTe " +
            "FROM ChiTietDatPhong ct " +
            "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "JOIN KhachHang kh ON dp.maKhachHang = kh.maKhachHang " +
            "JOIN Phong p ON ct.maPhong = p.maPhong " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "WHERE CAST(ct.ngayNhanThucTe AS DATE)=CAST(GETDATE() AS DATE)";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("soPhong",  rs.getString("maPhong"));
                row.put("hoTen",    rs.getString("hoTen"));
                row.put("tenLoai",  rs.getString("tenLoaiPhong"));
                row.put("ngayNhan", rs.getTimestamp("ngayNhanThucTe"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getCheckinHomNay: " + e.getMessage());
        }
        return list;
    }

    // ---- Check-out hôm nay ----
    public List<Map<String, Object>> getCheckoutHomNay() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT ct.maPhong, kh.hoTen, ct.ngayTraThucTe, p.trangThai " +
            "FROM ChiTietDatPhong ct " +
            "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "JOIN KhachHang kh ON dp.maKhachHang = kh.maKhachHang " +
            "JOIN Phong p ON ct.maPhong = p.maPhong " +
            "WHERE CAST(ct.ngayTraThucTe AS DATE)=CAST(GETDATE() AS DATE)";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("soPhong",  rs.getString("maPhong"));
                row.put("hoTen",    rs.getString("hoTen"));
                row.put("ngayTraDK", rs.getTimestamp("ngayTraThucTe"));
                String tt = rs.getString("trangThai");
                row.put("trangThai",
                    ("CLEANING".equals(tt) || "AVAILABLE".equals(tt)) ? "Đã trả" : "Chờ trả");
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getCheckoutHomNay: " + e.getMessage());
        }
        return list;
    }

    // ---- Hoạt động gần đây ----
    public List<Map<String, Object>> getHoatDongGanDay(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql =
            "SELECT TOP " + limit + " * FROM (" +
            "  SELECT N'Nh\u1EADn ph\u00F2ng' AS loai, ct.maPhong, kh.hoTen, ct.ngayNhanThucTe AS thoiGian " +
            "  FROM ChiTietDatPhong ct " +
            "  JOIN DatPhong dp ON ct.maDatPhong=dp.maDatPhong " +
            "  JOIN KhachHang kh ON dp.maKhachHang=kh.maKhachHang " +
            "  WHERE ct.ngayNhanThucTe IS NOT NULL " +
            "  UNION ALL " +
            "  SELECT N'Tr\u1EA3 ph\u00F2ng', ct.maPhong, kh.hoTen, ct.ngayTraThucTe " +
            "  FROM ChiTietDatPhong ct " +
            "  JOIN DatPhong dp ON ct.maDatPhong=dp.maDatPhong " +
            "  JOIN KhachHang kh ON dp.maKhachHang=kh.maKhachHang " +
            "  WHERE ct.ngayTraThucTe IS NOT NULL" +
            ") AS combined ORDER BY thoiGian DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("loai",     rs.getString("loai"));
                row.put("soPhong",  rs.getString("maPhong"));
                row.put("hoTen",    rs.getString("hoTen"));
                row.put("thoiGian", rs.getTimestamp("thoiGian"));
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getHoatDongGanDay: " + e.getMessage());
        }
        return list;
    }

    // ---- Top dịch vụ ----
    public List<Map<String, Object>> getTopDichVu(int topN, String ky) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT TOP " + topN + " dv.tenDichVu, SUM(sddv.soLuong) AS soLan, " +
            "  SUM(sddv.soLuong * sddv.donGiaLucDung) AS doanhThu " +
            "FROM SuDungDichVu sddv JOIN DichVu dv ON sddv.maDichVu=dv.maDichVu " +
            "WHERE " + getDateFilter("sddv.thoiGianDung", ky) + " " +
            "GROUP BY dv.tenDichVu ORDER BY soLan DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("tenDV",    rs.getString("tenDichVu"));
                row.put("soLan",    rs.getInt("soLan"));
                row.put("doanhThu", rs.getLong("doanhThu"));
                list.add(row);
            }
        } catch (SQLException e) {
            LOGGER.warning(TAG + " getTopDichVu: " + e.getMessage());
        }
        return list;
    }

    // ---- Cảnh báo & Nhắc nhở ----
    public List<Map<String, Object>> getAlerts() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            Connection conn = getConn();

            // Phòng đang vệ sinh
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT maPhong FROM Phong WHERE trangThai='CLEANING'")) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    String maPhong = rs.getString("maPhong");
                    row.put("id", "cleaning_" + maPhong);
                    row.put("title", "P." + maPhong + " – Đang vệ sinh");
                    row.put("desc",  "Cần kiểm tra & xác nhận sạch sẽ");
                    row.put("type",  "warning");
                    list.add(row);
                }
            }

            // Phòng đang bảo trì
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT maPhong FROM Phong WHERE trangThai='MAINTENANCE'")) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    String maPhong = rs.getString("maPhong");
                    row.put("id", "maint_" + maPhong);
                    row.put("title", "P." + maPhong + " – Đang bảo trì");
                    row.put("desc",  "Không cho khách thuê cho đến khi hoàn tất");
                    row.put("type",  "danger");
                    list.add(row);
                }
            }

            // Phòng quá hạn trả
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT ct.maPhong, kh.hoTen, dp.ngayTraDuKien FROM ChiTietDatPhong ct " +
                    "JOIN DatPhong dp ON ct.maDatPhong=dp.maDatPhong " +
                    "JOIN KhachHang kh ON dp.maKhachHang=kh.maKhachHang " +
                    "WHERE ct.ngayTraThucTe IS NULL " +
                    "  AND dp.ngayTraDuKien < GETDATE() " +
                    "  AND dp.trangThai='CHECKED_IN'")) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Timestamp duKien = rs.getTimestamp("ngayTraDuKien");
                    String timeStr = duKien != null ? new java.text.SimpleDateFormat("HH:mm").format(duKien) : "";
                    
                    row.put("title", "P." + rs.getString("maPhong") + " – Quá hạn trả (" + timeStr + ")");
                    row.put("desc",  rs.getString("hoTen") + " chưa làm thủ tục trả phòng");
                    row.put("type",  "danger");
                    row.put("id", "overdue_" + rs.getString("maPhong") + "_" + (duKien != null ? duKien.getTime() : 0));
                    list.add(row);
                }
            }

            // Phòng sắp đến giờ trả (trong 2 tiếng tới)
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT ct.maPhong, kh.hoTen, dp.ngayTraDuKien FROM ChiTietDatPhong ct " +
                    "JOIN DatPhong dp ON ct.maDatPhong=dp.maDatPhong " +
                    "JOIN KhachHang kh ON dp.maKhachHang=kh.maKhachHang " +
                    "WHERE ct.ngayTraThucTe IS NULL " +
                    "  AND dp.ngayTraDuKien BETWEEN GETDATE() AND DATEADD(HOUR, 2, GETDATE()) " +
                    "  AND dp.trangThai='CHECKED_IN'")) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Timestamp duKien = rs.getTimestamp("ngayTraDuKien");
                    String timeStr = duKien != null ? new java.text.SimpleDateFormat("HH:mm").format(duKien) : "ngay";
                    
                    row.put("title", "P." + rs.getString("maPhong") + " – Sắp trả phòng");
                    row.put("desc",  "Khách " + rs.getString("hoTen") + " dự kiến trả lúc " + timeStr);
                    row.put("type",  "info");
                    row.put("id", "upcoming_" + rs.getString("maPhong") + "_" + (duKien != null ? duKien.getTime() : 0));
                    list.add(row);
                }
            }


            // Đặt phòng hôm nay chưa check-in
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT dp.maDatPhong, kh.hoTen FROM DatPhong dp " +
                    "JOIN KhachHang kh ON dp.maKhachHang=kh.maKhachHang " +
                    "WHERE CAST(dp.ngayNhanDuKien AS DATE)=CAST(GETDATE() AS DATE) " +
                    "  AND dp.trangThai='CONFIRMED'")) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    String maDat = rs.getString("maDatPhong");
                    row.put("id", "noshow_" + maDat);
                    row.put("title", "Đặt " + maDat + " chưa check-in");
                    row.put("desc",  rs.getString("hoTen") + " dự kiến nhận phòng hôm nay");
                    row.put("type",  "info");
                    list.add(row);
                }
            }

        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getAlerts: " + e.getMessage());
        }
        return list;
    }
}