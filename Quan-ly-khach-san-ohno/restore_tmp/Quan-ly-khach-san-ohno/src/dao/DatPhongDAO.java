package dao;

import database.DatabaseConnection;
import entity.DatPhong;
import entity.KhachHang;
import entity.NhanVien;
import entity.enums.TrangThaiDatPhong;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatPhongDAO {

    private static final Logger LOGGER = Logger.getLogger(DatPhongDAO.class.getName());

    private final KhachHangDAO khachHangDAO = new KhachHangDAO();

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<DatPhong> getAll() {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM DatPhong ORDER BY ngayDat DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi lấy danh sách đặt phòng", e); }
        return list;
    }

    public DatPhong getById(String maDatPhong) {
        String sql = "SELECT * FROM DatPhong WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi lấy đặt phòng theo mã", e); }
        return null;
    }

    public DatPhong getByIdBasic(String maDatPhong) {
        String sql = "SELECT * FROM DatPhong WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs, false);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi lấy đặt phòng (basic)", e); }
        return null;
    }

    public boolean insert(DatPhong dp) {
        String sql = "INSERT INTO DatPhong(maDatPhong, maKhachHang, maNhanVien, ngayDat, ngayNhanDuKien, ngayTraDuKien, soNguoi, tienDatCoc, tongTienTamTinh, trangThai, ghiChu, loaiKhach, tenDoan) " +
                     "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dp.getMaDatPhong());
            ps.setString(2, dp.getKhachHang() != null ? dp.getKhachHang().getMaKhachHang() : null);
            ps.setString(3, dp.getNhanVien() != null ? dp.getNhanVien().getMaNhanVien() : null);
            ps.setTimestamp(4, dp.getNgayDat() != null ? Timestamp.valueOf(dp.getNgayDat()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setTimestamp(5, dp.getNgayNhanDuKien() != null ? Timestamp.valueOf(dp.getNgayNhanDuKien()) : null);
            ps.setTimestamp(6, dp.getNgayTraDuKien() != null ? Timestamp.valueOf(dp.getNgayTraDuKien()) : null);
            ps.setInt(7, dp.getSoLuongKhach());
            ps.setDouble(8, dp.getTienDatCoc());
            ps.setDouble(9, dp.getTongTienTamTinh());
            String statusStr = dp.getTrangThai() != null ? dp.getTrangThai().name() : "PENDING";
            if ("PARTIALLY_CHECKED_IN".equals(statusStr)) statusStr = "CHECKED_IN";
            ps.setString(10, statusStr);
            ps.setString(11, dp.getGhiChu());
            ps.setString(12, dp.getLoaiKhach() != null ? dp.getLoaiKhach() : "CA_NHAN");
            ps.setString(13, dp.getTenDoan());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi thêm đặt phòng", e); return false; }
    }

    public boolean update(DatPhong dp) {
        String sql = "UPDATE DatPhong SET maKhachHang=?, maNhanVien=?, ngayNhanDuKien=?, ngayTraDuKien=?, soNguoi=?, tienDatCoc=?, tongTienTamTinh=?, trangThai=?, ghiChu=?, loaiKhach=?, tenDoan=? WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dp.getKhachHang() != null ? dp.getKhachHang().getMaKhachHang() : null);
            ps.setString(2, dp.getNhanVien() != null ? dp.getNhanVien().getMaNhanVien() : null);
            ps.setTimestamp(3, dp.getNgayNhanDuKien() != null ? Timestamp.valueOf(dp.getNgayNhanDuKien()) : null);
            ps.setTimestamp(4, dp.getNgayTraDuKien() != null ? Timestamp.valueOf(dp.getNgayTraDuKien()) : null);
            ps.setInt(5, dp.getSoLuongKhach());
            ps.setDouble(6, dp.getTienDatCoc());
            ps.setDouble(7, dp.getTongTienTamTinh());
            String statusStr = dp.getTrangThai() != null ? dp.getTrangThai().name() : "PENDING";
            if ("PARTIALLY_CHECKED_IN".equals(statusStr)) statusStr = "CHECKED_IN";
            ps.setString(8, statusStr);
            ps.setString(9, dp.getGhiChu());
            ps.setString(10, dp.getLoaiKhach() != null ? dp.getLoaiKhach() : "CA_NHAN");
            ps.setString(11, dp.getTenDoan());
            ps.setString(12, dp.getMaDatPhong());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi cập nhật đặt phòng", e); return false; }
    }

    public boolean delete(String maDatPhong) {
        try (Connection con = getConn()) {
            con.setAutoCommit(false);
            try {
                String sqlDV = "DELETE FROM SuDungDichVu WHERE maChiTiet IN (SELECT maChiTiet FROM ChiTietDatPhong WHERE maDatPhong=?)";
                try (PreparedStatement ps = con.prepareStatement(sqlDV)) {
                    ps.setString(1, maDatPhong);
                    ps.executeUpdate();
                }
                
                String sqlCT = "DELETE FROM ChiTietDatPhong WHERE maDatPhong=?";
                try (PreparedStatement ps = con.prepareStatement(sqlCT)) {
                    ps.setString(1, maDatPhong);
                    ps.executeUpdate();
                }
                
                String sqlDP = "DELETE FROM DatPhong WHERE maDatPhong=?";
                boolean ok;
                try (PreparedStatement ps = con.prepareStatement(sqlDP)) {
                    ps.setString(1, maDatPhong);
                    ok = ps.executeUpdate() > 0;
                }
                
                con.commit();
                return ok;
            } catch (SQLException ex) {
                con.rollback();
                LOGGER.log(Level.SEVERE, "Lỗi xóa đặt phòng", ex);
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi kết nối khi xóa đặt phòng", e);
            return false;
        }
    }

    public String generateMaDatPhong() {
        java.time.LocalDate today = java.time.LocalDate.now();
        String prefix = String.format("DP-%d%02d%02d-", today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        String sql = "SELECT MAX(CAST(SUBSTRING(maDatPhong, LEN(?)+1, 10) AS INT)) FROM DatPhong WHERE maDatPhong LIKE ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, prefix); ps.setString(2, prefix + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return prefix + String.format("%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { LOGGER.log(Level.WARNING, "Lỗi sinh mã đặt phòng", e); }
        return prefix + "001";
    }

    public List<DatPhong> search(String kw, String status, String date) {
        List<DatPhong> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder("SELECT * FROM DatPhong WHERE 1=1 ");
        if (kw != null && !kw.isEmpty()) sb.append("AND (maDatPhong LIKE ? OR maKhachHang LIKE ?) ");
        if (status != null && !status.isEmpty()) sb.append("AND trangThai = ? ");
        if (date != null && !date.isEmpty()) sb.append("AND CAST(ngayDat AS DATE) = ? ");
        sb.append("ORDER BY ngayDat DESC");

        try (PreparedStatement ps = getConn().prepareStatement(sb.toString())) {
            int idx = 1;
            if (kw != null && !kw.isEmpty()) { 
                String k = "%" + kw + "%";
                ps.setString(idx++, k); ps.setString(idx++, k); 
            }
            if (status != null && !status.isEmpty()) ps.setString(idx++, status);
            if (date != null && !date.isEmpty()) ps.setString(idx++, date);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Lỗi tìm kiếm đặt phòng", e); }
        return list;
    }

    private DatPhong mapRow(ResultSet rs) throws SQLException {
        return mapRow(rs, true);
    }

    private DatPhong mapRow(ResultSet rs, boolean includeDetails) throws SQLException {
        DatPhong dp = new DatPhong();
        dp.setMaDatPhong(rs.getString("maDatPhong"));
        
        String maKH = rs.getString("maKhachHang");
        KhachHang kh = maKH != null ? khachHangDAO.getById(maKH) : null;
        if (kh == null) {
            kh = new KhachHang();
            kh.setMaKhachHang(maKH);
        }
        dp.setKhachHang(kh);

        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        dp.setNhanVien(nv);

        Timestamp tsNgayDat = rs.getTimestamp("ngayDat");
        if (tsNgayDat != null) dp.setNgayDat(tsNgayDat.toLocalDateTime());

        Timestamp tsNgayNhan = rs.getTimestamp("ngayNhanDuKien");
        if (tsNgayNhan != null) dp.setNgayNhanDuKien(tsNgayNhan.toLocalDateTime());

        Timestamp tsNgayTra = rs.getTimestamp("ngayTraDuKien");
        if (tsNgayTra != null) dp.setNgayTraDuKien(tsNgayTra.toLocalDateTime());

        dp.setSoLuongKhach(rs.getInt("soNguoi"));
        dp.setTienDatCoc(rs.getDouble("tienDatCoc"));
        dp.setTongTienTamTinh(rs.getDouble("tongTienTamTinh"));
        dp.setGhiChu(rs.getString("ghiChu"));
        
        // Đọc loại khách (safe: tương thích ngược với DB chưa có cột này)
        try {
            String lk = rs.getString("loaiKhach");
            dp.setLoaiKhach(lk != null ? lk : "CA_NHAN");
        } catch (SQLException ignored) { dp.setLoaiKhach("CA_NHAN"); }
        try {
            dp.setTenDoan(rs.getString("tenDoan"));
        } catch (SQLException ignored) { }
        
        String statusStr = rs.getString("trangThai");
        dp.setTrangThai(parseTrangThai(statusStr));
        
        // Load ChiTietDatPhong for this booking (optional to avoid recursive loops)
        if (includeDetails) {
            String maDatPhong = rs.getString("maDatPhong");
            if (maDatPhong != null) {
                ChiTietDatPhongDAO ctDAO = new ChiTietDatPhongDAO();
                dp.setDsChiTiet(ctDAO.getByDatPhong(maDatPhong));
            }
        }
        
        // Nâng cấp trạng thái CHECKED_IN lên PARTIALLY_CHECKED_IN nếu còn phòng chưa nhận
        if (dp.getTrangThai() == TrangThaiDatPhong.CHECKED_IN) {
            if (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty()) {
                boolean allIn = true;
                for (entity.ChiTietDatPhong ct : dp.getDsChiTiet()) {
                    if (ct.getPhong() == null || ct.getPhong().getTrangThai() != entity.enums.TrangThaiPhong.OCCUPIED) {
                        allIn = false;
                        break;
                    }
                }
                if (!allIn) dp.setTrangThai(TrangThaiDatPhong.PARTIALLY_CHECKED_IN);
            }
        }

        return dp;
    }

    private TrangThaiDatPhong parseTrangThai(String raw) {
        if (raw == null) return TrangThaiDatPhong.PENDING;
        String s = raw.trim();
        if (s.isEmpty()) return TrangThaiDatPhong.PENDING;

        // 1) Enum style values from DB
        try {
            return TrangThaiDatPhong.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            // Continue with localized mapping.
        }

        // 2) Localized labels (Vietnamese)
        String n = s.toLowerCase();
        if (n.contains("xác nhận")) return TrangThaiDatPhong.CONFIRMED;
        if (n.contains("đang check-in") || n.contains("partially")) return TrangThaiDatPhong.PARTIALLY_CHECKED_IN;
        if (n.contains("check-in") || n.contains("đã checkin") || n.contains("checked in")) return TrangThaiDatPhong.CHECKED_IN;
        if (n.contains("trả phòng") || n.contains("checkout") || n.contains("checked out")) return TrangThaiDatPhong.CHECKED_OUT;
        if (n.contains("hủy") || n.contains("cancel")) return TrangThaiDatPhong.CANCELLED;
        if (n.contains("chờ") || n.contains("pending")) return TrangThaiDatPhong.PENDING;

        return TrangThaiDatPhong.PENDING;
    }
}
