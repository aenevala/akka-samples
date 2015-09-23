package com.github.aenevala.cluster.data;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.cluster.sharding.ShardRegion;
import scala.concurrent.duration.Duration;

/**
 * Created by Antti on 22.9.2015.
 */
public class App {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create("dataSystem");
        Cluster cluster = Cluster.get(system);
        cluster.join(cluster.selfAddress());
        ActorRef dataRef = system.actorOf(Props.create(DataActor.class), "data");
        dataRef.tell("Original", ActorRef.noSender());
        ClusterShardingSettings settings = ClusterShardingSettings.create(system);
        ShardRegion.MessageExtractor messageExtractor = new ShardRegion.MessageExtractor() {

            @Override
            public String entityId(Object message) {
                if (message instanceof String) {
                    return (String) message;
                } else return "temp";
            }

            @Override
            public Object entityMessage(Object message) {
                return message;
            }

            @Override
            public String shardId(Object message) {
                return Integer.toString(message.hashCode() % 10);
            }
        };
        ClusterSharding.get(system).start("Data", Props.create(DataActor.class), settings, messageExtractor);
        ActorRef dataRegion = ClusterSharding.get(system).shardRegion("Data");
        dataRegion.tell("Antti", ActorRef.noSender());
        dataRef.tell("get", ActorRef.noSender());
        //Thread.sleep(2000L);
        //system.registerOnTermination(() -> cluster.leave(cluster.selfAddress()));
        //system.terminate();
        //system.awaitTermination(Duration.create(2, "seconds"));
        //System.exit(0);
    }
}
