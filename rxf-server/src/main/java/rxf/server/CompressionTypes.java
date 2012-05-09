package rxf.server;

/**
 * User: jim
 * Date: 4/30/12
 * Time: 12:18 AM
 */
enum CompressionTypes {
  gzip("gz"), bzip2("bz2"), xz;
  public String suffix;

  CompressionTypes(String... suffix) {
    if (suffix.length == 0) {
      this.suffix = name();
    } else
      this.suffix = suffix[0];
  }
}
