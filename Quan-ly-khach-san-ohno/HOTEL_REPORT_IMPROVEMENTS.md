# 🏨 PHÂN TÍCH & CẢI TIẾN BÁO CÁO THỐNG KÊ QUẢN LÝ KHÁCH SẠN

**Ngày phân tích:** 21/05/2026  
**Hệ thống:** Hotel OHNO - Quản Lý Khách Sạn

---

## 📊 SO SÁNH VỚI HỆ THỐNG THỰC TẾ

### ❌ Hiện Tại (Thiếu Các KPI Chuyên Nghiệp)

```
✓ Doanh thu phòng
✓ Lượt đặt phòng
✓ Khách mới
✓ Doanh thu dịch vụ
✓ Phòng trống/đang thuê/đã đặt
✗ RevPAR (Revenue Per Available Room)
✗ Occupancy Rate (%)
✗ Average Daily Rate (ADR)
✗ Lợi nhuận ròng (Net Profit)
✗ Chi phí hoạt động
✗ Khách quay lại (Repeat Rate)
✗ Độ dài lưu trú trung bình (ALOS)
```

### ✅ Tiêu Chuẩn Ngành Khách Sạn Quốc Tế

#### **1. KPI CHÍNH (Top Metrics)**

| Chỉ Tiêu           | Công Thức                   | Ý Nghĩa                    | Tiêu Chuẩn Tốt  |
| ------------------ | --------------------------- | -------------------------- | --------------- |
| **RevPAR**         | ADR × Occupancy%            | Thu nhập thực từ mỗi phòng | > 70% ADR       |
| **Occupancy Rate** | Phòng thuê / Tổng phòng     | % phòng có khách           | 70-85%          |
| **ADR**            | Tổng doanh thu / Phòng thuê | Giá trung bình/phòng/đêm   | Theo loại phòng |
| **ALOS**           | Tổng đêm / Lượt khách       | Ngày trung bình lưu trú    | 2-4 đêm         |
| **Repeat Rate**    | Khách quay lại / Tổng       | % khách quen               | > 30%           |

#### **2. KPI TÀI CHÍNH**

| Chỉ Tiêu              | Công Thức                                          | Ý Nghĩa                      |
| --------------------- | -------------------------------------------------- | ---------------------------- |
| **TRevPAR**           | (Room Revenue + Service Revenue) / Available Rooms | Doanh thu toàn diện          |
| **ARPAR**             | (ADR - Variable Cost) × Occupancy                  | Lợi nhuận thực tế từng phòng |
| **Operating Cost %**  | Operating Cost / Total Revenue                     | Hiệu suất chi phí            |
| **Net Profit Margin** | Net Profit / Total Revenue                         | Lợi nhuận ròng               |

#### **3. KPI KHÁCH HÀNG**

| Chỉ Tiêu                  | Ý Nghĩa                    |
| ------------------------- | -------------------------- |
| **Customer Satisfaction** | Đánh giá & review từ khách |
| **VIP Customer %**        | Tỷ lệ khách cao giá trị    |
| **International Guest %** | Tỷ lệ khách nước ngoài     |
| **Corporate Booking %**   | % Khách doanh nghiệp       |
| **Direct Booking %**      | % Đặt trực tiếp vs OTA     |

#### **4. KPI DỊCH VỤ**

| Chỉ Tiêu                      | Ý Nghĩa                            |
| ----------------------------- | ---------------------------------- |
| **Service Revenue per Guest** | Doanh thu dịch vụ trung bình/khách |
| **Top Services**              | Dịch vụ bán chạy nhất              |
| **Service Attachment Rate**   | % khách sử dụng dịch vụ            |

---

## 🎯 ĐỀ XUẤT CẢI TIẾN CHI TIẾT

### **PHẦN 1: BỔ SUNG CÁC KPI CHÍNH**

#### **A. Tính toán RevPAR (Ưu tiên cao)**

