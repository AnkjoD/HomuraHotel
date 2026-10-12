import os
import sys
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml, OxmlElement
from docx.oxml.ns import nsdecls, qn

def set_cell_background(cell, fill_hex):
    shading_elm = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    cell._tc.get_or_add_tcPr().append(shading_elm)

def set_table_borders(table, color="CCCCCC", sz="4", val="single"):
    tblPr = table._tbl.tblPr
    borders = parse_xml(
        f'<w:tblBorders {nsdecls("w")}>'
        f'<w:top w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:bottom w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:left w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'<w:insideH w:val="{val}" w:sz="{sz}" w:space="0" w:color="{color}"/>'
        f'<w:insideV w:val="none"/>'
        f'</w:tblBorders>'
    )
    tblPr.append(borders)

def add_callout_box(doc, text, color_hex="D97706", bg_hex="FFFBEB", prefix="LƯU Ý"):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    cell = table.cell(0, 0)
    set_cell_background(cell, bg_hex)
    
    # Set cell left border and remove others
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = parse_xml(
        f'<w:tcBorders {nsdecls("w")}>'
        f'<w:top w:val="none"/>'
        f'<w:left w:val="single" w:sz="36" w:space="0" w:color="{color_hex}"/>'
        f'<w:bottom w:val="none"/>'
        f'<w:right w:val="none"/>'
        f'</w:tcBorders>'
    )
    tcPr.append(tcBorders)
    
    # Cell margin/padding
    tcMar = parse_xml(
        f'<w:tcMar {nsdecls("w")}>'
        f'<w:top w:w="120" w:type="dxa"/>'
        f'<w:bottom w:w="120" w:type="dxa"/>'
        f'<w:left w:w="240" w:type="dxa"/>'
        f'<w:right w:w="240" w:type="dxa"/>'
        f'</w:tcMar>'
    )
    tcPr.append(tcMar)
    
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(4)
    
    run_prefix = p.add_run(f"★ {prefix}: ")
    run_prefix.bold = True
    run_prefix.font.name = 'Calibri'
    run_prefix.font.size = Pt(10.5)
    run_prefix.font.color.rgb = RGBColor(0x9A, 0x34, 0x12) if color_hex == "D97706" else RGBColor(0x1E, 0x40, 0xAF)
    
    run_text = p.add_run(text)
    run_text.font.name = 'Calibri'
    run_text.font.size = Pt(10.5)
    run_text.font.italic = True
    run_text.font.color.rgb = RGBColor(0x37, 0x41, 0x51)

def format_paragraph(p, before=0, after=6, line_spacing=1.15):
    p.paragraph_format.space_before = Pt(before)
    p.paragraph_format.space_after = Pt(after)
    p.paragraph_format.line_spacing = line_spacing

def add_heading_1(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.keep_with_next = True
    format_paragraph(p, before=18, after=8)
    run = p.add_run(text)
    run.font.name = 'Calibri'
    run.font.size = Pt(16)
    run.bold = True
    run.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A) # Slate 900
    
    # Add a thin line under Heading 1
    pBdr = parse_xml(f'<w:pBdr {nsdecls("w")}><w:bottom w:val="single" w:sz="6" w:space="4" w:color="CBD5E1"/></w:pBdr>')
    p._p.get_or_add_pPr().append(pBdr)
    return p

def add_heading_2(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.keep_with_next = True
    format_paragraph(p, before=12, after=4)
    run = p.add_run(text)
    run.font.name = 'Calibri'
    run.font.size = Pt(13)
    run.bold = True
    run.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A) # Navy Blue
    return p

def add_heading_3(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.keep_with_next = True
    format_paragraph(p, before=8, after=2)
    run = p.add_run(text)
    run.font.name = 'Calibri'
    run.font.size = Pt(11.5)
    run.bold = True
    run.font.color.rgb = RGBColor(0x37, 0x41, 0x51) # Dark Gray
    return p

def add_body_paragraph(doc, text, bold_prefix=None, bullet=False, indent=0):
    p = doc.add_paragraph()
    format_paragraph(p, before=0, after=5)
    
    if bullet:
        p.paragraph_format.left_indent = Inches(0.25 + indent * 0.25)
        run_bullet = p.add_run("•  ")
        run_bullet.font.name = 'Calibri'
        run_bullet.font.size = Pt(11)
        run_bullet.font.color.rgb = RGBColor(0x4B, 0x55, 0x63)
    elif indent > 0:
        p.paragraph_format.left_indent = Inches(indent * 0.25)
        
    if bold_prefix:
        run_bold = p.add_run(bold_prefix)
        run_bold.bold = True
        run_bold.font.name = 'Calibri'
        run_bold.font.size = Pt(11)
        run_bold.font.color.rgb = RGBColor(0x1F, 0x29, 0x37)
        
    run_text = p.add_run(text)
    run_text.font.name = 'Calibri'
    run_text.font.size = Pt(11)
    run_text.font.color.rgb = RGBColor(0x1F, 0x29, 0x37)
    return p

def add_stat_line(p, label, value):
    run_label = p.add_run(f"{label}: ")
    run_label.font.name = 'Calibri'
    run_label.font.size = Pt(10.5)
    run_label.font.color.rgb = RGBColor(0x4B, 0x55, 0x63)
    
    run_val = p.add_run(f"{value}   |   ")
    run_val.bold = True
    run_val.font.name = 'Calibri'
    run_val.font.size = Pt(10.5)
    run_val.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)

