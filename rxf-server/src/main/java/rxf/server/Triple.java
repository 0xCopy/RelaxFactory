package rxf.server;

/**
 * A pair with extra data.
 */
class Triple<A, B, C> extends Pair<A, B> {
  private final C c;

  public Triple(A a, B b, C... c) {
    super(a, b);
    this.c = c[0];
  }

  public C getC() {
    return c;
  }
}
