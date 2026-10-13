package database;

import entity.*;
import entity.enums.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

/** Isolated, transactional demo storage. Reads are detached just like JDBC rows. */
public final class DemoRepository {
    private static Map<Class<?>, List<Object>> tables = new HashMap<>();
    private static byte[] snapshot;
    static { reset(); }
    private DemoRepository() {}

    public static void reset() {
        DatabaseConnection.getInstance().lockOperation();
        try {
        tables.clear();
        DemoDataStore s = DemoDataStore.getInstance();
        seed(Phong.class, s.getRooms()); seed(KhachHang.class, s.getCustomers());
        seed(DatPhong.class, s.getBookings()); seed(LoaiPhong.class, s.getRoomTypes());
        seed(HuongNhin.class, s.getViews()); seed(DichVu.class, s.getServices());
        seed(BangGia.class, s.getPriceLists()); seed(TaiKhoan.class, s.getAccounts());
        seed(NhanVien.class, s.getStaffList()); seed(KhuyenMai.class, s.getPromotions());
        KenhDatPhong direct = new KenhDatPhong("DIRECT", "Trực tiếp", "DIRECT");
        direct.setTrangThai(true); insert(KenhDatPhong.class, direct);
        List<Phong> occupied = all(Phong.class).stream().filter(p -> p.getTrangThai() == TrangThaiPhong.OCCUPIED).toList();
        for (int i = 0; i < occupied.size(); i++) {
            Phong room = occupied.get(i);
            DatPhong dp = new DatPhong();
            dp.setMaDatPhong("DEMO" + (i + 1)); dp.setKhachHang(all(KhachHang.class).get(i));
            dp.setNhanVien(get(NhanVien.class, "NV003")); dp.setSoLuongKhach(1);
            dp.setNgayDat(LocalDateTime.now().minusDays(2));
            dp.setNgayNhanDuKien(LocalDateTime.now().minusDays(1).withHour(14).withMinute(0));
            dp.setNgayTraDuKien(LocalDateTime.now().plusDays(2).withHour(12).withMinute(0));
            dp.setTrangThai(TrangThaiDatPhong.CHECKED_IN); insert(DatPhong.class, dp);
            ChiTietDatPhong ct = new ChiTietDatPhong(); ct.setMaChiTiet("DEMOCT" + (i + 1));
            ct.setDatPhong(dp); ct.setPhong(room); ct.setKhachHang(dp.getKhachHang());
            ct.setNgayNhanDuKien(dp.getNgayNhanDuKien()); ct.setNgayTraDuKien(dp.getNgayTraDuKien());
            ct.setNgayNhanThucTe(dp.getNgayNhanDuKien()); ct.setGiaThucTeChot(room.getGiaTheoNgay());
            insert(ChiTietDatPhong.class, ct);
        }
        // Replace the old incomplete booking sample with linked stays.
        remove(DatPhong.class, d -> "DP20260501001".equals(d.getMaDatPhong()));
        snapshot = null;

        } finally { DatabaseConnection.getInstance().unlockOperation(); }
    }

