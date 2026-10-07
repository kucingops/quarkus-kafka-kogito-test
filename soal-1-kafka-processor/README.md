# Soal 1: Kafka Transaction Processor

Service yang menerima data transaksi e-commerce dari Kafka, memanipulasi datanya, lalu menulis hasilnya kembali ke Kafka dan menyimpannya ke database H2.

Stack: Java 17, Quarkus 3.15.3, Kafka (SmallRye Reactive Messaging), H2 + Hibernate ORM Panache.
Dataset: transaksi e-commerce buatan sendiri, lihat `data/sample-transactions.jsonl`.

## Alur

```
transactions-raw ──► TransactionConsumer ──► TransactionProcessor ──► H2 (tabel transactions)
  (JSON mentah)                                 │  parse JSON
                                                │  TransactionTransformer (manipulasi)
                                                │  cek duplikat + simpan
                                                ▼
                                   valid   ──► transactions-enriched (key = transactionId)
                                   invalid ──► transactions-dlq
```

| Topic | Arah | Isi |
|---|---|---|
| `transactions-raw` | input | JSON mentah, bisa kotor atau tidak valid |
| `transactions-enriched` | output | Transaksi yang sudah dibersihkan dan diperkaya |
| `transactions-dlq` | output | Payload asli yang ditolak beserta alasannya |

## Manipulasi data

Semua logika ada di `TransactionTransformer`.

| # | Manipulasi | Contoh |
|---|---|---|
| 1 | Validasi field wajib, amount > 0 dan dalam batas, mata uang didukung, panjang teks muat di kolom tabel | `amount: -5000` → ditolak ke DLQ |
| 2 | Normalisasi merchant: trim, rapikan spasi, uppercase | `"  toko   maju jaya "` → `"TOKO MAJU JAYA"` |
| 3 | Normalisasi metode bayar | `"e-wallet"` → `"E_WALLET"` |
| 4 | Konversi mata uang ke Rupiah (kurs statis) | `45.5 USD` → `728000 IDR` |
| 5 | Kategorisasi nilai | < 100rb `SMALL`, < 1jt `MEDIUM`, < 10jt `LARGE`, sisanya `VERY_LARGE` |
| 6 | Flag risiko tinggi untuk transaksi ≥ Rp10 juta | `1200 EUR` → `highRisk: true` |
| 7 | Masking email | `budi.santoso@gmail.com` → `bu**********@gmail.com` |
| 8 | Menambahkan waktu proses | `processedAt` |

Contoh input di `transactions-raw`:

```json
{"transactionId":"TRX-1003","customerId":"CUST-03","customerEmail":"andi.w@outlook.com",
 "merchant":"elektronik   sejahtera","amount":1250,"currency":"SGD",
 "paymentMethod":"bank-transfer","timestamp":"2026-10-01T10:45:00Z"}
```

Hasil di `transactions-enriched` (record key: `TRX-1003`):

```json
{"transactionId":"TRX-1003","customerId":"CUST-03","maskedEmail":"an****@outlook.com",
 "merchant":"ELEKTRONIK SEJAHTERA","originalAmount":1250,"originalCurrency":"SGD",
 "amountIdr":15000000,"paymentMethod":"BANK_TRANSFER","category":"VERY_LARGE",
 "highRisk":true,"transactionTime":"2026-10-01T10:45:00Z","processedAt":"..."}
```

Data yang sama tersimpan di tabel `transactions` dan bisa dibaca lewat `GET /transactions/TRX-1003`. Di respons REST itu `originalAmount` tampil dengan dua desimal (`1250.00`) karena kolomnya `NUMERIC(19,2)`.

Pesan yang ditolak masuk ke `transactions-dlq` dalam bentuk:

```json
{"rawPayload":"<payload asli>","reason":"amount must be greater than 0","rejectedAt":"..."}
```

Catatan singkat:

- Soal memberi pilihan output ke Kafka atau database. Di sini dikerjakan keduanya.
- Data yang tidak valid dikirim ke `transactions-dlq`, jadi consumer tidak berhenti karena satu pesan rusak. Exception tak terduga saat memproses satu pesan juga berakhir di DLQ dengan alasan `processing error: ...`.
- `transactionId` menjadi primary key dan dicek sebelum insert, jadi pesan duplikat hanya diproses sekali.
- Record di `transactions-enriched` memakai `transactionId` sebagai key.
- Record di `transactions-dlq` tidak memakai key.

