package dao;

import database.DatabaseConnection;
import entity.HuongNhin;
import entity.LoaiPhong;
import entity.Phong;
import entity.DatPhong;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PhongDAO {

    private static final Logger LOGGER = Logger.getLogger(PhongDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Phong> getAll() {
        List<Phong> list = new ArrayList<>();
        String sql =
            "SELECT p.*, lp.tenLoaiPhong, lp.giaTheoNgay, lp.soNguoiToiDa, " +
            "hn.tenHuongNhin, hn.heSoGia, hn.moTa AS moTaHuongNhin, " +
            "kh.hoTen AS tenKhachHienTai " +
            "FROM Phong p " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "LEFT JOIN HuongNhin hn ON p.maHuongNhin = hn.maHuongNhin " +
            "LEFT JOIN ChiTietDatPhong ct ON ct.maPhong = p.maPhong " +
            "  AND ct.ngayNhanThucTe IS NOT NULL AND ct.ngayTraThucTe IS NULL " +
            "LEFT JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "LEFT JOIN KhachHang kh ON dp.maKhachHang = kh.maKhachHang";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Phong p = mapRow(rs);
                try { p.setTenKhachHienTai(rs.getString("tenKhachHienTai")); } catch (Exception ignored) {}
                list.add(p);
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return list;
    }

    public Phong getById(String maPhong) {
        String sql = "SELECT p.*, lp.tenLoaiPhong, lp.giaTheoNgay, lp.soNguoiToiDa, " +
                     "hn.tenHuongNhin, hn.heSoGia, hn.moTa AS moTaHuongNhin " +
                     "FROM Phong p " +
                     "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
                     "LEFT JOIN HuongNhin hn ON p.maHuongNhin = hn.maHuongNhin " +
                     "WHERE p.maPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return null;
    }

    public boolean insert(Phong p) {
        String sql = "INSERT INTO Phong(maPhong, maLoaiPhong, tang, maHuongNhin, trangThai) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, p.getMaPhong());
            ps.setString(2, p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : null);
            ps.setInt(3, p.getTang());
            ps.setString(4, p.getMaHuongNhin());
            ps.setString(5, p.getTrangThai() != null ? p.getTrangThai().name() : "AVAILABLE");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); return false; }
    }

    public boolean update(Phong p) {
        String sql = "UPDATE Phong SET maLoaiPhong=?, tang=?, maHuongNhin=?, trangThai=? WHERE maPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : null);
            ps.setInt(2, p.getTang());
            ps.setString(3, p.getMaHuongNhin());
            ps.setString(4, p.getTrangThai() != null ? p.getTrangThai().name() : "AVAILABLE");
            ps.setString(5, p.getMaPhong());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); return false; }
    }

    public List<Phong> findAvailableRooms(java.time.LocalDateTime start, java.time.LocalDateTime end, int sucChua) {
        List<Phong> list = new ArrayList<>();
        String sql =
            "SELECT p.*, lp.tenLoaiPhong, lp.giaTheoNgay, lp.soNguoiToiDa, " +
            "hn.tenHuongNhin, hn.heSoGia, hn.moTa AS moTaHuongNhin, " +
            "kh.hoTen AS tenKhachHienTai " +
            "FROM Phong p " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "LEFT JOIN HuongNhin hn ON p.maHuongNhin = hn.maHuongNhin " +
            "LEFT JOIN ChiTietDatPhong ct2 ON ct2.maPhong = p.maPhong " +
            "  AND ct2.ngayNhanThucTe IS NOT NULL AND ct2.ngayTraThucTe IS NULL " +
            "LEFT JOIN DatPhong dpCurr ON ct2.maDatPhong = dpCurr.maDatPhong " +
            "LEFT JOIN KhachHang kh ON dpCurr.maKhachHang = kh.maKhachHang " +
            "WHERE lp.soNguoiToiDa >= ? " +
            "AND p.trangThai = 'AVAILABLE' " +
            "AND p.maPhong NOT IN (" +
            "  SELECT ctdp.maPhong FROM ChiTietDatPhong ctdp " +
            "  JOIN DatPhong dp ON ctdp.maDatPhong = dp.maDatPhong " +
            "  WHERE dp.trangThai NOT IN ('CANCELLED','CHECKED_OUT') " +
            "  AND dp.ngayNhanDuKien < ? " +
            "  AND dp.ngayTraDuKien > ? " +
            ")";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, sucChua);
            ps.setTimestamp(2, Timestamp.valueOf(end));
            ps.setTimestamp(3, Timestamp.valueOf(start));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Phong p = mapRow(rs);
                try { p.setTenKhachHienTai(rs.getString("tenKhachHienTai")); } catch (Exception ignored) {}
                list.add(p);
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return list;
    }

    public boolean delete(String maPhong) {
        String sql = "DELETE FROM Phong WHERE maPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); return false; }
    }

    public List<Phong> search(String kw, String tang, String huongNhin) {
        List<Phong> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT p.*, lp.tenLoaiPhong, lp.giaTheoNgay, lp.soNguoiToiDa, hn.tenHuongNhin, hn.heSoGia, hn.moTa AS moTaHuongNhin " +
            "FROM Phong p " +
            "JOIN LoaiPhong lp ON p.maLoaiPhong = lp.maLoaiPhong " +
            "LEFT JOIN HuongNhin hn ON p.maHuongNhin = hn.maHuongNhin " +
            "WHERE 1=1 ");
        if (kw != null && !kw.isEmpty()) sb.append("AND (p.maPhong LIKE ?) ");
        if (tang != null && !tang.isEmpty()) sb.append("AND p.tang = ? ");
        if (huongNhin != null && !huongNhin.isEmpty()) sb.append("AND hn.tenHuongNhin = ? ");

        try (PreparedStatement ps = getConn().prepareStatement(sb.toString())) {
            int idx = 1;
            if (kw != null && !kw.isEmpty()) ps.setString(idx++, "%" + kw + "%");
            if (tang != null && !tang.isEmpty()) ps.setInt(idx++, Integer.parseInt(tang));
            if (huongNhin != null && !huongNhin.isEmpty()) ps.setString(idx++, huongNhin);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return list;
    }

    public java.util.Map<String, Integer> getThongKeTrangThai() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        String sql = "SELECT trangThai, COUNT(*) FROM Phong GROUP BY trangThai";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String key = rs.getString(1);
                int count = rs.getInt(2);
                if ("AVAILABLE".equalsIgnoreCase(key)) key = "Có sẵn";
                else if ("OCCUPIED".equalsIgnoreCase(key)) key = "Đang thuê";
                else if ("MAINTENANCE".equalsIgnoreCase(key)) key = "Bảo trì";
                else if ("CLEANING".equalsIgnoreCase(key)) key = "Vệ sinh";
                map.put(key, count);
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return map;
    }

    public Phong getPhongById(String id) { return getById(id); }

    /**
     * Tìm các phòng có đặt trước (CONFIRMED) trong khoảng thời gian.
     * Trả về Map: maPhong -> maDatPhong (để hiện badge "Đã đặt" trên sơ đồ phòng).
     */
    public Map<String, String[]> getReservedRoomsForDate(LocalDateTime date) {
        Map<String, String[]> map = new HashMap<>();
        String sql =
            "SELECT ct.maPhong, dp.maDatPhong, kh.hoTen, dp.ngayNhanDuKien, dp.ngayTraDuKien " +
            "FROM ChiTietDatPhong ct " +
            "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "LEFT JOIN KhachHang kh ON dp.maKhachHang = kh.maKhachHang " +
            "WHERE dp.trangThai = 'CONFIRMED' " +
            "AND dp.ngayNhanDuKien <= DATEADD(DAY, 1, ?) " +
            "AND dp.ngayTraDuKien > ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            Timestamp ts = Timestamp.valueOf(date);
            ps.setTimestamp(1, ts);
            ps.setTimestamp(2, ts);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String maPhong = rs.getString("maPhong");
                String maDat   = rs.getString("maDatPhong");
                String tenKH   = rs.getString("hoTen");
                Timestamp tsNhan = rs.getTimestamp("ngayNhanDuKien");
                String ngayNhan = tsNhan != null ? new java.text.SimpleDateFormat("dd/MM HH:mm").format(tsNhan) : "";
                map.put(maPhong, new String[]{maDat, tenKH != null ? tenKH : "—", ngayNhan});
            }
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return map;
    }

    /**
     * Đếm số phòng đã có reservation (CONFIRMED/CHECKED_IN) chồng lấn khoảng thời gian.
     * Dùng để validate trước khi đặt phòng.
     */
    public boolean isRoomAvailable(String maPhong, LocalDateTime from, LocalDateTime to) {
        String sql =
            "SELECT COUNT(*) FROM ChiTietDatPhong ct " +
            "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
            "WHERE ct.maPhong = ? " +
            "AND dp.trangThai IN ('CONFIRMED', 'CHECKED_IN') " +
            "AND dp.ngayNhanDuKien < ? " +
            "AND dp.ngayTraDuKien > ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ps.setTimestamp(2, Timestamp.valueOf(to));
            ps.setTimestamp(3, Timestamp.valueOf(from));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in PhongDAO", e); }
        return true;
    }

    private Phong mapRow(ResultSet rs) throws SQLException {
        Phong p = new Phong();
        p.setMaPhong(rs.getString("maPhong"));
        p.setTang(rs.getInt("tang"));

        // Map HuongNhin object (includes heSoGia for price adjustment)
        String maHN = null;
        try { maHN = rs.getString("maHuongNhin"); } catch (Exception e) {}
        if (maHN != null) {
            HuongNhin hn = new HuongNhin();
            hn.setMaHuongNhin(maHN);
            try { hn.setTenHuongNhin(rs.getString("tenHuongNhin")); } catch (Exception e) {}
            try { hn.setHeSoGia(rs.getDouble("heSoGia")); } catch (Exception e) { hn.setHeSoGia(1.0); }
            p.setHuongNhin(hn);
        }

        LoaiPhong lp = new LoaiPhong();
        lp.setMaLoaiPhong(rs.getString("maLoaiPhong"));
        try {
            lp.setTenLoaiPhong(rs.getString("tenLoaiPhong"));
            lp.setGiaTheoNgay(rs.getDouble("giaTheoNgay"));
            lp.setSucChua(rs.getInt("soNguoiToiDa"));
        } catch (Exception e) {}
        p.setLoaiPhong(lp);
        p.setGiaTheoNgay(lp.getGiaTheoNgay());

        String statusStr = rs.getString("trangThai");
        if (statusStr != null) {
            try {
                p.setTrangThai(entity.enums.TrangThaiPhong.valueOf(statusStr.toUpperCase()));
            } catch (Exception e) {
                p.setTrangThai(entity.enums.TrangThaiPhong.AVAILABLE);
            }
        }
        return p;
    }
}

