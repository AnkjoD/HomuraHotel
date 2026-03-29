package dao;

import database.DatabaseConnection;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NhanVienDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public NhanVien getByMaNV(String maNV) {
        String sql = "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
                     "FROM NhanVien nv " +
                     "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
                     "WHERE nv.maNhanVien=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public NhanVien getById(String id) { return getByMaNV(id); }

    public List<NhanVien> getAll() {
        List<NhanVien> list = new ArrayList<>();
        String sql = "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
                     "FROM NhanVien nv " +
                     "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
                     "ORDER BY nv.hoTen";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<NhanVien> search(String keyword, String filterRole) {
        List<NhanVien> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
            "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
            "FROM NhanVien nv " +
            "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
            "WHERE 1=1"
        );
        if (keyword != null && !keyword.isBlank())
            sb.append(" AND (nv.hoTen LIKE ? OR nv.sdt LIKE ? OR nv.maNhanVien LIKE ?)");
        if (filterRole != null && !filterRole.equals("Tất cả"))
            sb.append(" AND tk.vaiTro = ?");
        
        sb.append(" ORDER BY nv.hoTen");
        try (PreparedStatement ps = getConn().prepareStatement(sb.toString())) {
            int idx = 1;
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword + "%";
                ps.setString(idx++, kw);
                ps.setString(idx++, kw);
                ps.setString(idx++, kw);
            }
            if (filterRole != null && !filterRole.equals("Tất cả"))
                ps.setString(idx, filterRole);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean insert(NhanVien nv) {
        String sqlNV = "INSERT INTO NhanVien(maNhanVien, hoTen, sdt, chucVu) VALUES(?,?,?,?)";
        String sqlTK = "INSERT INTO TaiKhoan(tenDangNhap, matKhau, vaiTro, trangThai, maNhanVien) VALUES(?,?,?,1,?)";
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlNV)) {
                    ps.setString(1, nv.getMaNhanVien());
                    ps.setString(2, nv.getHoTen());
                    ps.setString(3, nv.getSdt());
                    ps.setString(4, nv.getChucVu());
                    ps.executeUpdate();
                }
                TaiKhoan tk = nv.getTaiKhoan();
                if (tk != null) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlTK)) {
                        ps.setString(1, tk.getTenDangNhap() != null ? tk.getTenDangNhap() : nv.getMaNhanVien());
                        ps.setString(2, tk.getMatKhau() != null ? tk.getMatKhau() : "123456");
                        String role = tk.getVaiTro() != null ? tk.getVaiTro().name() : "RECEPTIONIST";
                        ps.setString(3, role);
                        ps.setString(4, nv.getMaNhanVien());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean update(NhanVien nv) {
        String sqlNV = "UPDATE NhanVien SET hoTen=?, sdt=?, chucVu=? WHERE maNhanVien=?";
        String sqlTK = "UPDATE TaiKhoan SET vaiTro=?" +
                       (nv.getTaiKhoan() != null && nv.getTaiKhoan().getMatKhau() != null
                           && !nv.getTaiKhoan().getMatKhau().isBlank() ? ", matKhau=?" : "") +
                       " WHERE maNhanVien=?";
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlNV)) {
                    ps.setString(1, nv.getHoTen());
                    ps.setString(2, nv.getSdt());
                    ps.setString(3, nv.getChucVu());
                    ps.setString(4, nv.getMaNhanVien());
                    ps.executeUpdate();
                }
                TaiKhoan tk = nv.getTaiKhoan();
                if (tk != null) {
                    boolean hasPw = tk.getMatKhau() != null && !tk.getMatKhau().isBlank();
                    String sql2 = "UPDATE TaiKhoan SET vaiTro=?" + (hasPw ? ", matKhau=?" : "") + " WHERE maNhanVien=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql2)) {
                        int idx = 1;
                        ps.setString(idx++, tk.getVaiTro() != null ? tk.getVaiTro().name() : "RECEPTIONIST");
                        if (hasPw) ps.setString(idx++, tk.getMatKhau());
                        ps.setString(idx, nv.getMaNhanVien());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                ex.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean delete(String maNhanVien) {
        // FK constraint means we should delete TaiKhoan first
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM TaiKhoan WHERE maNhanVien=?")) {
                    ps.setString(1, maNhanVien);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM NhanVien WHERE maNhanVien=?")) {
                    ps.setString(1, maNhanVien);
                    int res = ps.executeUpdate();
                    conn.commit();
                    return res > 0;
                }
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public String generateMaNV() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(CAST(SUBSTRING(maNhanVien,3,10) AS INT)) FROM NhanVien WHERE maNhanVien LIKE 'NV%'")) {
            if (rs.next()) return String.format("NV%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { e.printStackTrace(); }
        return "NV001";
    }

    private NhanVien mapRow(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        nv.setHoTen(rs.getString("hoTen"));
        nv.setSdt(rs.getString("sdt"));
        nv.setChucVu(rs.getString("chucVu"));

        // Map account if exists
        String tenDN = rs.getString("tenDangNhap");
        if (tenDN != null) {
            TaiKhoan tk = new TaiKhoan();
            tk.setTenDangNhap(tenDN);
            String roleStr = rs.getString("vaiTro");
            if (roleStr != null) {
                try {
                    tk.setVaiTro(VaiTro.valueOf(roleStr));
                } catch (Exception e) {
                    tk.setVaiTro(VaiTro.RECEPTIONIST);
                }
            }
            tk.setTrangThai(rs.getBoolean("trangThai"));
            tk.setNhanVien(nv);
            nv.setTaiKhoan(tk);
        }

        return nv;
    }
}
