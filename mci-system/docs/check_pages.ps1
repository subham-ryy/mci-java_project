$w = New-Object -ComObject Word.Application
$w.Visible = $false
$d = $w.Documents.Open('D:\java_project\mci-system\docs\MCI_System_Project_Report.docx')
$count = $d.ComputeStatistics(2)
Write-Host "PAGES_RESULT:$count"
$d.Close(0)
$w.Quit()
