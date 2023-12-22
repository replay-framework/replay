package play.server;

import play.Play;
import play.inject.BeanSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Starter {
  public static int start(String playId, BeanSource beanSource) {
    Play play = new Play(beanSource);
    play.init(playId);
    play.start();
    return start(play);
  }

  public static int start(String playId) {
    Play play = new Play();
    play.init(playId);
    play.start();
    return start(play);
  }

  public static int start(Play play) {
    try {
      Class<?> klass = Class.forName("play.server.Server");
      Constructor<?> constructor = klass.getDeclaredConstructor(Play.class);
      constructor.setAccessible(true);
      PlayServer server = (PlayServer) constructor.newInstance(play);
      return server.start();
    }
    catch (ClassNotFoundException | NoSuchMethodException |
           InvocationTargetException | InstantiationException | IllegalAccessException e) {
      throw new IllegalStateException("Failed to start server. " +
        "Please add the dependency io.github.replay-framework:netty3, io.github.replay-framework:netty4 or "
        + "io.github.replay-framework:javanet to your project, and make sure it precedes "
        + "io.github.replay-framework:framework in the runtime classpath", e);
    }
  }
}
