package dao;

import database.DemoRepository;

import database.DatabaseConnection;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class NhanVienDAO {

    private static final Logger LOGGER = Logger.getLogger(NhanVienDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public NhanVien getByMaNV(String maNV) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.get(NhanVien.class, maNV); }
        String sql = "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
                "FROM NhanVien nv " +
                "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
                "WHERE nv.maNhanVien=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapRow(rs);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO", e);
        }
        return null;
    }

    public NhanVien getById(String id) {
        return getByMaNV(id);
    }

    public List<NhanVien> getAll() {
        if (database.DatabaseConnection.isDemoMode()) return database.DemoRepository.all(entity.NhanVien.class);
        List<NhanVien> list = new ArrayList<>();
        String sql = "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
                "FROM NhanVien nv " +
                "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
                "WHERE nv.dangLamViec = 1 " +
                "ORDER BY nv.hoTen";
        try (Statement st = getConn().createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO", e);
        }
        return list;
    }

    public List<NhanVien> search(String keyword, String filterRole) {
        if (DatabaseConnection.isDemoMode()) { return getAll().stream().filter(x -> DemoRepository.matches(x.getHoTen(),keyword)||DemoRepository.matches(x.getMaNhanVien(),keyword)).collect(java.util.stream.Collectors.toList()); }
        List<NhanVien> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder(
                "SELECT nv.*, tk.tenDangNhap, tk.vaiTro, tk.trangThai " +
                        "FROM NhanVien nv " +
                        "LEFT JOIN TaiKhoan tk ON nv.maNhanVien = tk.maNhanVien " +
                        "WHERE nv.dangLamViec = 1");
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
            while (rs.next())
                list.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO", e);
        }
        return list;
    }

    public boolean insert(NhanVien nv) {
        if (nv.getTaiKhoan()!=null && (nv.getTaiKhoan().getMatKhau()==null || nv.getTaiKhoan().getMatKhau().isBlank())) return false;
        if (DatabaseConnection.isDemoMode()) return saveDemoStaff(nv, true);
        String sqlNV = "INSERT INTO NhanVien(maNhanVien, hoTen, sdt, chucVu, email, cccd, ngaySinh, gioiTinh, diaChi, ngayVaoLam, dangLamViec) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        String sqlTK = "INSERT INTO TaiKhoan(tenDangNhap, matKhau, vaiTro, trangThai, maNhanVien) VALUES(?,?,?,1,?)";
        try {
            Connection conn = getConn();
            DatabaseConnection.getInstance().beginTransaction();
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlNV)) {
                    ps.setString(1, nv.getMaNhanVien());
                    ps.setString(2, nv.getHoTen());
                    ps.setString(3, nv.getSdt());
                    ps.setString(4, nv.getChucVu());
                    ps.setString(5, nv.getEmail());
                    ps.setString(6, nv.getCccd());
                    ps.setDate(7, nv.getNgaySinh() != null ? java.sql.Date.valueOf(nv.getNgaySinh()) : null);
                    ps.setString(8, nv.getGioiTinh());
                    ps.setString(9, nv.getDiaChi());
                    ps.setDate(10, nv.getNgayVaoLam() != null ? java.sql.Date.valueOf(nv.getNgayVaoLam()) : null);
                    ps.setBoolean(11, nv.isDangLamViec());
                    ps.executeUpdate();
                }
                TaiKhoan tk = nv.getTaiKhoan();
                if (tk != null) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlTK)) {
                        ps.setString(1, tk.getTenDangNhap() != null ? tk.getTenDangNhap() : nv.getMaNhanVien());
                        ps.setString(2, util.PasswordUtils.hashPassword(tk.getMatKhau()));
                        String role = tk.getVaiTro() != null ? tk.getVaiTro().name() : "RECEPTIONIST";
                        ps.setString(3, role);
                        ps.setString(4, nv.getMaNhanVien());
                        ps.executeUpdate();
                    }
                }
                DatabaseConnection.getInstance().commitTransaction();
                return true;
            } catch (SQLException ex) {
                DatabaseConnection.getInstance().rollbackTransaction();
                LOGGER.log(Level.SEVERE, "Error inserting NhanVien", ex);
                return false;
            } finally {
                DatabaseConnection.getInstance().rollbackTransaction();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO insert", e);
            return false;
        }
    }

    public boolean update(NhanVien nv) {
        if (DatabaseConnection.isDemoMode()) return saveDemoStaff(nv, false);
        String sqlNV = "UPDATE NhanVien SET hoTen=?, sdt=?, chucVu=?, email=?, cccd=?, ngaySinh=?, gioiTinh=?, diaChi=?, ngayVaoLam=?, dangLamViec=? WHERE maNhanVien=?";
        try {
            Connection conn = getConn();
            DatabaseConnection.getInstance().beginTransaction();
            try {
                try (PreparedStatement ps = conn.prepareStatement(sqlNV)) {
                    ps.setString(1, nv.getHoTen());
                    ps.setString(2, nv.getSdt());
                    ps.setString(3, nv.getChucVu());
                    ps.setString(4, nv.getEmail());
                    ps.setString(5, nv.getCccd());
                    ps.setDate(6, nv.getNgaySinh() != null ? java.sql.Date.valueOf(nv.getNgaySinh()) : null);
                    ps.setString(7, nv.getGioiTinh());
                    ps.setString(8, nv.getDiaChi());
                    ps.setDate(9, nv.getNgayVaoLam() != null ? java.sql.Date.valueOf(nv.getNgayVaoLam()) : null);
                    ps.setBoolean(10, nv.isDangLamViec());
                    ps.setString(11, nv.getMaNhanVien());
                    ps.executeUpdate();
                }
                TaiKhoan tk = nv.getTaiKhoan();
                if (tk != null) {
                    boolean hasPw = tk.getMatKhau() != null && !tk.getMatKhau().isBlank();
                    String sqlTK = "UPDATE TaiKhoan SET vaiTro=?, trangThai=?" + (hasPw ? ", matKhau=?" : "") + " WHERE maNhanVien=?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlTK)) {
                        int idx = 1;
                        ps.setString(idx++, tk.getVaiTro() != null ? tk.getVaiTro().name() : "RECEPTIONIST");
                        ps.setBoolean(idx++, nv.isDangLamViec());
                        if (hasPw)
                            ps.setString(idx++, util.PasswordUtils.isHashed(tk.getMatKhau()) ? tk.getMatKhau() : util.PasswordUtils.hashPassword(tk.getMatKhau()));
                        ps.setString(idx, nv.getMaNhanVien());
                        ps.executeUpdate();
                    }
                }
                DatabaseConnection.getInstance().commitTransaction();
                return true;
            } catch (SQLException ex) {
                DatabaseConnection.getInstance().rollbackTransaction();
                LOGGER.log(Level.SEVERE, "Error updating NhanVien", ex);
                return false;
            } finally {
                DatabaseConnection.getInstance().rollbackTransaction();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO update", e);
            return false;
        }
    }

    public boolean delete(String maNhanVien) {
        if (DatabaseConnection.isDemoMode()) { entity.NhanVien x=getById(maNhanVien);if(x==null)return false;x.setDangLamViec(false);for(entity.TaiKhoan t:DemoRepository.all(entity.TaiKhoan.class))if(t.getNhanVien()!=null && maNhanVien.equals(t.getNhanVien().getMaNhanVien())){t.setTrangThai(false);DemoRepository.update(entity.TaiKhoan.class,t);}return DemoRepository.update(NhanVien.class,x); }
        // Soft delete: set dangLamViec = 0 and deactivate account
        try {
            Connection conn = getConn();
            DatabaseConnection.getInstance().beginTransaction();
            try {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE TaiKhoan SET trangThai=0 WHERE maNhanVien=?")) {
                    ps.setString(1, maNhanVien);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE NhanVien SET dangLamViec=0 WHERE maNhanVien=?")) {
                    ps.setString(1, maNhanVien);
                    int res = ps.executeUpdate();
                    DatabaseConnection.getInstance().commitTransaction();
                    return res > 0;
                }
            } catch (SQLException ex) {
                DatabaseConnection.getInstance().rollbackTransaction();
                throw ex;
            } finally {
                DatabaseConnection.getInstance().rollbackTransaction();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error in NhanVienDAO soft delete", e);
            return false;
        }
    }

    private boolean saveDemoStaff(NhanVien nv, boolean create) {
        DatabaseConnection db=DatabaseConnection.getInstance();
        try {
            db.beginTransaction();
            boolean saved=create?DemoRepository.insert(NhanVien.class,nv):DemoRepository.update(NhanVien.class,nv);
            if(!saved)throw new IllegalStateException("Cannot save staff");
            TaiKhoan supplied=nv.getTaiKhoan();
            if(supplied!=null) {
                TaiKhoan account=DemoRepository.all(TaiKhoan.class).stream().filter(t->t.getNhanVien()!=null && nv.getMaNhanVien().equals(t.getNhanVien().getMaNhanVien())).findFirst().orElse(null);
                if(create) { supplied.setNhanVien(nv); supplied.setTrangThai(nv.isDangLamViec()); if(!new TaiKhoanDAO().insert(supplied))throw new IllegalStateException("Cannot create staff account"); }
                else if(account!=null) {
                    account.setVaiTro(supplied.getVaiTro());account.setTrangThai(nv.isDangLamViec());account.setNhanVien(nv);
                    String password=supplied.getMatKhau();
                    if(password!=null && !password.isBlank())account.setMatKhau(util.PasswordUtils.isHashed(password)?password:util.PasswordUtils.hashPassword(password));
                    if(!DemoRepository.update(TaiKhoan.class,account))throw new IllegalStateException("Cannot update staff account");
                }
            }
            db.commitTransaction();return true;
        }catch(Exception e){db.rollbackTransaction();return false;}
    }

    public String generateMaNV() {
        return util.IdGenerator.next("NV");
    }

    public boolean isSdtExists(String sdt, String excludeMaNV) {
        if (DatabaseConnection.isDemoMode()) { return getAll().stream().anyMatch(x -> !java.util.Objects.equals(x.getMaNhanVien(),excludeMaNV) && java.util.Objects.equals(x.getSdt(),sdt)); }
        String sql = "SELECT COUNT(*) FROM NhanVien WHERE sdt = ?" + (excludeMaNV != null ? " AND maNhanVien <> ?" : "");
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sdt);
            if (excludeMaNV != null)
                ps.setString(2, excludeMaNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking SDT exists", e);
        }
        return false;
    }

    public boolean isCccdExists(String cccd, String excludeMaNV) {
        if (DatabaseConnection.isDemoMode()) { return getAll().stream().anyMatch(x -> !java.util.Objects.equals(x.getMaNhanVien(),excludeMaNV) && java.util.Objects.equals(x.getCccd(),cccd)); }
        String sql = "SELECT COUNT(*) FROM NhanVien WHERE cccd = ?" + (excludeMaNV != null ? " AND maNhanVien <> ?" : "");
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, cccd);
            if (excludeMaNV != null)
                ps.setString(2, excludeMaNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking CCCD exists", e);
        }
        return false;
    }

    private NhanVien mapRow(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        nv.setHoTen(rs.getString("hoTen"));
        nv.setSdt(rs.getString("sdt"));
        nv.setChucVu(rs.getString("chucVu"));
        nv.setEmail(rs.getString("email"));
        nv.setCccd(rs.getString("cccd"));
        Date birth = rs.getDate("ngaySinh");
        if (birth != null) nv.setNgaySinh(birth.toLocalDate());
        nv.setGioiTinh(rs.getString("gioiTinh"));
        nv.setDiaChi(rs.getString("diaChi"));
        Date join = rs.getDate("ngayVaoLam");
        if (join != null) nv.setNgayVaoLam(join.toLocalDate());
        nv.setDangLamViec(rs.getBoolean("dangLamViec"));

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


