package entity;

public class ChiTietBangGia {
    private int maChiTiet;
    private double giaNgay;
    private double giaGioDau;
    private double giaGioTiepTheo;
    private double phuPhiTraTre;

    private BangGia bangGia;
    private LoaiPhong loaiPhong;

    public ChiTietBangGia() {}

    public ChiTietBangGia(double giaNgay, double giaGioDau, double giaGioTiepTheo, BangGia bangGia, LoaiPhong loaiPhong) {
        this.giaNgay = giaNgay;
        this.giaGioDau = giaGioDau;
        this.giaGioTiepTheo = giaGioTiepTheo;
        this.bangGia = bangGia;
        this.loaiPhong = loaiPhong;
    }

    // Getters and Setters
    public int getMaChiTiet() { return maChiTiet; }
    public void setMaChiTiet(int maChiTiet) { this.maChiTiet = maChiTiet; }

    public double getGiaNgay() { return giaNgay; }
    public void setGiaNgay(double giaNgay) { this.giaNgay = giaNgay; }

    public double getGiaGioDau() { return giaGioDau; }
    public void setGiaGioDau(double giaGioDau) { this.giaGioDau = giaGioDau; }

    public double getGiaGioTiepTheo() { return giaGioTiepTheo; }
    public void setGiaGioTiepTheo(double giaGioTiepTheo) { this.giaGioTiepTheo = giaGioTiepTheo; }

    public double getPhuPhiTraTre() { return phuPhiTraTre; }
    public void setPhuPhiTraTre(double phuPhiTraTre) { this.phuPhiTraTre = phuPhiTraTre; }

    public BangGia getBangGia() { return bangGia; }
    public void setBangGia(BangGia bangGia) { this.bangGia = bangGia; }

    public LoaiPhong getLoaiPhong() { return loaiPhong; }
    public void setLoaiPhong(LoaiPhong loaiPhong) { this.loaiPhong = loaiPhong; }
}