```sql
-- Thêm vào ThongKeDAO.java
SELECT
    COUNT(DISTINCT maPhong) as tongPhong,
    COUNT(DISTINCT CASE WHEN trangThai='OCCUPIED' THEN maPhong END) as phongThue,
    SUM(giaThucTeChot) as doanhThuPhong,
    CAST(COUNT(DISTINCT CASE WHEN trangThai='OCCUPIED' THEN maPhong END) * 100
         / COUNT(DISTINCT maPhong) AS FLOAT) as occupancyRate,
    CAST(SUM(giaThucTeChot) / COUNT(DISTINCT maPhong) AS FLOAT) as revpar,
    CAST(SUM(giaThucTeChot) / NULLIF(COUNT(DISTINCT CASE WHEN trangThai='OCCUPIED' THEN maPhong END), 0) AS FLOAT) as adr
FROM ChiTietDatPhong
WHERE CAST(ngayNhanThucTe AS DATE) BETWEEN @startDate AND @endDate
```

#### **B. Tính toán Lợi Nhuận Ròng**

```sql
-- Thêm vào Database Schema
-- Tính toán: Doanh Thu - Chi Phí Hoạt Động
SELECT
    SUM(CASE WHEN loai='PHONG' THEN thanhTien ELSE 0 END) as roomRevenue,
    SUM(CASE WHEN loai='DICH_VU' THEN thanhTien ELSE 0 END) as serviceRevenue,
    (SELECT SUM(soTien) FROM ChiPhi WHERE maGiaoCa IN (...)) as operatingCost,
    SUM(...) - operatingCost as netProfit
FROM ChiTietHoaDon
```

#### **C. Thống Kê Khách Quay Lại**

```sql
-- Khách quay lại nhiều lần
SELECT
    COUNT(*) as totalGuests,
    COUNT(CASE WHEN soLanO > 1 THEN maKhachHang END) as repeatGuests,
    CAST(COUNT(CASE WHEN soLanO > 1 THEN maKhachHang END) * 100
         / COUNT(*) AS FLOAT) as repeatRate
FROM KhachHang
```

#### **D. ALOS (Average Length of Stay)**

```sql
SELECT
    COUNT(*) as totalBookings,
    SUM(DATEDIFF(day, ngayNhanThucTe, ngayTraThucTe)) as totalNights,
    CAST(SUM(DATEDIFF(day, ngayNhanThucTe, ngayTraThucTe))
         / NULLIF(COUNT(*), 0) AS FLOAT) as alos
FROM ChiTietDatPhong
WHERE ngayNhanThucTe IS NOT NULL AND ngayTraThucTe IS NOT NULL
```

---

### **PHẦN 2: CẢI TIẾN GIAO DIỆN BÁO CÁO**

#### **Cấu Trúc Báo Cáo Chuyên Nghiệp**

```
📋 BÁO CÁO QUẢN LÝ KHÁCH SẠN (Tháng 05/2026)
════════════════════════════════════════════════

┌─ I. HIỆU QUẢ KINH DOANH (Key Metrics)
│  ├─ RevPAR: 1,234,567 đ
│  ├─ Occupancy Rate: 78.5%
│  ├─ ADR: 1,567,890 đ
│  └─ ALOS: 3.2 đêm
│
├─ II. DOANH THU & LỢI NHUẬN
│  ├─ Doanh thu phòng: 250,000,000 đ
│  ├─ Doanh thu dịch vụ: 45,000,000 đ
│  ├─ Chi phí hoạt động: 120,000,000 đ
│  ├─ TRevPAR: 1,967,890 đ
│  └─ Lợi nhuận ròng: 175,000,000 đ (Net Margin: 35%)
│
├─ III. KHÁCH HÀNG
│  ├─ Tổng khách: 450 người
│  ├─ Khách quay lại: 135 người (30%)
│  ├─ Khách VIP: 28 người (6.2%)
│  ├─ Khách quốc tế: 87 người (19.3%)
│  ├─ Khách doanh nghiệp: 156 người (34.7%)
│  └─ Đặt trực tiếp: 45% | OTA: 55%
│
├─ IV. PHÒNG
│  ├─ Tổng phòng: 40
│  ├─ Phòng trống: 9 (22.5%)
│  ├─ Đang thuê: 31 (77.5%)
│  ├─ Vệ sinh: 2 (5%)
│  └─ Top Phòng: P501 (8,234,567 đ) | P403 | P301
│
├─ V. DỊCH VỤ
│  ├─ Tổng dịch vụ bán: 245 lượt
│  ├─ Revenue per Guest: 100,000 đ
│  └─ Top Services:
│      1. Nước suối: 89 lượt (36.3%)
│      2. Thuê xe máy: 67 lượt (27.3%)
│      3. Ăn sáng thêm: 45 lượt (18.4%)
│
├─ VI. KÊNH BOOKING
│  ├─ Direct: 202 (45%)
│  ├─ Booking.com: 134 (30%)
│  ├─ Agoda: 89 (20%)
│  ├─ Traveloka: 25 (5%)
│  └─ Hoahoong bán: 8,234,567 đ (cần đóng cho OTA)
│
└─ VII. NHẬN XÉT & DỰ BÁO
   ├─ Tăng trưởng: +15% so với tháng trước
   ├─ Occupancy trending: ↗ (tốt)
   ├─ Dự báo tháng sau: 290,000,000 đ (nếu xu hướng giữ)
   └─ Cần chú ý: Chi phí tăng 8%, cần kiểm soát
```

