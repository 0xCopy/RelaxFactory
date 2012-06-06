package rxf.server;

import java.lang.annotation.*;

public interface CouchService<E> {
  E find(String key);

  CouchTx persist(E entity);

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface View {
    String map();

    String reduce() default "";
  }
}
