# Script tự động phân tách 126 file thành 19 commit riêng biệt
# Cấu hình ngày trong tháng 4 và tháng 5 năm 2026 để tô xanh biểu đồ Github

# Root: Đảm bảo git đang ở thư mục gốc chứa .git
cd D:\HK2-2026\PTUD\Hotel_FH

# Group 1: Ngày 06/04/2026 - .gitignore và tài liệu mô tả đặc tả UML
Write-Output "Committing Group 1..."
git add .gitignore Quan-ly-khach-san-ohno/DOC_DAC_TA_UML.docx Quan-ly-khach-san-ohno/DOC_DAC_TA_UML.md
$env:GIT_AUTHOR_DATE="2026-04-06T10:00:00"; $env:GIT_COMMITTER_DATE="2026-04-06T10:00:00"
git commit -m "docs: initialize UML specification and requirement analysis documents"

# Group 2: Ngày 08/04/2026 - Bản vẽ Use Case tổng quan
Write-Output "Committing Group 2..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/use_case_overall.png Quan-ly-khach-san-ohno/img/uml_diagrams/plantuml_src/use_case_overall.puml
$env:GIT_AUTHOR_DATE="2026-04-08T11:00:00"; $env:GIT_COMMITTER_DATE="2026-04-08T11:00:00"
git commit -m "docs(uml): add system-wide use case diagrams and plantuml sources"

# Group 3: Ngày 10/04/2026 - Biểu đồ lớp (Class Diagram)
Write-Output "Committing Group 3..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/class_diagram.png Quan-ly-khach-san-ohno/img/uml_diagrams/plantuml_src/class_diagram.puml
$env:GIT_AUTHOR_DATE="2026-04-10T14:30:00"; $env:GIT_COMMITTER_DATE="2026-04-10T14:30:00"
git commit -m "docs(uml): add class diagram and structural modeling sources"

# Group 4: Ngày 13/04/2026 - Sequence Diagram UC 3.1 - 3.5
Write-Output "Committing Group 4..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_1.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_2.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_3.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_4.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_5.png
$env:GIT_AUTHOR_DATE="2026-04-13T15:20:00"; $env:GIT_COMMITTER_DATE="2026-04-13T15:20:00"
git commit -m "docs(uml): add sequence diagrams for reservation and check-in use cases (UC 3.1 - 3.5)"

# Group 5: Ngày 15/04/2026 - Sequence Diagram UC 3.6 - 3.10
Write-Output "Committing Group 5..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_6.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_7.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_8.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_9.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_10.png
$env:GIT_AUTHOR_DATE="2026-04-15T16:10:00"; $env:GIT_COMMITTER_DATE="2026-04-15T16:10:00"
git commit -m "docs(uml): add sequence diagrams for service booking use cases (UC 3.6 - 3.10)"

# Group 6: Ngày 17/04/2026 - Sequence Diagram UC 3.11 - 3.15
Write-Output "Committing Group 6..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_11.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_12.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_13.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_14.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_15.png
$env:GIT_AUTHOR_DATE="2026-04-17T09:45:00"; $env:GIT_COMMITTER_DATE="2026-04-17T09:45:00"
git commit -m "docs(uml): add sequence diagrams for invoice and checkout use cases (UC 3.11 - 3.15)"

# Group 7: Ngày 20/04/2026 - Sequence Diagram UC 3.16 - 3.20
Write-Output "Committing Group 7..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_16.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_17.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_18.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_19.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_20.png
$env:GIT_AUTHOR_DATE="2026-04-20T10:30:00"; $env:GIT_COMMITTER_DATE="2026-04-20T10:30:00"
git commit -m "docs(uml): add sequence diagrams for employee management use cases (UC 3.16 - 3.20)"

# Group 8: Ngày 22/04/2026 - Sequence Diagram UC 3.21 - 3.25
Write-Output "Committing Group 8..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_21.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_22.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_23.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_24.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_25.png
$env:GIT_AUTHOR_DATE="2026-04-22T13:15:00"; $env:GIT_COMMITTER_DATE="2026-04-22T13:15:00"
git commit -m "docs(uml): add sequence diagrams for room management use cases (UC 3.21 - 3.25)"

# Group 9: Ngày 24/04/2026 - Sequence Diagram UC 3.26 - 3.30
Write-Output "Committing Group 9..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_26.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_27.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_28.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_29.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_30.png
$env:GIT_AUTHOR_DATE="2026-04-24T14:40:00"; $env:GIT_COMMITTER_DATE="2026-04-24T14:40:00"
git commit -m "docs(uml): add sequence diagrams for statistics and system settings (UC 3.26 - 3.30)"

# Group 10: Ngày 27/04/2026 - Sequence Diagram UC 3.31 - 3.33
Write-Output "Committing Group 10..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_31.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_32.png Quan-ly-khach-san-ohno/img/uml_diagrams/sequence_uc_3_33.png
$env:GIT_AUTHOR_DATE="2026-04-27T16:00:00"; $env:GIT_COMMITTER_DATE="2026-04-27T16:00:00"
git commit -m "docs(uml): add remaining sequence diagrams for notifications and logs (UC 3.31 - 3.33)"

