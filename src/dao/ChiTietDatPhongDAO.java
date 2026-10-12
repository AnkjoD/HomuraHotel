package dao;

import database.DatabaseConnection;
import entity.ChiTietDatPhong;
import entity.DatPhong;
import entity.KhachHang;
import entity.Phong;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChiTietDatPhongDAO {

    private static final Logger LOGGER = Logger.getLogger(ChiTietDatPhongDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<ChiTietDatPhong> getByDatPhong(String maDatPhong) {
        List<ChiTietDatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM ChiTietDatPhong WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in ChiTietDatPhongDAO", e);
        }
        return list;
    }

    public boolean insert(ChiTietDatPhong ct) {
        String sql = "INSERT INTO ChiTietDatPhong(maChiTiet, maDatPhong, maPhong, maKhachHang, ngayNhanDuKien, ngayTraDuKien, ngayNhanThucTe, ngayTraThucTe, giaThucTeChot, phuPhiPhatSinh, daThanhToan) "
                +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, ct.getMaChiTiet());
            ps.setString(2, ct.getDatPhong() != null ? ct.getDatPhong().getMaDatPhong() : null);
            ps.setString(3, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
            ps.setString(4, ct.getKhachHang() != null ? ct.getKhachHang().getMaKhachHang() : null);
            ps.setTimestamp(5, ct.getNgayNhanDuKien() != null ? Timestamp.valueOf(ct.getNgayNhanDuKien()) : null);
            ps.setTimestamp(6, ct.getNgayTraDuKien() != null ? Timestamp.valueOf(ct.getNgayTraDuKien()) : null);
            ps.setTimestamp(7, ct.getNgayNhanThucTe() != null ? Timestamp.valueOf(ct.getNgayNhanThucTe()) : null);
            ps.setTimestamp(8, ct.getNgayTraThucTe() != null ? Timestamp.valueOf(ct.getNgayTraThucTe()) : null);
            ps.setDouble(9, ct.getGiaThucTeChot());
            ps.setDouble(10, ct.getPhuPhiPhatSinh());
            ps.setBoolean(11, ct.isDaThanhToan());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in ChiTietDatPhongDAO", e);
            return false;
        }
    }

    public boolean update(ChiTietDatPhong ct) {
        String sql = "UPDATE ChiTietDatPhong SET maDatPhong=?, maPhong=?, maKhachHang=?, ngayNhanDuKien=?, ngayTraDuKien=?, ngayNhanThucTe=?, ngayTraThucTe=?, giaThucTeChot=?, phuPhiPhatSinh=?, daThanhToan=? WHERE maChiTiet=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, ct.getDatPhong() != null ? ct.getDatPhong().getMaDatPhong() : null);
            ps.setString(2, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
            ps.setString(3, ct.getKhachHang() != null ? ct.getKhachHang().getMaKhachHang() : null);
            ps.setTimestamp(4, ct.getNgayNhanDuKien() != null ? Timestamp.valueOf(ct.getNgayNhanDuKien()) : null);
            ps.setTimestamp(5, ct.getNgayTraDuKien() != null ? Timestamp.valueOf(ct.getNgayTraDuKien()) : null);
            ps.setTimestamp(6, ct.getNgayNhanThucTe() != null ? Timestamp.valueOf(ct.getNgayNhanThucTe()) : null);
            ps.setTimestamp(7, ct.getNgayTraThucTe() != null ? Timestamp.valueOf(ct.getNgayTraThucTe()) : null);
            ps.setDouble(8, ct.getGiaThucTeChot());
            ps.setDouble(9, ct.getPhuPhiPhatSinh());
            ps.setBoolean(10, ct.isDaThanhToan());
            ps.setString(11, ct.getMaChiTiet());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in ChiTietDatPhongDAO", e);
            return false;
        }
    }

    private ChiTietDatPhong mapRow(ResultSet rs) throws SQLException {
        ChiTietDatPhong ct = new ChiTietDatPhong();
        ct.setMaChiTiet(rs.getString("maChiTiet"));

        // Load Phong with full data
        String soPhong = rs.getString("maPhong");
        if (soPhong != null) {
            Phong p = new PhongDAO().getById(soPhong);
            ct.setPhong(p != null ? p : new Phong());
        }

        // Load DatPhong with full data including KhachHang
        String maDatPhong = rs.getString("maDatPhong");
        if (maDatPhong != null) {
            DatPhong dp = new DatPhongDAO().getByIdBasic(maDatPhong);
            ct.setDatPhong(dp);
        }

        // Load KhachHang rieng cua phong (nguoi luu tru thuc te)
        String maKhachHang = rs.getString("maKhachHang");
        if (maKhachHang != null) {
            KhachHang kh = new KhachHangDAO().getById(maKhachHang);
            ct.setKhachHang(kh);
        }

        // Per-room planned dates (nullable)
        Timestamp plannedCheckin = rs.getTimestamp("ngayNhanDuKien");
        if (plannedCheckin != null)
            ct.setNgayNhanDuKien(plannedCheckin.toLocalDateTime());

        Timestamp plannedCheckout = rs.getTimestamp("ngayTraDuKien");
        if (plannedCheckout != null)
            ct.setNgayTraDuKien(plannedCheckout.toLocalDateTime());

        Timestamp checkin = rs.getTimestamp("ngayNhanThucTe");
        if (checkin != null)
            ct.setNgayNhanThucTe(checkin.toLocalDateTime());

        Timestamp checkout = rs.getTimestamp("ngayTraThucTe");
        if (checkout != null)
            ct.setNgayTraThucTe(checkout.toLocalDateTime());

        ct.setGiaThucTeChot(rs.getDouble("giaThucTeChot"));
        ct.setPhuPhiPhatSinh(rs.getDouble("phuPhiPhatSinh"));
        
        try {
            ct.setDaThanhToan(rs.getBoolean("daThanhToan"));
        } catch (SQLException ignored) {
            // Backward compatibility with older schema
            ct.setDaThanhToan(false);
        }

        return ct;
    }

    public String generateMaChiTiet() {
        try (Statement st = getConn().createStatement();
                ResultSet rs = st
                        .executeQuery("SELECT MAX(CAST(SUBSTRING(maChiTiet,3,10) AS INT)) FROM ChiTietDatPhong")) {
            if (rs.next())
                return String.format("CT%04d", rs.getInt(1) + 1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in ChiTietDatPhongDAO", e);
        }
        return "CT0001";
    }

    public ChiTietDatPhong getById(String id) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM ChiTietDatPhong WHERE maChiTiet=?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in ChiTietDatPhongDAO", e);
        }
        return null;
    }

    public List<ChiTietDatPhong> getAll() {
        List<ChiTietDatPhong> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM ChiTietDatPhong")) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in ChiTietDatPhongDAO", e);
        }
        return list;
    }

    /** Kiểm tra xem phòng có lượt stay nào chưa trả phòng không */
    public boolean hasActiveStay(String maPhong) {
        String sql = "SELECT COUNT(*) FROM ChiTietDatPhong WHERE maPhong=? AND ngayTraThucTe IS NULL AND ngayNhanThucTe IS NOT NULL";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error", e); }
        return false;
    }

    /** Lấy lượt stay đang hoạt động của một phòng (được ưu tiên gán cho các thao tác Check-out/Dịch vụ) */
    public ChiTietDatPhong getActiveStayByPhong(String maPhong) {
        String sql = "SELECT TOP 1 * FROM ChiTietDatPhong WHERE maPhong=? AND ngayTraThucTe IS NULL AND ngayNhanThucTe IS NOT NULL ORDER BY ngayNhanThucTe DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error", e); }
        return null;
    }

    /** Tìm một đơn đặt phòng khác đang có lịch trùng với khoảng thời gian dự định */
    public ChiTietDatPhong getConflictingStay(String maPhong, LocalDateTime start, LocalDateTime end, String excludeMaDatPhong) {
        String sql = "SELECT TOP 1 ct.* FROM ChiTietDatPhong ct " +
                     "JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
                     "WHERE ct.maPhong = ? " +
                     "AND dp.trangThai IN ('PENDING', 'CONFIRMED', 'CHECKED_IN', 'PARTIALLY_CHECKED_IN') " +
                     "AND ( " +
                     "  (dp.trangThai IN ('PENDING', 'CONFIRMED') AND ct.ngayTraThucTe IS NULL) " +
                     "  OR " +
                     "  (dp.trangThai IN ('CHECKED_IN', 'PARTIALLY_CHECKED_IN') AND ct.ngayNhanThucTe IS NOT NULL AND ct.ngayTraThucTe IS NULL) " +
                     ") " +
                     "AND dp.maDatPhong != ? " +
                     "AND COALESCE(ct.ngayNhanDuKien, dp.ngayNhanDuKien) < ? " +
                     "AND COALESCE(ct.ngayTraDuKien, dp.ngayTraDuKien) > ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maPhong);
            ps.setString(2, excludeMaDatPhong != null ? excludeMaDatPhong : "");
            ps.setTimestamp(3, Timestamp.valueOf(end));
            ps.setTimestamp(4, Timestamp.valueOf(start));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error", e); }
        return null;
    }

    public boolean deleteByDatPhong(String maDatPhong) {
        String sql = "DELETE FROM ChiTietDatPhong WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khi xóa chi tiết đặt phòng cho mã: " + maDatPhong, e);
            return false;
        }
    }

    /** Lấy danh sách các phòng đang được thuê thuộc cùng 1 đơn đặt phòng (Đoàn/Group) */
    public java.util.Map<String, String> getActiveGroupRoomsMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        String sql = "SELECT ct.maPhong, ct.maDatPhong, dp.tenDoan FROM ChiTietDatPhong ct " +
                     "INNER JOIN DatPhong dp ON ct.maDatPhong = dp.maDatPhong " +
                     "WHERE ct.ngayTraThucTe IS NULL AND ct.ngayNhanThucTe IS NOT NULL " +
                     "AND dp.loaiKhach = 'DOAN'";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String maPhong = rs.getString("maPhong");
                String maDatPhong = rs.getString("maDatPhong");
                String tenDoan = rs.getString("tenDoan");
                if (tenDoan == null || tenDoan.trim().isEmpty()) {
                    tenDoan = "Đoàn tự do";
                }
                map.put(maPhong, tenDoan + "|" + maDatPhong);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error", e);
        }
        return map;
    }
}
