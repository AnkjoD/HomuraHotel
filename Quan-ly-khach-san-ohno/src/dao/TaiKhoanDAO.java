package dao;

import database.DatabaseConnection;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;
import service.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaiKhoanDAO {

    private static final Logger LOGGER = Logger.getLogger(TaiKhoanDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Đăng nhập — hỗ trợ cả mật khẩu đã hash và plaintext (legacy).
     * Nếu phát hiện mật khẩu plaintext, tự động nâng cấp sang hash.
     */
    public TaiKhoan dangNhap(String tenDangNhap, String matKhau) {
        // Lấy tài khoản theo tên đăng nhập (không so sánh mật khẩu trong SQL)
        String sql = "SELECT tk.*, nv.hoTen, nv.sdt, nv.chucVu " +
                     "FROM TaiKhoan tk " +
                     "JOIN NhanVien nv ON tk.maNhanVien = nv.maNhanVien " +
                     "WHERE tk.tenDangNhap=? AND tk.trangThai=1";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tenDangNhap);
            ps.setQueryTimeout(8);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("matKhau");

                // Kiểm tra mật khẩu
                boolean passwordMatch;
                if (PasswordUtils.isHashed(storedPassword)) {
                    // Mật khẩu đã hash → verify bằng PasswordUtils
                    passwordMatch = PasswordUtils.verifyPassword(matKhau, storedPassword);
                } else {
                    // Mật khẩu plaintext legacy → so sánh trực tiếp
                    passwordMatch = matKhau.equals(storedPassword);
                    if (passwordMatch) {
                        // Tự động nâng cấp: hash mật khẩu và lưu lại vào DB
                        String hashed = PasswordUtils.hashPassword(matKhau);
                        upgradePassword(tenDangNhap, hashed);
                        LOGGER.info("Đã nâng cấp mật khẩu plaintext → hash cho tài khoản: " + tenDangNhap);
                    }
                }

                if (passwordMatch) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi đăng nhập", e);
        }
        return null;
    }

    /**
     * Nâng cấp mật khẩu từ plaintext sang hash (silent upgrade).
     */
    private void upgradePassword(String tenDangNhap, String hashedPassword) {
        String sql = "UPDATE TaiKhoan SET matKhau=? WHERE tenDangNhap=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setString(2, tenDangNhap);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Không thể nâng cấp mật khẩu cho: " + tenDangNhap, e);
        }
    }

    public boolean insert(TaiKhoan tk) {
        String sql = "INSERT INTO TaiKhoan(tenDangNhap, maNhanVien, matKhau, vaiTro, trangThai, lanDangNhapCuoi) " +
                     "VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tk.getTenDangNhap());
            ps.setString(2, tk.getNhanVien().getMaNhanVien());
            // Luôn hash mật khẩu trước khi lưu
            String hashedPwd = PasswordUtils.isHashed(tk.getMatKhau()) 
                ? tk.getMatKhau() 
                : PasswordUtils.hashPassword(tk.getMatKhau());
            ps.setString(3, hashedPwd);
            ps.setString(4, tk.getVaiTro().name());
            ps.setBoolean(5, tk.isTrangThai());
            ps.setTimestamp(6, tk.getLanDangNhapCuoi() != null ? Timestamp.valueOf(tk.getLanDangNhapCuoi()) : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi thêm tài khoản", e);
            return false;
        }
    }

    public boolean updateMatKhau(String tenDangNhap, String matKhauMoi) {
        // Luôn hash mật khẩu mới
        String hashedPwd = PasswordUtils.isHashed(matKhauMoi) 
            ? matKhauMoi 
            : PasswordUtils.hashPassword(matKhauMoi);
        String sql = "UPDATE TaiKhoan SET matKhau=? WHERE tenDangNhap=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, hashedPwd);
            ps.setString(2, tenDangNhap);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật mật khẩu", e);
            return false;
        }
    }

    public List<TaiKhoan> getAll() {
        List<TaiKhoan> list = new ArrayList<>();
        String sql = "SELECT tk.*, nv.hoTen, nv.sdt, nv.chucVu " +
                     "FROM TaiKhoan tk " +
                     "JOIN NhanVien nv ON tk.maNhanVien = nv.maNhanVien " +
                     "ORDER BY nv.hoTen";
        try (Statement st = getConn().createStatement(); 
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy danh sách tài khoản", e);
        }
        return list;
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
        updateMatKhau(tenDangNhap, newPwd);
    }
}