# Group 11: Ngày 29/04/2026 - Activity Diagram UC 3.2 - 3.7
Write-Output "Committing Group 11..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_2_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_3_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_4_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_5_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_6_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_7_plantuml.png
$env:GIT_AUTHOR_DATE="2026-04-29T10:15:00"; $env:GIT_COMMITTER_DATE="2026-04-29T10:15:00"
git commit -m "docs(uml): add activity diagrams for booking and scheduling flows (UC 3.2 - 3.7)"

# Group 12: Ngày 04/05/2026 - Activity Diagram UC 3.8 - 3.12
Write-Output "Committing Group 12..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_8_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_9_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_10_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_11_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_12_plantuml.png
$env:GIT_AUTHOR_DATE="2026-05-04T11:20:00"; $env:GIT_COMMITTER_DATE="2026-05-04T11:20:00"
git commit -m "docs(uml): add activity diagrams for service management flows (UC 3.8 - 3.12)"

# Group 13: Ngày 06/05/2026 - Activity Diagram UC 3.13 - 3.18
Write-Output "Committing Group 13..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_13_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_14_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_15_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_16_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_17_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_18_plantuml.png
$env:GIT_AUTHOR_DATE="2026-05-06T13:45:00"; $env:GIT_COMMITTER_DATE="2026-05-06T13:45:00"
git commit -m "docs(uml): add activity diagrams for check-out and payment flows (UC 3.13 - 3.18)"

# Group 14: Ngày 08/05/2026 - Activity Diagram UC 3.19 - 3.24
Write-Output "Committing Group 14..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_19_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_20_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_21_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_22_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_23_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_24_plantuml.png
$env:GIT_AUTHOR_DATE="2026-05-08T14:30:00"; $env:GIT_COMMITTER_DATE="2026-05-08T14:30:00"
git commit -m "docs(uml): add activity diagrams for staff registration and shift scheduling (UC 3.19 - 3.24)"

# Group 15: Ngày 11/05/2026 - Activity Diagram UC 3.25 - 3.30
Write-Output "Committing Group 15..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_25_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_26_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_27_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_28_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_29_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_30_plantuml.png
$env:GIT_AUTHOR_DATE="2026-05-11T15:10:00"; $env:GIT_COMMITTER_DATE="2026-05-11T15:10:00"
git commit -m "docs(uml): add activity diagrams for database backups and system maintenance (UC 3.25 - 3.30)"

# Group 16: Ngày 13/05/2026 - Activity Diagram UC 3.31 - 3.33 & Leftover pngs
Write-Output "Committing Group 16..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_31_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_32_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_33_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_1a_plantuml.png Quan-ly-khach-san-ohno/img/uml_diagrams/activity_uc_3_1b_plantuml.png
$env:GIT_AUTHOR_DATE="2026-05-13T16:05:00"; $env:GIT_COMMITTER_DATE="2026-05-13T16:05:00"
git commit -m "docs(uml): add remaining activity diagrams for system audit trails (UC 3.31 - 3.33)"

# Group 17: Ngày 15/05/2026 - PlantUML source codes recursively
Write-Output "Committing Group 17..."
git add Quan-ly-khach-san-ohno/img/uml_diagrams/plantuml_src/
$env:GIT_AUTHOR_DATE="2026-05-15T10:30:00"; $env:GIT_COMMITTER_DATE="2026-05-15T10:30:00"
git commit -m "docs(uml): add source files for activity diagram specifications"

# Group 18: Ngày 18/05/2026 - Báo cáo cải tiến & Phân quyền hệ thống
Write-Output "Committing Group 18..."
git add Quan-ly-khach-san-ohno/HOTEL_REPORT_IMPROVEMENTS.md Quan-ly-khach-san-ohno/ROLE_PERMISSIONS.md
$env:GIT_AUTHOR_DATE="2026-05-18T13:20:00"; $env:GIT_COMMITTER_DATE="2026-05-18T13:20:00"
git commit -m "docs: document role-based access control and system optimization suggestions"

# Group 19: Ngày 20/05/2026 - Build tools, Deploy scripts & Scratch files
Write-Output "Committing Group 19..."
git add Quan-ly-khach-san-ohno/HotelOhno.bat Quan-ly-khach-san-ohno/pack_jar.bat Quan-ly-khach-san-ohno/scratch/ Quan-ly-khach-san-ohno/build_result.txt
$env:GIT_AUTHOR_DATE="2026-05-20T14:45:00"; $env:GIT_COMMITTER_DATE="2026-05-20T14:45:00"
git commit -m "chore: add system build tools, deployment scripts, and test modules"

# Xóa các biến môi trường
Remove-Item Env:\GIT_AUTHOR_DATE; Remove-Item Env:\GIT_COMMITTER_DATE
Write-Output "Successfully committed all groups!"
