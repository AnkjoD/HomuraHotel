@echo off
:: Chạy ứng dụng dưới dạng cửa sổ ẩn console (dùng javaw)
start javaw -cp "bin;lib/*;mssql-jdbc-12.10.0.jre11.jar" main.Main
exit
