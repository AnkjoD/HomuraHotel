package entity;

import entity.KhachHang;
import entity.NhanVien;
import entity.HoaDon;
import entity.ChiTietDatPhong;
import entity.SuDungDichVu;
import entity.enums.TrangThaiDatPhong;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.time.ZoneId;

public class DatPhong {
    private String maDatPhong;
    private LocalDateTime ngayDat;
    private LocalDateTime ngayNhanDuKien;
    private LocalDateTime ngayTraDuKien;
    private int soLuongKhach;
    private double tienDatCoc;
    private double tongTienTamTinh;
    private TrangThaiDatPhong trangThai;
    private String ghiChu;

    private KhachHang khachHang;
    private NhanVien nhanVien;
    private HoaDon hoaDon;
    private List<ChiTietDatPhong> dsChiTiet = new ArrayList<>();
    private List<SuDungDichVu> dsDichVu = new ArrayList<>();

    public DatPhong() {}

    // Getters and Setters
    public String getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(String maDatPhong) { this.maDatPhong = maDatPhong; }

    public LocalDateTime getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDateTime ngayDat) { this.ngayDat = ngayDat; }

    public LocalDateTime getNgayNhanDuKien() { return ngayNhanDuKien; }
    public void setNgayNhanDuKien(LocalDateTime ngayNhanDuKien) { this.ngayNhanDuKien = ngayNhanDuKien; }

    public LocalDateTime getNgayTraDuKien() { return ngayTraDuKien; }
    public void setNgayTraDuKien(LocalDateTime ngayTraDuKien) { this.ngayTraDuKien = ngayTraDuKien; }

    public int getSoLuongKhach() { return soLuongKhach; }
    public void setSoLuongKhach(int soLuongKhach) { this.soLuongKhach = soLuongKhach; }

    public double getTienDatCoc() { return tienDatCoc; }
    public void setTienDatCoc(double tienDatCoc) { this.tienDatCoc = tienDatCoc; }

    public double getTongTienTamTinh() { return tongTienTamTinh; }
    public void setTongTienTamTinh(double tongTienTamTinh) { this.tongTienTamTinh = tongTienTamTinh; }

    public TrangThaiDatPhong getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiDatPhong trangThai) { this.trangThai = trangThai; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }

    public List<ChiTietDatPhong> getDsChiTiet() { return dsChiTiet; }
    public void setDsChiTiet(List<ChiTietDatPhong> dsChiTiet) { this.dsChiTiet = dsChiTiet; }

    public List<SuDungDichVu> getDsDichVu() { return dsDichVu; }
    public void setDsDichVu(List<SuDungDichVu> dsDichVu) { this.dsDichVu = dsDichVu; }

    // Compatibility aliases for Legacy UI
    public LocalDateTime getNgayNhan() { return ngayNhanDuKien; }
    public void setNgayNhan(LocalDateTime ngayNhan) { this.ngayNhanDuKien = ngayNhan; }
    public LocalDateTime getNgayTra() { return ngayTraDuKien; }
    public void setNgayTra(LocalDateTime ngayTra) { this.ngayTraDuKien = ngayTra; }
    public int getSoKhach() { return soLuongKhach; }
    public void setSoKhach(int soKhach) { this.soLuongKhach = soKhach; }
    
    public LocalDateTime getNgayNhanDK() { return ngayNhanDuKien; }
    public void setNgayNhanDK(LocalDateTime d) { this.ngayNhanDuKien = d; }
    public LocalDateTime getNgayTraDK() { return ngayTraDuKien; }
    public void setNgayTraDK(LocalDateTime d) { this.ngayTraDuKien = d; }

    public Date getNgayNhanDK_Date() { return ngayNhanDuKien != null ? Date.from(ngayNhanDuKien.atZone(ZoneId.systemDefault()).toInstant()) : null; }
    public Date getNgayTraDK_Date() { return ngayTraDuKien != null ? Date.from(ngayTraDuKien.atZone(ZoneId.systemDefault()).toInstant()) : null; }

    public void setNgayNhanDK(Date d) { if (d != null) this.ngayNhanDuKien = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); }
    public void setNgayTraDK(Date d) { if (d != null) this.ngayTraDuKien = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(); }

    // Legacy String setters/getters
    public void setTrangThai(String s) {
        if (s == null) return;
        try {
            this.trangThai = TrangThaiDatPhong.valueOf(s.toUpperCase());
        } catch (Exception e) {
            if (s.contains("xác nhận")) this.trangThai = TrangThaiDatPhong.CONFIRMED;
            else if (s.contains("Hủy")) this.trangThai = TrangThaiDatPhong.CANCELLED;
        }
    }
    
    public String getSoPhong() { return (dsChiTiet != null && !dsChiTiet.isEmpty()) ? dsChiTiet.get(0).getPhong().getMaPhong() : "—"; }
    public void setSoPhong(String p) { /* Ignored, set via dsChiTiet */ }
    public void setMaKH(String maKH) { /* Ignored, set via khachHang */ }
    public String getMaKH() { return khachHang != null ? khachHang.getMaKhachHang() : null; }
    public String getTenKhachHang() { return khachHang != null ? khachHang.getHoTen() : "—"; }
    public String getTenLoaiPhong() { return (dsChiTiet != null && !dsChiTiet.isEmpty()) ? dsChiTiet.get(0).getTenLoaiPhong() : ""; }
    public void setMaNV(String maNV) {}

    public void xacNhan() {
        this.trangThai = TrangThaiDatPhong.CONFIRMED;
    }

    public void huy(String lyDo) {
        this.trangThai = TrangThaiDatPhong.CANCELLED;
        this.ghiChu = (this.ghiChu != null ? this.ghiChu + " | " : "") + "Hủy: " + lyDo;
    }

    public double tinhTienTamTinh() {
        double total = 0;
        for (ChiTietDatPhong ct : dsChiTiet) {
            total += ct.getGiaThucTeChot();
        }
        this.tongTienTamTinh = total;
        return total;
    }
}