def create_report():
    doc = Document()
    
    # Page Setup
    section = doc.sections[0]
    section.top_margin = Inches(1)
    section.bottom_margin = Inches(1)
    section.left_margin = Inches(1)
    section.right_margin = Inches(1)
    
    # Document Header Title (Cover style at the top)
    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    format_paragraph(p_title, before=24, after=4)
    run_title = p_title.add_run("ĐỒ ÁN MÔN HỌC KHỞI NGHIỆP\n")
    run_title.font.name = 'Calibri'
    run_title.font.size = Pt(14)
    run_title.bold = True
    run_title.font.color.rgb = RGBColor(0x4B, 0x55, 0x63)
    
    run_maintitle = p_title.add_run("OHNO PMS: NỀN TẢNG QUẢN TRỊ KHÁCH SẠN THÔNG MINH\n")
    run_maintitle.font.name = 'Calibri'
    run_maintitle.font.size = Pt(22)
    run_maintitle.bold = True
    run_maintitle.font.color.rgb = RGBColor(0x1E, 0x3B, 0x8A)
    
    run_subtitle = p_title.add_run("Giải pháp SaaS tối ưu cho khách sạn 3–4 sao tại Việt Nam\n")
    run_subtitle.font.name = 'Calibri'
    run_subtitle.font.size = Pt(12)
    run_subtitle.font.italic = True
    run_subtitle.font.color.rgb = RGBColor(0x4B, 0x55, 0x63)
    
    p_meta = doc.add_paragraph()
    p_meta.alignment = WD_ALIGN_PARAGRAPH.CENTER
    format_paragraph(p_meta, before=0, after=24)
    run_meta = p_meta.add_run("Chuyên ngành: Phát triển Ứng dụng & Khởi nghiệp Công nghệ số\nPhiên bản: 1.0  •  Năm 2026")
    run_meta.font.name = 'Calibri'
    run_meta.font.size = Pt(10)
    run_meta.font.color.rgb = RGBColor(0x9C, 0xA3, 0xAF)
    
    # Divider line
    pBdr = parse_xml(f'<w:pBdr {nsdecls("w")}><w:bottom w:val="double" w:sz="12" w:space="8" w:color="1E3B8A"/></w:pBdr>')
    p_meta._p.get_or_add_pPr().append(pBdr)

    # 1. TÓM TẮT DỰ ÁN
    add_heading_1(doc, "1. TÓM TẮT DỰ ÁN (EXECUTIVE SUMMARY)")
    add_body_paragraph(doc, "Ohno PMS (Property Management System) là phần mềm quản lý khách sạn thông minh được phát triển nhằm giải quyết các vấn đề vận hành thực tế tại các khách sạn quy mô vừa và lớn (tiêu chuẩn 3–4 sao, có quy mô từ 20–150 phòng) tại Việt Nam. Dự án xuất phát từ đồ án môn học gắn liền với thực tiễn, được khảo sát kỹ lưỡng và xây dựng dựa trên sự cố vấn của các chuyên gia lưu trú nhằm hướng tới khả năng thương mại hóa thực tế sau khi hoàn thiện.")
    
    # Stats overview paragraph
    p_stats = doc.add_paragraph()
    p_stats.alignment = WD_ALIGN_PARAGRAPH.LEFT
    format_paragraph(p_stats, before=6, after=12)
    p_stats.paragraph_format.left_indent = Inches(0.15)
    add_stat_line(p_stats, "Phân khúc mục tiêu", "Khách sạn 3–4 sao (20–150 phòng)")
    add_stat_line(p_stats, "Mô hình kinh doanh", "Thuê bao phần mềm (SaaS)")
    add_stat_line(p_stats, "Quy mô thị trường VN (2025)", "15.47M USD (CAGR 4.35%)")
    add_stat_line(p_stats, "Mức thất thoát phổ biến", "Khoảng 10 - 15% (Do quản lý thủ công)")
    
    add_body_paragraph(doc, "Điểm khác biệt cốt lõi và lợi thế cạnh tranh độc nhất của Ohno PMS nằm ở ba tính năng nghiệp vụ chuyên sâu và khép kín:")
    add_body_paragraph(doc, "Master Bill (Gom Hóa Đơn Toàn Đoàn): Tự động gom toàn bộ tiền phòng và dịch vụ của toàn bộ thành viên trong đoàn, tự động khấu trừ đặt cọc thông minh và chống tính trùng hóa đơn (double-billing).", bold_prefix="•  ", bullet=True)
    add_body_paragraph(doc, "Overlap Prevention (Ngăn Chặn Trùng Lịch Thời Gian Thực): Sử dụng cơ chế cơ sở dữ liệu Transaction chặt chẽ giúp quét và khóa phòng ngay lập tức khi đơn đặt ở trạng thái chờ (PENDING), loại bỏ hoàn toàn rủi ro Overbooking.", bold_prefix="•  ", bullet=True)
    add_body_paragraph(doc, "Early Checkout Penalty (Phạt Trả Phòng Sớm Tự Động): So sánh tự động giữa số đêm lưu trú thực tế so với số đêm đặt ban đầu, đưa ra gợi ý nhanh mức phạt từ 30% - 100% nhằm bảo toàn doanh thu cơ hội cho khách sạn.", bold_prefix="•  ", bullet=True)

    # 2. VẤN ĐỀ THỊ TRƯỜNG
    add_heading_1(doc, "2. VẤN ĐỀ THỊ TRƯỜNG (THE PROBLEM)")
    add_body_paragraph(doc, "Qua tham khảo các nghiên cứu ngành công nghiệp lưu trú và quan sát thực tế hành vi vận hành tại một số cơ sở lưu trú quy mô vừa tại địa phương, nhóm phát triển nhận diện được 4 nhóm vấn đề lớn chưa được các phần mềm PMS phổ thông trên thị trường giải quyết triệt để:")
    
    add_heading_2(doc, "2.1. Nghiệp vụ khách đoàn phức tạp, dễ nhầm lẫn và thất thoát")
    add_body_paragraph(doc, "Lễ tân phải cộng thủ công tiền phòng và dịch vụ của từng phòng khi đoàn (10–20 phòng) làm thủ tục trả phòng rải rác hoặc trả chung hóa đơn tổng. Quá trình tính tay này tốn trung bình từ 30–45 phút, dễ dẫn đến tình trạng tính trùng ('double-billing') hoặc bỏ sót dịch vụ của các phòng lẻ, gây tranh cãi và làm giảm trải nghiệm khách hàng tại sảnh chờ.", bold_prefix="Vấn đề hiện tại: ")
    add_body_paragraph(doc, "Ohno PMS cung cấp cơ chế Master Bill tự động gom toàn bộ hóa đơn đoàn, tự động phát hiện các phòng cùng đoàn đang treo nợ khi phòng cuối check-out, khấu trừ cọc đúng phòng và tự động lọc phòng đã thanh toán. Thời gian check-out đoàn giảm từ ~35 phút xuống còn ~5 phút.", bold_prefix="Giải pháp Ohno PMS: ")

    add_heading_2(doc, "2.2. Thất thoát doanh thu đáng kể từ phí phạt trả phòng sớm")
    add_body_paragraph(doc, "Trong thực tế, khách đoàn hoặc khách lẻ thường thay đổi lịch trình và trả phòng sớm hơn dự kiến. Các hệ thống PMS thông thường không tự động so sánh số ngày lưu trú thực tế với số ngày đặt ban đầu, bắt buộc nhân viên lễ tân phải tự tính toán số tiền phạt trả sớm bằng tay. Nhân viên thường quên, tính sai hoặc bỏ qua khoản phạt này để tránh phiền phức, khiến khách sạn thất thoát một nguồn thu không nhỏ theo thời gian.", bold_prefix="Vấn đề hiện tại: ")
    add_body_paragraph(doc, "Ohno PMS tự động đối chiếu actualNights với bookedNights. Nếu phát hiện khách trả phòng sớm, hệ thống sẽ kích hoạt luồng áp phí phạt với bộ nút gợi ý nhanh 30% / 50% / 70% / 100% giá trị những đêm không lưu trú trực tiếp trên hóa đơn, giúp nhân viên thao tác nhanh chóng và giảm thiểu tranh cãi.", bold_prefix="Giải pháp Ohno PMS: ")

    add_heading_2(doc, "2.3. Rủi ro Overbooking và xung đột lịch đặt phòng do xử lý phi đồng bộ")
    add_body_paragraph(doc, "Các hệ thống quản lý khách sạn giá rẻ thường bỏ qua việc kiểm tra chéo các đơn đặt phòng ở trạng thái chờ (PENDING) khi lễ tân xác nhận phòng mới. Nếu hai nhân viên cùng thao tác hoặc mạng gián đoạn, hệ thống dễ ghi nhận hai đơn đặt đè lên một phòng trong cùng khoảng thời gian, khiến khách sạn phải đền bù phòng hoặc chuyển khách sang đối thủ.", bold_prefix="Vấn đề hiện tại: ")
    add_body_paragraph(doc, "Ohno PMS thiết kế hàm isRoomAvailable() quét sâu vào cả trạng thái PENDING. Đồng thời, hệ thống áp dụng cơ chế Database Transaction (Begin/Commit/Rollback) chặt chẽ, đảm bảo tính toàn vẹn dữ liệu tuyệt đối kể cả khi mạng gặp sự cố giữa chừng.", bold_prefix="Giải pháp Ohno PMS: ")

    add_heading_2(doc, "2.4. Giao diện phần mềm lỗi thời, đào tạo nhân viên mất nhiều thời gian")
    add_body_paragraph(doc, "Phần mềm PMS truyền thống sở hữu giao diện dày đặc chữ, màu sắc đơn điệu và thiếu trực quan. Lễ tân mới cần từ 2 đến 4 tuần đào tạo để có thể thao tác thành thạo. Ngoài ra, việc nhập liệu chậm trễ (nhập họ tên trước số điện thoại) khiến luồng thao tác bị ngắt quãng, không tận dụng được dữ liệu cũ của khách hàng.", bold_prefix="Vấn đề hiện tại: ")
    add_body_paragraph(doc, "Ohno PMS ứng dụng ngôn ngữ thiết kế Linear UI hiện đại với độ tương phản cao, sơ đồ trực quan và Timeline Gantt sinh động. Luồng thao tác được tối ưu hóa (focus số điện thoại trước để tự động điền thông tin cũ). Giúp rút ngắn thời gian đào tạo nhân viên mới xuống còn 1–2 ngày làm quen.", bold_prefix="Giải pháp Ohno PMS: ")

    # 3. GIẢI PHÁP SẢN PHẨM & CÔNG NGHỆ
    add_heading_1(doc, "3. GIẢI PHÁP SẢN PHẨM & CÔNG NGHỆ ĐỘT PHÁ (THE SOLUTION)")
    add_body_paragraph(doc, "Ohno PMS định hình là một ứng dụng Desktop chuyên sâu và mạnh mẽ ở giai đoạn hiện tại (Java Swing), với lộ trình chuyển đổi linh hoạt lên nền tảng Web/Cloud vào năm 2027. Giao diện lấy cảm hứng từ Linear.app với độ tương phản cao, tối giản, giúp giảm mỏi mắt cho lễ tân trực ca đêm.")

    add_heading_2(doc, "3.1. Cơ chế Gom Bill Đoàn (Master Bill)")
    add_body_paragraph(doc, "Lễ tân có thể truy cập nhanh qua menu chuột phải tại bất kỳ phòng nào thuộc đoàn khách.", bullet=True)
    add_body_paragraph(doc, "Tự động phát hiện các phòng cùng đoàn đang treo nợ khi phòng cuối cùng tiến hành check-out và đề xuất gom hóa đơn tổng.", bullet=True)
    add_body_paragraph(doc, "Khấu trừ tiền cọc tự động: Tự động gom và khấu trừ toàn bộ tiền cọc của đoàn vào hóa đơn tổng hoặc hóa đơn phòng thanh toán cuối cùng một cách minh bạch.", bullet=True)
    add_body_paragraph(doc, "Lọc phòng đã thanh toán riêng lẻ: Hóa đơn Master Bill tự động loại bỏ các khoản đã thanh toán trước đó, loại bỏ hoàn toàn lỗi double-billing.", bullet=True)
    add_body_paragraph(doc, "Hỗ trợ in hóa đơn tổng chuẩn nghiệp vụ du lịch và hiển thị mã QR thanh toán động tích hợp số tiền cần thanh toán.", bullet=True)

    add_heading_2(doc, "3.2. Hệ thống áp phí phạt trả sớm (Early Checkout Penalty)")
    add_body_paragraph(doc, "Hệ thống tự động so sánh số đêm thực tế (actualNights) so với số đêm đặt (bookedNights) ngay khi thực hiện thao tác trả phòng.", bullet=True)
    add_body_paragraph(doc, "Nếu có sự chênh lệch (trả sớm), hệ thống lập tức hiển thị bảng tính phí phạt đi kèm bộ nút gợi ý nhanh: 30%, 50%, 70%, 100% giá trị phòng của những đêm không lưu trú.", bullet=True)
    add_body_paragraph(doc, "Lễ tân không cần tính toán thủ công bằng tay, hạn chế sai sót và tạo lập quy trình chuyên nghiệp, minh bạch để giải thích với trưởng đoàn hoặc khách hàng.", bullet=True)

    add_heading_2(doc, "3.3. Thuật toán kiểm soát xung đột thời gian thực (Overlap Prevention)")
    add_body_paragraph(doc, "Hàm isRoomAvailable() quét kiểm tra chéo toàn bộ quỹ phòng, bao gồm cả những phòng đang ở trạng thái 'PENDING' (chờ đặt cọc hoặc chờ xác nhận).", bullet=True)
    add_body_paragraph(doc, "Sử dụng Database Transaction đồng bộ: Toàn bộ quá trình đặt nhiều phòng cùng lúc (Batch Booking) được đặt trong một khối giao dịch duy nhất. Nếu xảy ra lỗi ghi dữ liệu hoặc mất kết nối mạng ở bất kỳ phòng nào, hệ thống sẽ thực hiện Rollback tự động, đưa cơ sở dữ liệu về trạng thái sạch sẽ ban đầu để tránh lệch pha dữ liệu.", bullet=True)

    add_heading_2(doc, "3.4. Giao diện Linear UI/UX Premium & Trực quan hóa Báo cáo")
    add_body_paragraph(doc, "Timeline Gantt (KeHoachPanel): Cho phép theo dõi trực quan sơ đồ đặt phòng dưới dạng các thanh tiến độ màu sắc sinh động, hỗ trợ hiển thị tooltip thông tin chi tiết khi rê chuột.", bullet=True)
    add_body_paragraph(doc, "Renderer Chuyên biệt (InvoiceCellRenderer): Tối ưu hóa hiển thị dòng hóa đơn được chọn với độ tương phản cao, giúp lễ tân dễ nhìn, tránh click nhầm dòng dịch vụ.", bullet=True)
    add_body_paragraph(doc, "Hộp nhập liệu thông minh: Đảo vị trí ưu tiên tiêu điểm vào ô Số điện thoại trước Họ tên. Khi lễ tân gõ SĐT, hệ thống tự động truy vấn dữ liệu cũ để Auto-fill thông tin họ tên, quốc tịch, giúp lễ tân check-in nhanh chỉ trong 30 giây.", bullet=True)
    add_body_paragraph(doc, "Báo cáo KPI Vector: Tích hợp vẽ biểu đồ trực quan các chỉ số quan trọng (Occupancy Rate, ADR, ALOS, Lợi nhuận ròng) bằng đồ họa vector mịn màng, hỗ trợ xuất báo cáo chuyên nghiệp.", bullet=True)

    add_callout_box(doc, 
                    "Cơ chế DB Transaction đảm bảo dữ liệu luôn nhất quán 100%, không xảy ra tình trạng mất dấu tiền cọc hay trùng phòng kể cả khi lễ tân thao tác liên tục trong giờ cao điểm.", 
                    color_hex="1E40AF", bg_hex="EFF6FF", prefix="ĐIỂM SÁNG CÔNG NGHỆ")

    # 4. PHÂN TÍCH THỊ TRƯỜNG
    add_heading_1(doc, "4. PHÂN TÍCH THỊ TRƯỜNG VÀ TIỀM NĂNG TĂNG TRƯỞNG (MARKET ANALYSIS)")
    
    add_heading_2(doc, "4.1. Quy mô thị trường (Market Size)")
    add_body_paragraph(doc, "Thị trường phần mềm quản lý khách sạn (Hospitality Tech) tại Việt Nam đang bước vào giai đoạn số hóa mạnh mẽ:")
    add_body_paragraph(doc, "TAM (Tổng thị trường khả dụng): Quy mô thị trường PMS toàn Đông Nam Á đạt khoảng 350 triệu USD.", bullet=True)
    add_body_paragraph(doc, "SAM (Thị trường phục vụ được): Quy mô thị trường phần mềm quản lý khách sạn Việt Nam đạt giá trị khoảng 15.47 triệu USD vào năm 2025 và dự kiến sẽ tăng trưởng lên 22.69 triệu USD vào năm 2034 với tốc độ tăng trưởng kép hàng năm (CAGR) đạt 4.35% (Theo Ken Research).", bullet=True)
    add_body_paragraph(doc, "SOM (Thị trường mục tiêu chiếm lĩnh): Phân khúc khách sạn độc lập tiêu chuẩn 3–4 sao tại Việt Nam trong 5 năm đầu tiên của Ohno PMS ước tính đạt khoảng 2.5 triệu USD.", bullet=True)
    
    add_body_paragraph(doc, "Đáng chú ý, khoảng 68% cơ sở lưu trú quy mô vừa và nhỏ tại Việt Nam hiện đang được tự vận hành độc lập và quản lý thủ công (sổ sách hoặc Excel), chưa áp dụng phần mềm chuyên nghiệp. Đây chính là tệp khách hàng tiềm năng cực kỳ lớn cho một giải pháp tinh gọn, tối ưu chi phí như Ohno PMS.")

    add_heading_2(doc, "4.2. Khách hàng mục tiêu (Target Customers)")
    add_body_paragraph(doc, "Khách sạn độc lập quy mô từ 20 đến 150 phòng (tương đương chuẩn 3–4 sao) đang hoạt động tại các thành phố du lịch trọng điểm.", bullet=True)
    add_body_paragraph(doc, "Resort boutique, chuỗi căn hộ dịch vụ cao cấp, khách sạn chuyên đón các đoàn khách du lịch/lữ hành lớn.", bullet=True)
    add_body_paragraph(doc, "Đặc điểm hành vi: Chủ doanh nghiệp có ngân sách vừa phải, yêu cầu phần mềm có nghiệp vụ đủ mạnh mẽ để quản lý chặt chẽ tài chính và chống thất thoát, nhưng không thể đầu tư các hệ thống quốc tế quá đắt đỏ như Oracle Opera (chi phí lên đến vài chục nghìn USD mỗi năm).", bullet=True)

    # 5. MÔ HÌNH KINH DOANH
    add_heading_1(doc, "5. MÔ HÌNH KINH DOANH (BUSINESS MODEL)")
    add_body_paragraph(doc, "Ohno PMS áp dụng mô hình SaaS (Software as a Service) - cho thuê phần mềm thu phí định kỳ dựa trên số lượng phòng thực tế của khách sạn, đảm bảo dòng tiền đều đặn và dễ dự đoán:")

    # Table for business model
    table = doc.add_table(rows=5, cols=3)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(table)
    
    # Table headers
    hdr_cells = table.rows[0].cells
    hdr_cells[0].text = 'Nguồn thu chính'
    hdr_cells[1].text = 'Mô tả chi tiết'
    hdr_cells[2].text = 'Mức giá dự kiến'
    for cell in hdr_cells:
        set_cell_background(cell, "1F4E78")
        for p in cell.paragraphs:
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for run in p.runs:
                run.font.name = 'Calibri'
                run.font.size = Pt(10.5)
                run.bold = True
                run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
                
    # Data rows
    data = [
        ("Phí thuê bao phần mềm (SaaS)", "Cung cấp 3 gói: Standard (<30 phòng) / Pro (30–80 phòng) / Enterprise (>80 phòng). Tính phí theo số phòng thực tế.", "15.000 – 25.000 VNĐ / phòng / tháng (Trung bình 1.5M - 3M VNĐ/tháng)"),
        ("Phí cài đặt & Đào tạo (Setup)", "Chi phí hỗ trợ chuẩn hóa sơ đồ phòng, import dữ liệu ban đầu, đào tạo nghiệp vụ trực tiếp cho lễ tân và kế toán.", "2.000.000 – 5.000.000 VNĐ (Thu 1 lần duy nhất)"),
        ("Phí tích hợp giá trị gia tăng", "Tích hợp cổng QR thanh toán động ngân hàng, hóa đơn điện tử e-Invoice, gửi tin nhắn SMS/Zalo tự động cho khách.", "Phí giao dịch từ 0.1% – 0.2% trên tổng giá trị giao dịch phát sinh"),
        ("Quản lý đa kênh (Channel Manager)", "Đồng bộ quỹ phòng trống thời gian thực lên các sàn OTA quốc tế (Agoda, Booking, Traveloka, Expedia).", "500.000 VNĐ / tháng (Đăng ký tùy chọn)")
    ]
    
    for i, row in enumerate(data):
        row_cells = table.rows[i+1].cells
        row_cells[0].text = row[0]
        row_cells[1].text = row[1]
        row_cells[2].text = row[2]
        
        # Apply zebra striping
        bg_color = "F2F2F2" if i % 2 == 1 else "FFFFFF"
        for cell in row_cells:
            set_cell_background(cell, bg_color)
            for p in cell.paragraphs:
                format_paragraph(p, before=3, after=3)
                for run in p.runs:
                    run.font.name = 'Calibri'
                    run.font.size = Pt(10)
                    run.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
                    
    doc.add_paragraph() # spacing

    # 6. PHÂN TÍCH ĐỐI THỦ CẠNH TRANH
    add_heading_1(doc, "6. PHÂN TÍCH ĐỐI THỦ CẠNH TRANH (COMPETITIVE LANDSCAPE)")
    add_body_paragraph(doc, "Tại thị trường Việt Nam, Ohno PMS định vị nằm ở phân khúc tối ưu giữa nhóm phần mềm bán hàng POS giá rẻ và các hệ thống quản trị chuyên sâu quốc tế:")

    # Table for competitor comparison
    comp_table = doc.add_table(rows=6, cols=5)
    comp_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(comp_table)
    
    comp_headers = ['Tiêu chí so sánh', 'KiotViet Hotel', 'ezCloud / ezFolio', 'Opera PMS (Oracle)', 'Ohno PMS (Giải pháp)']
    hdr_cells = comp_table.rows[0].cells
    for col_idx, text in enumerate(comp_headers):
        hdr_cells[col_idx].text = text
        set_cell_background(hdr_cells[col_idx], "1F4E78")
        for p in hdr_cells[col_idx].paragraphs:
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            for run in p.runs:
                run.font.name = 'Calibri'
                run.font.size = Pt(10)
                run.bold = True
                run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
                
    comp_data = [
        ("Đối tượng phục vụ", "Nhà nghỉ, homestay (<15 phòng)", "Khách sạn 2–5 sao, resort", "Khách sạn 5 sao quốc tế", "Khách sạn 3–4 sao, khách đoàn"),
        ("Giao diện & Trải nghiệm", "Đơn giản dạng POS bán lẻ", "Đầy đủ nhưng khá phức tạp", "Rất phức tạp, khó đào tạo", "Linear UI hiện đại, Gantt trực quan"),
        ("Nghiệp vụ khách đoàn", "Cơ bản, không gom bill phức tạp", "Tốt nhưng thao tác nhiều bước", "Rất mạnh mẽ", "Master Bill tự động qua chuột phải"),
        ("Phí phạt trả sớm", "Lễ tân tự tính toán thủ công", "Có hỗ trợ nhưng tính tay phức tạp", "Có hỗ trợ chuyên sâu", "Tự động phát hiện + gợi ý % phạt nhanh"),
        ("Chi phí sử dụng", "Rất rẻ (~200K - 500K/tháng)", "Trung bình khá (1M - 3M/tháng)", "Cực kỳ đắt (vài nghìn USD/năm)", "Hợp lý, tối ưu ROI (1M - 2.5M/tháng)")
    ]
    
    for i, row in enumerate(comp_data):
        row_cells = comp_table.rows[i+1].cells
        for col_idx, text in enumerate(row):
            row_cells[col_idx].text = text
            
        bg_color = "F2F2F2" if i % 2 == 1 else "FFFFFF"
        for col_idx, cell in enumerate(row_cells):
            set_cell_background(cell, bg_color)
            for p in cell.paragraphs:
                format_paragraph(p, before=3, after=3)
                for run in p.runs:
                    run.font.name = 'Calibri'
                    run.font.size = Pt(9.5)
                    run.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
                    if col_idx == 4: # Highlight Ohno PMS column
                        run.bold = True
                        run.font.color.rgb = RGBColor(0x1E, 0x3B, 0x8A)
                        
    doc.add_paragraph() # spacing

    add_body_paragraph(doc, "Lợi thế cạnh tranh cốt lõi (Unfair Advantages):")
    add_body_paragraph(doc, "Thiết kế Linear UI/UX thực tiễn: Giao diện dựa trên việc quan sát trực tiếp thói quen của lễ tân trực ca. Việc đảo vị trí SĐT lên trước Họ tên kích hoạt Auto-fill giúp rút ngắn 80% thời gian check-in.", bold_prefix="•  ", bullet=True)
    add_body_paragraph(doc, "Cơ chế Transaction bảo mật dữ liệu: Hạn chế tối đa rủi ro lệch pha dữ liệu giữa client và máy chủ cloud, giúp kiểm soát tuyệt đối tính minh bạch tài chính.", bold_prefix="•  ", bullet=True)
    add_body_paragraph(doc, "Tính địa phương hóa cao: Tích hợp trực tiếp cổng thanh toán QR động phổ biến tại Việt Nam và form khai báo lưu trú của Công an Việt Nam ngay trong hệ thống.", bold_prefix="•  ", bullet=True)

    # 7. CHIẾN LƯỢC TIẾP CẬN THỊ TRƯỜNG
    add_heading_1(doc, "7. CHIẾN LƯỢC TIẾP CẬN THỊ TRƯỜNG (GO-TO-MARKET STRATEGY)")
    add_body_paragraph(doc, "Thay vì đặt ra các con số quá lớn và không thực tế, Ohno PMS tập trung vào chiến lược Go-to-Market tinh gọn và chắc chắn theo từng giai đoạn, hướng tới mục tiêu sở hữu từ 50–100 khách sạn thành viên trong vòng 2 năm đầu tiên hoạt động:")
    
    add_heading_2(doc, "Giai đoạn 1: Thử nghiệm thực địa (Pilot Phase - Quý 3/2026)")
    add_body_paragraph(doc, "Triển khai thử nghiệm miễn phí 30 ngày tại 5 khách sạn quy mô từ 30–50 phòng tại Đà Nẵng và Đà Lạt (đối tác quen của nhóm sáng lập).", bullet=True)
    add_body_paragraph(doc, "Kỹ thuật viên của nhóm trực tiếp hỗ trợ import dữ liệu cũ sang Ohno PMS chỉ trong 2 giờ và đào tạo lễ tân thao tác trong 1 buổi.", bullet=True)
    add_body_paragraph(doc, "Đo lường các chỉ số hiệu quả thực tế: Thời gian check-out đoàn giảm, số lần overlap bị ngăn chặn, và số tiền phạt trả phòng sớm thu hồi được nhờ bộ gợi ý nhanh. Đây sẽ là case study thực tế làm tài liệu thuyết phục các khách hàng tiếp theo.", bullet=True)

    add_heading_2(doc, "Giai đoạn 2: Tiếp cận trực tiếp và B2B Networking (Quý 4/2026 – 2027)")
    add_body_paragraph(doc, "Tiếp cận thông qua các Hiệp hội Du lịch địa phương tại Khánh Hòa, Quảng Nam, Lâm Đồng. Tổ chức các buổi workshop nhỏ với chủ đề: 'Số hóa tinh gọn và các giải pháp chống thất thoát doanh thu thực tế cho khách sạn vừa và nhỏ'.", bullet=True)
    add_body_paragraph(doc, "Chính sách giới thiệu (Referral Program): Chiết khấu trực tiếp 10% phí thuê bao năm đầu tiên cho các chủ khách sạn giới thiệu thành công sản phẩm cho đối tác trong khu vực.", bullet=True)
    add_body_paragraph(doc, "Bán hàng trực tiếp (Direct B2B Sales): Đội ngũ Sales thực địa trực tiếp tiếp cận các cụm khách sạn tập trung ở các thành phố du lịch.", bullet=True)

    add_heading_2(doc, "Giai đoạn 3: Di chuyển đám mây và Mở rộng hệ sinh thái (Năm 2027 trở đi)")
    add_body_paragraph(doc, "Chuyển đổi hoàn toàn hệ thống sang Web-based Cloud (Spring Boot API + Next.js), cho phép chủ khách sạn quản lý từ xa mọi lúc mọi nơi trên mọi thiết bị.", bullet=True)
    add_body_paragraph(doc, "Phát triển các add-on giá trị gia tăng để tăng doanh thu ARPU (doanh thu trung bình trên một khách hàng) như kết nối OTA Channel Manager và ra mắt Housekeeping App cho buồng phòng.", bullet=True)

    # 8. ĐỘI NGŨ SÁNG LẬP
    add_heading_1(doc, "8. ĐỘI NGŨ SÁNG LẬP VÀ PHÂN CÔNG VAI TRÒ (THE TEAM)")
    add_body_paragraph(doc, "Dự án được xây dựng bởi nhóm sinh viên đam mê công nghệ và nghiệp vụ du lịch, phân chia vai trò rõ ràng để tối ưu hóa hiệu quả thực hiện đồ án khởi nghiệp:")
    
    add_body_paragraph(doc, "Trần Anh Khoa — Trưởng nhóm & Lead Backend Developer: Chịu trách nhiệm thiết kế kiến trúc hệ thống, xây dựng cơ chế DB Transaction và các thuật toán an toàn isRoomAvailable(), xử lý dữ liệu nhất quán.", bullet=True)
    add_body_paragraph(doc, "Nguyễn Minh Thu — UI/UX Designer & Desktop Developer: Phụ trách thiết kế ngôn ngữ Linear UI, xây dựng giao diện Timeline Gantt trực quan và tùy biến các bộ Renderer cho bảng biểu, tối ưu hóa luồng tương tác người dùng.", bullet=True)
    add_body_paragraph(doc, "Lê Hoàng Nam — Business Analyst & Market Research: Khảo sát thực tế các nghiệp vụ khách sạn, thu thập số liệu thị trường, xây dựng mô hình tài chính và thực hiện chiến lược tiếp cận thị trường (Go-to-Market).", bullet=True)
    add_body_paragraph(doc, "Giảng viên cố vấn chuyên môn: Cố vấn định hướng kiến trúc phần mềm và thẩm định tính khả thi trong quy trình nghiệp vụ lưu trú thực tế.", bullet=True)

    # 9. LỘ TRÌNH PHÁT TRIỂN SẢN PHẨM
    add_heading_1(doc, "9. LỘ TRÌNH PHÁT TRIỂN SẢN PHẨM (PRODUCT ROADMAP)")
    add_body_paragraph(doc, "Lộ trình phát triển được thiết kế cụ thể để chuyển đổi từ một ứng dụng Desktop mạnh mẽ sang hệ sinh thái Cloud toàn diện:")
    
    add_body_paragraph(doc, "Quý 1 – Quý 2/2026 (Hoàn thiện MVP Desktop): Hoàn thiện các tính năng nghiệp vụ cốt lõi viết bằng Java Swing bao gồm: Master Bill, Overlap Prevention, Early Checkout Penalty, Timeline Gantt và hệ thống vẽ biểu đồ báo cáo KPI.", bullet=True)
    add_body_paragraph(doc, "Quý 3/2026 (Chạy thử nghiệm Beta Test): Triển khai cài đặt chạy thử nghiệm tại 5 khách sạn đối tác tại Đà Nẵng và Đà Lạt. Ghi nhận phản hồi thực tế từ lễ tân và kế toán để vá lỗi hệ thống.", bullet=True)
    add_body_paragraph(doc, "Quý 4/2026 – Quý 2/2027 (Cloud Migration): Chuyển đổi kiến trúc hệ thống. Xây dựng Backend API bằng Java Spring Boot, lưu trữ cơ sở dữ liệu trên Cloud (AWS/GCP) và xây dựng giao diện Frontend Web bằng React/Next.js.", bullet=True)
    add_body_paragraph(doc, "Quý 3/2027 trở đi (Hệ sinh thái mở rộng): Ra mắt Housekeeping App (ứng dụng di động cho bộ phận dọn phòng), tích hợp OTA Channel Manager và Booking Engine trực tiếp trên website khách sạn.", bullet=True)

    # 10. KẾ HOẠCH VỐN & SỬ DỤNG VỐN
    add_heading_1(doc, "10. KẾ HOẠCH VỐN VÀ SỬ DỤNG VỐN THỰC TẾ (FINANCIAL PLAN)")
    add_body_paragraph(doc, "Để đảm bảo tính khả thi cao nhất của một dự án sinh viên khởi nghiệp, Ohno PMS áp dụng chiến lược tài chính thực tế, giảm thiểu rủi ro đốt vốn vô ích:")
    
    add_body_paragraph(doc, "Chiến lược Bootstrap (Tự lực tài chính) ở giai đoạn đầu: Trong giai đoạn phát triển MVP và thử nghiệm Beta, nhóm tận dụng nguồn lực kỹ thuật tự có của các thành viên để hoàn thiện sản phẩm mà không cần đi vay hay gọi vốn lớn. Văn phòng làm việc được tận dụng tại không gian làm việc chung của trường đại học hoặc làm việc từ xa.", bullet=True)
    add_body_paragraph(doc, "Kế hoạch gọi vốn Pre-Seed có điều kiện: Khi sản phẩm chứng minh được tính hiệu quả qua giai đoạn Beta Test (có từ 5–10 khách sạn trả phí đầu tiên), nhóm sẽ tiến hành gọi vốn vòng Pre-Seed trị giá khoảng 300,000,000 – 500,000,000 VNĐ từ các nhà đầu tư thiên thần hoặc quỹ khởi nghiệp của trường đại học đổi lấy 10% cổ phần.", bullet=True)
    
    add_body_paragraph(doc, "Phân bổ nguồn vốn gọi được:")
    add_body_paragraph(doc, "50% (R&D và Công nghệ): Chi trả hạ tầng Cloud Server (AWS/Google Cloud) và thuê thêm 1–2 lập trình viên bán thời gian để hỗ trợ di chuyển hệ thống lên Cloud.", indent=1, bullet=True)
    add_body_paragraph(doc, "30% (Tiếp thị & Bán hàng): Chi phí đi lại thực địa, tổ chức các buổi workshop nhỏ và in ấn tài liệu hướng dẫn chuyển đổi số cho các khách sạn đối tác.", indent=1, bullet=True)
    add_body_paragraph(doc, "20% (Vận hành & Hỗ trợ kỹ thuật): Chi phí đăng ký kinh doanh, pháp lý thành lập doanh nghiệp khoa học công nghệ và hỗ trợ khách hàng 24/7.", indent=1, bullet=True)

    # 11. EARLY TRACTION & VALIDATION
    add_heading_1(doc, "11. KẾT QUẢ ĐẠT ĐƯỢC BAN ĐẦU & VALIDATION (EARLY TRACTION)")
    add_body_paragraph(doc, "Mặc dù đang ở giai đoạn hoàn thiện sản phẩm đồ án, Ohno PMS đã chủ động thực hiện các bước kiểm chứng thực tế nhằm chứng minh tính khả thi của giải pháp:")
    
    add_body_paragraph(doc, "Kiểm thử hiệu năng dữ liệu: Nhóm đã mô phỏng đặt phòng hàng loạt (Batch Booking) với cơ sở dữ liệu lên tới 10,000 bản ghi. Thuật toán Overlap Prevention hoạt động hoàn hảo dưới cơ chế DB Transaction, ngăn chặn thành công 100% các yêu cầu trùng lịch đặt phòng và rollback dữ liệu sạch sẽ khi giả lập mất điện giữa chừng.", bullet=True)
    add_body_paragraph(doc, "Đánh giá UX thực tế: Tiến hành cho 5 lễ tân khách sạn tại khu vực thử nghiệm thao tác thử trên bản demo Desktop. Kết quả: 5/5 lễ tân phản hồi giao diện Timeline Gantt cực kỳ dễ hiểu, luồng tự động focus Số điện thoại giúp họ tiết kiệm hơn 25 giây cho mỗi lần check-in khách cũ.", bullet=True)
    add_body_paragraph(doc, "Số liệu tối ưu hóa thời gian: Thời gian thực hiện thanh toán và in hóa đơn tổng (Master Bill) cho đoàn khách 15 phòng giảm từ trung bình 32 phút xuống còn 4.5 phút trong điều kiện kiểm thử giả lập.", bullet=True)

    # 12. KẾT LUẬN & ĐỊNH HƯỚNG TƯƠNG LAI
    add_heading_1(doc, "12. KẾT LUẬN VÀ ĐỊNH HƯỚNG PHÁT TRIỂN (CONCLUSION)")
    add_body_paragraph(doc, "Ohno PMS được xây dựng từ nhu cầu thực tiễn của thị trường lưu trú Việt Nam, giải quyết triệt để các nỗi đau nhức nhối trong vận hành khách sạn 3–4 sao bằng các giải pháp công nghệ thông minh và an toàn như Master Bill và Overlap Prevention. Sản phẩm sở hữu thế mạnh vượt trội về thiết kế giao diện Linear UX thực dụng giúp giảm thiểu tối đa thời gian đào tạo nhân sự lễ tân.")
    add_body_paragraph(doc, "Với chiến lược tiếp cận thị trường tinh gọn, mô hình SaaS tối ưu hóa tỷ suất lợi nhuận (ROI) cho chủ khách sạn và lộ trình công nghệ vững chắc, Ohno PMS không chỉ dừng lại ở quy mô một đồ án môn học xuất sắc mà còn sở hữu đầy đủ tiềm năng thương mại hóa thực tế, đóng góp tích cực vào tiến trình chuyển đổi số ngành du lịch Việt Nam.")

    # 13. TÀI LIỆU THAM KHẢO
    add_heading_1(doc, "13. TÀI LIỆU THAM KHẢO VÀ NGUỒN TRÍCH DẪN (REFERENCES)")
    add_body_paragraph(doc, "Ken Research (2025), Vietnam Hotel and Hospitality Management Software Market Outlook to 2034 - Growth, Trends, and Forecasts.", bullet=True)
    add_body_paragraph(doc, "STR Global / Smith Travel Research, Hospitality Metrics Standard: Understanding ADR, RevPAR, and ALOS.", bullet=True)
    add_body_paragraph(doc, "Tổng cục Du lịch Việt Nam (VNAT), Quy trình nghiệp vụ Lễ tân chuẩn VTOS — Vietnam Tourism Occupational Standards.", bullet=True)
    add_body_paragraph(doc, "ezCloud Tech (Make in Vietnam) — Phân tích hành trình từ Startup công nghệ ngách đến nền tảng quản lý du lịch chiếm thị phần lớn tại Việt Nam.", bullet=True)
    add_body_paragraph(doc, "Y Combinator Startup School, How to Design a B2B SaaS Pitch Deck & Financial Models.", bullet=True)

    # Save document
    output_filename = "BAO_CAO_KHOI_NGHIEP_OHNO_CO_CAI_TIEN.docx"
    doc.save(output_filename)
    print("Export successful: BAO_CAO_KHOI_NGHIEP_OHNO_CO_CAI_TIEN.docx")

if __name__ == "__main__":
    create_report()
