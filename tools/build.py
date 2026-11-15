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
import subprocess
import struct

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
GLSLANG_VERSION = "16.6.0"
GLSLANG_RELEASE_URL = f"https://github.com/KhronosGroup/glslang/releases/download/{GLSLANG_VERSION}/"

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


def make_stub_so(arch="aarch64"):
    is_64 = arch in ("aarch64", "arm64", "x86_64")
    if is_64:
        elf_hdr = bytearray(64)
        elf_hdr[0:8] = b"\x7fELF\x02\x01\x01\x00"
        struct.pack_into("<HHIQQQIHHHHHH", elf_hdr, 16, 3, 183 if "arm" in arch or "aarch" in arch else 62, 1, 0, 64, 0, 0, 64, 56, 2, 0, 0, 0)
        ph_load = bytearray(56)
        struct.pack_into("<IIQQQQQQ", ph_load, 0, 1, 5, 0, 0, 0, 0x200, 0x200, 0x1000)
        ph_dyn = bytearray(56)
        struct.pack_into("<IIQQQQQQ", ph_dyn, 0, 2, 6, 0x100, 0x100, 0x100, 112, 112, 8)
        dyn_sec = bytearray()
        for tag, val in [(4, 0x170), (5, 0x180), (6, 0x190), (10, 16), (11, 24), (16, 0), (0, 0)]:
            dyn_sec.extend(struct.pack("<QQ", tag, val))
        pad = b"\x00" * (0x100 - (64 + 56 * 2))
        dt_hash = struct.pack("<IIII", 1, 1, 0, 0)
        dynstr = b"\x00libandroid.so\x00\x00"
        dynsym = b"\x00" * 24
        data = elf_hdr + ph_load + ph_dyn + pad + dyn_sec + dt_hash + dynstr + dynsym
        return bytes(data) + b"\x00" * (512 - len(data))
    else:
        elf_hdr = bytearray(52)
        elf_hdr[0:8] = b"\x7fELF\x01\x01\x01\x00"
        struct.pack_into("<HHIIIIIHHHHHH", elf_hdr, 16, 3, 40, 1, 0, 52, 0, 0, 52, 32, 2, 0, 0, 0)
        ph_load = bytearray(32)
        struct.pack_into("<IIIIIIII", ph_load, 0, 1, 0, 0, 0, 0x200, 0x200, 5, 0x1000)
        ph_dyn = bytearray(32)
        struct.pack_into("<IIIIIIII", ph_dyn, 0, 2, 0x80, 0x80, 0x80, 56, 56, 6, 4)
        dyn_sec = bytearray()
        for tag, val in [(4, 0xC0), (5, 0xD0), (6, 0xE0), (10, 16), (11, 16), (16, 0), (0, 0)]:
            dyn_sec.extend(struct.pack("<II", tag, val))
        pad = b"\x00" * (0x80 - (52 + 32 * 2))
        dt_hash = struct.pack("<IIII", 1, 1, 0, 0)
        dynstr = b"\x00libandroid.so\x00\x00"
        dynsym = b"\x00" * 16
        data = elf_hdr + ph_load + ph_dyn + pad + dyn_sec + dt_hash + dynstr + dynsym
        return bytes(data) + b"\x00" * (256 - len(data))


