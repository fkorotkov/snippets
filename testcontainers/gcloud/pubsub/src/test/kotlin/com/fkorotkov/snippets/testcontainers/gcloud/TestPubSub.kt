package com.fkorotkov.snippets.testcontainers.gcloud

import com.google.api.gax.grpc.FixedChannelProvider
import com.google.cloud.pubsub.spi.v1.TopicAdminClient
import com.google.cloud.pubsub.spi.v1.TopicAdminSettings
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import org.junit.ClassRule
import org.junit.Test
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
}