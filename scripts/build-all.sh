#!/bin/bash
set -e
cd "$(dirname "$0")/.."
echo "Building all modules..."
./mvnw clean package -DskipTests -q
echo "Build complete."
