"""
Comprehensive fix for users.controller.ts
Removes all duplicate method definitions that appear after decorators
"""

file_path = 'src/users/users.controller.ts'

with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

# Track which methods we've seen
seen_methods = set()
fixed_lines = []
skip_mode = False
skip_depth = 0
i = 0

while i < len(lines):
    line = lines[i]
    stripped = line.strip()
    
    # If we're in skip mode, count braces
    if skip_mode:
        skip_depth += stripped.count('{') - stripped.count('}')
        if skip_depth <= 0:
            skip_mode = False
        i += 1
        continue
    
    # Check if this line is a duplicate method definition
    # Pattern: method_name(@Decorator...) {
    is_duplicate = False
    
    # List of methods that might be duplicated
    duplicate_patterns = [
        'findOne(@Param',
        'findAll(@Query',
        'update(@Param',
        'remove(@Param',
        'linkChild(@Param',
        'getChildren(@Param',
        'getChildrenCompact(@Req',
        'createChild(@Param',
        'getChildData(@Param',
        'uploadPhoto(',
    ]
    
    for pattern in duplicate_patterns:
        if pattern in stripped and '{' in stripped:
            # Check if previous non-empty line was a closing decorator })
            # Look back to find last non-empty line
            j = i - 1
            while j >= 0 and not lines[j].strip():
                j -= 1
            
            if j >= 0:
                prev_line = lines[j].strip()
                # If previous line is }) or just }, this is likely a duplicate
                if prev_line in ['})','}',' })', '})', '  })']:
                    print(f"Found duplicate method at line {i+1}: {stripped[:50]}")
                    is_duplicate = True
                    skip_mode = True
                    skip_depth = 1
                    break
    
    if not is_duplicate:
        fixed_lines.append(line)
    
    i += 1

# Write back
with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(fixed_lines)

print("\n✓ Removed all duplicate method definitions from users.controller.ts")
print("Run 'npm run build' to check for remaining errors")
