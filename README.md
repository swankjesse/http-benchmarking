HTTP Benchmarking
-----------------

Run:

```
./gradlew jmh
```

OkHttp 3.14.2
-------------

```
Benchmark           (bodySize)  (protocol)  (scheme)   Mode  Cnt     Score       Error  Units
HttpBenchmark.post        1024      HTTP/1      http  thrpt    3  4167.067 ±  9650.795  ops/s
HttpBenchmark.post        1024      HTTP/1     https  thrpt    3  4035.544 ±  2408.994  ops/s
HttpBenchmark.post        1024      HTTP/2      http  thrpt    3  4395.365 ±  2054.020  ops/s
HttpBenchmark.post        1024      HTTP/2     https  thrpt    3  3408.461 ± 10459.466  ops/s
```


Troubleshooting
---------------

The JMH plugin loses its benchmark list. Killing everything seems to fix.

```
./gradlew clean ; ./gradlew --stop
```