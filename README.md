HTTP Benchmarking
-----------------

Run:

```
./gradlew jmh
```

Results
-------

These measured my MacBook Pro's thermal throttling more than anything. The results aren't
particularly reproducible though they suggest OkHttp 4.x has consistent throughput with its
predecessor.

```
(release)  (bodySize)  (protocol)  (scheme)   Mode  Cnt     Score       Error  Units
   3.14.2        1024      HTTP/1      http  thrpt    3  4167.067 ±  9650.795  ops/s
   3.14.2        1024      HTTP/1     https  thrpt    3  4035.544 ±  2408.994  ops/s
   3.14.2        1024      HTTP/2      http  thrpt    3  4395.365 ±  2054.020  ops/s
   3.14.2        1024      HTTP/2     https  thrpt    3  3408.461 ± 10459.466  ops/s
4.0.0-RC1        1024      HTTP/1      http  thrpt    3  3719.197 ± 12727.441  ops/s
4.0.0-RC1        1024      HTTP/1     https  thrpt    3  4015.923 ±  3989.535  ops/s
4.0.0-RC1        1024      HTTP/2      http  thrpt    3  4279.681 ±  1813.710  ops/s
4.0.0-RC1        1024      HTTP/2     https  thrpt    3  4032.366 ±  2541.970  ops/s
```


Troubleshooting
---------------

The JMH plugin loses its benchmark list. Killing everything seems to fix.

```
./gradlew clean ; ./gradlew --stop
```