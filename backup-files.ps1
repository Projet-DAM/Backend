# Backup corrupted files
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
Copy-Item "src/users/users.controller.ts" "src/users/users.controller.ts.backup_$timestamp"
Copy-Item "src/users/users.service.ts" "src/users/users.service.ts.backup_$timestamp"

Write-Host "Backup created with timestamp: $timestamp"
Write-Host "Backups saved to:"
Write-Host "  - src/users/users.controller.ts.backup_$timestamp"
Write-Host "  - src/users/users.service.ts.backup_$timestamp"