---

### **PHẦN 3: CHI TIẾT CÀI TIẾN TỪNG PHẦN**

#### **📊 1. Dashboard Tổng Quan - Thêm 4 KPI Quan Trọng**

**Thêm vào ThongKePanel.java buildKPIRow():**

```java
// Hiện tại: Doanh Thu, Lượt Đặt, Khách Mới
// Thêm mới:
addStatCard(wrapper, "RevPAR", String.format("%,.0fđ", revpar), "📈", UIConstants.SUCCESS);
addStatCard(wrapper, "Occupancy", String.format("%.1f%%", occupancy), "🏠", UIConstants.PRIMARY);
addStatCard(wrapper, "Lợi Nhuận", String.format("%,.0fđ", profit), "💰", new Color(0x8B5CF6));
addStatCard(wrapper, "Khách Quay Lại", String.format("%.1f%%", repeatRate), "🔄", UIConstants.INFO);
```

---

#### **📈 2. Bảng So Sánh Xu Hướng (Trend)**

**Thêm vào báo cáo Excel:**

```
Period | Revenue | RevPAR | Occupancy | Profit | Trend
-------|---------|--------|-----------|--------|-------
Tháng 3| 220M    | 1.1M   | 72.5%     | 150M   | ↗
Tháng 4| 240M    | 1.2M   | 76.2%     | 165M   | ↗
Tháng 5| 250M    | 1.234M | 78.5%     | 175M   | ↗
Năm sau| 290M    | 1.45M  | 82%       | 205M   | (Dự)
```

---

#### **💰 3. Phân Tích Chi Phí & Lợi Nhuận**

**Thêm vào Database:**

```
Doanh thu phòng:        250M (71.4%)
├─ Chi phí housekeeping: 35M (10%)
├─ Chi phí utilities:    28M (8%)
├─ Chi phí staff:        42M (12%)
└─ Chi phí bảo trì:      15M (4.3%)

Doanh thu dịch vụ:      45M (12.8%)
Doanh thu khác:         55M (15.8%)

Tổng doanh thu:         350M
Tổng chi phí:           120M (34.3%)
─────────────────
Lợi nhuận ròng:         230M (65.7%) ✓ TUYỆT!
```

---

#### **👥 4. Phân Tích Khách Hàng Chi Tiết**

**Thêm vào báo cáo:**

```
A. PHÂN KHÚC KHÁCH
├─ VIP (>20 lần / >50M đ): 28 người | 156.7M đ | 35% doanh thu
├─ Loyal (5-19 lần):        67 người | 89.3M đ  | 20% doanh thu
├─ Regular (2-4 lần):       92 người | 65.4M đ  | 15% doanh thu
└─ One-time (1 lần):        263 người | 48.6M đ | 11% doanh thu

B. PHÂN TÍCH THEO QUỐC TỊCH
├─ Việt Nam: 363 (80.7%)  | 248M đ
├─ Thái Lan: 45 (10%)     | 34M đ
├─ Trung Quốc: 28 (6.2%)  | 42M đ
└─ Khác: 14 (3.1%)        | 26M đ

C. PHÂN TÍCH THEO LOẠI KHÁCH
├─ Cá nhân: 294 (65.3%)   | 156M đ | ADR: 1.2M
├─ Doanh nghiệp: 156 (34.7%) | 194M đ | ADR: 1.9M ⭐
└─ Đoàn: 0

D. PHÂN TÍCH THEO KÊNH
├─ Direct (45%): Cao margin, cần ưu tiên
├─ OTA (55%): Cần quản lý commision 12-18%
└─ Recommendation: Tăng Direct từ 45% → 60% → Tiết kiệm 5-8M đ
```

