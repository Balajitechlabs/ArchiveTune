#!/usr/bin/env python3
"""
BTL Music Identity Setup Script
Applies Option A (Architect Recommended):
- Changes applicationId to "com.btl.music"
- Updates app branding to "BTL Music"
- Updates shortcuts target packages
- Preserves internal code packages for seamless upstream updates
"""

import os
import re
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent

def replace_in_file(file_path: Path, pattern: str, replacement: str) -> bool:
    if not file_path.exists():
        print(f"[-] File not found: {file_path}")
        return False

    content = file_path.read_text(encoding="utf-8")
    new_content, count = re.subn(pattern, replacement, content)

    if count > 0:
        file_path.write_text(new_content, encoding="utf-8")
        print(f"[+] Updated ({count} match{'es' if count > 1 else ''}): {file_path.relative_to(PROJECT_ROOT)}")
        return True
    else:
        print(f"[.] No changes needed: {file_path.relative_to(PROJECT_ROOT)}")
        return False

def main():
    print("=" * 60)
    print("Setting up BTL Music Identity (Option A)")
    print(f"Target Project: {PROJECT_ROOT}")
    print("=" * 60)

    changes = 0

    # 1. Update applicationId in app/build.gradle.kts
    build_gradle = PROJECT_ROOT / "app" / "build.gradle.kts"
    if replace_in_file(
        build_gradle,
        r'applicationId\s*=\s*"moe\.rukamori\.archivetune"',
        'applicationId = "com.btl.music"'
    ):
        changes += 1

    # 2. Update app_name in res/values/app_name.xml (Main)
    main_app_name = PROJECT_ROOT / "app" / "src" / "main" / "res" / "values" / "app_name.xml"
    if replace_in_file(
        main_app_name,
        r'>ArchiveTune<',
        '>BTL Music<'
    ):
        changes += 1

    # 3. Update app_name in res/values/app_name.xml (Debug)
    debug_app_name = PROJECT_ROOT / "app" / "src" / "debug" / "res" / "values" / "app_name.xml"
    if replace_in_file(
        debug_app_name,
        r'>ArchiveTune Debug<',
        '>BTL Music Debug<'
    ):
        changes += 1

    # 4. Update app_name in res/values/app_name.xml (Nightly)
    nightly_app_name = PROJECT_ROOT / "app" / "src" / "nightly" / "res" / "values" / "app_name.xml"
    if replace_in_file(
        nightly_app_name,
        r'>ArchiveTune Nightly<',
        '>BTL Music Nightly<'
    ):
        changes += 1

    # 5. Update app_name in res/values-vi/app_name.xml (Nightly Vietnamese locale)
    nightly_vi_app_name = PROJECT_ROOT / "app" / "src" / "nightly" / "res" / "values-vi" / "app_name.xml"
    if replace_in_file(
        nightly_vi_app_name,
        r'>ArchiveTune Nightly<',
        '>BTL Music Nightly<'
    ):
        changes += 1

    # 6. Update shortcuts targetPackage (Main)
    main_shortcuts = PROJECT_ROOT / "app" / "src" / "main" / "res" / "xml-v25" / "shortcuts.xml"
    if replace_in_file(
        main_shortcuts,
        r'android:targetPackage="moe\.rukamori\.archivetune"',
        'android:targetPackage="com.btl.music"'
    ):
        changes += 1

    # 7. Update shortcuts targetPackage (Debug)
    debug_shortcuts = PROJECT_ROOT / "app" / "src" / "debug" / "res" / "xml-v25" / "shortcuts.xml"
    if replace_in_file(
        debug_shortcuts,
        r'android:targetPackage="moe\.rukamori\.archivetune\.debug"',
        'android:targetPackage="com.btl.music.debug"'
    ):
        changes += 1

    print("=" * 60)
    print(f"Summary: Applied identity changes across {changes} files.")
    print("applicationId: com.btl.music")
    print("App Name:      BTL Music")
    print("=" * 60)

if __name__ == "__main__":
    main()
