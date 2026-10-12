package database;

import entity.*;
import entity.enums.*;
import java.time.LocalDateTime;
import java.util.*;

public class DemoDataStore {
    private static final DemoDataStore instance = new DemoDataStore();

    private final List<Phong> rooms = new ArrayList<>();
    private final List<KhachHang> customers = new ArrayList<>();
    private final List<DatPhong> bookings = new ArrayList<>();
    private final List<LoaiPhong> roomTypes = new ArrayList<>();
    private final List<HuongNhin> views = new ArrayList<>();
    private final List<DichVu> services = new ArrayList<>();
    private final List<BangGia> priceLists = new ArrayList<>();
    private final List<TaiKhoan> accounts = new ArrayList<>();
    private final List<NhanVien> staffList = new ArrayList<>();
    private final List<KhuyenMai> promotions = new ArrayList<>();

    private DemoDataStore() {
        initData();
    }

    public static DemoDataStore getInstance() {
        return instance;
    }

    private void initData() {
        // 1. Staff & Accounts
        NhanVien nvAdmin = new NhanVien("NV001", "Akemi Homura", "0901234567", "Quản trị viên");
        nvAdmin.setEmail("homura@homurahotel.com");
        NhanVien nvManager = new NhanVien("NV002", "Kaname Madoka", "0912345678", "Quản lý khách sạn");
        nvManager.setEmail("madoka@homurahotel.com");
        NhanVien nvRecept = new NhanVien("NV003", "Miki Sayaka", "0923456789", "Lễ tân");
        nvRecept.setEmail("sayaka@homurahotel.com");
        staffList.addAll(Arrays.asList(nvAdmin, nvManager, nvRecept));

        TaiKhoan tkAdmin = new TaiKhoan();
        tkAdmin.setTenDangNhap("admin");
        tkAdmin.setMatKhau("admin");
        tkAdmin.setVaiTro(VaiTro.ADMIN);
        tkAdmin.setTrangThai(true);
        tkAdmin.setNhanVien(nvAdmin);
        nvAdmin.setTaiKhoan(tkAdmin);

        TaiKhoan tkManager = new TaiKhoan();
        tkManager.setTenDangNhap("manager");
        tkManager.setMatKhau("123");
        tkManager.setVaiTro(VaiTro.MANAGER);
        tkManager.setTrangThai(true);
        tkManager.setNhanVien(nvManager);
        nvManager.setTaiKhoan(tkManager);

        TaiKhoan tkRecept = new TaiKhoan();
        tkRecept.setTenDangNhap("recept1");
        tkRecept.setMatKhau("123");
        tkRecept.setVaiTro(VaiTro.RECEPTIONIST);
        tkRecept.setTrangThai(true);
        tkRecept.setNhanVien(nvRecept);
        nvRecept.setTaiKhoan(tkRecept);

        accounts.addAll(Arrays.asList(tkAdmin, tkManager, tkRecept));

        // 2. Views
        HuongNhin hnPn = new HuongNhin("HN01", "Hướng phố", 1.0);
        hnPn.setMoTa("Nhìn ra phố phường sầm uất");
        HuongNhin hnBi = new HuongNhin("HN03", "Hướng biển", 1.3);
        hnBi.setMoTa("Nhìn ra bãi biển và đại dương");
        HuongNhin hnPa = new HuongNhin("HN06", "VIP Panorama", 1.5);
        hnPa.setMoTa("Tầm nhìn toàn cảnh 180 độ");
        views.addAll(Arrays.asList(hnPn, hnBi, hnPa));

        // 3. Room Types
        LoaiPhong lpSingle = new LoaiPhong("LP001", "Standard Single", 1, "Phòng tiêu chuẩn 1 người");
        lpSingle.setGiaTheoNgay(400000.0);
        LoaiPhong lpDouble = new LoaiPhong("LP002", "Standard Double", 2, "Phòng tiêu chuẩn 2 người");
        lpDouble.setGiaTheoNgay(650000.0);
        LoaiPhong lpDeluxe = new LoaiPhong("LP003", "Deluxe Ocean", 2, "Phòng cao cấp hướng biển");
        lpDeluxe.setGiaTheoNgay(950000.0);
        LoaiPhong lpSuite = new LoaiPhong("LP004", "Presidential Suite", 4, "Phòng tổng thống hoàng gia");
        lpSuite.setGiaTheoNgay(2200000.0);
        roomTypes.addAll(Arrays.asList(lpSingle, lpDouble, lpDeluxe, lpSuite));

        // 4. Customers
        KhachHang kh1 = new KhachHang("KH001", "Tomoe Mami", "0988776655", "012345678901");
        kh1.setEmail("mami@homurahotel.com");
        kh1.setHangKhachHang("VIP");

        KhachHang kh2 = new KhachHang("KH002", "Sakura Kyouko", "0977665544", "012345678902");
        kh2.setEmail("kyouko@homurahotel.com");
        kh2.setHangKhachHang("THUONG");

        KhachHang kh3 = new KhachHang("KH003", "Nagisa Momoe", "0966554433", "012345678903");
        kh3.setEmail("nagisa@homurahotel.com");
        kh3.setHangKhachHang("THUONG");

        customers.addAll(Arrays.asList(kh1, kh2, kh3));

        // 5. Rooms
        createRoom("P101", 1, TrangThaiPhong.AVAILABLE, lpSingle, hnPn, null, null);
        createRoom("P102", 1, TrangThaiPhong.OCCUPIED, lpDouble, hnPn, "Tomoe Mami", "0988776655");
        createRoom("P103", 1, TrangThaiPhong.CLEANING, lpSingle, hnPn, null, null);
        createRoom("P104", 1, TrangThaiPhong.AVAILABLE, lpDouble, hnPn, null, null);

        createRoom("P201", 2, TrangThaiPhong.AVAILABLE, lpDouble, hnBi, null, null);
        createRoom("P202", 2, TrangThaiPhong.OCCUPIED, lpDeluxe, hnBi, "Sakura Kyouko", "0977665544");
        createRoom("P203", 2, TrangThaiPhong.MAINTENANCE, lpDeluxe, hnBi, null, null);
        createRoom("P204", 2, TrangThaiPhong.AVAILABLE, lpDouble, hnBi, null, null);

        createRoom("P301", 3, TrangThaiPhong.AVAILABLE, lpDeluxe, hnPa, null, null);
        createRoom("P302", 3, TrangThaiPhong.OCCUPIED, lpSuite, hnPa, "Nagisa Momoe", "0966554433");
        createRoom("P303", 3, TrangThaiPhong.AVAILABLE, lpSuite, hnPa, null, null);
        createRoom("P304", 3, TrangThaiPhong.CLEANING, lpDeluxe, hnPa, null, null);

        // 6. Services
        createService("DV001", "Nước khoáng Lavie", 15000.0, "Chai", "Đồ uống minibar");
        createService("DV002", "Coca Cola", 20000.0, "Lon", "Đồ uống minibar");
        createService("DV003", "Bữa sáng Buffet", 120000.0, "Suất", "Ẩm thực nhà hàng Homura");
        createService("DV004", "Giặt ủi cao cấp", 50000.0, "Kg", "Dịch vụ giặt sấy trong ngày");
        createService("DV005", "Dịch vụ Spa & Massage", 350000.0, "Lần", "Liệu trình thư giãn 60 phút");

        // 7. Promotions
        KhuyenMai km1 = new KhuyenMai();
        km1.setMaKhuyenMai("KM001");
        km1.setTenKhuyenMai("Ưu đãi Homura");
        km1.setLoaiGiam(LoaiGiam.PERCENT);
        km1.setGiaTriGiam(20.0);
        km1.setSoLuong(100);
        km1.setTrangThai(true);
        promotions.add(km1);

        // 8. Price lists
        BangGia bg = new BangGia();
        bg.setMaBangGia("BG001");
        bg.setTenBangGia("Bảng giá tiêu chuẩn 2026");
        bg.setMoTa("Áp dụng cho mọi kênh đặt phòng");
        bg.setTrangThai(true);
        priceLists.add(bg);

        // 9. Bookings
        DatPhong dp1 = new DatPhong();
        dp1.setMaDatPhong("DP20260501001");
        dp1.setKhachHang(kh1);
        dp1.setNhanVien(nvRecept);
        dp1.setNgayDat(LocalDateTime.now().minusDays(2));
        dp1.setNgayNhan(LocalDateTime.now().minusHours(4));
        dp1.setNgayTra(LocalDateTime.now().plusDays(2));
        dp1.setTrangThai(TrangThaiDatPhong.CHECKED_IN);
        dp1.setTienDatCoc(500000.0);
        dp1.setTongTienTamTinh(1300000.0);
        bookings.add(dp1);
    }

