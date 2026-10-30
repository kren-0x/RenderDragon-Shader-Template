# RenderDragon Shader Template

A template and build setup for Minecraft Bedrock Edition Render Dragon shaders (`.material.bin`) targeting Windows, Android, and iOS.

## Project structure

```
├── build.bat         # Windows build script (opens interactive menu without args)
├── build.sh          # Linux/Android build script
├── requirements.txt
├── LICENSE
├── include/          # Shared shader headers (.dragonh, .sh)
├── materials/        # Shader source code (vertex.sc, fragment.sc, varying.def.sc)
│   └── project.json  # Material pass and compiler config
└── tools/
    └── build.py      # Build script
```

## Setup and building

### Windows

Requirements: Python 3.9+ with pip added to PATH.

1. Install dependencies:
   ```cmd
   pip install -r requirements.txt
   ```

2. Build shaders:
   Run without arguments to open the interactive menu:
   ```cmd
   build.bat
   ```

   Or pass arguments directly:
   ```cmd
   build.bat windows
   build.bat android
   build.bat RenderChunk
   build.bat RenderChunk android
   build.bat setup
   build.bat setup v26.50
   build.bat clean
   ```

Compiled `.material.bin` files are placed in `build/<platform>/`.

### Android (Termux)

1. In Termux, install Python and Git, then grant storage access:
   ```bash
   pkg update && pkg install python git -y
   termux-setup-storage
   ```

2. Go to the project directory and install requirements:
   ```bash
   cd /sdcard/RenderDragon-Shader-Template
   pip install -r requirements.txt
   chmod +x build.sh
   ```

3. Build:
   Run without arguments for the interactive menu:
   ```bash
   ./build.sh
   ```

   Or pass arguments directly:
   ```bash
   ./build.sh android
   ./build.sh RenderChunk
   ./build.sh setup
   ./build.sh clean
   ```

`shaderc` and base material definitions download automatically on the first build (synced with the latest release from [mcpebd/serialized-materials](https://github.com/mcpebd/serialized-materials)).

### Adding new materials

The build script automatically detects materials in the `materials/` directory. If you add your own material:

1. Create a folder for it in `materials/` with your shader source files (`vertex.sc`, `fragment.sc`, `varying.def.sc`, `config.json`).
2. Add the material folder name to `include_patterns` in `materials/project.json`.
3. Run `build.bat setup` (or `./build.sh setup`) to fetch and extract its base `.material.json` definition from the serialized materials repository.

## Credits

- [RenderDragonSourceCodeInv](https://github.com/SurvivalApparatusCommunication/RenderDragonSourceCodeInv) - Reference for deobfuscated, readable shader source code
- [mcbe-shader-codebase](https://github.com/veka0/mcbe-shader-codebase) - Reference for latest vanilla obfuscated shader code
- [serialized-materials](https://github.com/mcpebd/serialized-materials) - Serialized material JSON definitions

## License

[MIT](LICENSE)
