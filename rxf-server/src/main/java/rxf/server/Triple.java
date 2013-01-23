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

  @Override
  public boolean equals(Object o) {
    if (this != o) {
      if (o instanceof Triple && super.equals(o)) {

        Triple triple = (Triple) o;

        return !(null != c ? !c.equals(triple.c) : null != triple.c);

      }
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (null != c ? c.hashCode() : 0);
    return result;
  }
}
