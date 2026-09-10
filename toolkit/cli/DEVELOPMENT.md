# Snow-White CLI

## Development

```bash
# Install dependencies
bun install

# Run in development mode
bun run dev

# Type check
bun run type-check

# Build for current platform
bun run build

# Build native binaries for all platforms
bun run build:all
```

## Building Native Binaries

The project includes scripts to build native binaries for multiple platforms:

- `build:linux-x64` - Linux AMD64
- `build:linux-arm64` - Linux ARM64
- `build:macos-x64` - macOS Intel
- `build:macos-arm64` - macOS Apple Silicon
- `build:windows-x64` - Windows AMD64
- `build:all` - All platforms

Built binaries will be in the `dist/` directory.
