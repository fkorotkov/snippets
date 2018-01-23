package com.fkorotkov.snippets.testcontainers.gcloud

import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.*
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.SubscriptionName
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import org.junit.ClassRule
import org.junit.Test
import org.testcontainers.containers.wait.LogMessageWaitStrategy
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail


class TestPubSub {
  companion object {
    val projectName = "testing"
    val emulatorPort = 8888

    @ClassRule
    @JvmField
    public val pubsubContainer: GoogleCloudContainer =
      GoogleCloudContainer()
        .withExposedPorts(emulatorPort)
        .withCommand("/bin/sh", "-c",
          """
          gcloud beta emulators pubsub start \
            --project $projectName \
            --host-port=0.0.0.0:$emulatorPort
        """)
        .waitingFor(LogMessageWaitStrategy().withRegEx("(?s).*started.*$"))
  }

  @Test
  fun endToEndTest() {
    pubsubContainer.followOutput { print(it.utf8String) }

    val topicName = TopicName.create(projectName, "bar")

    val channel = ManagedChannelBuilder.forAddress(
      pubsubContainer.containerIpAddress,
      pubsubContainer.getMappedPort(emulatorPort)
    ).usePlaintext(true).build()
    val channelProvider = FixedTransportChannelProvider.create(
      GrpcTransportChannel.create(channel)
    )

    val topicAdminClient = TopicAdminClient.create(
      TopicAdminSettings.newBuilder()
        .setTransportChannelProvider(channelProvider)
        .build()
    )

    topicAdminClient.createTopic(topicName)

    println("Creating subscriber....")

    val subscriptionAdminClient = SubscriptionAdminClient.create(
      SubscriptionAdminSettings.newBuilder()
        .setTransportChannelProvider(channelProvider)
        .build()
    )

    val subscription = SubscriptionName.create(projectName, "MySubscriber")
    subscriptionAdminClient.createSubscription(subscription, topicName, PushConfig.getDefaultInstance(), 0)

    val messagesFromSubscriber = mutableListOf<String>()
    val subscriber = Subscriber.defaultBuilder(subscription, (MessageReceiver { message, consumer ->
      messagesFromSubscriber.add(message.data.toStringUtf8())
      consumer.ack()
    }))
      .setChannelProvider(channelProvider)
      .build()
    subscriber.startAsync()

    println("Subscriber created!")
    println("Publishing message...")

    val publisher = Publisher.defaultBuilder(topicName)
      .setChannelProvider(channelProvider)
      .build()

    val pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("Hello!")).build()
    val messageId = publisher.publish(pubsubMessage).get()

    assertNotNull(messageId)

    println("Message $messageId published!")

    println("Waiting for subscribers...")

    var maxRetryCount = 20
    while (!messagesFromSubscriber.contains(messageId) && maxRetryCount-- > 0) {
      Thread.sleep(50)
    }

    if (maxRetryCount == 0) {
      fail("Subscribers failed to process messages in time!")
    }

    assertEquals(messagesFromSubscriber.size, 1)
    assertEquals(messagesFromSubscriber[0], "Hello!")
  }
}