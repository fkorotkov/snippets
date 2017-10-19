package com.fkorotkov.example;

import com.fkorotkov.example.grpc.ExampleGrpc;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class StreamShutdownTest {
  @Test
  public void testGracefulShutdown() throws IOException, InterruptedException {
    int port = 8239;

    ExampleServiceImpl serviceImpl = new ExampleServiceImpl();
    Server server = ServerBuilder.forPort(port)
      .addService(serviceImpl)
      .directExecutor()
      .build();
    server.start();

    ExampleGrpc.ExampleStub exampleStub = ExampleGrpc.newStub(
      ManagedChannelBuilder.forAddress("localhost", port)
        .directExecutor()
        .build()
    );

    StreamObserver<Empty> stream = exampleStub.devNull(new ResponseObserver());

    new Thread(() -> {
      int seconds = 20;
      while (seconds-- > 0) {
        stream.onNext(Empty.getDefaultInstance());
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          stream.onError(e);
          return;
        }
      }
      stream.onCompleted();
    }).start();

    server.shutdown();
    server.awaitTermination();

    // should be actually 20
    Assert.assertEquals(0, serviceImpl.counter.get());
  }

  private static class ResponseObserver implements StreamObserver<Empty> {
    @Override
    public void onNext(Empty value) {
    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onCompleted() {

    }
  }
}
