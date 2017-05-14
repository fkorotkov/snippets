package com.google.cloud.pubsub.spi.v1

import com.google.pubsub.v1.PullRequest

object PubSubTestingUtil {
  fun waitForEmptySubscrivers(subscriptionAdminClient: SubscriptionAdminClient, subscribers: List<Subscriber>) {
    var subscribersWithMessages = subscribers
    var maxRetryCount = 20
    while (subscribersWithMessages.isNotEmpty() && maxRetryCount-- > 0) {
      subscribersWithMessages = subscribersWithMessages.filter { hasMessages(subscriptionAdminClient, it) }
      Thread.sleep(50)
    }
    if (subscribersWithMessages.isNotEmpty()) {
      val subscriberNamesWithMessages = subscribersWithMessages.map { it.subscriptionName.subscription }
      throw IllegalStateException(
        "Some subscribers still have unacknowledged messages: ${subscriberNamesWithMessages.joinToString()}"
      )
    }
  }

  private fun hasMessages(subscriptionAdminClient: SubscriptionAdminClient, subscriber: Subscriber): Boolean {
    val request = PullRequest.newBuilder()
      .setSubscriptionWithSubscriptionName(subscriber.subscriptionName)
      .setMaxMessages(1)
      .build()
    return !subscriptionAdminClient.pull(request).receivedMessagesList.isEmpty()
  }
}