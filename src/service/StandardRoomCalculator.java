package service;

import entity.BangGia;
import entity.ChiTietDatPhong;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tính tiền phòng theo mô hình PMS chuẩn:
 * - Tiền phòng = giaThucTeChot (đã chốt khi check-in) × số đêm
 * - Phụ phí = nhận sớm (trước 14:00) + trả trễ (sau 12:00)
 */
public class StandardRoomCalculator implements ITinhTienPhong {

    public static long countNights(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) throw new IllegalArgumentException("Khoảng lưu trú không hợp lệ");
        return Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()));
    }

    @Override
    public double tinhTienPhong(ChiTietDatPhong ctdp, List<BangGia> dsBangGia) {
        if (ctdp == null || ctdp.getPhong() == null) return 0;
        BangGiaService bangGiaService = new BangGiaService();
        double policyPrice = bangGiaService.layGiaHienHanh(ctdp.getPhong().getLoaiPhong() != null ? ctdp.getPhong().getLoaiPhong().getMaLoaiPhong() : "");

        // Ưu tiên giá từ bảng giá hiện hành (nếu có chính sách mới)
        double donGia = ctdp.getGiaThucTeChot() > 0 ? ctdp.getGiaThucTeChot() : policyPrice;
        if (donGia <= 0 && ctdp.getPhong() != null) {
            donGia = ctdp.getPhong().getGiaTheoNgay();
        }

        LocalDateTime checkOut = ctdp.getNgayTraThucTe();
        if (checkOut == null && ctdp.getNgayTraDuKien() != null) {
            checkOut = ctdp.getNgayTraDuKien();
        }
        if (checkOut == null && ctdp.getDatPhong() != null) {
            checkOut = ctdp.getDatPhong().getNgayTraDuKien();
        }
        if (checkOut == null) checkOut = LocalDateTime.now();

        LocalDateTime checkIn = ctdp.getNgayNhanThucTe();
        if (checkIn == null && ctdp.getNgayNhanDuKien() != null) {
            checkIn = ctdp.getNgayNhanDuKien();
        }
        if (checkIn == null && ctdp.getDatPhong() != null) {
            checkIn = ctdp.getDatPhong().getNgayNhanDuKien();
        }
        if (checkIn == null) checkIn = checkOut.minusDays(1); // Fallback safe

        // Tính số ngày dựa trên giờ thực tế để tránh overcharge khi đổi phòng cùng ngày
        long soNgay = countNights(checkIn, checkOut); // Round up: tối thiểu 1 đêm

        BigDecimal basePrice = BigDecimal.valueOf(donGia);
        BigDecimal days = BigDecimal.valueOf(soNgay);

        // Fix: Use BigDecimal internally to prevent floating point inaccuracies
        return basePrice.multiply(days)
                .setScale(0, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public double tinhPhuPhi(ChiTietDatPhong ctdp, LocalDateTime gioNhan, LocalDateTime gioTra) {
        if (gioNhan == null || gioTra == null || ctdp == null || ctdp.getPhong() == null) return 0;

        BangGiaService bangGiaService = new BangGiaService();
        String maLoaiPhong = ctdp.getPhong().getLoaiPhong() != null ? ctdp.getPhong().getLoaiPhong().getMaLoaiPhong() : "";
        double donGia = ctdp.getGiaThucTeChot() > 0 ? ctdp.getGiaThucTeChot() : bangGiaService.layGiaHienHanh(maLoaiPhong);
        if (donGia <= 0) donGia = ctdp.getGiaThucTeChot() > 0 ? ctdp.getGiaThucTeChot() : ctdp.getPhong().getGiaTheoNgay();

        BigDecimal phuPhi = BigDecimal.ZERO;
        BigDecimal bdDonGia = BigDecimal.valueOf(donGia);

        // --- PHỤ PHÍ NHẬN PHÒNG SỚM (EARLY CHECK-IN) ---
        // Chính sách: <05h (100%), <09h (50%), <12h (30%)
        if (gioNhan.getHour() < 14) {
            if (gioNhan.getHour() < 5) {
                phuPhi = phuPhi.add(bdDonGia); // 100%
            } else if (gioNhan.getHour() < 9) {
                phuPhi = phuPhi.add(bdDonGia.multiply(BigDecimal.valueOf(0.5))); // 50%
            } else if (gioNhan.getHour() < 12) {
                phuPhi = phuPhi.add(bdDonGia.multiply(BigDecimal.valueOf(0.3))); // 30%
            }
        }

        // --- PHỤ PHÍ TRẢ PHÒNG TRỄ (LATE CHECK-OUT) ---
        // Chính sách: >13h (30%), >15h (50%), >18h (100%)
        // Chỉ áp dụng khi ngày trả sau ngày nhận phòng (ít nhất ở qua đêm)
        if (gioTra.toLocalDate().isAfter(gioNhan.toLocalDate()) && gioTra.getHour() > 12) {
            if (gioTra.getHour() >= 18) {
                phuPhi = phuPhi.add(bdDonGia); // 100%
            } else if (gioTra.getHour() >= 15) {
                phuPhi = phuPhi.add(bdDonGia.multiply(BigDecimal.valueOf(0.5))); // 50%
            } else if (gioTra.getHour() >= 13) {
                phuPhi = phuPhi.add(bdDonGia.multiply(BigDecimal.valueOf(0.3))); // 30%
            }
        }

        return phuPhi.setScale(0, RoundingMode.HALF_UP).doubleValue();
    }
}
