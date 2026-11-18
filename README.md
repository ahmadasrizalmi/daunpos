# DaunPOS — Design Kit

Desain UI aplikasi kasir kafe **DaunPOS** untuk Android (smartphone & tablet), dibuat dari PRD `prd_aplikasi_kasir_android_daunpos.md`.

- Gaya: modern, minimalis, "clean & cozy" kafe kekinian
- Warna utama: hijau muda (sage/mint) dengan aksen putih dan abu-abu terang
- Semua layar tersedia dalam 2 varian: **phone 390px** dan **tablet 1024px**

## Isi folder

```
kasir/
├─ design/                        # SEMUA aset desain di satu folder
│  ├─ DaunPOS.pen                 # file project pen.dev (editable, semua layer & aset)
│  ├─ DaunPOS-semua-halaman.pdf   # 18 halaman (9 phone + 9 tablet), siap share/print
│  ├─ index.html                  # galeri desain, buka di browser
│  ├─ phone/                      # PNG 2x
│  └─ tablet/                     # PNG 2x
├─ app/                           # source code aplikasi Android (Kotlin + Compose)
├─ APP_BUILD.md                   # dokumentasi build & fitur
└─ prd_aplikasi_kasir_android_daunpos.md
```

## Design token

| Token | Nilai | Pemakaian |
| --- | --- | --- |
| `green-deep` | `#1F5C43` | teks aksen, tombol sekunder, tab aktif |
| `green` | `#6FC08D` | tombol utama (CTA) |
| `green-soft` | `#E7F4EA` | highlight, badge, chip aktif |
| `mint` | `#C9EDD6` | bar grafik, avatar, aksen lembut |
| `ink` | `#14251E` | teks utama |
| `ink-soft` | `#6E8078` | teks sekunder |
| `line` | `#E6EBE3` | border 1px, divider |
| `bg` | `#F5F7F2` | latar aplikasi |
| `surface` | `#FFFFFF` | kartu, sheet, top bar |
| `danger` / `danger-soft` | `#C9483C` / `#FBEAE7` | stok habis, transaksi batal |
| Font heading | Plus Jakarta Sans | judul, angka besar, label tombol |
| Font body | Inter | paragraf, metadata |
| Font struk | IBM Plex Mono | pratinjau struk thermal |
| Radius | 8–30 px (kartu 18–20, sheet 30, tombol 12–15, chip 10–20) | |
| Spacing | padding layar 20 (phone) / 24 (tablet), gap section 14–16, gap item 8–12 | |

## Daftar layar

| # | Layar | Isi utama |
| --- | --- | --- |
| 1 | **Login & Koneksi** | Brand + logo SVG, PIN 4 digit dengan keypad, link lupa PIN / masuk sebagai pemilik (tablet: split-screen + daftar fitur) |
| 2 | **POS / Kasir** | Header (logo, kasir, status printer, avatar), tab kategori, grid menu + badge stok habis, bar keranjang & tombol Bayar. Tablet: grid 4 kolom + panel keranjang dengan stepper qty |
| 3 | **Pembayaran** | Total tagihan, metode Tunai/QRIS/Transfer, input nominal + tombol cepat, kembalian real-time, tombol Selesaikan Transaksi |
| 4 | **Transaksi Berhasil** | Checkmark sukses, ringkasan pembayaran, aksi Cetak Struk / Kirim WhatsApp / Transaksi Baru, pratinjau struk 58mm |
| 5 | **Laporan Penjualan** | Filter tanggal, kartu KPI (pendapatan, transaksi, item terjual, menu terlaris), grafik tren harian, riwayat transaksi dengan badge status + aksi reprint, ekspor PDF/Excel |
| 6 | **Detail Transaksi** | Rincian item, total, info pembayaran, catatan dapur, tombol Cetak Ulang Struk / WhatsApp / PDF |
| 7 | **Manajemen Menu** | Search, filter kategori, daftar menu dengan toggle Tersedia/Habis, edit & hapus, tombol tambah menu |
| 8 | **Pengaturan Printer** | Kartu koneksi (RPP02N, status Terhubung, Putuskan), tab Bluetooth/USB/LAN, pemindai perangkat + daftar perangkat, ukuran kertas 58/80mm, toggle cetak otomatis & laci kasir, tes cetak |
| 9 | **Kustomisasi Struk** | Live preview struk thermal (monospace, 58mm) + upload logo, header, footer, toggle tampilan, QR struk digital |

Alur utama: Login → POS (pilih menu) → Bayar → Pembayaran → Berhasil/Cetak → Laporan → Detail → Reprint.

## Catatan implementasi

- Nominal pada semua layar konsisten: order `#INV-0183`, 4 item, total **Rp 89.000**, tunai Rp 100.000, kembalian Rp 11.000.
- Produk, foto menu, dan logo adalah aset hasil generate (foto stok + logo SVG) yang sudah tertanam di file `.pen`.
- Layar memakai layout flex (`fit_content`) tanpa scroll artinya semua konten selalu tampil penuh.

## Cara buka

1. **Edit desain**: buka `design/DaunPOS.pen` di pen.dev (drag & drop / Open file).
2. **Lihat hasil**: buka `design/index.html` di browser, atau PDF `design/DaunPOS-semua-halaman.pdf`.
3. **Aset PNG**: folder `design/phone` dan `design/tablet` (skala 2x, siap untuk spec/handoff developer).
