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
                    "SELECT COUNT(*) FROM ChiTietDatPhong ct " +
                    "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
                    "WHERE dp.trangThai IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'PARTIALLY_CHECKED_IN', 'CHECKED_OUT') " +
                    "  AND (CAST(COALESCE(ct.ngayNhanDuKien, dp.ngayNhanDuKien) AS DATE) = CAST(GETDATE() AS DATE) " +
                    "       OR CAST(ct.ngayNhanThucTe AS DATE) = CAST(GETDATE() AS DATE))")) {
                stats.put("checkinHomNay", rs.next() ? rs.getInt(1) : 0);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) FROM ChiTietDatPhong ct " +
                    "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
                    "WHERE dp.trangThai IN ('CONFIRMED', 'CHECKED_IN', 'PARTIALLY_CHECKED_IN', 'CHECKED_OUT') " +
                    "  AND (CAST(COALESCE(ct.ngayTraDuKien, dp.ngayTraDuKien) AS DATE) = CAST(GETDATE() AS DATE) " +
                    "       OR CAST(ct.ngayTraThucTe AS DATE) = CAST(GETDATE() AS DATE))")) {
                stats.put("checkoutHomNay", rs.next() ? rs.getInt(1) : 0);
            }
            stats.put("doanhThuHomNay", sumRevenueToday(conn));

        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getDashboardStats: " + e.getMessage());
        }
        return stats;
    }

    public Map<String, Object> getThongKeKy(String ky) {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            Connection conn = getConn();
            int days = getDaysInPeriod(ky);
            stats.put("days", days);

            // Get total rooms count
            int totalRooms = 1;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Phong")) {
                if (rs.next()) totalRooms = Math.max(1, rs.getInt(1));
            }
            stats.put("tongPhong", totalRooms);

            int availableRoomNights = totalRooms * days;
            stats.put("availableRoomNights", availableRoomNights);

            // Total revenue, Room revenue, Service revenue
            long doanhThu = sumRevenueByKy(conn, ky);
            long doanhThuPhong = sumRoomRevenueByKy(conn, ky);
            long doanhThuDV = sumServiceRevenueByKy(conn, ky);
            long operatingCost = sumOperatingCostByKy(conn, ky);
            long netProfit = doanhThu - operatingCost;
            double netProfitMargin = doanhThu > 0 ? (netProfit * 100.0 / doanhThu) : 0.0;

            stats.put("doanhThu", doanhThu);
            stats.put("doanhThuPhong", doanhThuPhong);
            stats.put("doanhThuDV", doanhThuDV);
            stats.put("operatingCost", operatingCost);
            stats.put("netProfit", netProfit);
            stats.put("netProfitMargin", netProfitMargin);

            // Stays length and bookings count
            int luotDatPhong = 0;
            int occupiedRoomNights = 0;
            String sqlOccupied = "SELECT COUNT(*), ISNULL(SUM(CASE WHEN DATEDIFF(day, ngayNhanThucTe, COALESCE(ngayTraThucTe, GETDATE())) = 0 THEN 1 ELSE DATEDIFF(day, ngayNhanThucTe, COALESCE(ngayTraThucTe, GETDATE())) END), 0) "
                               + "FROM ChiTietDatPhong WHERE ngayNhanThucTe IS NOT NULL AND "
                               + getDateFilter("ngayNhanThucTe", ky);
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlOccupied)) {
                if (rs.next()) {
                    luotDatPhong = rs.getInt(1);
                    occupiedRoomNights = rs.getInt(2);
                }
            }
            stats.put("luotDatPhong", luotDatPhong);
            stats.put("occupiedRoomNights", occupiedRoomNights);

            double occupancyRate = availableRoomNights > 0 ? (occupiedRoomNights * 100.0 / availableRoomNights) : 0.0;
            double adr = occupiedRoomNights > 0 ? (doanhThuPhong / (double) occupiedRoomNights) : 0.0;
            double revpar = availableRoomNights > 0 ? (doanhThuPhong / (double) availableRoomNights) : 0.0;
            double trevpar = availableRoomNights > 0 ? (doanhThu / (double) availableRoomNights) : 0.0;
            double alos = luotDatPhong > 0 ? (occupiedRoomNights / (double) luotDatPhong) : 0.0;

            stats.put("occupancyRate", occupancyRate);
            stats.put("adr", adr);
            stats.put("revpar", revpar);
            stats.put("trevpar", trevpar);
            stats.put("alos", alos);

            // Repeat rate and guest counts
            Map<String, Object> guestSegs = getKhachHangSegments(ky);
            stats.put("khachMoi", guestSegs.getOrDefault("totalGuests", 0)); // For compatibility with older references
            stats.put("totalGuests", guestSegs.getOrDefault("totalGuests", 0));
            stats.put("repeatGuests", guestSegs.getOrDefault("repeatGuests", 0));
            stats.put("repeatRate", guestSegs.getOrDefault("repeatRate", 0.0));

            // Current rooms occupied count
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM Phong WHERE trangThai='OCCUPIED'")) {
                stats.put("phongDangThue", rs.next() ? rs.getInt(1) : 0);
            }

            // --- PREVIOUS PERIOD FOR TREND COMPARISON ---
            long doanhThuTruoc = sumRevenuePrevKy(conn, ky);
            long doanhThuPhongTruoc = sumRoomRevenuePrevKy(conn, ky);
            long doanhThuDVTruoc = sumServiceRevenuePrevKy(conn, ky);
            long operatingCostTruoc = sumOperatingCostPrevKy(conn, ky);
            long netProfitTruoc = doanhThuTruoc - operatingCostTruoc;
            double netProfitMarginTruoc = doanhThuTruoc > 0 ? (netProfitTruoc * 100.0 / doanhThuTruoc) : 0.0;

            stats.put("doanhThuTruoc", doanhThuTruoc);
            stats.put("doanhThuPhongTruoc", doanhThuPhongTruoc);
            stats.put("doanhThuDVTruoc", doanhThuDVTruoc);
            stats.put("operatingCostTruoc", operatingCostTruoc);
            stats.put("netProfitTruoc", netProfitTruoc);
            stats.put("netProfitMarginTruoc", netProfitMarginTruoc);

            int luotDatPhongTruoc = 0;
            int occupiedRoomNightsTruoc = 0;
            String sqlOccupiedPrev = "SELECT COUNT(*), ISNULL(SUM(CASE WHEN DATEDIFF(day, ngayNhanThucTe, COALESCE(ngayTraThucTe, GETDATE())) = 0 THEN 1 ELSE DATEDIFF(day, ngayNhanThucTe, COALESCE(ngayTraThucTe, GETDATE())) END), 0) "
                                   + "FROM ChiTietDatPhong WHERE ngayNhanThucTe IS NOT NULL AND "
                                   + getPreviousDateFilter("ngayNhanThucTe", ky);
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlOccupiedPrev)) {
                if (rs.next()) {
                    luotDatPhongTruoc = rs.getInt(1);
                    occupiedRoomNightsTruoc = rs.getInt(2);
                }
            }
            stats.put("luotDatPhongTruoc", luotDatPhongTruoc);
            stats.put("occupiedRoomNightsTruoc", occupiedRoomNightsTruoc);

            int availableRoomNightsTruoc = totalRooms * days; // assuming same number of days
            stats.put("availableRoomNightsTruoc", availableRoomNightsTruoc);

            double occupancyRateTruoc = availableRoomNightsTruoc > 0 ? (occupiedRoomNightsTruoc * 100.0 / availableRoomNightsTruoc) : 0.0;
            double adrTruoc = occupiedRoomNightsTruoc > 0 ? (doanhThuPhongTruoc / (double) occupiedRoomNightsTruoc) : 0.0;
            double revparTruoc = availableRoomNightsTruoc > 0 ? (doanhThuPhongTruoc / (double) availableRoomNightsTruoc) : 0.0;
            double trevparTruoc = availableRoomNightsTruoc > 0 ? (doanhThuTruoc / (double) availableRoomNightsTruoc) : 0.0;
            double alosTruoc = luotDatPhongTruoc > 0 ? (occupiedRoomNightsTruoc / (double) luotDatPhongTruoc) : 0.0;

            stats.put("occupancyRateTruoc", occupancyRateTruoc);
            stats.put("adrTruoc", adrTruoc);
            stats.put("revparTruoc", revparTruoc);
            stats.put("trevparTruoc", trevparTruoc);
            stats.put("alosTruoc", alosTruoc);

            // Guest repeat rate prev
            Map<String, Object> guestSegsPrev = getKhachHangSegmentsPrev(ky);
            stats.put("khachMoiTruoc", guestSegsPrev.getOrDefault("totalGuests", 0));
            stats.put("totalGuestsTruoc", guestSegsPrev.getOrDefault("totalGuests", 0));
            stats.put("repeatGuestsTruoc", guestSegsPrev.getOrDefault("repeatGuests", 0));
            stats.put("repeatRateTruoc", guestSegsPrev.getOrDefault("repeatRate", 0.0));

        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getThongKeKy: " + e.getMessage());
        }
        return stats;
    }

    // ---- Doanh thu theo ngày/tháng tùy theo kỳ ----
    public List<long[]> getDoanhThuTheoNgay(String ky) {
        List<long[]> result = new ArrayList<>();
        String dateCol = "ngayLap";
        
        // Xác định kiểu group (theo ngày hay theo tháng)
        boolean groupByMonth = "nam".equals(ky) || "quy".equals(ky);
        
        // Nếu là custom, nếu khoảng cách > 60 ngày thì group theo tháng, ngược lại group theo ngày
        if (ky != null && ky.startsWith("custom:")) {
            try {
                String[] parts = ky.split(":");
                java.time.LocalDate d1 = java.time.LocalDate.parse(parts[1]);
                java.time.LocalDate d2 = java.time.LocalDate.parse(parts[2]);
                if (java.time.temporal.ChronoUnit.DAYS.between(d1, d2) > 62) {
                    groupByMonth = true;
                }
            } catch (Exception ignored) {}
        }

        String selectPart = groupByMonth 
            ? "YEAR(" + dateCol + ") * 100 + MONTH(" + dateCol + ")" 
            : "YEAR(" + dateCol + ") * 10000 + MONTH(" + dateCol + ") * 100 + DAY(" + dateCol + ")";
            
        if ("7ngay".equals(ky)) {
            selectPart = "CAST(" + dateCol + " AS DATE)";
        } else if ("thang".equals(ky) || (ky == null || ky.isEmpty())) {
            selectPart = "DAY(" + dateCol + ")";
        } else if ("nam".equals(ky) || "quy".equals(ky)) {
            selectPart = "MONTH(" + dateCol + ")";
        }

        String sql = "SELECT " + selectPart + " AS unit, ISNULL(SUM(tongThanhToan),0) AS dt " +
                     "FROM HoaDon " +
                     "WHERE " + getDateFilter(dateCol, ky) + " " +
                     "GROUP BY " + selectPart + " ORDER BY unit";
        
        Map<Object, Long> dataMap = new LinkedHashMap<>(); // Use LinkedHashMap to preserve order
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                if ("7ngay".equals(ky)) {
                    dataMap.put(rs.getDate("unit").toString(), rs.getLong("dt"));
                } else {
                    dataMap.put(rs.getLong("unit"), rs.getLong("dt"));
                }
            }
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING getDoanhThuTheoNgay: " + e.getMessage());
        }

        // Điền đầy các giá trị thiếu (Zero-filling)
        Calendar cal = Calendar.getInstance();
        if ("7ngay".equals(ky)) {
            for (int i = 6; i >= 0; i--) {
                Calendar d = (Calendar) cal.clone();
                d.add(Calendar.DAY_OF_YEAR, -i);
                String dateStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(d.getTime());
                int dayLabel = d.get(Calendar.DAY_OF_MONTH);
                result.add(new long[]{dayLabel, dataMap.getOrDefault(dateStr, 0L)});
            }
        } else if ("thang".equals(ky) || (ky == null || ky.isEmpty())) {
            int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 1; i <= maxDay; i++) {
                result.add(new long[]{i, dataMap.getOrDefault((long)i, 0L)});
            }
        } else if ("nam".equals(ky)) {
            for (int i = 1; i <= 12; i++) {
                result.add(new long[]{i, dataMap.getOrDefault((long)i, 0L)});
            }
        } else if ("quy".equals(ky)) {
            int currentMonth = cal.get(Calendar.MONTH) + 1;
            int startMonth = ((currentMonth - 1) / 3) * 3 + 1;
            for (int i = startMonth; i < startMonth + 3; i++) {
                result.add(new long[]{i, dataMap.getOrDefault((long)i, 0L)});
            }
        } else {
            // Custom: Tạo timeline đầy đủ từ ngày bắt đầu đến ngày kết thúc
            try {
                String[] parts = ky.split(":");
                java.time.LocalDate d1 = java.time.LocalDate.parse(parts[1]);
                java.time.LocalDate d2 = java.time.LocalDate.parse(parts[2]);
                
                if (groupByMonth) {
                    // Timeline theo tháng
                    java.time.LocalDate current = d1.withDayOfMonth(1);
                    while (!current.isAfter(d2)) {
                        long unit = current.getYear() * 100 + current.getMonthValue();
                        long revenue = dataMap.getOrDefault(unit, 0L);
                        result.add(new long[]{current.getMonthValue(), revenue, unit});
                        current = current.plusMonths(1);
                    }
                } else {
                    // Timeline theo ngày
                    java.time.LocalDate current = d1;
                    while (!current.isAfter(d2)) {
                        long unit = current.getYear() * 10000 + current.getMonthValue() * 100 + current.getDayOfMonth();
                        long revenue = dataMap.getOrDefault(unit, 0L);
                        result.add(new long[]{current.getDayOfMonth(), revenue, unit});
                        current = current.plusDays(1);
                    }
                }
            } catch (Exception e) {
                // Fallback nếu có lỗi parse
                for (Map.Entry<Object, Long> entry : dataMap.entrySet()) {
                    long unit = (long) entry.getKey();
                    long label = (unit > 10000) ? unit % 100 : unit;
                    result.add(new long[]{label, entry.getValue(), unit});
                }
            }
        }
        
        return result;
    }

    // ---- Doanh thu 7 ngày gần nhất (cho dashboard bar chart) ----
    public List<long[]> getDoanhThu7Ngay() {
        List<long[]> result = new ArrayList<>();
        // dayOffset: 0=hôm nay, 1=hôm qua, ... 6=6 ngày trước — trả về desc để chart vẽ từ cũ→mới
        String sql =
            "SELECT DATEDIFF(DAY, ngayLap, GETDATE()) AS dayOffset, " +
            "       ISNULL(SUM(tongThanhToan), 0) AS dt " +
            "FROM HoaDon " +
            "WHERE ngayLap >= DATEADD(DAY, -6, CAST(GETDATE() AS DATE)) " +
            "GROUP BY DATEDIFF(DAY, ngayLap, GETDATE()) " +
            "ORDER BY dayOffset DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new long[]{ rs.getLong("dayOffset"), rs.getLong("dt") });
            }
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING getDoanhThu7Ngay: " + e.getMessage());
        }
        return result;
    }

    private String getDateFilter(String dateCol, String ky) {
        boolean yoy = false;
        if (ky != null && ky.startsWith("yoy:")) {
            yoy = true;
            ky = ky.substring(4);
        }
        if (ky != null && ky.startsWith("custom:")) {
            String[] parts = ky.split(":");
            if (parts.length == 3) {
                String d1 = parts[1];
                String d2 = parts[2];
                if (d1.matches("\\d{4}-\\d{2}-\\d{2}") && d2.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    if (yoy) {
                        try {
                            java.time.LocalDate ld1 = java.time.LocalDate.parse(d1).minusYears(1);
                            java.time.LocalDate ld2 = java.time.LocalDate.parse(d2).minusYears(1);
                            return dateCol + " >= '" + ld1 + " 00:00:00' AND " + dateCol + " <= '" + ld2 + " 23:59:59'";
                        } catch (Exception e) {
                            // fallback
                        }
                    }
                    return dateCol + " >= '" + d1 + " 00:00:00' AND " + dateCol + " <= '" + d2 + " 23:59:59'";
                }
            }
        }
        
        String getdateStr = yoy ? "DATEADD(YEAR,-1,GETDATE())" : "GETDATE()";
        
        switch (ky != null ? ky : "thang") {
            case "7ngay":
                if (yoy) {
                    return dateCol + " >= DATEADD(DAY,-7,DATEADD(YEAR,-1,GETDATE())) AND " + dateCol + " <= DATEADD(YEAR,-1,GETDATE())";
                }
                return dateCol + " >= DATEADD(DAY,-7,GETDATE())";
            case "quy":
                return "DATEPART(QUARTER," + dateCol + ")=DATEPART(QUARTER," + getdateStr + ") " +
                       "AND YEAR(" + dateCol + ")=YEAR(" + getdateStr + ")";
            case "nam":
                return "YEAR(" + dateCol + ")=YEAR(" + getdateStr + ")";
            case "thang":
            default:
                return "MONTH(" + dateCol + ")=MONTH(" + getdateStr + ") AND YEAR(" + dateCol + ")=YEAR(" + getdateStr + ")";
        }
    }

    private String getPreviousDateFilter(String dateCol, String ky) {
        if (ky != null && ky.startsWith("custom:")) {
            try {
                String[] parts = ky.split(":");
                java.time.LocalDate d1 = java.time.LocalDate.parse(parts[1]);
                java.time.LocalDate d2 = java.time.LocalDate.parse(parts[2]);
                long days = java.time.temporal.ChronoUnit.DAYS.between(d1, d2) + 1;
                java.time.LocalDate prevD1 = d1.minusDays(days);
                java.time.LocalDate prevD2 = d2.minusDays(days);
                return dateCol + " >= '" + prevD1 + " 00:00:00' AND " + dateCol + " <= '" + prevD2 + " 23:59:59'";
            } catch (Exception e) {
                return "1=0";
            }
        }
        switch (ky != null ? ky : "thang") {
            case "7ngay":
                return dateCol + " >= DATEADD(DAY,-14,GETDATE()) AND " + dateCol + " < DATEADD(DAY,-7,GETDATE())";
            case "quy":
                return "DATEPART(QUARTER," + dateCol + ")=DATEPART(QUARTER,DATEADD(QUARTER,-1,GETDATE())) " +
                       "AND YEAR(" + dateCol + ")=YEAR(DATEADD(QUARTER,-1,GETDATE()))";
            case "nam":
                return "YEAR(" + dateCol + ")=YEAR(DATEADD(YEAR,-1,GETDATE()))";
            case "thang":
            default:
                return "MONTH(" + dateCol + ")=MONTH(DATEADD(MONTH,-1,GETDATE())) " +
                       "AND YEAR(" + dateCol + ")=YEAR(DATEADD(MONTH,-1,GETDATE()))";
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

    private long sumRevenuePrevKy(Connection conn, String ky) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(tongThanhToan),0) FROM HoaDon WHERE " + getPreviousDateFilter("ngayLap", ky))) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumRevenuePrevKy: " + e.getMessage());
        }
        return 0L;
    }

    private long sumServiceRevenuePrevKy(Connection conn, String ky) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(tongTienDichVu),0) FROM HoaDon WHERE " + getPreviousDateFilter("ngayLap", ky))) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumServiceRevenuePrevKy: " + e.getMessage());
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
            "SELECT ct.maPhong, kh.hoTen, lp.tenLoaiPhong, ct.ngayNhanThucTe, " +
            "       COALESCE(ct.ngayNhanDuKien, dp.ngayNhanDuKien) AS ngayNhanDuKien " +
            "FROM ChiTietDatPhong ct " +
            "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "JOIN KhachHang kh ON dp.maKhachHang = kh.maKhachHang " +
            "JOIN Phong p ON ct.maPhong = p.maPhong " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "WHERE dp.trangThai IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'PARTIALLY_CHECKED_IN', 'CHECKED_OUT') " +
            "  AND (CAST(COALESCE(ct.ngayNhanDuKien, dp.ngayNhanDuKien) AS DATE) = CAST(GETDATE() AS DATE) " +
            "       OR CAST(ct.ngayNhanThucTe AS DATE) = CAST(GETDATE() AS DATE))";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("soPhong",  rs.getString("maPhong"));
                row.put("hoTen",    rs.getString("hoTen"));
                row.put("tenLoai",  rs.getString("tenLoaiPhong"));
                Timestamp actIn = rs.getTimestamp("ngayNhanThucTe");
                Timestamp planIn = rs.getTimestamp("ngayNhanDuKien");
                row.put("ngayNhan", actIn != null ? actIn : planIn);
                row.put("trangThai", actIn != null ? "Đã nhận" : "Chờ nhận");
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
            "SELECT ct.maPhong, kh.hoTen, ct.ngayTraThucTe, " +
            "       COALESCE(ct.ngayTraDuKien, dp.ngayTraDuKien) AS ngayTraDuKien, p.trangThai " +
            "FROM ChiTietDatPhong ct " +
            "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "JOIN KhachHang kh ON dp.maKhachHang = kh.maKhachHang " +
            "JOIN Phong p ON ct.maPhong = p.maPhong " +
            "WHERE dp.trangThai IN ('CONFIRMED', 'CHECKED_IN', 'PARTIALLY_CHECKED_IN', 'CHECKED_OUT') " +
            "  AND (CAST(COALESCE(ct.ngayTraDuKien, dp.ngayTraDuKien) AS DATE) = CAST(GETDATE() AS DATE) " +
            "       OR CAST(ct.ngayTraThucTe AS DATE) = CAST(GETDATE() AS DATE))";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("soPhong",  rs.getString("maPhong"));
                row.put("hoTen",    rs.getString("hoTen"));
                Timestamp actOut = rs.getTimestamp("ngayTraThucTe");
                Timestamp planOut = rs.getTimestamp("ngayTraDuKien");
                row.put("ngayTraDK", actOut != null ? actOut : planOut);
                String tt = rs.getString("trangThai");
                row.put("trangThai",
                    (actOut != null || "CLEANING".equals(tt) || "AVAILABLE".equals(tt)) ? "Đã trả" : "Chờ trả");
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

    private String formatRoom(String maPhong) {
        if (maPhong == null) return "";
        String trimmed = maPhong.trim();
        if (trimmed.toUpperCase().startsWith("P") && trimmed.length() > 1 && Character.isDigit(trimmed.charAt(1))) {
            return trimmed.substring(1);
        }
        return trimmed;
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
                    row.put("title", "P." + formatRoom(maPhong) + " – Đang vệ sinh");
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
                    row.put("title", "P." + formatRoom(maPhong) + " – Đang bảo trì");
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
                    String rm = rs.getString("maPhong");
                    
                    row.put("title", "P." + formatRoom(rm) + " – Quá hạn trả (" + timeStr + ")");
                    row.put("desc",  rs.getString("hoTen") + " chưa làm thủ tục trả phòng");
                    row.put("type",  "danger");
                    row.put("id", "overdue_" + rm + "_" + (duKien != null ? duKien.getTime() : 0));
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
                    String rm = rs.getString("maPhong");
                    
                    row.put("title", "P." + formatRoom(rm) + " – Sắp trả phòng");
                    row.put("desc",  "Khách " + rs.getString("hoTen") + " dự kiến trả lúc " + timeStr);
                    row.put("type",  "info");
                    row.put("id", "upcoming_" + rm + "_" + (duKien != null ? duKien.getTime() : 0));
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

    private int getDaysInPeriod(String ky) {
        boolean yoy = false;
        if (ky != null && ky.startsWith("yoy:")) {
            yoy = true;
            ky = ky.substring(4);
        }
        if (ky != null && ky.startsWith("custom:")) {
            try {
                String[] parts = ky.split(":");
                java.time.LocalDate d1 = java.time.LocalDate.parse(parts[1]);
                java.time.LocalDate d2 = java.time.LocalDate.parse(parts[2]);
                return (int) java.time.temporal.ChronoUnit.DAYS.between(d1, d2) + 1;
            } catch (Exception e) {
                return 30; // fallback
            }
        }
        switch (ky != null ? ky : "thang") {
            case "7ngay":
                return 7;
            case "quy":
                java.time.LocalDate now = java.time.LocalDate.now();
                int quarter = (now.getMonthValue() - 1) / 3 + 1;
                java.time.LocalDate qStart = java.time.LocalDate.of(now.getYear(), (quarter - 1) * 3 + 1, 1);
                java.time.LocalDate qEnd = qStart.plusMonths(3).minusDays(1);
                return (int) java.time.temporal.ChronoUnit.DAYS.between(qStart, qEnd) + 1;
            case "nam":
                return java.time.LocalDate.now().isLeapYear() ? 366 : 365;
            case "thang":
            default:
                return java.time.LocalDate.now().lengthOfMonth();
        }
    }

    private long sumOperatingCostByKy(Connection conn, String ky) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(soTien),0) FROM ChiPhi WHERE " + getDateFilter("thoiGian", ky))) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumOperatingCostByKy: " + e.getMessage());
        }
        return 0L;
    }

    private long sumOperatingCostPrevKy(Connection conn, String ky) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(soTien),0) FROM ChiPhi WHERE " + getPreviousDateFilter("thoiGian", ky))) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumOperatingCostPrevKy: " + e.getMessage());
        }
        return 0L;
    }

    private long sumRoomRevenueByKy(Connection conn, String ky) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(tongTienPhong),0) FROM HoaDon WHERE " + getDateFilter("ngayLap", ky))) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumRoomRevenueByKy: " + e.getMessage());
        }
        return 0L;
    }

    private long sumRoomRevenuePrevKy(Connection conn, String ky) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT ISNULL(SUM(tongTienPhong),0) FROM HoaDon WHERE " + getPreviousDateFilter("ngayLap", ky))) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.err.println(TAG + " WARNING sumRoomRevenuePrevKy: " + e.getMessage());
        }
        return 0L;
    }

    public Map<String, Object> getKhachHangSegments(String ky) {
        return getKhachHangSegmentsHelper(ky, false);
    }

    public Map<String, Object> getKhachHangSegmentsPrev(String ky) {
        return getKhachHangSegmentsHelper(ky, true);
    }

    private Map<String, Object> getKhachHangSegmentsHelper(String ky, boolean isPrev) {
        Map<String, Object> segments = new LinkedHashMap<>();
        int totalGuests = 0;
        int repeatGuests = 0;
        int vipGuests = 0;
        int internationalGuests = 0;
        int corporateBookings = 0;
        int directBookings = 0;
        int bookingsWithService = 0;
        int totalBookings = 0;

        String filter = isPrev ? getPreviousDateFilter("ct.ngayNhanThucTe", ky) : getDateFilter("ct.ngayNhanThucTe", ky);

        try {
            Connection conn = getConn();
            
            String sqlGuests = "SELECT "
                             + "  COUNT(DISTINCT dp.maKhachHang) AS total_guests, "
                             + "  SUM(CASE WHEN kh.vipLevel <> 'BRONZE' AND kh.vipLevel IS NOT NULL THEN 1 ELSE 0 END) AS vip_guests, "
                             + "  SUM(CASE WHEN kh.quocTich <> N'Việt Nam' AND kh.quocTich <> N'Viet Nam' AND kh.quocTich IS NOT NULL THEN 1 ELSE 0 END) AS int_guests "
                             + "FROM DatPhong dp "
                             + "JOIN ChiTietDatPhong ct ON dp.maDatPhong = ct.maDatPhong "
                             + "JOIN KhachHang kh ON dp.maKhachHang = kh.maKhachHang "
                             + "WHERE ct.ngayNhanThucTe IS NOT NULL AND " + filter;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlGuests)) {
                if (rs.next()) {
                    totalGuests = rs.getInt("total_guests");
                    vipGuests = rs.getInt("vip_guests");
                    internationalGuests = rs.getInt("int_guests");
                }
            }

            String sqlRepeat = "SELECT COUNT(DISTINCT dp.maKhachHang) AS repeat_guests "
                             + "FROM DatPhong dp "
                             + "JOIN ChiTietDatPhong ct ON dp.maDatPhong = ct.maDatPhong "
                             + "WHERE ct.ngayNhanThucTe IS NOT NULL AND " + filter + " AND "
                             + "(SELECT COUNT(*) FROM DatPhong dp2 JOIN ChiTietDatPhong ct2 ON dp2.maDatPhong = ct2.maDatPhong "
                             + " WHERE dp2.maKhachHang = dp.maKhachHang AND ct2.ngayNhanThucTe IS NOT NULL) > 1";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlRepeat)) {
                if (rs.next()) {
                    repeatGuests = rs.getInt("repeat_guests");
                }
            }

            String sqlChannels = "SELECT "
                               + "  COUNT(DISTINCT dp.maDatPhong) AS total_bookings, "
                               + "  SUM(CASE WHEN dp.maKenh = 'CORPORATE' OR dp.loaiKhach = 'DOAN' THEN 1 ELSE 0 END) AS corp_bookings, "
                               + "  SUM(CASE WHEN dp.maKenh IN ('DIRECT', 'WEBSITE') OR dp.maKenh IS NULL THEN 1 ELSE 0 END) AS direct_bookings "
                               + "FROM DatPhong dp "
                               + "JOIN ChiTietDatPhong ct ON dp.maDatPhong = ct.maDatPhong "
                               + "WHERE ct.ngayNhanThucTe IS NOT NULL AND " + filter;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlChannels)) {
                if (rs.next()) {
                    totalBookings = rs.getInt("total_bookings");
                    corporateBookings = rs.getInt("corp_bookings");
                    directBookings = rs.getInt("direct_bookings");
                }
            }

            String sqlServices = "SELECT COUNT(DISTINCT ct.maDatPhong) AS b_service "
                               + "FROM ChiTietDatPhong ct "
                               + "WHERE ct.ngayNhanThucTe IS NOT NULL AND " + filter + " AND "
                               + "(ct.maChiTiet IN (SELECT maChiTiet FROM SuDungDichVu) OR "
                               + " ct.maDatPhong IN (SELECT maDatPhong FROM SuDungDichVu) OR "
                               + " ct.maChiTiet IN (SELECT maChiTiet FROM SU_DUNG_DICH_VU) OR "
                               + " ct.maDatPhong IN (SELECT maDatPhong FROM SU_DUNG_DICH_VU))";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlServices)) {
                if (rs.next()) {
                    bookingsWithService = rs.getInt("b_service");
                }
            }

        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getKhachHangSegmentsHelper: " + e.getMessage());
        }

        double repeatRate = totalGuests > 0 ? (repeatGuests * 100.0 / totalGuests) : 0.0;
        double serviceAttachmentRate = totalBookings > 0 ? (bookingsWithService * 100.0 / totalBookings) : 0.0;

        segments.put("totalGuests", totalGuests);
        segments.put("repeatGuests", repeatGuests);
        segments.put("repeatRate", repeatRate);
        segments.put("vipGuests", vipGuests);
        segments.put("internationalGuests", internationalGuests);
        segments.put("corporateBookings", corporateBookings);
        segments.put("directBookings", directBookings);
        segments.put("serviceAttachmentRate", serviceAttachmentRate);
        segments.put("totalBookings", totalBookings);

        return segments;
    }

    public List<Map<String, Object>> getChiTietPhongStats(String ky) {
        List<Map<String, Object>> list = new ArrayList<>();
        int days = Math.max(1, getDaysInPeriod(ky));
        String filterCt = getDateFilter("ct.ngayNhanThucTe", ky);
        String filterHd = getDateFilter("hd.ngayLap", ky);
        
        String sql = "SELECT p.maPhong, lp.tenLoaiPhong, "
                   + "  ISNULL(SUM(CASE WHEN DATEDIFF(day, ct.ngayNhanThucTe, COALESCE(ct.ngayTraThucTe, GETDATE())) = 0 THEN 1 ELSE DATEDIFF(day, ct.ngayNhanThucTe, COALESCE(ct.ngayTraThucTe, GETDATE())) END), 0) AS occupiedNights, "
                   + "  ISNULL(SUM(hd.tongTienPhong), 0) AS revenue "
                   + "FROM Phong p "
                   + "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong "
                   + "LEFT JOIN ChiTietDatPhong ct ON p.maPhong = ct.maPhong AND ct.ngayNhanThucTe IS NOT NULL AND " + filterCt + " "
                   + "LEFT JOIN HoaDon hd ON ct.maDatPhong = hd.maDatPhong AND " + filterHd + " "
                   + "GROUP BY p.maPhong, lp.tenLoaiPhong "
                   + "ORDER BY revenue DESC, occupiedNights DESC";
        
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                int occupied = rs.getInt("occupiedNights");
                long rev = rs.getLong("revenue");
                
                map.put("soPhong", rs.getString("maPhong"));
                map.put("tenLoai", rs.getString("tenLoaiPhong"));
                map.put("occupiedNights", occupied);
                map.put("revenue", rev);
                map.put("adr", occupied > 0 ? (rev / (double) occupied) : 0.0);
                map.put("revpar", rev / (double) days);
                list.add(map);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getChiTietPhongStats: " + e.getMessage());
        }
        return list;
    }

    public List<Map<String, Object>> getChiTietDichVuStats(String ky) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT dv.tenDichVu, MAX(sddv.donGiaLucDung) AS unitPrice, "
                   + "  SUM(sddv.soLuong) AS quantity, "
                   + "  SUM(sddv.soLuong * sddv.donGiaLucDung) AS revenue "
                   + "FROM SuDungDichVu sddv JOIN DichVu dv ON sddv.maDichVu = dv.maDichVu "
                   + "WHERE " + getDateFilter("sddv.thoiGianDung", ky) + " "
                   + "GROUP BY dv.tenDichVu ORDER BY revenue DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("tenDV", rs.getString("tenDichVu"));
                map.put("unitPrice", rs.getDouble("unitPrice"));
                map.put("quantity", rs.getInt("quantity"));
                map.put("revenue", rs.getDouble("revenue"));
                list.add(map);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getChiTietDichVuStats: " + e.getMessage());
        }
        return list;
    }

    public List<Map<String, Object>> getPhanTichKenhStats(String ky) {
        List<Map<String, Object>> list = new ArrayList<>();
        String filterCt = getDateFilter("ct.ngayNhanThucTe", ky);
        String filterHd = getDateFilter("hd.ngayLap", ky);
        
        String sql = "SELECT dp.maKenh, "
                   + "  COUNT(DISTINCT dp.maDatPhong) AS bookingsCount, "
                   + "  ISNULL(SUM(hd.tongTienPhong), 0) AS roomRevenue "
                   + "FROM DatPhong dp "
                   + "JOIN ChiTietDatPhong ct ON dp.maDatPhong = ct.maDatPhong "
                   + "LEFT JOIN HoaDon hd ON dp.maDatPhong = hd.maDatPhong AND " + filterHd + " "
                   + "WHERE ct.ngayNhanThucTe IS NOT NULL AND " + filterCt + " "
                   + "GROUP BY dp.maKenh "
                   + "ORDER BY roomRevenue DESC";
        
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                String maKenh = rs.getString("maKenh");
                if (maKenh == null) maKenh = "DIRECT";
                
                int bookingsCount = rs.getInt("bookingsCount");
                double roomRevenue = rs.getDouble("roomRevenue");
                
                String tenKenh = maKenh;
                double commRate = 0.0;
                switch (maKenh.toUpperCase()) {
                    case "DIRECT":
                        tenKenh = "Trực tiếp";
                        commRate = 0.0;
                        break;
                    case "WEBSITE":
                        tenKenh = "Website";
                        commRate = 0.0;
                        break;
                    case "BOOKING":
                        tenKenh = "Booking.com";
                        commRate = 0.15;
                        break;
                    case "AGODA":
                        tenKenh = "Agoda";
                        commRate = 0.15;
                        break;
                    case "TRAVELOKA":
                        tenKenh = "Traveloka";
                        commRate = 0.15;
                        break;
                    case "EXPEDIA":
                        tenKenh = "Expedia";
                        commRate = 0.15;
                        break;
                    case "CORPORATE":
                        tenKenh = "Doanh nghiệp";
                        commRate = 0.05;
                        break;
                    case "TRAVEL_AGT":
                        tenKenh = "Đại lý du lịch";
                        commRate = 0.10;
                        break;
                }
                
                double commission = roomRevenue * commRate;
                double netRevenue = roomRevenue - commission;
                
                map.put("maKenh", maKenh);
                map.put("tenKenh", tenKenh);
                map.put("bookingsCount", bookingsCount);
                map.put("roomRevenue", roomRevenue);
                map.put("commission", commission);
                map.put("netRevenue", netRevenue);
                list.add(map);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getPhanTichKenhStats: " + e.getMessage());
        }
        return list;
    }

    public List<Map<String, Object>> getPhanTichKhachStats(String ky) {
        List<Map<String, Object>> list = new ArrayList<>();
        String filterCt = getDateFilter("ct.ngayNhanThucTe", ky);
        String filterHd = getDateFilter("hd.ngayLap", ky);
        
        String sql = "SELECT TOP 50 kh.hoTen, kh.sdt, kh.cccd, kh.quocTich, kh.vipLevel, "
                   + "  COUNT(DISTINCT dp.maDatPhong) AS visits, "
                   + "  ISNULL(SUM(hd.tongThanhToan), 0) AS totalSpent "
                   + "FROM KhachHang kh "
                   + "JOIN DatPhong dp ON kh.maKhachHang = dp.maKhachHang "
                   + "JOIN ChiTietDatPhong ct ON dp.maDatPhong = ct.maDatPhong "
                   + "LEFT JOIN HoaDon hd ON dp.maDatPhong = hd.maDatPhong AND " + filterHd + " "
                   + "WHERE ct.ngayNhanThucTe IS NOT NULL AND " + filterCt + " "
                   + "GROUP BY kh.maKhachHang, kh.hoTen, kh.sdt, kh.cccd, kh.quocTich, kh.vipLevel "
                   + "ORDER BY totalSpent DESC";
                   
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("hoTen", rs.getString("hoTen"));
                map.put("sdt", rs.getString("sdt"));
                map.put("cccd", rs.getString("cccd"));
                map.put("quocTich", rs.getString("quocTich"));
                map.put("vipLevel", rs.getString("vipLevel"));
                map.put("visits", rs.getInt("visits"));
                map.put("totalSpent", rs.getDouble("totalSpent"));
                list.add(map);
            }
        } catch (SQLException e) {
            System.err.println(TAG + " ERROR getPhanTichKhachStats: " + e.getMessage());
        }
        return list;
    }
}