def check_and_apply_termux_fix():
    os.makedirs(LIB_DIR, exist_ok=True)
    dest_so = os.path.join(LIB_DIR, "libc++_shared.so")
    dest_lzma = os.path.join(LIB_DIR, "liblzma.so")
    dest_android = os.path.join(LIB_DIR, "libandroid.so")

    termux_prefix = os.environ.get("PREFIX", "/data/data/com.termux/files/usr")
    termux_usr_lib = os.path.join(termux_prefix, "lib")
    local_lib = os.path.expanduser("~/.local/lib")

    if not os.path.exists(dest_android):
        created = False
        for comp in ("clang", "gcc"):
            try:
                res = subprocess.run([comp, "-shared", "-o", dest_android, "-x", "c", os.devnull], capture_output=True)
                if res.returncode == 0:
                    created = True
                    break
            except Exception:
                pass
        if not created:
            try:
                data = make_stub_so(platform.machine().lower())
                with open(dest_android, "wb") as f:
                    f.write(data)
                created = True
            except Exception:
                pass
        if created:
            print(f" {c_green('Applied')} Termux fix: created stub libandroid.so in {c_bold('tools/lib')}")

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

    system_lzma_candidates = [
        "/apex/com.android.runtime/lib64/bionic/liblzma.so",
        "/apex/com.android.art/lib64/liblzma.so",
        "/system/lib64/liblzma.so",
        "/apex/com.android.runtime/lib/bionic/liblzma.so",
        "/apex/com.android.art/lib/liblzma.so",
        "/system/lib/liblzma.so",
    ]
    detected_system_lzma = None
    for src in system_lzma_candidates:
        if os.path.exists(src):
            detected_system_lzma = src
            if not os.path.exists(dest_lzma):
                try:
                    shutil.copyfile(src, dest_lzma)
                    print(f" {c_green('Applied')} Termux fix: copied liblzma.so to {c_bold('tools/lib')}")
                except Exception:
                    pass
            break

    if detected_system_lzma:
        curr_preload = os.environ.get("LD_PRELOAD", "")
        if detected_system_lzma not in curr_preload:
            os.environ["LD_PRELOAD"] = f"{detected_system_lzma}:{curr_preload}".strip(":")

    system_paths = [
        LIB_DIR,
        local_lib,
        "/apex/com.android.runtime/lib64/bionic",
        "/apex/com.android.art/lib64",
        "/system/lib64",
        "/apex/com.android.runtime/lib/bionic",
        "/system/lib",
    ]
    ld_paths = [p for p in system_paths if os.path.isdir(p) or p == LIB_DIR]

    current_ld = os.environ.get("LD_LIBRARY_PATH", "")
    if current_ld:
        for p in current_ld.split(":"):
            if p and p not in ld_paths and not p.startswith(termux_usr_lib) and not p.startswith("/data/data/com.termux/files/usr/lib"):
                ld_paths.append(p)
    os.environ["LD_LIBRARY_PATH"] = ":".join(ld_paths)

    termux_bin = os.path.join(termux_prefix, "bin")
    glsl_val = os.path.join(termux_bin, "glslangValidator")
    glsl_sym = os.path.join(termux_bin, "glslang")
    if os.path.exists(glsl_val) and not os.path.exists(glsl_sym):
        try:
            os.symlink(glsl_val, glsl_sym)
        except Exception:
            pass


def is_termux():
    return bool(
        os.environ.get("TERMUX_VERSION")
        or os.path.isdir("/data/data/com.termux")
        or (os.environ.get("PREFIX") and "termux" in os.environ.get("PREFIX"))
    )


def find_glslang_path():
    termux_bin = os.path.join(os.environ.get("PREFIX", "/data/data/com.termux/files/usr"), "bin")
    candidates = [
        "glslangValidator",
        "glslang",
        os.path.join(TOOLS_DIR, "glslangValidator.exe" if os.name == "nt" else "glslangValidator"),
        os.path.join(TOOLS_DIR, "glslang.exe" if os.name == "nt" else "glslang"),
        os.path.join(termux_bin, "glslangValidator"),
        os.path.join(termux_bin, "glslang"),
        "/data/data/com.termux/files/usr/bin/glslangValidator",
        "/data/data/com.termux/files/usr/bin/glslang",
        "/usr/bin/glslangValidator",
        "/usr/local/bin/glslangValidator",
        "/usr/bin/glslang",
        "/usr/local/bin/glslang",
    ]
    for c in candidates:
        if os.path.isabs(c):
            if os.path.isfile(c) and (os.name == "nt" or os.access(c, os.X_OK)):
                return c
        else:
            w = shutil.which(c)
            if w:
                return w
    return None


