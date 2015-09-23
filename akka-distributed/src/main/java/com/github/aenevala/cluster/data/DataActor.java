package com.github.aenevala.cluster.data;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.cluster.ddata.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import scala.concurrent.duration.Duration;

import static akka.cluster.ddata.Replicator.*;

/**
 * Created by Antti on 22.9.2015.
 */
public class DataActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private final Cluster node = Cluster.get(context().system());
    private final ActorRef replicator = DistributedData.get(context().system()).replicator();
    private final LWWRegister<String> name = LWWRegister.create(node, "Test");
    private final Key<LWWRegister<String>> nameKey = LWWRegisterKey.create("name");

    public DataActor() {
        receive(ReceiveBuilder
                .match(String.class, s -> s.equals("get"), s -> get())
                .match(String.class, a -> setName(a))
                .match(GetSuccess.class, response -> log.info("Get response: {}", response.dataValue()))
                .match(Changed.class, ch -> ch.key().equals(nameKey), ch -> log.info("Changed name to {}", ch.dataValue()))
                .matchAny(a -> log.warning("Unhandled: {}", a)).build());
    }

    private void get() {
        Get<LWWRegister<String>> get = new Get<>(nameKey, new Replicator.ReadMajority(Duration.create(3, "seconds")));
        replicator.tell(get, self());
    }

    private void setName(String s) {
        log.info("Setting name as '{}'", s);
        Update<LWWRegister<String>> update = new Update<>(nameKey, LWWRegister.create(node, ""), Replicator.writeLocal(),
                curr -> curr.withValue(node, s));
        replicator.tell(update, self());
    }

    @Override
    public void preStart() throws Exception {
        Subscribe<LWWRegister<String>> subscribe = new Subscribe<>(nameKey,self());
        replicator.tell(subscribe, self());
    }
}
