package com.fkorotkov.snippets.testcontainers.gcloud

import org.testcontainers.containers.GenericContainer

class GoogleCloudContainer : GenericContainer<GoogleCloudContainer>("google/cloud-sdk:latest")