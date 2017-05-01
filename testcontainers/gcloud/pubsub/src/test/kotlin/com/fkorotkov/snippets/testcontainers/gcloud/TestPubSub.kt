package com.fkorotkov.snippets.testcontainers.gcloud

import com.google.api.gax.core.FixedCredentialsProvider
import com.google.api.gax.grpc.ApiException
import com.google.api.gax.grpc.InstantiatingChannelProvider
import com.google.cloud.NoCredentials
import com.google.cloud.pubsub.spi.v1.TopicAdminClient
import com.google.cloud.pubsub.spi.v1.TopicAdminSettings
import com.google.pubsub.v1.TopicName
import io.grpc.StatusRuntimeException
import org.junit.Assert.fail
import org.junit.ClassRule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


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

    val containerHost: String
      get() = "${pubsubContainer.containerIpAddress}:${pubsubContainer.getMappedPort(emulatorPort)}"
  }

  /**
   * @see https://github.com/GoogleCloudPlatform/google-cloud-java/issues/1973
   */
  @Test
  fun testIssue1973() {
    pubsubContainer.followOutput { print(it.utf8String) }

    val channelProvider = InstantiatingChannelProvider.newBuilder()
      .setEndpoint(containerHost)
      .setCredentialsProvider(FixedCredentialsProvider.create(NoCredentials.getInstance()))
      .build()

    val topic = TopicName.create(projectName, "foo")

    val adminSettings = TopicAdminSettings.defaultBuilder()
      .setChannelProvider(channelProvider)
      .build()

    val topicAdminClient = TopicAdminClient.create(adminSettings)

    try {
      topicAdminClient.createTopic(topic)
      fail("Expected an StatusRuntimeException to be thrown")
    } catch (ex: ApiException) {
      assertTrue(ex.cause is StatusRuntimeException)
      assertTrue(ex.cause?.cause is IllegalStateException)
      assertEquals(ex.cause?.cause?.message, "OAuth2Credentials instance does not support refreshing the access token. An instance with a new access token should be used, or a derived type that supports refreshing.")
    }
  }
}