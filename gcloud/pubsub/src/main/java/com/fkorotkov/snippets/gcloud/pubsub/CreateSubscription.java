package com.fkorotkov.snippets.gcloud.pubsub;

import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;

import java.io.IOException;
import java.util.concurrent.Executors;

public class CreateSubscription {
    public static String TOPIC = "TestTopic";
    public static String SUBSCRIPTION_NAME = "TestSubscription";

    public static void main(String[] args) throws IOException {
        String projectName = args[0];

        System.out.println("Using " + projectName + " as project name!");

        InstantiatingGrpcChannelProvider channelProvider =
          TopicAdminSettings.defaultGrpcTransportProviderBuilder().build();

        ListeningScheduledExecutorService executorService =
                MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(8));
        FixedExecutorProvider executorProvider = FixedExecutorProvider.create(executorService);

        SubscriptionAdminSettings adminSettings = SubscriptionAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setExecutorProvider(executorProvider)
                .build();

        SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(adminSettings);

        TopicName topic = TopicName.of(projectName, TOPIC);
        SubscriptionName subscriptionName = SubscriptionName.of(projectName, SUBSCRIPTION_NAME);

        System.out.println("Creating subscription...");
        subscriptionAdminClient.createSubscription(
                subscriptionName,
                topic,
                PushConfig.getDefaultInstance(),
                0
        );
        System.out.println("Subscription created!!!");
    }
}
