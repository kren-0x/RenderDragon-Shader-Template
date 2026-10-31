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
import json

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MATERIALS_DIR = os.path.join(ROOT_DIR, "materials")
INCLUDE_DIR = os.path.join(ROOT_DIR, "include")
BUILD_DIR = os.path.join(ROOT_DIR, "build")
TOOLS_DIR = os.path.join(ROOT_DIR, "tools")
LIB_DIR = os.path.join(TOOLS_DIR, "lib")
BASE_MATERIALS_DIR = os.path.join(TOOLS_DIR, "materials")

DEV_RELEASE_URL = "https://github.com/devendrn/newb-shader/releases/download/dev/"
SERIALIZED_MATERIALS_REPO = "mcpebd/serialized-materials"
SERIALIZED_MATERIALS_RELEASES_URL = f"https://github.com/{SERIALIZED_MATERIALS_REPO}/releases"
DEFAULT_SERIALIZED_VERSION = "v26.60"

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


def get_available_materials():
    if not os.path.isdir(MATERIALS_DIR):
        return []
    return [
        d for d in sorted(os.listdir(MATERIALS_DIR))
        if os.path.isdir(os.path.join(MATERIALS_DIR, d)) and not d.startswith((".", "_"))
    ]


def get_latest_serialized_version():
    """Detect the latest release tag from mcpebd/serialized-materials."""
    try:
        req = urllib.request.Request(
            f"{SERIALIZED_MATERIALS_RELEASES_URL}/latest",
            headers={"User-Agent": "Mozilla/5.0"}
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            final_url = resp.geturl()
            tag = final_url.rstrip("/").split("/")[-1]
            if tag and tag != "latest":
                return tag
    except Exception:
        pass

    try:
        req = urllib.request.Request(
            f"https://api.github.com/repos/{SERIALIZED_MATERIALS_REPO}/releases/latest",
            headers={"User-Agent": "Mozilla/5.0", "Accept": "application/vnd.github.v3+json"}
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            tag = data.get("tag_name")
            if tag:
                return tag
    except Exception:
        pass

    return DEFAULT_SERIALIZED_VERSION


def normalize_version_tag(version):
    if not version or version.lower() == "latest":
        return get_latest_serialized_version()
    v = version.strip()
    if not v.startswith("v") and v[0].isdigit():
        return f"v{v}"
    return v


def get_current_installed_version():
    ver_file = os.path.join(BASE_MATERIALS_DIR, ".version")
    if os.path.exists(ver_file):
        try:
            with open(ver_file, "r", encoding="utf-8") as f:
                return f.read().strip()
        except Exception:
            pass
    return None


def are_base_materials_present():
    if not os.path.isdir(BASE_MATERIALS_DIR):
        return False
    required = [f"{m}.material.json" for m in get_available_materials()]
    if not required:
        return os.path.exists(os.path.join(BASE_MATERIALS_DIR, "Sky.material.json"))
    return all(os.path.exists(os.path.join(BASE_MATERIALS_DIR, r)) for r in required)


def setup_materials(version=None, force=False):
    tag = normalize_version_tag(version)
    os.makedirs(BASE_MATERIALS_DIR, exist_ok=True)

    current_ver = get_current_installed_version()
    if not force and are_base_materials_present() and (current_ver == tag or version is None):
        return True

    zip_url = f"https://github.com/{SERIALIZED_MATERIALS_REPO}/archive/refs/tags/{tag}.zip"
    zip_path = os.path.join(TOOLS_DIR, f"serialized-{tag}.zip")

    print(f" {c_yellow('Downloading')} serialized materials {c_cyan(tag)} from {SERIALIZED_MATERIALS_REPO}...")

    try:
        req = urllib.request.Request(zip_url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=30) as resp, open(zip_path, "wb") as out_f:
            shutil.copyfileobj(resp, out_f)
    except urllib.error.HTTPError as e:
        if e.code == 404:
            print(f" {c_red('Error:')} Version '{tag}' not found on {SERIALIZED_MATERIALS_RELEASES_URL}")
            print(f" Available releases: {SERIALIZED_MATERIALS_RELEASES_URL}")
        else:
            print(f" {c_red('Download failed:')} HTTP {e.code} {e.reason}")
        if os.path.exists(zip_path):
            os.remove(zip_path)
        return False
    except Exception as e:
        print(f" {c_red('Download failed:')} {e}")
        if os.path.exists(zip_path):
            os.remove(zip_path)
        return False

    try:
        extracted = 0

        with zipfile.ZipFile(zip_path, "r") as zf:
            for member in zf.infolist():
                if member.is_dir():
                    continue
                filename = os.path.basename(member.filename)
                if filename.endswith(".material.json"):
                    dest_file = os.path.join(BASE_MATERIALS_DIR, filename)
                    with zf.open(member) as src_f, open(dest_file, "wb") as dst_f:
                        dst_f.write(src_f.read())
                    extracted += 1

        ver_file = os.path.join(BASE_MATERIALS_DIR, ".version")
        with open(ver_file, "w", encoding="utf-8") as f:
            f.write(tag)

        print(f" {c_green('Extracted')} {extracted} base materials ({tag}) into {c_bold('tools/materials')}")
        return True
    except Exception as e:
        print(f" {c_red('Extraction failed:')} {e}")
        return False
    finally:
        if os.path.exists(zip_path):
            try:
                os.remove(zip_path)
            except Exception:
                pass


def check_and_apply_termux_fix():
    os.makedirs(LIB_DIR, exist_ok=True)
    dest_so = os.path.join(LIB_DIR, "libc++_shared.so")

    termux_prefix = os.environ.get("PREFIX", "/data/data/com.termux/files/usr")
    termux_usr_lib = os.path.join(termux_prefix, "lib")
    local_lib = os.path.expanduser("~/.local/lib")

    if not os.path.exists(dest_so):
        candidates = [
            os.path.join(termux_usr_lib, "libc++_shared.so"),
            "/data/data/com.termux/files/usr/lib/libc++_shared.so",
            os.path.join(local_lib, "libc++_shared.so"),
        ]
        for src in candidates:
            if os.path.exists(src):
                try:
                    shutil.copyfile(src, dest_so)
                    print(f" {c_green('Applied')} Termux fix: copied libc++_shared.so to {c_bold('tools/lib')}")
                    break
                except Exception:
                    pass

    ld_paths = [LIB_DIR]
    if os.path.isdir(local_lib):
        ld_paths.append(local_lib)

    current_ld = os.environ.get("LD_LIBRARY_PATH", "")
    if current_ld:
        for p in current_ld.split(":"):
            if p and p not in ld_paths and not p.startswith(termux_usr_lib) and not p.startswith("/data/data/com.termux/files/usr/lib"):
                ld_paths.append(p)
    os.environ["LD_LIBRARY_PATH"] = ":".join(ld_paths)


def ensure_setup(version=None, force=False):
    check_and_apply_termux_fix()
    shaderc_path, shaderc_url = get_shaderc_path()
    os.makedirs(TOOLS_DIR, exist_ok=True)
    os.makedirs(BASE_MATERIALS_DIR, exist_ok=True)

    if not os.path.exists(shaderc_path):
        print(f" {c_yellow('Downloading')} shaderc compiler...")
        urllib.request.urlretrieve(shaderc_url, shaderc_path)
        os.chmod(shaderc_path, 0o755)
        print(f" {c_green('Downloaded')} shaderc compiler.")

    if force or not are_base_materials_present():
        setup_materials(version=version, force=force)

    return shaderc_path


def build_materials(target_platform="windows", material_name=None):
    t0 = time.time()
    check_and_apply_termux_fix()
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
        err_msg = str(e)
        print(f"\n {c_red('✗ Build failed:')} {err_msg}")
        if "libc++_shared.so" in err_msg or "CANNOT LINK EXECUTABLE" in err_msg:
            print(f"\n {c_yellow('Hint:')} Termux requires libc++_shared.so for shaderc.")
            print(f" Run: {c_cyan('pkg install libc++')} in Termux.")
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
  build.bat setup [version]      Download base materials (default: latest release)
  build.bat clean                Delete build/ artifacts

{c_bold("Platforms:")}
  windows, android, ios, multiplatform

{c_bold("Examples:")}
  build.bat
  build.bat windows
  build.bat android
  build.bat RenderChunk
  build.bat RenderChunk android
  build.bat setup
  build.bat setup v26.50
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
            current = get_current_installed_version() or "none"
            latest = get_latest_serialized_version()
            print()
            print(f"  {c_bold('Base Materials Setup')} ({SERIALIZED_MATERIALS_REPO})")
            print(f"  Current installed: {c_cyan(current)} | Latest available: {c_green(latest)}")
            try:
                ver_input = input(f"  {c_bold('Enter version')} [{latest}]: ").strip()
            except (KeyboardInterrupt, EOFError):
                print()
                continue
            target_ver = ver_input if ver_input else latest
            ensure_setup(version=target_ver, force=True)
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
        ver = args[1] if len(args) > 1 else None
        ensure_setup(version=ver, force=True)
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
