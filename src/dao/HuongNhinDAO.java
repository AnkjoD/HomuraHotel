package dao;

import database.DemoRepository;

import database.DatabaseConnection;
import entity.HuongNhin;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho bảng HuongNhin (Room View lookup).
 */
public class HuongNhinDAO {

    private static final Logger LOGGER = Logger.getLogger(HuongNhinDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<HuongNhin> getAll() {
        if (database.DatabaseConnection.isDemoMode()) return database.DemoRepository.all(entity.HuongNhin.class);
        List<HuongNhin> list = new ArrayList<>();
        String sql = "SELECT * FROM HuongNhin ORDER BY thuTu, tenHuongNhin";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in HuongNhinDAO", e); }
        return list;
    }

    public HuongNhin getById(String maHuongNhin) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.get(HuongNhin.class, maHuongNhin); }
        String sql = "SELECT * FROM HuongNhin WHERE maHuongNhin=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maHuongNhin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in HuongNhinDAO", e); }
        return null;
    }

    public boolean insert(HuongNhin hn) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.insert(HuongNhin.class, hn); }
        String sql = "INSERT INTO HuongNhin(maHuongNhin, tenHuongNhin, moTa, heSoGia, thuTu) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, hn.getMaHuongNhin());
            ps.setString(2, hn.getTenHuongNhin());
            ps.setString(3, hn.getMoTa());
            ps.setDouble(4, hn.getHeSoGia());
            ps.setInt(5, hn.getThuTu());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in HuongNhinDAO", e); return false; }
    }

    public boolean update(HuongNhin hn) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.update(HuongNhin.class, hn); }
        String sql = "UPDATE HuongNhin SET tenHuongNhin=?, moTa=?, heSoGia=?, thuTu=? WHERE maHuongNhin=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, hn.getTenHuongNhin());
            ps.setString(2, hn.getMoTa());
            ps.setDouble(3, hn.getHeSoGia());
            ps.setInt(4, hn.getThuTu());
            ps.setString(5, hn.getMaHuongNhin());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in HuongNhinDAO", e); return false; }
    }

    public boolean delete(String maHuongNhin) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.remove(entity.HuongNhin.class,x -> maHuongNhin.equals(x.getMaHuongNhin())); }
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM HuongNhin WHERE maHuongNhin=?")) {
            ps.setString(1, maHuongNhin);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in HuongNhinDAO", e); return false; }
    }

    /** Generate next mã: HN07, HN08... */
    public String generateMa() {
        return util.IdGenerator.next("HN");
    }

    /** Đếm số phòng đang dùng hướng nhìn này */
    public int countPhongByHuongNhin(String maHuongNhin) {
        if (DatabaseConnection.isDemoMode()) { return (int)new PhongDAO().getAll().stream().filter(p -> p.getHuongNhin()!=null && maHuongNhin.equals(p.getHuongNhin().getMaHuongNhin())).count(); }
        String sql = "SELECT COUNT(*) FROM Phong WHERE maHuongNhin=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maHuongNhin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in HuongNhinDAO", e); }
        return 0;
    }

    private HuongNhin mapRow(ResultSet rs) throws SQLException {
        HuongNhin hn = new HuongNhin();
        hn.setMaHuongNhin(rs.getString("maHuongNhin"));
        hn.setTenHuongNhin(rs.getString("tenHuongNhin"));
        hn.setMoTa(rs.getString("moTa"));
        try { hn.setHeSoGia(rs.getDouble("heSoGia")); } catch (Exception e) { hn.setHeSoGia(1.0); }
        try { hn.setThuTu(rs.getInt("thuTu")); } catch (Exception e) {}
        return hn;
    }
}


