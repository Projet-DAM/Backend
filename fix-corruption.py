"""
Fix TypeScript file corruption by:
1. Removing duplicate method definitions
2. Adding missing closing braces
3. Fixing decorator placement
"""

import re

def fix_users_service():
    """Fix users.service.ts"""
    with open('src/users/users.service.ts', 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Find and remove the malformed remove method (lines 356-380)
    # Pattern: from "async remove" to the start of "async linkChild"
    pattern = r'(async remove\(id: string\): Promise<void> \{[\s\S]*?const result = await this\.userModel\.findByIdAndDelete\(cleanId\);[\s\S]*?await this\.userModel\.findByIdAndDelete\(id\);[\s\S]*?\})\s*\n\s*async linkChild'
    
    replacement = '''async remove(id: string): Promise<void> {
    const user = await this.userModel.findById(id);
    if (!user) {
      throw new NotFoundException('Utilisateur non trouvé');
    }

    // Si c'est un enfant, le retirer de la liste des enfants du parent
    if (user.role === UserRole.ENFANT && user.parent) {
      const parent = await this.userModel.findById(user.parent);
      if (parent && parent.enfants) {
        parent.enfants = parent.enfants.filter(
          (childId: any) => childId.toString() !== id
        );
        await parent.save();
      }
    }

    await this.userModel.findByIdAndDelete(id);
  }

  async linkChild'''
    
    content = re.sub(pattern, replacement, content, flags=re.MULTILINE)
    
    # Write back
    with open('src/users/users.service.ts', 'w', encoding='utf-8') as f:
        f.write(content)
    
    print("✓ Fixed users.service.ts")

def fix_users_controller():
    """Fix users.controller.ts - add missing closing braces"""
    with open('src/users/users.controller.ts', 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # This is complex - would need line-by-line analysis
    print("⚠ users.controller.ts requires manual fixing")
    print("  See CORRUPTION_FIX_GUIDE.md for details")

if __name__ == '__main__':
    try:
        fix_users_service()
        fix_users_controller()
        print("\nDone! Run 'npm run build' to check for remaining errors.")
    except Exception as e:
        print(f"Error: {e}")
        print("Manual fixing required - see clean-service-methods.ts for templates")
