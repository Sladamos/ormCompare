$ContainerName = "pg-orm"
$User = "postgres"
$Db = "postgres"

Write-Host "`n=== STATUS TABEL W BAZIE PG-ORM ===" -ForegroundColor Cyan

$Tables = @("producer", "product", "review")

foreach ($Table in $Tables) {
    $Count = docker exec -i $ContainerName psql -U $User -d $Db -t -c "SELECT count(*) FROM $Table;" 2>$null

    if ($LASTEXITCODE -eq 0) {
        $FormattedCount = $Count.Trim()
        Write-Host "Tabela " -NoNewline
        Write-Host "$($Table.PadRight(10))" -ForegroundColor Yellow -NoNewline
        Write-Host ": $FormattedCount rekordow" -ForegroundColor Green
    } else {
        Write-Host "Błąd: Nie można pobrać danych z tabeli $Table" -ForegroundColor Red
    }
}

Write-Host "====================================`n" -ForegroundColor Cyan