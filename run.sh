#!/usr/bin/env bash
# Starts the planet simulation. Builds it first if something changed.
set -e
cd "$(dirname "$0")"

# WSL: render on the real graphics card instead of the slow software renderer.
if [ -e /dev/dxg ]; then
    export GALLIUM_DRIVER="${GALLIUM_DRIVER:-d3d12}"
    # Prefer the NVIDIA card over the built-in Intel chip.
    export MESA_D3D12_DEFAULT_ADAPTER_NAME="${MESA_D3D12_DEFAULT_ADAPTER_NAME:-NVIDIA}"
fi

./gradlew -q installDist
exec build/install/planets/bin/planets "$@"
