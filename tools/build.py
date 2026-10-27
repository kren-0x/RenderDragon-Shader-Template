#!/usr/bin/env python3
"""
RenderDragon Shader Builder
Terminal build tool with colored interactive menu and flexible CLI.
"""

import os
import sys
import time
import shutil
import platform
import zipfile
import urllib.request

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MATERIALS_DIR = os.path.join(ROOT_DIR, "materials")
INCLUDE_DIR = os.path.join(ROOT_DIR, "include")
BUILD_DIR = os.path.join(ROOT_DIR, "build")
TOOLS_DIR = os.path.join(ROOT_DIR, "tools")
BASE_MATERIALS_DIR = os.path.join(TOOLS_DIR, "materials")

DEV_RELEASE_URL = "https://github.com/devendrn/newb-shader/releases/download/dev/"
MAT_SRC_URL = DEV_RELEASE_URL + "src-materials-1.26.51.zip"

if hasattr(sys.stdout, "reconfigure"):
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
if hasattr(sys.stderr, "reconfigure"):
    try:
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

if os.name == "nt":
    os.system("")

USE_COLOR = sys.stdout.isatty() and not os.environ.get("NO_COLOR")


def style(text, *codes):
    if not USE_COLOR or not codes:
        return text
    return "".join(codes) + str(text) + "\033[0m"


BOLD = "\033[1m"
DIM = "\033[2m"
CYAN = "\033[36m"
GREEN = "\033[32m"
YELLOW = "\033[33m"
RED = "\033[31m"


def c_bold(t): return style(t, BOLD)
def c_cyan(t): return style(t, CYAN)
def c_green(t): return style(t, GREEN)
def c_yellow(t): return style(t, YELLOW)
def c_red(t): return style(t, RED)
def c_dim(t): return style(t, DIM)


def get_shaderc_path():
    system = platform.system()
    arch = platform.machine()
    name = "shaderc"
    if system == "Windows":
        name += "-win-x64.exe"
    elif system == "Darwin":
        name += "-osx-x64"
    elif system in ("Linux", "Android"):
        if arch in ("aarch64", "arm64"):
            name += "-android-arm64"
        elif "arm" in arch:
            name += "-android-arm"
        else:
            name += "-linux-x64"
    else:
        name += "-win-x64.exe"
    return os.path.join(TOOLS_DIR, name if system == "Windows" else "shaderc"), DEV_RELEASE_URL + name


def ensure_setup():
    shaderc_path, shaderc_url = get_shaderc_path()
    os.makedirs(TOOLS_DIR, exist_ok=True)
    os.makedirs(BASE_MATERIALS_DIR, exist_ok=True)

    test_mat = os.path.join(BASE_MATERIALS_DIR, "Sky.material.json")
    if not os.path.exists(shaderc_path):
        print(f" {c_yellow('Downloading')} shaderc compiler...")
        urllib.request.urlretrieve(shaderc_url, shaderc_path)
        os.chmod(shaderc_path, 0o755)
        print(f" {c_green('Downloaded')} shaderc compiler.")

    if not os.path.exists(test_mat):
        print(f" {c_yellow('Downloading')} base material templates...")
        zip_path = os.path.join(TOOLS_DIR, "materials.zip")
        urllib.request.urlretrieve(MAT_SRC_URL, zip_path)
        with zipfile.ZipFile(zip_path, "r") as zf:
            zf.extractall(BASE_MATERIALS_DIR)
        os.remove(zip_path)
        print(f" {c_green('Extracted')} base materials.")

    return shaderc_path


def get_available_materials():
    if not os.path.isdir(MATERIALS_DIR):
        return []
    return [
        d for d in sorted(os.listdir(MATERIALS_DIR))
        if os.path.isdir(os.path.join(MATERIALS_DIR, d)) and not d.startswith((".", "_"))
    ]


def build_materials(target_platform="windows", material_name=None):
    t0 = time.time()
    shaderc_path = ensure_setup()

    out_dir = os.path.join(BUILD_DIR, target_platform)
    os.makedirs(out_dir, exist_ok=True)

    all_materials = get_available_materials()
    mat_patterns = []
    if material_name and material_name.lower() not in ("all", "*"):
        matched = None
        for m in all_materials:
            if m.lower() == material_name.lower():
                matched = m
                break
        if not matched:
            print(f" {c_red('Error:')} Material '{material_name}' not found.")
            print(f" Available: {', '.join(all_materials)}")
            return False
        mat_patterns = [os.path.join(MATERIALS_DIR, matched)]
        print(f" {c_bold('Building')} {c_cyan(matched)} for {c_yellow(target_platform)}...")
    else:
        print(f" {c_bold('Building all')} {c_cyan(str(len(all_materials)))} materials for {c_yellow(target_platform)}...")

    import lazurite.project.project as lp

    try:
        lp.compile(
            project_path=MATERIALS_DIR,
            profiles=[target_platform],
            output_folder=out_dir,
            material_patterns=mat_patterns,
            shaderc_path=shaderc_path,
            defines=[],
        )
        elapsed = time.time() - t0
        rel_out = os.path.relpath(out_dir, ROOT_DIR)
        print(f"\n {c_green('✓ Done')} in {elapsed:.1f}s -> {c_bold(rel_out)}")
        return True
    except Exception as e:
        print(f"\n {c_red('✗ Build failed:')} {e}")
        return False


