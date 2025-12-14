# Script to fix missing closing braces in users.controller.ts
$file = "src/users/users.controller.ts"
$content = Get-Content $file -Raw

# The pattern: methods are missing closing braces before the next set of decorators
# We need to add } before lines that start with decorators after a return statement

# Read line by line
$lines = Get-Content $file
$fixed = @()
$inMethod = $false
$lastNonEmptyLine = ""

for ($i = 0; $i < $lines.Count; $i++) {
    $line = $lines[$i]
    $trimmed = $line.Trim()
    
    # Check if this line starts with @ (decorator) and previous line was a return or closing brace
    if ($trimmed -match '^@' -and $lastNonEmptyLine -match '(return .+;|^\})$') {
        # Add closing brace before this decorator
        $fixed += "  }"
        $fixed += ""
    }
    
    $fixed += $line
    
    if ($trimmed -ne "") {
        $lastNonEmptyLine = $trimmed
    }
}

# Write back
$fixed | Set-Content $file -Encoding UTF8

Write-Host "Fixed users.controller.ts - added missing closing braces"
Write-Host "Please run 'npm run build' to check for remaining errors"
