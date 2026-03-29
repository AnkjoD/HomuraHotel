package entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BangGia {
    private String maBangGia;
    private String tenBangGia;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private boolean trangThai;

    private List<ChiTietBangGia> dsChiTiet = new ArrayList<>();

    public BangGia() {}

    public boolean kiemTraHieuLuc() {
        LocalDateTime now = LocalDateTime.now();
        return (now.isAfter(ngayBatDau) || now.isEqual(ngayBatDau)) && 
               (now.isBefore(ngayKetThuc) || now.isEqual(ngayKetThuc)) && trangThai;
    }

    // Getters and Setters
    public String getMaBangGia() { return maBangGia; }
    public void setMaBangGia(String maBangGia) { this.maBangGia = maBangGia; }

    public String getTenBangGia() { return tenBangGia; }
    public void setTenBangGia(String tenBangGia) { this.tenBangGia = tenBangGia; }

    public LocalDateTime getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDateTime ngayBatDau) { this.ngayBatDau = ngayBatDau; }

    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

    public boolean isTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }

    public List<ChiTietBangGia> getDsChiTiet() { return dsChiTiet; }
    public void setDsChiTiet(List<ChiTietBangGia> dsChiTiet) { this.dsChiTiet = dsChiTiet; }

    // Compatibility aliases for legacy UI
    public double getGiaTheoGio() { return 0.0; }
    public double getGiaTheoNgay() { return 0.0; }
    public double getGiaTheoTuan() { return 0.0; }

    public String getTenLoaiPhong() { return "Tất cả"; }
    public String getTrangThai() { return trangThai ? "Đang áp dụng" : "Ngừng áp dụng"; }

    public double getGiaTu2Ngay() { return 0.0; }
    public double getPhuThu() { return 0.0; }
    public java.util.Date getApDungTu() { return ngayBatDau != null ? java.util.Date.from(ngayBatDau.atZone(java.time.ZoneId.systemDefault()).toInstant()) : null; }
    public java.util.Date getApDungDen() { return ngayKetThuc != null ? java.util.Date.from(ngayKetThuc.atZone(java.time.ZoneId.systemDefault()).toInstant()) : null; }
}
