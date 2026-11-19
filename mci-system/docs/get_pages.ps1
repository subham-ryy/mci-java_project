$w = New-Object -ComObject Word.Application
$w.Visible = $false
$d = $w.Documents.Open('D:\java_project\mci-system\docs\MCI_System_Project_Report.docx')
$res = @()
foreach ($p in $d.Paragraphs) {
    $t = $p.Range.Text.Trim()
    if ($t -match '^CHAPTER \d+:' -or $t -eq 'REFERENCES' -or $t -match '^APPENDIX [A-E]:') {
        $page = $p.Range.Information(3)
        $res += "$t|$page"
    }
}
$d.Close(0)
$w.Quit()
$res | Out-File -FilePath 'D:\java_project\mci-system\docs\chapter_pages.txt' -Encoding utf8
Write-Host "Done"
