package entity;

public class KhachHang {
    private String maKhachHang;
    private String hoTen;
    private String sdt;
    private String cccd;

    public KhachHang() {}

    public KhachHang(String maKhachHang, String hoTen, String sdt, String cccd) {
        this.maKhachHang = maKhachHang;
        this.hoTen = hoTen;
        this.sdt = sdt;
        this.cccd = cccd;
    }

    public KhachHang timKiem(String keyword) {
        // Search logic based on keyword (hoTen, sdt, or cccd)
        if (this.hoTen.contains(keyword) || this.sdt.contains(keyword) || this.cccd.contains(keyword)) {
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

    public String getMaKH() { return maKhachHang; }
    public void setMaKH(String maKH) { this.maKhachHang = maKH; }
    public String getSoDienThoai() { return sdt; }
    public void setSoDienThoai(String sdt) { this.sdt = sdt; }
    public String getEmail() { return ""; }
    public void setEmail(String email) {}
    public void setTrangThai(String status) {}

    @Override
    public String toString() {
        return hoTen + " (" + maKhachHang + ")";
    }
}
