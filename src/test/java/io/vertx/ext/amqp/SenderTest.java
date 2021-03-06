package io.vertx.ext.amqp;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SenderTest extends ArtemisTestBase {

  //TODO Test the error with bad credentials

  @Test
  public void testThatMessagedAreSent() {
    String queue = UUID.randomUUID().toString();
    List<String> list = new CopyOnWriteArrayList<>();
    usage.consumeStrings(queue, 1, 1, TimeUnit.MINUTES, null, list::add);
    AmqpClient.create(new AmqpClientOptions()
      .setHost(host)
      .setPort(port)
      .setUsername(username)
      .setPassword(password)
    ).connect(connection -> {
        System.out.println("Connected: " + connection.result());
        connection.result().sender(queue, done -> {
          if (done.failed()) {
            done.cause().printStackTrace();
          } else {
            // Sending a few messages
            done.result().send(AmqpMessage.create().body("hello").address(queue).build());
            done.result().send(AmqpMessage.create().body("world").address(queue).build());
          }
        });
      }
    );

    await().until(() -> list.size() == 2);
    assertThat(list).containsExactly("hello", "world");
  }

  @Test
  public void testThatMessagedContainingJsonObjectsAreSent() {
    String queue = UUID.randomUUID().toString();
    List<String> list = new CopyOnWriteArrayList<>();
    usage.consumeStrings(queue, 1, 1, TimeUnit.MINUTES, null, list::add);
    AmqpClient.create(new AmqpClientOptions()
      .setHost(host)
      .setPort(port)
      .setUsername(username)
      .setPassword(password)
    ).connect(connection -> {
        System.out.println("Connected: " + connection.result());
        connection.result().sender(queue, done -> {
          if (done.failed()) {
            done.cause().printStackTrace();
          } else {
            JsonObject json1 = new JsonObject().put("message", "hello").put("count", 1);
            JsonObject json2 = new JsonObject().put("message", "world").put("count", 2);
            done.result().send(AmqpMessage.create().body(json1).address(queue).build());
            done.result().send(AmqpMessage.create().body(json2).address(queue).build());
          }
        });
      }
    );

    await().until(() -> list.size() == 2);
    JsonObject res1 = new JsonObject(list.get(0));
    JsonObject res2 = new JsonObject(list.get(1));
    assertThat(res1.getString("message")).isEqualTo("hello");
    assertThat(res1.getInteger("count")).isEqualTo(1);
    assertThat(res2.getString("message")).isEqualTo("world");
    assertThat(res2.getInteger("count")).isEqualTo(2);
  }

  @Test
  public void testThatMessagedAreAcknowledged() {
    String queue = UUID.randomUUID().toString();
    List<String> list = new CopyOnWriteArrayList<>();
    AtomicInteger acks = new AtomicInteger();
    usage.consumeStrings(queue, 1, 1, TimeUnit.MINUTES, null, list::add);
    AmqpClient.create(new AmqpClientOptions()
      .setHost(host)
      .setPort(port)
      .setUsername(username)
      .setPassword(password)
    ).connect(connection -> {
        System.out.println("Connected: " + connection.result());
        connection.result().sender(queue, done -> {
          if (done.failed()) {
            done.cause().printStackTrace();
          } else {
            // Sending a few messages
            done.result().sendWithAck(AmqpMessage.create().body("hello").address(queue).build(), x -> {
              if (x.succeeded()) {
                acks.incrementAndGet();
              }
            });
            done.result().sendWithAck(AmqpMessage.create().body("world").address(queue).build(), x -> {
              if (x.succeeded()) {
                acks.incrementAndGet();
              }
            });
          }
        });
      }
    );

    await().until(() -> list.size() == 2);
    await().until(() -> acks.get() == 2);
    assertThat(list).containsExactly("hello", "world");
  }
}
