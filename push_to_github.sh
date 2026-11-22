#!/usr/bin/env bash
set -e

GITHUB_USER="${1:-irgunawan}"
REPO_NAME="${2:-Winanp}"
VERSION_TAG="${3:-v2.91.0}"

echo "=== Menyiapkan repositori Git untuk $GITHUB_USER/$REPO_NAME ==="
git init
git branch -M main
git add .
git commit -m "Release Winanp Classic $VERSION_TAG (MP3/FLAC + GitHub OTA Updater for Redmi Note 11)"

if git remote | grep -q "^origin$"; then
  git remote set-url origin "https://github.com/$GITHUB_USER/$REPO_NAME.git"
else
  git remote add origin "https://github.com/$GITHUB_USER/$REPO_NAME.git"
fi

echo "=== Mengirim source code ke GitHub ==="
git push -u origin main

echo "=== Mengirim tag $VERSION_TAG untuk memicu build APK OTA di GitHub Actions ==="
git tag -f "$VERSION_TAG"
git push -f origin "$VERSION_TAG"

echo "Selesai! Cek tab Actions & Releases di https://github.com/$GITHUB_USER/$REPO_NAME"