    private void createRoom(String id, int floor, TrangThaiPhong status, LoaiPhong lp, HuongNhin hn, String guest, String phone) {
        Phong p = new Phong(id, "Tầng " + floor, status);
        p.setTang(floor);
        p.setLoaiPhong(lp);
        p.setHuongNhin(hn);
        if (guest != null) {
            p.setTenKhachHienTai(guest);
            p.setSdtKhachHienTai(phone);
            p.setCheckInTime(LocalDateTime.now().minusHours(5));
            p.setExpectedCheckOutTime(LocalDateTime.now().plusDays(2));
        }
        rooms.add(p);
    }

    private void createService(String id, String name, double price, String unit, String desc) {
        DichVu dv = new DichVu();
        dv.setMaDichVu(id);
        dv.setTenDichVu(name);
        dv.setDonGia(price);
        dv.setDonViTinh(unit);
        dv.setMoTa(desc);
        dv.setTrangThai(true);
        services.add(dv);
    }

    public List<Phong> getRooms() {
        return new ArrayList<>(rooms);
    }

    public List<KhachHang> getCustomers() {
        return new ArrayList<>(customers);
    }

    public List<DatPhong> getBookings() {
        return new ArrayList<>(bookings);
    }

    public List<LoaiPhong> getRoomTypes() {
        return new ArrayList<>(roomTypes);
    }

    public List<HuongNhin> getViews() {
        return new ArrayList<>(views);
    }

    public List<DichVu> getServices() {
        return new ArrayList<>(services);
    }

    public List<BangGia> getPriceLists() {
        return new ArrayList<>(priceLists);
    }

    public List<TaiKhoan> getAccounts() {
        return new ArrayList<>(accounts);
    }

    public List<NhanVien> getStaffList() {
        return new ArrayList<>(staffList);
    }

    public List<KhuyenMai> getPromotions() {
        return new ArrayList<>(promotions);
    }

    public Phong findRoomById(String id) {
        for (Phong p : rooms) {
            if (p.getMaPhong().equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    public synchronized void updateRoomStatus(String roomId, TrangThaiPhong status) {
        Phong p = findRoomById(roomId);
        if (p != null) {
            p.setTrangThai(status);
        }
    }
}
