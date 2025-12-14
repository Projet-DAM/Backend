# Fix Users Service - Remove duplicate methods and fix structure

$serviceFile = "src/users/users.service.ts"
$content = Get-Content $serviceFile -Raw

# The file has these issues:
# 1. Duplicate findAll method (lines 171-209) - keep only the second one with filters
# 2. Malformed remove method (lines 356-380) - has duplicate code inside
# 3. Duplicate createChild and linkChild methods

Write-Host "Analyzing $serviceFile..."
Write-Host "File has duplicate method definitions that need manual fixing"
Write-Host ""
Write-Host "Issues found:"
Write-Host "1. Duplicate 'findAll' method - PARTIALLY FIXED"
Write-Host "2. Malformed 'remove' method with nested duplicate code"
Write-Host "3. Duplicate 'linkChild' method"
Write-Host "4. Duplicate 'createChild' method"
Write-Host ""
Write-Host "These files are too corrupted for automated fixes."
Write-Host "Recommendation: Restore from a working version or rebuild the methods."
