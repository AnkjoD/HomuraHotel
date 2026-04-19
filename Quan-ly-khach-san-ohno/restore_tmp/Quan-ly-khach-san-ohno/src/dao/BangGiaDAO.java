package dao;

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
        List<BangGia> list = new ArrayList<>();
        String sql = "SELECT * FROM BangGia ORDER BY ngayBatDau DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); }
        return list;
    }

    public BangGia getById(String maBangGia) {
        String sql = "SELECT * FROM BangGia WHERE maBangGia=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maBangGia);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); }
        return null;
    }

    public boolean insert(BangGia bg) {
        String sql = "INSERT INTO BangGia(maBangGia, tenBangGia, ngayBatDau, ngayKetThuc, isKichHoat) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, bg.getMaBangGia());
            ps.setString(2, bg.getTenBangGia());
            ps.setTimestamp(3, Timestamp.valueOf(bg.getNgayBatDau()));
            ps.setTimestamp(4, Timestamp.valueOf(bg.getNgayKetThuc()));
            ps.setBoolean(5, bg.isTrangThai());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); return false; }
    }

    public boolean update(BangGia bg) {
        String sql = "UPDATE BangGia SET tenBangGia=?, ngayBatDau=?, ngayKetThuc=?, isKichHoat=? WHERE maBangGia=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, bg.getTenBangGia());
            ps.setTimestamp(2, Timestamp.valueOf(bg.getNgayBatDau()));
            ps.setTimestamp(3, Timestamp.valueOf(bg.getNgayKetThuc()));
            ps.setBoolean(4, bg.isTrangThai());
            ps.setString(5, bg.getMaBangGia());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); return false; }
    }

    public boolean delete(String maBangGia) {
        // Delete chi tiet first (FK constraint)
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM ChiTietBangGia WHERE maBangGia=?")) {
            ps.setString(1, maBangGia);
            ps.executeUpdate();
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); }
        // Then delete bang gia
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM BangGia WHERE maBangGia=?")) {
            ps.setString(1, maBangGia);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); return false; }
    }

    public List<BangGia> search(String keyword, String status) {
        List<BangGia> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM BangGia WHERE 1=1 ");
        if (keyword != null && !keyword.isEmpty()) sql.append("AND (tenBangGia LIKE ? OR maBangGia LIKE ?) ");
        if (status != null && !status.isEmpty()) sql.append("AND isKichHoat = ? ");
        sql.append("ORDER BY ngayBatDau DESC");

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isEmpty()) { ps.setString(idx++, "%" + keyword + "%"); ps.setString(idx++, "%" + keyword + "%"); }
            if (status != null && !status.isEmpty()) ps.setBoolean(idx++, "Active".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status));
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); }
        return list;
    }

    /** Find active BangGia that covers a specific date */
    public BangGia findActiveOnDate(java.time.LocalDateTime date) {
        String sql = "SELECT * FROM BangGia WHERE isKichHoat = 1 " +
                     "AND ? BETWEEN ngayBatDau AND ngayKetThuc " +
                     "ORDER BY ngayBatDau DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(date));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); }
        return null;
    }

    /** Find active BangGia that covers today for any loaiPhong */
    public BangGia findActiveToday() {
        String sql = "SELECT * FROM BangGia WHERE isKichHoat = 1 " +
                     "AND GETDATE() BETWEEN ngayBatDau AND ngayKetThuc " +
                     "ORDER BY ngayBatDau DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); }
        return null;
    }

    public String generateMaBangGia() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(CAST(SUBSTRING(maBangGia,3,10) AS INT)) FROM BangGia WHERE maBangGia LIKE 'BG%'")) {
            if (rs.next() && rs.getObject(1) != null) return String.format("BG%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in BangGiaDAO", e); }
        return "BG001";
    }

    private BangGia mapRow(ResultSet rs) throws SQLException {
        BangGia bg = new BangGia();
        bg.setMaBangGia(rs.getString("maBangGia"));
        bg.setTenBangGia(rs.getString("tenBangGia"));
        
        Timestamp start = rs.getTimestamp("ngayBatDau");
        if (start != null) bg.setNgayBatDau(start.toLocalDateTime());
        
        Timestamp end = rs.getTimestamp("ngayKetThuc");
        if (end != null) bg.setNgayKetThuc(end.toLocalDateTime());
        
        bg.setTrangThai(rs.getBoolean("isKichHoat"));
        return bg;
    }
}


