package rxf.shared;

/**
 * User: jim Date: 4/30/12 Time: 12:18 AM
 */
public enum CompressionTypes {
  gzip("gz"), bzip2("bz2"), xz;
  public String suffix;

  CompressionTypes(String... suffix) {
    this.suffix = suffix.length == 0 ? name() : suffix[0];
  }
}
