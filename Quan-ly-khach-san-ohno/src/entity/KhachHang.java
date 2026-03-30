package entity;

public class KhachHang {
    private String maKhachHang;
    private String hoTen;
    private String sdt;
    private String cccd;
    private java.time.LocalDate ngaySinh;
    private String gioiTinh;
    private String quocTich = "Việt Nam";

    public KhachHang() {}

    public KhachHang(String maKhachHang, String hoTen, String sdt, String cccd) {
        this.maKhachHang = maKhachHang;
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.cccd = cccd;
    }

    public KhachHang(String maKhachHang, String hoTen, String sdt, String cccd, java.time.LocalDate ngaySinh, String gioiTinh, String quocTich) {
        this.maKhachHang = maKhachHang;
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.cccd = cccd;
        this.ngaySinh = ngaySinh;
        this.gioiTinh = gioiTinh;
        this.quocTich = quocTich;
    }

    public java.time.LocalDate getNgaySinh() { return ngaySinh; }
    public void setNgaySinh(java.time.LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }

    public String getGioiTinh() { return gioiTinh; }
    public void setGioiTinh(String gioiTinh) { this.gioiTinh = gioiTinh; }

    public String getQuocTich() { return quocTich; }
    public void setQuocTich(String quocTich) { this.quocTich = quocTich; }

    public KhachHang timKiem(String keyword) {
        // Search logic based on keyword (hoTen, sdt, or cccd)
        if (this.hoTen.contains(keyword) || this.sdt.contains(keyword) || (this.cccd != null && this.cccd.contains(keyword))) {
            return this;
        }
        return null;
    }

    // Getters and Setters
    public String getMaKhachHang() { return maKhachHang; }
    public void setMaKhachHang(String maKhachHang) { this.maKhachHang = maKhachHang; }

    public String getHoTen() { return hoTen; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }

    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }

    private String email;
    private String trangThai = "Hoạt động";

    public String getMaKH() { return maKhachHang; }
    public void setMaKH(String maKH) { this.maKhachHang = maKH; }
    public String getSoDienThoai() { return sdt; }
    public void setSoDienThoai(String sdt) { this.sdt = sdt; }
    public String getEmail() { return email != null ? email : ""; }
    public void setEmail(String email) { this.email = email; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String status) { this.trangThai = status; }

    @Override
    public String toString() {
        return hoTen + " (" + maKhachHang + ")";
    }
}
