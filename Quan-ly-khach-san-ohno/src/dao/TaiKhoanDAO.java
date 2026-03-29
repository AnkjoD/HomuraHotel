package dao;

import database.DatabaseConnection;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;
import java.sql.*;

public class TaiKhoanDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public TaiKhoan dangNhap(String tenDangNhap, String matKhau) {
        String sql = "SELECT tk.*, nv.hoTen, nv.sdt, nv.chucVu " +
                     "FROM TaiKhoan tk " +
                     "JOIN NhanVien nv ON tk.maNhanVien = nv.maNhanVien " +
                     "WHERE tk.tenDangNhap=? AND tk.matKhau=? AND tk.trangThai=1";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.setString(2, matKhau);
            ps.setQueryTimeout(8);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean insert(TaiKhoan tk) {
        String sql = "INSERT INTO TaiKhoan(tenDangNhap, maNhanVien, matKhau, vaiTro, trangThai, lanDangNhapCuoi) " +
                     "VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tk.getTenDangNhap());
            ps.setString(2, tk.getNhanVien().getMaNhanVien());
            ps.setString(3, tk.getMatKhau());
            ps.setString(4, tk.getVaiTro().name());
            ps.setBoolean(5, tk.isTrangThai());
            ps.setTimestamp(6, tk.getLanDangNhapCuoi() != null ? Timestamp.valueOf(tk.getLanDangNhapCuoi()) : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateMatKhau(String tenDangNhap, String matKhauMoi) {
        String sql = "UPDATE TaiKhoan SET matKhau=? WHERE tenDangNhap=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, matKhauMoi);
            ps.setString(2, tenDangNhap);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private TaiKhoan mapRow(ResultSet rs) throws SQLException {
        TaiKhoan tk = new TaiKhoan();
        tk.setTenDangNhap(rs.getString("tenDangNhap"));
        tk.setMatKhau(rs.getString("matKhau"));
        tk.setVaiTro(VaiTro.valueOf(rs.getString("vaiTro")));
        tk.setTrangThai(rs.getBoolean("trangThai"));
        Timestamp ts = rs.getTimestamp("lanDangNhapCuoi");
        if (ts != null) tk.setLanDangNhapCuoi(ts.toLocalDateTime());

        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        nv.setHoTen(rs.getString("hoTen"));
        nv.setSdt(rs.getString("sdt"));
        nv.setChucVu(rs.getString("chucVu"));
        tk.setNhanVien(nv);
        nv.setTaiKhoan(tk);

        return tk;
    }

    public void doiMatKhau(String tenDangNhap, String newPwd) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
