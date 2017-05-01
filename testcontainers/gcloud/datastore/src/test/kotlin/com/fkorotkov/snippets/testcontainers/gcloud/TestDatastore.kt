package com.fkorotkov.snippets.testcontainers.gcloud

import com.google.cloud.NoCredentials
import com.google.cloud.datastore.Datastore
import com.google.cloud.datastore.DatastoreOptions
import org.junit.ClassRule
import org.junit.Test
import kotlin.test.assertTrue


class TestDatastore {
  companion object {
    val projectName = "testing"
    val emulatorPort = 8888

    @ClassRule @JvmField
    public val datastoreContainer: GoogleCloudContainer =
      GoogleCloudContainer()
        .withExposedPorts(emulatorPort)
        .withCommand("/bin/sh", "-c",
          """
          gcloud beta emulators datastore start --no-legacy \
            --project $projectName \
            --host-port=0.0.0.0:$emulatorPort \
            --consistency=1
        """)

    val datastoreService: Datastore by lazy {
      val containerHost = "${datastoreContainer.containerIpAddress}:${datastoreContainer.getMappedPort(emulatorPort)}"

      DatastoreOptions.newBuilder()
        .setProjectId(projectName)
        .setHost(containerHost)
        .setCredentials(NoCredentials.getInstance())
        .build()
        .service
    }
  }

  @Test
  fun testIdAllocation() {
    val keyFactory = datastoreService.newKeyFactory().setKind("Animal")

    val allocatedId = datastoreService.allocateId(keyFactory.newKey())
    assertTrue(allocatedId.id > 0)
  }
}