## Cara menjalankan

Prasyarat: JDK 17, Maven 3.9, dan Podman. Tidak ada file compose: di dev mode Quarkus Dev Services otomatis menyalakan container broker Kafka dan mematikannya saat aplikasi berhenti. Karena broker-nya tetap berupa container, Podman harus aktif. Arahkan ke socket Podman dulu:

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

```bash
mvn quarkus:dev
```

Saat pertama kali dijalankan, image broker diunduh dulu (sekitar 230 MB), jadi start pertama lebih lama.

Di terminal lain, kirim dataset contoh lalu lihat hasilnya:

```bash
./data/publish-sample.sh            # kirim 9 baris contoh: 5 valid, 2 invalid, 1 duplikat, 1 JSON rusak
curl localhost:8080/transactions    # data yang tersimpan di H2
curl localhost:8080/transactions/summary
curl "localhost:8080/transactions?highRisk=true"
```

Broker di kedua cara jalan adalah Apache Kafka, dinyalakan otomatis sebagai container:

| Cara jalan | Broker | Dinyalakan oleh |
|---|---|---|
| `mvn quarkus:dev` | Apache Kafka 3.7.0 (`quay.io/strimzi-test-container/test-container:latest-kafka-3.7.0`) | Quarkus Dev Services, provider `strimzi` (`quarkus.kafka.devservices.provider=strimzi`) |
| `mvn test -Dtest=KafkaContainerIT` | Apache Kafka 3.8.0 (`apache/kafka:3.8.0`) | `KafkaContainerResource` (Testcontainers) |

Endpoint REST:

| Method | Path | Fungsi |
|---|---|---|
| POST | `/transactions/publish` | Mengirim body apa adanya ke `transactions-raw` (untuk demo) |
| GET | `/transactions?category=&highRisk=` | Daftar transaksi tersimpan, terbaru dulu. Kedua filter opsional |
| GET | `/transactions/{id}` | Detail satu transaksi |
| GET | `/transactions/summary` | Jumlah dan total nilai per kategori |

## Testing

Ada dua jenis test.

**Unit test (full mock, tanpa Podman).** Memakai Mockito, jadi Kafka dan database tidak dibutuhkan.

```bash
mvn test
```

| Test | Yang di-mock | Yang diuji |
|---|---|---|
| `TransactionTransformerTest` | – (logika murni) | Normalisasi, konversi kurs, batas kategori, flag risiko, masking, validasi |
| `TransactionProcessorTest` | Transformer, repository | Data valid disimpan, duplikat tidak disimpan ulang, data invalid dan JSON rusak ditolak |
| `TransactionConsumerTest` | Processor, emitter Kafka | Hasil valid ke topic output dengan key `transactionId`, yang ditolak dan error tak terduga ke DLQ, duplikat tidak dikirim |
| `TransactionResourceTest` | Repository, emitter Kafka | Endpoint publish, list, detail, dan 404 |

**Container test (butuh Podman).** `KafkaContainerIT` menjalankan aplikasi dengan broker Apache Kafka sungguhan di dalam container. `KafkaContainerResource` menyalakan image `apache/kafka:3.8.0` lewat Testcontainers, lalu mengisi `kafka.bootstrap.servers` dengan alamat container itu. Dev Services dimatikan di profil test (`%test.quarkus.kafka.devservices.enabled=false`) supaya tidak ada broker kedua. Test mengirim pesan ke `transactions-raw`, lalu memastikan hasilnya benar-benar ter-produce ke `transactions-enriched` / `transactions-dlq` dan benar-benar ter-insert ke database. Satu test juga memastikan consumer tetap memproses pesan berikutnya setelah menerima pesan yang nilainya tidak muat di tabel.

Pastikan variabel Podman di bagian "Cara menjalankan" sudah di-set, lalu:

```bash
mvn test -Dtest=KafkaContainerIT
```

Test ini tidak ikut jalan di `mvn test` biasa karena nama class-nya berakhiran `IT`.

## Struktur kode

```
src/main/java/com/example/transactions
├── messaging/TransactionConsumer.java    # consumer Kafka + pengirim ke topic output/DLQ
├── service/TransactionProcessor.java     # parse, transform, simpan
├── service/TransactionTransformer.java   # inti manipulasi data
├── persistence/                          # entity dan repository tabel transactions
├── api/TransactionResource.java          # REST
└── model/                                # RawTransaction, EnrichedTransaction, dll.
```
