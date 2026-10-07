# Marketplace Tech Test

Repo ini berisi jawaban dua soal. Masing-masing adalah project Quarkus terpisah yang bisa dibuild dan dijalankan sendiri.

Keduanya adalah prototipe untuk tech test, bukan aplikasi produksi. H2 in-memory dipakai dengan sengaja untuk keperluan test; datanya hilang setiap kali aplikasi berhenti.

| Folder | Soal | Ringkasan |
|---|---|---|
| [`soal-1-kafka-processor`](soal-1-kafka-processor/README.md) | Kafka → manipulasi → Kafka / DB | Membaca transaksi e-commerce dari Kafka, membersihkan dan memperkaya datanya, lalu menulis hasilnya ke topic lain dan ke database H2. |
| [`soal-2-checkout-kogito`](soal-2-checkout-kogito/README.md) | BPMN checkout + Kogito | Proses checkout marketplace dalam BPMN yang dieksekusi Kogito, lengkap dengan penjelasan tiap service. |

## Tech stack

- Java 17
- Quarkus 3.15.3
- Kafka (SmallRye Reactive Messaging)
- Kogito / jBPM 10.1.0
- H2 in-memory + Hibernate ORM Panache

## Prasyarat

- JDK 17
- Maven 3.9
- Podman, hanya untuk Soal 1. Repo ini tidak punya file compose, tapi Kafka tetap berjalan sebagai container: `mvn quarkus:dev` dan container test-nya sama-sama menyalakan broker Kafka sendiri lewat Podman. `mvn test` di kedua soal dan `mvn quarkus:dev` di Soal 2 tidak butuh Podman.

## Setup Podman

Container broker dinyalakan lewat Testcontainers, yang berbicara ke Podman melalui socket. Jalankan sekali di terminal yang dipakai untuk Soal 1:

```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
```

## Menjalankan

```bash
# Test
(cd soal-1-kafka-processor && mvn test)                          # unit test (Mockito)
(cd soal-1-kafka-processor && mvn test -Dtest=KafkaContainerIT)  # container test (Apache Kafka), butuh Podman
(cd soal-2-checkout-kogito && mvn test)                          # unit test (Mockito) + test proses BPMN

# Soal 1 -> http://localhost:8080
cd soal-1-kafka-processor && mvn quarkus:dev

# Soal 2 -> http://localhost:8081/q/swagger-ui
cd soal-2-checkout-kogito && mvn quarkus:dev
```

Detail tiap soal ada di README masing-masing folder.