def clean_build():
    if os.path.exists(BUILD_DIR):
        shutil.rmtree(BUILD_DIR)
        print(f" {c_green('Cleaned')} build/ directory.")
    else:
        print(f" {c_dim('Build directory is already clean.')}")


def print_help():
    default_plat = "windows" if os.name == "nt" else "android"
    print(f"""
{style("RenderDragon Shader Builder", BOLD, CYAN)}

{c_bold("Usage:")}
  build.bat                      Open interactive menu
  build.bat [platform]           Build all materials for target platform
  build.bat [material]           Build single material for {default_plat}
  build.bat [material] [plat]    Build single material for target platform
  build.bat clean                Delete build/ artifacts
  build.bat setup                Check and download compiler if missing

{c_bold("Platforms:")}
  windows, android, ios, multiplatform

{c_bold("Examples:")}
  build.bat
  build.bat windows
  build.bat android
  build.bat RenderChunk
  build.bat RenderChunk android
  build.bat clean
""")


def run_interactive():
    mats = get_available_materials()
    default_plat = "windows" if os.name == "nt" else "android"

    while True:
        print()
        print(f"  {style('RenderDragon Shader Builder', BOLD, CYAN)}")
        print(f"  {style('───────────────────────────', DIM)}")
        print(f"  {c_bold('Platforms:')}")
        print(f"    {c_cyan('1')} windows   {c_cyan('2')} android   {c_cyan('3')} ios   {c_cyan('4')} multiplatform")
        print()
        print(f"  {c_bold('Materials:')}")
        print(f"    {c_cyan('0')}  all materials")

        half = (len(mats) + 1) // 2
        for i in range(half):
            idx1 = i + 1
            name1 = mats[i]
            col1 = f"[{idx1:2d}] {name1:<22}"
            if i + half < len(mats):
                idx2 = i + half + 1
                name2 = mats[i + half]
                col2 = f"[{idx2:2d}] {name2}"
                print(f"    {c_dim(col1)} {c_dim(col2)}")
            else:
                print(f"    {c_dim(col1)}")

        print()
        print(f"  {c_bold('Options:')}")
        print(f"    {c_cyan('c')} clean build/   {c_cyan('s')} setup/check   {c_cyan('q')} quit")
        print()

        try:
            choice = input(f"  {c_bold('Select platform')} [{default_plat}]: ").strip().lower()
        except (KeyboardInterrupt, EOFError):
            print()
            break

        if choice in ("q", "quit", "exit"):
            break
        if choice in ("c", "clean"):
            clean_build()
            continue
        if choice in ("s", "setup"):
            ensure_setup()
            continue

        plat_map = {
            "1": "windows", "2": "android", "3": "ios", "4": "multiplatform",
            "w": "windows", "a": "android", "i": "ios", "m": "multiplatform",
            "windows": "windows", "android": "android", "ios": "ios", "multiplatform": "multiplatform"
        }
        selected_plat = plat_map.get(choice, default_plat if choice == "" else choice)

        try:
            mat_choice = input(f"  {c_bold('Select material')} [all]: ").strip()
        except (KeyboardInterrupt, EOFError):
            print()
            break

        selected_mat = None
        if mat_choice.isdigit():
            idx = int(mat_choice)
            if idx == 0:
                selected_mat = None
            elif 1 <= idx <= len(mats):
                selected_mat = mats[idx - 1]
            else:
                print(f"  {c_red('Invalid material number.')}")
                continue
        elif mat_choice and mat_choice.lower() not in ("all", "*"):
            selected_mat = mat_choice

        print()
        build_materials(selected_plat, selected_mat)

        try:
            again = input(f"\n  {c_dim('Press Enter to continue or q to quit: ')}").strip().lower()
            if again in ("q", "quit", "exit"):
                break
        except (KeyboardInterrupt, EOFError):
            print()
            break


def parse_and_run(args):
    if not args:
        run_interactive()
        return

    first = args[0].lower()
    if first in ("-h", "--help", "help"):
        print_help()
        return
    if first in ("c", "clean", "--clean"):
        clean_build()
        return
    if first in ("s", "setup", "--setup", "init"):
        ensure_setup()
        return
    if first in ("-i", "--interactive", "menu"):
        run_interactive()
        return

    all_materials = {m.lower(): m for m in get_available_materials()}
    target_platform = "windows" if os.name == "nt" else "android"
    target_mat = None

    for arg in args:
        low = arg.lower()
        if low in ("windows", "win"):
            target_platform = "windows"
        elif low in ("android",):
            target_platform = "android"
        elif low in ("ios",):
            target_platform = "ios"
        elif low in ("multiplatform", "all-plat"):
            target_platform = "multiplatform"
        elif low in all_materials:
            target_mat = all_materials[low]
        elif low == "all":
            target_mat = None
        else:
            matched = False
            for m_low, m_orig in all_materials.items():
                if low == m_low or low in m_low:
                    target_mat = m_orig
                    matched = True
                    break
            if not matched:
                print(f" {c_red('Unknown argument:')} '{arg}'")
                print_help()
                return

    build_materials(target_platform, target_mat)


if __name__ == "__main__":
    parse_and_run(sys.argv[1:])
