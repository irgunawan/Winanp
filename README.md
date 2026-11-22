# WINANP Classic (Redmi Note 11 Edition) - MP3 & FLAC Player + GitHub OTA

**Winanp** adalah aplikasi pemutar musik Android bergaya **Winamp Classic 2.91** yang dioptimalkan khusus untuk **Xiaomi Redmi Note 11** (`spes` / `spesn`, Snapdragon 680, layar AMOLED 2400x1080 90Hz, Android 11/12/13/14).

## Fitur Utama
1. **Pemutar Audio MP3 & Lossless FLAC (24-bit / 96kHz)**: Menggunakan `androidx.media3:media3-exoplayer` dan hardware decoder PCM Snapdragon 680 serta pemindaian otomatis `MediaStore.Audio` untuk seluruh file `.mp3`, `.flac`, `.wav`, dan `.ogg`.
2. **Tampilan & Menu Klasik 3-Panel**:
   - **Main Window**: Layar LCD hijau klasik, jam digital LED (mode Elapsed/Remaining), teks berjalan (marquee), indikator bitrate (`kbps`) & sample rate (`kHz`), indikator `MONO`/`STEREO`, slider Volume & Balance, serta tombol transport klasik (`|<`, `>`, `||`, `■`, `>|`, `⏏`).
   - **Equalizer Window (10-Band + Preamp)**: Pengaturan frekuensi `60Hz`, `170Hz`, `310Hz`, `600Hz`, `1kHz`, `3kHz`, `6kHz`, `12kHz`, `14kHz`, `16kHz`, kurva spline real-time, dan tombol `PRESETS` (Full Bass, Rock, Techno, Headphone Redmi, Flat).
   - **Playlist Editor Window**: Daftar lagu bergaya klasik dengan badge format `MP3` / `FLAC`, tombol `+ ADD`, `- REM`, `SORT`, dan total waktu.
3. **Tombol Update OTA (Over-The-Air via GitHub Releases)**:
   - Terletak di pojok kanan atas Main Window (`UPDATE OTA`).
   - Memeriksa versi terbaru langsung dari `https://api.github.com/repos/irgunawan/Winanp/releases/latest`.
   - Mengunduh file `Winanp-release.apk` dari GitHub Releases menggunakan `DownloadManager` dan langsung memicu instalasi pembaruan APK melalui `FileProvider`.
4. **GitHub Actions Otomatis (`.github/workflows/android-ota-release.yml`)**:
   - Setiap kali Anda mendorong kode atau membuat tag versi baru (misalnya `v2.92.0`), GitHub Actions otomatis meng-compile APK dan menerbitkannya ke halaman **GitHub Releases** agar semua pengguna Redmi Note 11 bisa langsung menekan tombol **UPDATE OTA** di aplikasi.

## Cara Push ke Akun GitHub Anda (`irgunawan/Winanp`)
Jalankan skrip `push_to_github.sh` atau perintah berikut di terminal komputer Anda:

```bash
git init
git branch -M main
git add .
git commit -m "Initial release Winanp Classic v2.91.0 for Redmi Note 11 with OTA Updater"
git remote add origin https://github.com/irgunawan/Winanp.git
git push -u origin main

# Membuat tag rilis pertama agar GitHub Actions otomatis membuat APK OTA:
git tag v2.91.0
git push origin v2.91.0
```
