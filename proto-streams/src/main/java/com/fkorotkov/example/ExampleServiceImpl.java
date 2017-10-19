package com.fkorotkov.example;

import com.fkorotkov.example.grpc.ExampleGrpc;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicInteger;

public class ExampleServiceImpl extends ExampleGrpc.ExampleImplBase {
  public volatile AtomicInteger counter = new AtomicInteger(0);

  @Override
  public StreamObserver<Empty> devNull(StreamObserver<Empty> responseObserver) {
    return new StreamObserver<Empty>() {
      @Override
      public void onNext(Empty value) {
        counter.incrementAndGet();
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {

      }
    };
  }
}
