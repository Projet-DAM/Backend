"""
Remove duplicate method definitions from users.controller.ts
These are methods that appear after decorators within another method body
"""

file_path = 'src/users/users.controller.ts'

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Patterns to identify duplicate standalone method definitions
# These appear after }) lines and are simpler versions of earlier methods
duplicate_patterns = [
    ('update(@Param', 'update('),
    ('remove(@Param', 'remove('),
    ('linkChild(@Param', 'linkChild('),
    ('getChildren(@Param', 'getChildren('),
    ('createChild(@Param', 'createChild('),
    ('findOne(@Param', 'findOne('),
    ('findAll(@Query', 'findAll('),
]

fixed_lines = []
skip_until_closing = False
skip_depth = 0
i = 0

while i < len(lines):
    line = lines[i]
    stripped = line.strip()
    
    # Check if this is a duplicate method definition
    is_duplicate = False
    if not skip_until_closing:
        for pattern, method in duplicate_patterns:
            if pattern in stripped and stripped.endswith('{'):
                # This looks like a duplicate - skip until we find its closing brace
                is_duplicate = True
                skip_until_closing = True
                skip_depth = 1
                break
    
    if skip_until_closing:
        # Count braces to find the end of this method
        skip_depth += stripped.count('{') - stripped.count('}')
        if skip_depth <= 0:
            skip_until_closing = False
        i += 1
        continue
    
    if not is_duplicate:
        fixed_lines.append(line)
    
    i += 1

# Write back
with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(fixed_lines)

print("✓ Removed duplicate method definitions from users.controller.ts")
print("Run 'npm run build' to check for remaining errors")