---

#### **🎯 5. Top Performers & Recommendations**

**Phòng/Dịch Vụ/Kênh/Khách:**

```
🏆 TOP PHÒNG
1. P501 (Suite Family)     | 8.2M đ | 28 lượt | Occupancy: 95%
2. P403 (Suite Family)     | 7.9M đ | 26 lượt | Occupancy: 93%
3. P301 (Deluxe Twin)      | 6.3M đ | 21 lượt | Occupancy: 85%

💡 Insight: Suite Family có doanh thu cao nhất → Tăng giá 10% có thể +500K/đêm

🛎️ TOP DỊCH VỤ
1. Nước suối    | 89 lượt | 1.3M đ | Cross-sell: 19.8% khách
2. Thuê xe máy  | 67 lượt | 10M đ  | Cross-sell: 14.9% khách
3. Ăn sáng      | 45 lượt | 8.1M đ | Cross-sell: 10% khách

💡 Insight: Chỉ 19.8% khách mua dịch vụ → Target: 35% → +3M đ doanh thu

📅 KÊNH BOOKING
1. Booking.com  | 30% | Commission: 15% | -11.3M đ mất
2. Agoda        | 20% | Commission: 18% | -8.1M đ mất
3. Direct       | 45% | 0% comm         | ✓ Tiêu chuẩn

💡 Insight: OTA = 55% nhưng mất -19.4M đ commission
   → Chiến lược: Tăng Direct → 60%, giảm OTA → 40% = Tiết kiệm 4M đ/tháng
```

---

#### **📊 6. Biểu Đồ & Heatmap**

**Thêm vào giao diện:**

```
1. Occupancy Heatmap by Room Type & Day of Week
   ┌─────┬───┬───┬───┬───┬───┬───┬───┐
   │     │Mon│Tue│Wed│Thu│Fri│Sat│Sun│
   ├─────┼───┼───┼───┼───┼───┼───┼───┤
   │LP001│ 65│ 72│ 78│ 82│ 95│ 100│ 88│
   │LP004│ 88│ 92│ 95│ 98│ 100│ 100│ 95│
   │LP005│ 75│ 80│ 85│ 90│ 98│ 100│ 92│
   └─────┴───┴───┴───┴───┴───┴───┴───┘
   → Highlight: Thứ 7 & Chủ Nhật lúc nào cũng 100%
   → Chance: Tăng giá cuối tuần +20%

2. Revenue Trend (7 ngày qua)
   300M ┤                     ╭─
   250M ┤           ╭─────╮  │
   200M ┤      ╭────╯     ╰──╯
   150M ┤    ╭─╯
   100M ┤   ─╯
        └─────────────────────→
   → ADR trending UP ✓

3. Service Attachment Rate by Guest Type
   VIP:     45% (18 người dùng dịch vụ)
   Loyal:   38% (25 người)
   Regular: 28% (26 người)
   One-time: 8% (21 người)
   → Cross-sell VIP nhiều hơn = +Revenue
```

---

### **PHẦN 4: EXPORT EXCEL CHUYÊN NGHIỆP**

#### **Cấu Trúc File Excel Đề Xuất:**

```
Sheet 1: TỔNG QUAN
- Header: Logo + Tiêu đề + Ngày xuất
- Section 1: KPI Chính (RevPAR, Occupancy, ADR, ALOS, Repeat Rate)
- Section 2: Doanh Thu & Lợi Nhuận (với biểu đồ pie)
- Section 3: Khách Hàng (phân khúc, quốc tịch, loại)
- Section 4: Dự Báo & Recommendations

Sheet 2: CHI TIẾT PHÒNG
- Từng phòng: Occupancy, Doanh thu, Giá trung bình, Trend

Sheet 3: CHI TIẾT DỊCH VỤ
- Từng dịch vụ: Lượt bán, Doanh thu, % khách dùng

Sheet 4: PHÂN TÍCH KÊNH
- Booking channel: Lượt, Commission, Net Revenue

Sheet 5: PHÂN TÍCH KHÁCH
- Top 50 khách: VIP score, ALOS, Spending, Repeat

Sheet 6: SO SÁNH YoY (Year over Year)
- Tháng này vs Tháng năm ngoái
- Tính % tăng/giảm theo từng KPI
```

