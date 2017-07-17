package bbcursive.lib;

import java.nio.ByteBuffer;

/**
 * Created by jim on 1/17/16.
 */
public class Int {
  public static Integer parseInt(ByteBuffer r) {
    long x = 0;
    boolean neg = false;

    Integer res = null;
    if (r.hasRemaining()) {
      int i = r.get();
      switch (i) {
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          x = x * 10 + i - '0';
          break;
        case '-':
          neg = true;
        case '+':

      }
      while (r.hasRemaining()) {
        i = r.get();
        switch (i) {
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            x = x * 10 + i - '0';
            break;
          case '-':
            neg = true;
          case '+':
            break;

        }
      }
      res = (int) ((neg ? -x : x) & 0xffffffffL);
    }
    return res;
  }

  public static Integer parseInt(String r) {
    long x = 0;
    boolean neg = false;

    Integer res = null;

    int length = r.length();
    if (0 < length) {
      int i = r.charAt(0);
      switch (i) {
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          x = x * 10 + i - '0';
          break;
        case '-':
          neg = true;
        case '+':
          break;

      }

      for (int j = 1; j < length; j++) {
        i = r.charAt(i);
        switch (i) {
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            x = x * 10 + i - '0';
            break;
          case '-':
            neg = true;
          case '+':
            break;
        }
      }

      res = (int) ((neg ? -x : x) & 0xffffffffL);
    }
    return res;
  }
}
