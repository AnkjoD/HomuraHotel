$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $PSScriptRoot 'build.ps1')
$databaseName = 'HomuraHotel_Test_' + (Get-Date -Format 'yyyyMMdd_HHmmss') + '_' + [Guid]::NewGuid().ToString('N').Substring(0,6)
$temporarySql = [IO.Path]::GetTempFileName()
$oldUrl=$env:DB_URL; $oldUser=$env:DB_USER; $oldPassword=$env:DB_PASSWORD
try {
    $sql="IF DB_ID('$databaseName') IS NOT NULL THROW 50000, 'Test database already exists', 1;`nGO`n"
    $sql += [IO.File]::ReadAllText((Join-Path $projectRoot 'database/complete_database.sql')).Replace('HotelMs',$databaseName)
    $sql += @'

INSERT INTO NhanVien(maNhanVien,hoTen,sdt,chucVu) VALUES('NV003',N'Nhan vien test SQL','0900000003',N'Le tan');
INSERT INTO Phong(maPhong,tang,maHuongNhin,maLoaiPhong,trangThai) VALUES
('P104',1,'HN01','LP002','AVAILABLE'),('P201',2,'HN03','LP002','AVAILABLE'),
('P204',2,'HN03','LP002','AVAILABLE'),('P303',3,'HN06','LP004','AVAILABLE');
GO
'@
    [IO.File]::WriteAllText($temporarySql,$sql,[Text.UTF8Encoding]::new($false))
    & docker cp $temporarySql sql_server_container:/tmp/homura-isolated-test.sql
    if($LASTEXITCODE -ne 0){throw 'Cannot copy initialization SQL'}
    & docker exec sql_server_container sh -c 'SQLCMDPASSWORD="$MSSQL_SA_PASSWORD" /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -i /tmp/homura-isolated-test.sql'
    if($LASTEXITCODE -ne 0){throw 'Initialization failed'}
    $env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=$databaseName;encrypt=true;trustServerCertificate=true"
    $env:DB_USER='sa'
    $env:DB_PASSWORD=(& docker exec sql_server_container printenv MSSQL_SA_PASSWORD).Trim()
    foreach($test in @('test.HomuraBusinessTest','test.HomuraSqlReadTest')) {
        $arguments=@('-Dfile.encoding=UTF-8','-ea','-cp',"$projectRoot/bin;$projectRoot/lib/*",$test)
        if($test -eq 'test.HomuraBusinessTest'){$arguments+='--sql'}
        & java @arguments
        if($LASTEXITCODE -ne 0){throw "SQL test failed: $test"}
    }
    Write-Host "SQL tests passed. Database retained for inspection: $databaseName"
}finally{
    $env:DB_URL=$oldUrl;$env:DB_USER=$oldUser;$env:DB_PASSWORD=$oldPassword
    Remove-Item -LiteralPath $temporarySql -ErrorAction SilentlyContinue
}
