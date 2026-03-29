package entity;

import entity.TaiKhoan;

public class NhanVien {
    private String maNhanVien;
    private String hoTen;
    private String sdt;
    private String chucVu;

    private TaiKhoan taiKhoan;

    public NhanVien() {}

    public NhanVien(String maNhanVien, String hoTen, String sdt, String chucVu) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.chucVu = chucVu;
    }

    // Getters and Setters
    public String getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(String maNhanVien) { this.maNhanVien = maNhanVien; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }

    public String getChucVu() { return chucVu; }
    public void setChucVu(String chucVu) { this.chucVu = chucVu; }

    public TaiKhoan getTaiKhoan() { return taiKhoan; }
    public void setTaiKhoan(TaiKhoan taiKhoan) { this.taiKhoan = taiKhoan; }

    public String getSoDienThoai() { return sdt; }

    @Override
    public String toString() {
        return hoTen + " (" + maNhanVien + ")";
    }
}
