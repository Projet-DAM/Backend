"""
Fix missing closing braces in users.controller.ts
Adds } before decorators that appear after return statements
"""

file_path = 'src/users/users.controller.ts'

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

fixed_lines = []
last_non_empty = ""

for i, line in enumerate(lines):
    stripped = line.strip()
    
    # If this line starts with @ and previous non-empty line was a return statement
    # Add a closing brace
    if stripped.startswith('@') and ('return ' in last_non_empty and last_non_empty.endswith(';')):
        fixed_lines.append('  }\n')
        fixed_lines.append('\n')
    
    fixed_lines.append(line)
    
    if stripped:
        last_non_empty = stripped

# Write back
with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(fixed_lines)

print("✓ Fixed users.controller.ts - added missing closing braces")
print("Run 'npm run build' to check for remaining errors")