    private static <T> void seed(Class<T> type, List<T> rows) {
        tables.put(type, new ArrayList<>(copy(rows)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T copy(T value) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(value); }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (T) in.readObject();
            }
        } catch (IOException | ClassNotFoundException e) { throw new IllegalStateException("Cannot copy demo data", e); }
    }

    public static <T> List<T> all(Class<T> type) {
        DatabaseConnection.getInstance().lockOperation();
        try {
        List<T> result = new ArrayList<>();
        for (Object row : tables.getOrDefault(type, List.of())) result.add(type.cast(row));
        return copy(result);

        } finally { DatabaseConnection.getInstance().unlockOperation(); }
    }

    public static <T> T get(Class<T> type, String id) {
        return all(type).stream().filter(row -> Objects.equals(key(row), id)).findFirst().orElse(null);
    }
    public static <T> List<T> find(Class<T> type, Predicate<T> predicate) {
        return new ArrayList<>(all(type).stream().filter(predicate).toList());
    }
    public static <T> boolean insert(Class<T> type, T value) {
        DatabaseConnection.getInstance().lockOperation();
        try {
        if (value == null || key(value) == null || get(type, key(value)) != null || !valid(value)) return false;
        tables.computeIfAbsent(type, ignored -> new ArrayList<>()).add(copy(value)); return true;

        } finally { DatabaseConnection.getInstance().unlockOperation(); }
    }
    public static <T> boolean update(Class<T> type, T value) {
        DatabaseConnection.getInstance().lockOperation();
        try {
        if (value == null || get(type, key(value)) == null) return false;
        List<Object> rows = tables.get(type);
        rows.removeIf(row -> Objects.equals(key(row), key(value))); rows.add(copy(value)); return true;

        } finally { DatabaseConnection.getInstance().unlockOperation(); }
    }
    public static <T> boolean remove(Class<T> type, Predicate<T> predicate) {
        DatabaseConnection.getInstance().lockOperation();
        try {
        tables.computeIfAbsent(type, ignored -> new ArrayList<>()).removeIf(row -> predicate.test(type.cast(row)));
        return true;

        } finally { DatabaseConnection.getInstance().unlockOperation(); }
    }
    public static boolean insertAccount(TaiKhoan account) {
        TaiKhoan stored=copy(account);stored.setMatKhau(util.PasswordUtils.hashPassword(stored.getMatKhau()));
        return insert(TaiKhoan.class,stored);
    }
    public static String nextId(String prefix) { return util.IdGenerator.next(prefix); }
    public static boolean matches(String value, String query) {
        return query == null || query.isBlank() || (value != null && value.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)));
    }
    public static void begin() {
        DatabaseConnection.getInstance().lockOperation();
        try {
        if (snapshot != null) throw new IllegalStateException("Nested demo transaction");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) { out.writeObject(tables); }
            snapshot = bytes.toByteArray();
        } catch (IOException e) { throw new IllegalStateException(e); }

        } finally { DatabaseConnection.getInstance().unlockOperation(); }
    }
    public static void commit() {
        DatabaseConnection.getInstance().lockOperation();
        try { snapshot = null;
        } finally { DatabaseConnection.getInstance().unlockOperation(); }
    }
    @SuppressWarnings("unchecked")
    public static void rollback() {
        DatabaseConnection.getInstance().lockOperation();
        try {
        if (snapshot == null) return;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(snapshot))) {
            tables = (Map<Class<?>, List<Object>>) in.readObject(); snapshot = null;
        } catch (IOException | ClassNotFoundException e) { throw new IllegalStateException(e); }

        } finally { DatabaseConnection.getInstance().unlockOperation(); }
    }
    public static DatPhong booking(String id, boolean details) {
        DatPhong dp = get(DatPhong.class, id);
        if (dp != null && details) dp.setDsChiTiet(stays(id));
        return dp;
    }
    public static ChiTietDatPhong stay(String id) {
        ChiTietDatPhong ct = get(ChiTietDatPhong.class, id);
        if (ct != null) {
            ct.setDatPhong(booking(ct.getMaDatPhong(), false));
            ct.setPhong(get(Phong.class, ct.getPhong().getMaPhong()));
        }
        return ct;
    }
    public static List<ChiTietDatPhong> stays(String bookingId) {
        return find(ChiTietDatPhong.class, c -> Objects.equals(c.getMaDatPhong(), bookingId))
                .stream().map(c -> stay(c.getMaChiTiet())).collect(java.util.stream.Collectors.toList());
    }
    public static ChiTietDatPhong conflict(String room, LocalDateTime start, LocalDateTime end, String exclude) {
        if (start == null || end == null || !end.isAfter(start)) throw new IllegalArgumentException("Invalid stay interval");
        for (ChiTietDatPhong raw : all(ChiTietDatPhong.class)) {
            ChiTietDatPhong ct = stay(raw.getMaChiTiet()); DatPhong dp = ct.getDatPhong();
            if (dp == null || ct.getPhong() == null || !room.equals(ct.getPhong().getMaPhong())
                    || Objects.equals(dp.getMaDatPhong(), exclude) || ct.getNgayTraThucTe() != null) continue;
            if (!EnumSet.of(TrangThaiDatPhong.PENDING, TrangThaiDatPhong.CONFIRMED,
                    TrangThaiDatPhong.CHECKED_IN, TrangThaiDatPhong.PARTIALLY_CHECKED_IN).contains(dp.getTrangThai())) continue;
            LocalDateTime from = ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : dp.getNgayNhanDuKien();
            LocalDateTime to = ct.getNgayTraDuKien() != null ? ct.getNgayTraDuKien() : dp.getNgayTraDuKien();
            if (from != null && to != null && from.isBefore(end) && to.isAfter(start)) return ct;
        }
        return null;
    }
    private static boolean valid(Object value) {
        if (value instanceof ChiTietDatPhong c) return c.getDatPhong()!=null && get(DatPhong.class,c.getMaDatPhong())!=null && c.getPhong()!=null && get(Phong.class,c.getPhong().getMaPhong())!=null;
        if (value instanceof SuDungDichVu s) return s.getCtdp()!=null && get(ChiTietDatPhong.class,s.getCtdp().getMaChiTiet())!=null && s.getDichVu()!=null && get(DichVu.class,s.getDichVu().getMaDichVu())!=null && s.getSoLuong()>0 && Double.isFinite(s.getDonGiaLuu()) && s.getDonGiaLuu()>=0;
        if (value instanceof ChiTietHoaDon c) return c.getHoaDon()!=null && get(HoaDon.class,c.getHoaDon().getMaHoaDon())!=null;
        return true;
    }
    private static String key(Object value) {
        if (value instanceof Phong x) return x.getMaPhong();
        if (value instanceof KhachHang x) return x.getMaKhachHang();
        if (value instanceof DatPhong x) return x.getMaDatPhong();
        if (value instanceof ChiTietDatPhong x) return x.getMaChiTiet();
        if (value instanceof LoaiPhong x) return x.getMaLoaiPhong();
        if (value instanceof HuongNhin x) return x.getMaHuongNhin();
        if (value instanceof DichVu x) return x.getMaDichVu();
        if (value instanceof BangGia x) return x.getMaBangGia();
        if (value instanceof TaiKhoan x) return x.getTenDangNhap();
        if (value instanceof NhanVien x) return x.getMaNhanVien();
        if (value instanceof KhuyenMai x) return x.getMaKhuyenMai();
        if (value instanceof HoaDon x) return x.getMaHoaDon();
        if (value instanceof ChiTietHoaDon x) return x.getMaChiTietHoaDon();
        if (value instanceof SuDungDichVu x) return x.getMaSuDung();
        if (value instanceof KenhDatPhong x) return x.getMaKenh();
        if (value instanceof GiaoCa x) return x.getMaGiaoCa();
        if (value instanceof ChiTietBangGia x) return x.getBangGia().getMaBangGia() + ":" + x.getLoaiPhong().getMaLoaiPhong();
        if (value instanceof TienNghi x) return x.getMaTienNghi();
        if (value instanceof LichSuTrangThaiPhong x) return x.getMaLichSu();
        if (value instanceof ChiPhi x) return String.valueOf(x.getMaChiPhi());
        if (value instanceof NhatKyHeThong x) return String.valueOf(x.getMaLog());
        throw new IllegalArgumentException("Unsupported demo entity: " + value.getClass());
    }
}