def ensure_glslang():
    existing = find_glslang_path()
    if existing:
        return existing

    if is_termux():
        print(f" {c_yellow('Installing')} glslang in Termux...")
        try:
            subprocess.run(["pkg", "install", "-y", "glslang"], capture_output=True)
        except Exception:
            pass
        termux_bin = os.path.join(os.environ.get("PREFIX", "/data/data/com.termux/files/usr"), "bin")
        v = os.path.join(termux_bin, "glslangValidator")
        s = os.path.join(termux_bin, "glslang")
        if os.path.exists(v) and not os.path.exists(s):
            try:
                os.symlink(v, s)
            except Exception:
                pass
        return find_glslang_path()

    system = platform.system()
    machine = platform.machine().lower()
    archive_name = None
    binary_name = "glslang.exe" if system == "Windows" else "glslang"

    if system == "Windows" and machine in ("amd64", "x86_64", "x86", "arm64"):
        archive_name = f"glslang-{GLSLANG_VERSION}-windows-x86_64-release.zip"
    elif system == "Linux" and machine in ("x86_64", "amd64"):
        archive_name = f"glslang-{GLSLANG_VERSION}-linux-x86_64-release.zip"
    elif system == "Darwin":
        archive_name = f"glslang-{GLSLANG_VERSION}-macos-universal-release.zip"

    if not archive_name:
        return None

    dest_bin = os.path.join(TOOLS_DIR, binary_name)
    url = GLSLANG_RELEASE_URL + archive_name
    zip_path = os.path.join(TOOLS_DIR, archive_name)

    print(f" {c_yellow('Downloading')} glslang validator from KhronosGroup...")
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=30) as resp, open(zip_path, "wb") as out_f:
            shutil.copyfileobj(resp, out_f)
        with zipfile.ZipFile(zip_path, "r") as zf:
            for member in zf.infolist():
                if os.path.basename(member.filename) in ("glslang", "glslang.exe", "glslangValidator", "glslangValidator.exe"):
                    with zf.open(member) as src, open(dest_bin, "wb") as dst:
                        shutil.copyfileobj(src, dst)
                    os.chmod(dest_bin, 0o755)
                    print(f" {c_green('Extracted')} glslang validator to {c_bold(dest_bin)}")
                    return dest_bin
    except Exception as e:
        print(f" {c_yellow('Notice:')} Could not download glslang ({e}).")
    finally:
        if os.path.exists(zip_path):
            try:
                os.remove(zip_path)
            except Exception:
                pass

    return find_glslang_path()


def format_compile_error(err):
    err_str = str(err).strip()
    if not err_str:
        return "Unknown error"
    lines = [line.rstrip() for line in err_str.splitlines()]
    output = []
    for line in lines:
        s = line.strip()
        if not s:
            if output and output[-1] != "":
                output.append("")
            continue
        if s == "stdin":
            continue
        output.append(f"   {line}")
    return "\n".join(output) if output else "Unknown error"


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

    ensure_glslang()

    if force or not are_base_materials_present():
        setup_materials(version=version, force=force)

    return shaderc_path


