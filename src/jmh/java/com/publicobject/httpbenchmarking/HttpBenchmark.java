package com.publicobject.httpbenchmarking;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@SuppressWarnings("ALL")
public class HttpBenchmark {
  public static final MediaType MEDIA_TYPE = MediaType.get("text/plain");

  @Param({"1024"})
  public int bodySize;

  @Param({"HTTP/1", "HTTP/2"})
  public String protocol;

  @Param({"http", "https"})
  public String scheme;

  private Buffer body;
  private MockWebServer server;
  private OkHttpClient client;

  @Setup() public void setUp() throws Exception {

    body = new Buffer();
    Random dice = new Random();
    for (int i = 0; i < bodySize; i += 4) {
      body.writeInt(dice.nextInt());
    }

    server = new MockWebServer();
    Logger.getLogger(MockWebServer.class.getName()).setLevel(Level.WARNING);
    server.setDispatcher(new Dispatcher() {
      @Override public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        return new MockResponse().setBody(body.clone());
      }
    });

    OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

    if (scheme.equals("https")) {
      HeldCertificate heldCertificate = new HeldCertificate.Builder()
          .addSubjectAlternativeName(InetAddress.getByName("localhost").getCanonicalHostName())
          .build();
      HandshakeCertificates handshakeCertificates = new HandshakeCertificates.Builder()
          .heldCertificate(heldCertificate)
          .addTrustedCertificate(heldCertificate.certificate())
          .build();
      server.useHttps(handshakeCertificates.sslSocketFactory(), false);
      server.setProtocols(protocol.equals("HTTP/2")
          ? Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1)
          : Arrays.asList(Protocol.HTTP_1_1));

      clientBuilder.sslSocketFactory(
          handshakeCertificates.sslSocketFactory(), handshakeCertificates.trustManager());

    } else if (scheme.equals("http") && protocol.equals("HTTP/2")) {
      server.setProtocols(Arrays.asList(Protocol.H2_PRIOR_KNOWLEDGE));
      clientBuilder.protocols(Arrays.asList(Protocol.H2_PRIOR_KNOWLEDGE));

    }

    server.start();
    client = clientBuilder.build();
  }

  @TearDown public void tearDown() throws Exception {
    server.shutdown();
  }

  @Benchmark
  @Fork(value = 1, warmups = 1)
  @Measurement(time = 5, timeUnit = TimeUnit.SECONDS, iterations = 3)
  @Warmup(time = 5, timeUnit = TimeUnit.SECONDS, iterations = 1)
  @BenchmarkMode(Mode.Throughput)
  @OutputTimeUnit(TimeUnit.SECONDS)
  @Threads(32)
  public void post(Blackhole blackhole) throws IOException {
    Call call = client.newCall(new Request.Builder()
        .url(server.url("/"))
        .post(new RequestBody() {
          @Override public MediaType contentType() {
            return MEDIA_TYPE;
          }

          @Override public void writeTo(BufferedSink sink) throws IOException {
            sink.writeAll(body.clone());
          }
        })
        .build());

    try (Response response = call.execute()) {
      ByteString responseBytes = response.body().source().readByteString();
      blackhole.consume(responseBytes);
    }
  }

  public static void main(String[] args) throws Exception {
    Options options = new OptionsBuilder()
        .include(HttpBenchmark.class.getSimpleName())
        .build();
    new Runner(options).run();
  }
}
