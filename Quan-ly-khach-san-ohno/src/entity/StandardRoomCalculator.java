package entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tính tiền phòng theo mô hình PMS chuẩn:
 * - Tiền phòng = giaThucTeChot (đã chốt khi check-in) × số đêm
 * - Phụ phí = nhận sớm (trước 14:00) + trả trễ (sau 12:00)
 */
public class StandardRoomCalculator implements ITinhTienPhong {

    @Override
    public double tinhTienPhong(ChiTietDatPhong ctdp, List<BangGia> dsBangGia) {
        if (ctdp == null || ctdp.getPhong() == null) return 0;
        
        // Giá đã chốt khi check-in
        double donGia = ctdp.getGiaThucTeChot();
        if (donGia <= 0 && ctdp.getPhong() != null) {
            donGia = ctdp.getPhong().getGiaTheoNgay();
        }
        
        long soNgay = Math.max(1, ctdp.tinhSoNgayO());
        
        return donGia * soNgay;
    }

    @Override
    public double tinhPhuPhi(LocalDateTime gioNhan, LocalDateTime gioTra) {
        if (gioNhan == null || gioTra == null) return 0;
        
        double phuPhi = 0;
        
        // Phụ phí nhận phòng sớm (trước 14:00)
        if (gioNhan.getHour() < 14) {
            if (gioNhan.getHour() < 6) {
                phuPhi += 100000; // Nhận quá sớm (trước 6h)
            } else if (gioNhan.getHour() < 12) {
                phuPhi += 50000;  // Nhận sớm (6h-12h)
            }
        }
        
        // Phụ phí trả phòng trễ (sau 12:00)
        if (gioTra.getHour() > 12) {
            if (gioTra.getHour() > 18) {
                phuPhi += 150000; // Trả rất trễ (sau 18h) - tính thêm 0.5 đêm
            } else if (gioTra.getHour() > 15) {
                phuPhi += 100000; // Trả trễ (15h-18h)
            } else {
                phuPhi += 50000;  // Trả hơi trễ (12h-15h)
            }
        }
        
        return phuPhi;
    }
}
