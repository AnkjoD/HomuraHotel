package dao;

import database.DemoRepository;

import database.DatabaseConnection;
import entity.BangGia;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class BangGiaDAO {

    private static final Logger LOGGER = Logger.getLogger(BangGiaDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<BangGia> getAll() {
        if (database.DatabaseConnection.isDemoMode()) return database.DemoRepository.all(entity.BangGia.class);
        List<BangGia> list = new ArrayList<>();
        String sql = "SELECT * FROM BangGia ORDER BY mucUuTien ASC, ngayBatDau DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            // Fallback for old schema without mucUuTien
            list.clear();
            String sqlFallback = "SELECT * FROM BangGia ORDER BY ngayBatDau DESC";
            try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sqlFallback)) {
                while (rs.next())
                    list.add(mapRow(rs));
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", ex);
            }
        }
        return list;
    }

    public BangGia getById(String maBangGia) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.get(BangGia.class, maBangGia); }
        String sql = "SELECT * FROM BangGia WHERE maBangGia=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maBangGia);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e);
        }
        return null;
    }

    public boolean insert(BangGia bg) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.insert(BangGia.class, bg); }
        String sql = "INSERT INTO BangGia(maBangGia, tenBangGia, ngayBatDau, ngayKetThuc, isKichHoat, loaiBangGia, doiTuongApDung, mucUuTien, moTa, ngayCuoiTuan) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, bg.getMaBangGia());
            ps.setString(2, bg.getTenBangGia());
            ps.setTimestamp(3, Timestamp.valueOf(bg.getNgayBatDau()));
            ps.setTimestamp(4, Timestamp.valueOf(bg.getNgayKetThuc()));
            ps.setBoolean(5, bg.isTrangThai());
            ps.setString(6, bg.getLoaiBangGia() != null ? bg.getLoaiBangGia() : "RACK");
            ps.setString(7, bg.getDoiTuongApDung() != null ? bg.getDoiTuongApDung() : "ALL");
            ps.setInt(8, bg.getMucUuTien());
            ps.setString(9, bg.getMoTa());
            ps.setString(10, bg.getNgayCuoiTuan() != null ? bg.getNgayCuoiTuan() : "SATURDAY,SUNDAY");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback for old schema
            return insertLegacy(bg);
        }
    }

    private boolean insertLegacy(BangGia bg) {
        String sql = "INSERT INTO BangGia(maBangGia, tenBangGia, ngayBatDau, ngayKetThuc, isKichHoat) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, bg.getMaBangGia());
            ps.setString(2, bg.getTenBangGia());
            ps.setTimestamp(3, Timestamp.valueOf(bg.getNgayBatDau()));
            ps.setTimestamp(4, Timestamp.valueOf(bg.getNgayKetThuc()));
            ps.setBoolean(5, bg.isTrangThai());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e);
            return false;
        }
    }

    public boolean update(BangGia bg) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.update(BangGia.class, bg); }
        String sql = "UPDATE BangGia SET tenBangGia=?, ngayBatDau=?, ngayKetThuc=?, isKichHoat=?, loaiBangGia=?, doiTuongApDung=?, mucUuTien=?, moTa=?, ngayCuoiTuan=? WHERE maBangGia=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, bg.getTenBangGia());
            ps.setTimestamp(2, Timestamp.valueOf(bg.getNgayBatDau()));
            ps.setTimestamp(3, Timestamp.valueOf(bg.getNgayKetThuc()));
            ps.setBoolean(4, bg.isTrangThai());
            ps.setString(5, bg.getLoaiBangGia() != null ? bg.getLoaiBangGia() : "RACK");
            ps.setString(6, bg.getDoiTuongApDung() != null ? bg.getDoiTuongApDung() : "ALL");
            ps.setInt(7, bg.getMucUuTien());
            ps.setString(8, bg.getMoTa());
            ps.setString(9, bg.getNgayCuoiTuan() != null ? bg.getNgayCuoiTuan() : "SATURDAY,SUNDAY");
            ps.setString(10, bg.getMaBangGia());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback
            String sqlFb = "UPDATE BangGia SET tenBangGia=?, ngayBatDau=?, ngayKetThuc=?, isKichHoat=? WHERE maBangGia=?";
            try (PreparedStatement ps = getConn().prepareStatement(sqlFb)) {
                ps.setString(1, bg.getTenBangGia());
                ps.setTimestamp(2, Timestamp.valueOf(bg.getNgayBatDau()));
                ps.setTimestamp(3, Timestamp.valueOf(bg.getNgayKetThuc()));
                ps.setBoolean(4, bg.isTrangThai());
                ps.setString(5, bg.getMaBangGia());
                return ps.executeUpdate() > 0;
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", ex);
                return false;
            }
        }
    }

    public boolean delete(String maBangGia) {
        if (DatabaseConnection.isDemoMode()) { BangGia x=getById(maBangGia);if(x==null)return false;x.setTrangThai(false);return update(x); }
        // Delete chi tiet first (FK constraint)
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM ChiTietBangGia WHERE maBangGia=?")) {
            ps.setString(1, maBangGia);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e);
        }
        // Then delete bang gia
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM BangGia WHERE maBangGia=?")) {
            ps.setString(1, maBangGia);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e);
            return false;
        }
    }

    public List<BangGia> search(String keyword, String status) {
        if (DatabaseConnection.isDemoMode()) { return getAll().stream().filter(x -> DemoRepository.matches(x.getTenBangGia(),keyword)).collect(java.util.stream.Collectors.toList()); }
        List<BangGia> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM BangGia WHERE 1=1 ");
        if (keyword != null && !keyword.isEmpty())
            sql.append("AND (tenBangGia LIKE ? OR maBangGia LIKE ?) ");
        if (status != null && !status.isEmpty())
            sql.append("AND isKichHoat = ? ");
        sql.append("ORDER BY ngayBatDau DESC");

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isEmpty()) {
                ps.setString(idx++, "%" + keyword + "%");
                ps.setString(idx++, "%" + keyword + "%");
            }
            if (status != null && !status.isEmpty())
                ps.setBoolean(idx++, "Active".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status));

            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e);
        }
        return list;
    }

    public List<BangGia> getByLoai(String loaiBangGia) {
        if (DatabaseConnection.isDemoMode()) { return getAll().stream().filter(x -> java.util.Objects.equals(loaiBangGia,x.getLoaiBangGia())).collect(java.util.stream.Collectors.toList()); }
        List<BangGia> list = new ArrayList<>();
        String sql = "SELECT * FROM BangGia WHERE loaiBangGia=? AND isKichHoat=1 ORDER BY mucUuTien ASC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, loaiBangGia);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e);
        }
        return list;
    }

    /**
     * Kiểm tra chồng lấn thời gian của các bảng giá đang Active.
     * Trả về bảng giá bị trùng nếu có.
     */
    public BangGia checkOverlap(BangGia bg) {
        if (DatabaseConnection.isDemoMode()) { return getAll().stream().filter(x -> !java.util.Objects.equals(x.getMaBangGia(),bg.getMaBangGia()) && x.isTrangThai() && x.getMucUuTien()==bg.getMucUuTien() && java.util.Objects.equals(x.getDoiTuongApDung(),bg.getDoiTuongApDung()) && (x.getNgayBatDau()==null || bg.getNgayKetThuc()==null || !x.getNgayBatDau().isAfter(bg.getNgayKetThuc())) && (x.getNgayKetThuc()==null || bg.getNgayBatDau()==null || !x.getNgayKetThuc().isBefore(bg.getNgayBatDau()))).findFirst().orElse(null); }
        String sql = "SELECT TOP 1 * FROM BangGia WHERE isKichHoat = 1 AND maBangGia <> ? " +
                "AND doiTuongApDung = ? AND mucUuTien = ? " +
                "AND ((ngayBatDau <= ? AND ngayKetThuc >= ?) OR (ngayBatDau <= ? AND ngayKetThuc >= ?) OR (? <= ngayBatDau AND ? >= ngayKetThuc))";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, bg.getMaBangGia() != null ? bg.getMaBangGia() : "");
            ps.setString(2, bg.getDoiTuongApDung() != null ? bg.getDoiTuongApDung() : "ALL");
            ps.setInt(3, bg.getMucUuTien());
            ps.setTimestamp(4, Timestamp.valueOf(bg.getNgayBatDau()));
            ps.setTimestamp(5, Timestamp.valueOf(bg.getNgayBatDau()));
            ps.setTimestamp(6, Timestamp.valueOf(bg.getNgayKetThuc()));
            ps.setTimestamp(7, Timestamp.valueOf(bg.getNgayKetThuc()));
            ps.setTimestamp(8, Timestamp.valueOf(bg.getNgayBatDau()));
            ps.setTimestamp(9, Timestamp.valueOf(bg.getNgayKetThuc()));

            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in checkOverlap", e);
        }
        return null;
    }

    /** Find active BangGia that covers a specific date */
    public BangGia findActiveOnDate(java.time.LocalDateTime date) {
        if (DatabaseConnection.isDemoMode()) { return findBestRate(date,"ALL"); }
        String sql = "SELECT * FROM BangGia WHERE isKichHoat = 1 " +
                "AND ? BETWEEN ngayBatDau AND ngayKetThuc " +
                "ORDER BY mucUuTien ASC, ngayBatDau DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(date));
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e);
        }
        return null;
    }

    /**
     * Tìm bảng giá tốt nhất cho một đối tượng cụ thể tại một thời điểm.
     * Ưu tiên: mucUuTien nhỏ nhất → rate plan cụ thể hơn được áp dụng trước.
     */
    public BangGia findBestRate(java.time.LocalDateTime date, String doiTuong) {
        if (DatabaseConnection.isDemoMode()) { return getAll().stream().filter(x -> x.isTrangThai() && (x.getNgayBatDau()==null || !date.isBefore(x.getNgayBatDau())) && (x.getNgayKetThuc()==null || !date.isAfter(x.getNgayKetThuc())) && ("ALL".equals(x.getDoiTuongApDung()) || java.util.Objects.equals(doiTuong,x.getDoiTuongApDung()))).min(java.util.Comparator.comparingInt(BangGia::getMucUuTien)).orElse(null); }
        String sql = "SELECT * FROM BangGia WHERE isKichHoat = 1 " +
                "AND ? BETWEEN ngayBatDau AND ngayKetThuc " +
                "AND (doiTuongApDung = 'ALL' OR doiTuongApDung = ?) " +
                "ORDER BY CASE WHEN doiTuongApDung = ? THEN 0 ELSE 1 END ASC, mucUuTien ASC, maBangGia DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(date));
            ps.setString(2, doiTuong != null ? doiTuong : "ALL");
            ps.setString(3, doiTuong != null ? doiTuong : "ALL");
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            // Fallback to basic query
            return findActiveOnDate(date);
        }
        return null;
    }

    /** Find active BangGia that covers today for any loaiPhong */
    public BangGia findActiveToday() {
        if (DatabaseConnection.isDemoMode()) { return findBestRate(java.time.LocalDateTime.now(),"ALL"); }
        String sql = "SELECT * FROM BangGia WHERE isKichHoat = 1 " +
                "AND GETDATE() BETWEEN ngayBatDau AND ngayKetThuc " +
                "ORDER BY ngayBatDau DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e);
        }
        return null;
    }

    public String generateMaBangGia() {
        return util.IdGenerator.next("BG");
    }

    private BangGia mapRow(ResultSet rs) throws SQLException {
        BangGia bg = new BangGia();
        bg.setMaBangGia(rs.getString("maBangGia"));
        bg.setTenBangGia(rs.getString("tenBangGia"));

        Timestamp start = rs.getTimestamp("ngayBatDau");
        if (start != null)
            bg.setNgayBatDau(start.toLocalDateTime());

        Timestamp end = rs.getTimestamp("ngayKetThuc");
        if (end != null)
            bg.setNgayKetThuc(end.toLocalDateTime());

        bg.setTrangThai(rs.getBoolean("isKichHoat"));

        // Rate Plan fields (safe read)
        try {
            bg.setLoaiBangGia(rs.getString("loaiBangGia"));
        } catch (SQLException ignored) {
            bg.setLoaiBangGia("RACK");
        }
        try {
            bg.setDoiTuongApDung(rs.getString("doiTuongApDung"));
        } catch (SQLException ignored) {
            bg.setDoiTuongApDung("ALL");
        }
        try {
            bg.setMucUuTien(rs.getInt("mucUuTien"));
        } catch (SQLException ignored) {
            bg.setMucUuTien(100);
        }
        try {
            bg.setMoTa(rs.getString("moTa"));
        } catch (SQLException ignored) {
        }
        try {
            bg.setNgayCuoiTuan(rs.getString("ngayCuoiTuan"));
        } catch (SQLException ignored) {
            bg.setNgayCuoiTuan("SATURDAY,SUNDAY");
        }

        return bg;
    }
}