---

## 🚀 ƯU TIÊN THỰC HIỆN

### **Phase 1 (Tuần 1-2) - QUICK WINS**

```
✓ Thêm RevPAR & Occupancy Rate vào Dashboard
✓ Thêm Khách Quay Lại (Repeat Rate)
✓ Thêm ALOS (Average Length of Stay)
✓ Cải tiến export Excel (thêm tính lợi nhuận ròng)
```

### **Phase 2 (Tuần 3-4) - ENHANCEMENTS**

```
✓ Thêm biểu đồ Trend (7 ngày, 30 ngày, YoY)
✓ Heatmap Occupancy theo ngày/loại phòng
✓ Phân tích chi phí & lợi nhuận
✓ Top performers & recommendations
```

### **Phase 3 (Tháng 2) - ADVANCED**

```
✓ Revenue forecasting (AI-powered)
✓ Price optimization suggestions
✓ Customer segmentation analysis
✓ Benchmark vs industry standards
```

---

## 📋 SO SÁNH TRƯỚC/SAU

### **TRƯỚC (Hiện Tại)**

```
✗ Thiếu RevPAR (chỉ tiêu #1 của khách sạn)
✗ Không biết occupancy rate cụ thể %
✗ Không tính lợi nhuận (chỉ có doanh thu)
✗ Không biết khách quay lại bao nhiêu
✗ Không có insight về độ dài lưu trú
✗ Báo cáo Excel đơn sơ, không chuyên nghiệp
```

### **SAU (Cải Tiến)**

```
✓ Hiển thị RevPAR = 1,234,567 đ (biết hiệu quả từng phòng)
✓ Occupancy Rate = 78.5% (biết % phòng có khách)
✓ Lợi nhuận ròng = 175M đ, Margin 65.7% (biết khả năng sinh lợi)
✓ Khách quay lại = 30% (biết độ loyal của khách)
✓ ALOS = 3.2 đêm (biết khách ở bao lâu)
✓ Báo cáo Excel có:
  - Biểu đồ, heatmap, so sánh xu hướng
  - Recommendations cụ thể (tăng giá, chiến lược kênh, cross-sell)
  - Phân tích chi tiết khách/phòng/dịch vụ
  - So sánh YoY (tháng này vs tháng năm ngoái)
```

---

## 🎓 THAM KHẢO QUỐC TẾ

**Khách sạn 4-5 sao thực tế sử dụng:**

- RevPAR tracking hàng ngày
- Occupancy vs Forecast hàng tuần
- Profit & Loss statement hàng tháng
- Revenue Management Dashboard (Real-time)
- Customer Analytics (VIP scoring, repeat rate)
- Competitive set benchmarking

**Hệ thống nổi tiếng:**

- Oracle Hospitality (Marriott)
- IDeaS Revenue Management (Hilton)
- Micros PMS (Intercontinental)
- Hotelogix

---

## 💡 KỲ VỌNG KẾT QUẢ

Sau khi cải tiến, bạn sẽ:

- ✅ Biết rõ hiệu quả kinh doanh từng phòng (RevPAR)
- ✅ Hiểu mức độ sử dụng phòng (Occupancy %)
- ✅ Tối ưu giá (ADR optimization)
- ✅ Nhận diện khách VIP để ưu tiên
- ✅ Tính chính xác lợi nhuận ròng
- ✅ Có recommendations để tăng doanh thu
- ✅ Báo cáo trông "chuyên nghiệp" như khách sạn thực tế

**Dự kiến tăng doanh thu:** 10-15% trong 3 tháng (bằng cách:)

- Tăng ADR nhờ dynamic pricing
- Tăng Direct booking (giảm OTA commission)
- Cross-sell dịch vụ hơn (19% → 35%)
- Ưu tiên khách doanh nghiệp (ADR cao hơn)

---

**Bạn muốn tôi bắt đầu cải tiến từ phần nào trước?** 🚀