def build_materials(target_platform="windows", material_name=None, continue_on_error=False):
    t0 = time.time()
    check_and_apply_termux_fix()
    shaderc_path = ensure_setup()
    glslang_path = ensure_glslang()

    if not glslang_path and target_platform in ("android", "multiplatform"):
        print(f" {c_yellow('Warning:')} glslangValidator not found. ESSL shader syntax validation is disabled.")

    try:
        test_run = subprocess.run([shaderc_path, "-v"], capture_output=True)
        if test_run.returncode != 0:
            err_msg = test_run.stderr.decode("utf-8", errors="replace") or test_run.stdout.decode("utf-8", errors="replace")
            print(f"\n {c_red('✗ Compiler error:')} {err_msg.strip()}")
            if "Xzs_Construct" in err_msg or "libunwindstack" in err_msg:
                print(f"\n {c_yellow('Hint:')} Termux requires Android system liblzma.so for shaderc.")
            elif "libc++_shared.so" in err_msg:
                print(f"\n {c_yellow('Hint:')} Run: {c_cyan('pkg install libc++')}")
            return False
    except Exception as e:
        print(f"\n {c_red('✗ Failed to invoke compiler:')} {e}")
        return False

    out_dir = os.path.join(BUILD_DIR, target_platform)
    os.makedirs(out_dir, exist_ok=True)

    all_materials = get_available_materials()
    import lazurite.project.project as lp

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
        print(f" {c_bold('Building')} {c_cyan(matched)} for {c_yellow(target_platform)}...")
        try:
            lp.compile(
                project_path=MATERIALS_DIR,
                profiles=[target_platform],
                output_folder=out_dir,
                material_patterns=[os.path.join(MATERIALS_DIR, matched)],
                shaderc_path=shaderc_path,
                glslang_path=glslang_path,
                defines=[],
            )
            elapsed = time.time() - t0
            rel_out = os.path.relpath(out_dir, ROOT_DIR)
            print(f"\n {c_green('✓ Done')} in {elapsed:.1f}s -> {c_bold(rel_out)}")
            return True
        except Exception as e:
            print(f"\n {c_red('✗ Build failed for')} {c_bold(matched)}:")
            print(c_red(format_compile_error(e)))
            return False

    print(f" {c_bold('Building all')} {c_cyan(str(len(all_materials)))} materials for {c_yellow(target_platform)}...")

    success_list = []
    failed_list = []

    for idx, mat in enumerate(all_materials, 1):
        prefix = f"[{idx}/{len(all_materials)}] {mat}"
        print(f" {c_bold(prefix)}...", end="", flush=True)
        m_t0 = time.time()
        try:
            lp.compile(
                project_path=MATERIALS_DIR,
                profiles=[target_platform],
                output_folder=out_dir,
                material_patterns=[os.path.join(MATERIALS_DIR, mat)],
                shaderc_path=shaderc_path,
                glslang_path=glslang_path,
                defines=[],
            )
            m_elapsed = time.time() - m_t0
            print(f"\r {c_green('✓')} {c_bold(prefix)} ({m_elapsed:.1f}s)")
            success_list.append(mat)
        except Exception as e:
            m_elapsed = time.time() - m_t0
            formatted_err = format_compile_error(e)
            print(f"\r {c_red('✗')} {c_bold(prefix)} ({m_elapsed:.1f}s)")
            print(f"\n{c_red(formatted_err)}\n")
            failed_list.append((mat, formatted_err))
            if not continue_on_error:
                print(f" {c_red('Build halted due to compilation error in')} {c_bold(mat)}.")
                return False

    elapsed = time.time() - t0
    rel_out = os.path.relpath(out_dir, ROOT_DIR)

    print(f"\n{c_bold('=' * 60)}")
    print(f" Summary ({c_yellow(target_platform)}) in {elapsed:.1f}s -> {c_bold(rel_out)}")
    print(f"  {c_green('✓ Succeeded:')} {len(success_list)}/{len(all_materials)}")
    if failed_list:
        print(f"  {c_red('✗ Failed:')} {len(failed_list)}")
        for f_name, f_reason in failed_list:
            first_err = [ln.strip() for ln in f_reason.splitlines() if ln.strip()]
            summary_line = first_err[0] if first_err else "Error"
            print(f"    - {c_bold(f_name)}: {c_dim(summary_line)}")
        print(c_bold("=" * 60))
        return False

    print(c_bold("=" * 60))
    return True


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
    continue_on_error = False

    for arg in args:
        low = arg.lower()
        if low in ("--continue", "--keep-going", "-k"):
            continue_on_error = True
        elif low in ("windows", "win"):
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
                sys.exit(1)

    success = build_materials(target_platform, target_mat, continue_on_error=continue_on_error)
    if not success:
        sys.exit(1)


if __name__ == "__main__":
    parse_and_run(sys.argv[1:])
