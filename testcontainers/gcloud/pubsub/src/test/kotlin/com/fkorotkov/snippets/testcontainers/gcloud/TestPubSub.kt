package com.fkorotkov.snippets.testcontainers.gcloud

import com.google.api.gax.grpc.FixedChannelProvider
import com.google.cloud.pubsub.spi.v1.*
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.SubscriptionName
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import org.junit.ClassRule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class TestPubSub {
  companion object {
    val projectName = "testing"
    val emulatorPort = 8888

    @ClassRule @JvmField
    public val pubsubContainer: GoogleCloudContainer =
      GoogleCloudContainer()
        .withExposedPorts(emulatorPort)
        .withCommand("/bin/sh", "-c",
          """
          gcloud beta emulators pubsub start \
            --project $projectName \
            --host-port=0.0.0.0:$emulatorPort
        """)
  }

  /**
   * @see https://github.com/GoogleCloudPlatform/google-cloud-java/issues/1973
   */
  @Test
  fun testWorkaroundForIssue1973() {
    pubsubContainer.followOutput { print(it.utf8String) }

    val channel = ManagedChannelBuilder.forAddress(
      pubsubContainer.containerIpAddress,
      pubsubContainer.getMappedPort(emulatorPort)
    ).usePlaintext(true).build()

    val topic = TopicName.create(projectName, "foo")

    val adminSettings = TopicAdminSettings.defaultBuilder()
      .setChannelProvider(FixedChannelProvider.create(channel))
      .build()

    val topicAdminClient = TopicAdminClient.create(adminSettings)

    assertNotNull(topicAdminClient.createTopic(topic))
  }

  @Test
  fun endToEndTest() {
    pubsubContainer.followOutput { print(it.utf8String) }

    val topicName = TopicName.create(projectName, "bar")

    val channel = ManagedChannelBuilder.forAddress(
      pubsubContainer.containerIpAddress,
      pubsubContainer.getMappedPort(emulatorPort)
    ).usePlaintext(true).build()
    val channelProvider = FixedChannelProvider.create(channel)

    val topicAdminClient = TopicAdminClient.create(
      TopicAdminSettings.defaultBuilder()
        .setChannelProvider(channelProvider)
        .build()
    )
    val topic = topicAdminClient.createTopic(topicName)

    println("Creating subscriber....")

    val subscriptionAdminClient = SubscriptionAdminClient.create(
      SubscriptionAdminSettings.defaultBuilder()
        .setChannelProvider(channelProvider)
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
    PubSubTestingUtil.waitForEmptySubscrivers(subscriptionAdminClient, listOf(subscriber))

    assertEquals(messagesFromSubscriber.size, 1)
    assertEquals(messagesFromSubscriber[0], "Hello")
  }
}