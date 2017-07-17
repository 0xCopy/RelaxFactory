package bbcursive;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

import static bbcursive.std.bb;

/**
 * some kind of less painful way to do byteBuffer operations and a few new ones thrown in.
 * <p/>
 * evidence that this can be more terse than what jdk pre-8 allows:
 * 
 * <pre>
 *
 * res.add(bb(nextChunk, rewind));
 * res.add((ByteBuffer) nextChunk.rewind());
 *
 *
 * </pre>
 */
@FunctionalInterface
public interface Cursive extends UnaryOperator<ByteBuffer> {
  enum pre implements UnaryOperator<ByteBuffer> {
    duplicate {

      public ByteBuffer apply(ByteBuffer target) {
        return target.duplicate();
      }
    },
    flip {

      public ByteBuffer apply(ByteBuffer target) {
        return (ByteBuffer) target.flip();
      }
    },
    slice {

      public ByteBuffer apply(ByteBuffer target) {
        return target.slice();
      }
    },
    mark {

      public ByteBuffer apply(ByteBuffer target) {
        return (ByteBuffer) target.mark();
      }
    },
    reset {

      public ByteBuffer apply(ByteBuffer target) {
        return (ByteBuffer) target.reset();
      }
    },
    /**
     * exists in both pre and post Cursive atoms.
     */
    rewind {

      public ByteBuffer apply(ByteBuffer target) {
        return (ByteBuffer) target.rewind();
      }
    },
    /**
     * rewinds, dumps to console but returns unchanged buffer
     */
    debug {

      public ByteBuffer apply(ByteBuffer target) {
        System.err.println("%%: " + std.str(target, duplicate, rewind));
        return target;
      }
    },
    ro {

      public ByteBuffer apply(ByteBuffer target) {
        return target.asReadOnlyBuffer();
      }
    },

    /**
     * perfoms get until non-ws returned. then backtracks.by one.
     * <p/>
     * <p/>
     * resets position and throws BufferUnderFlow if runs out of space before success
     */

    forceSkipWs {
      public ByteBuffer apply(ByteBuffer target) {
        int position = target.position();

        while (target.hasRemaining() && Character.isWhitespace(target.get()));
        if (!target.hasRemaining()) {
          target.position(position);
          throw new BufferUnderflowException();
        }
        return bb(target, back1);
      }
    },
    skipWs {
      public ByteBuffer apply(ByteBuffer target) {
        boolean rem, captured = false;
        boolean r;
        while (rem =
            target.hasRemaining()
                && (captured |=
                    (r = Character.isWhitespace(0xff & ((ByteBuffer) target.mark()).get()))) && r);
        return captured && rem ? (ByteBuffer) target.reset() : captured ? target : null;
      }
    },
    toWs {

      public ByteBuffer apply(ByteBuffer target) {
        while (target.hasRemaining() && !Character.isWhitespace(target.get())) {
        }
        return target;
      }
    },
    /**
     * @throws java.nio.BufferUnderflowException if EOL was not reached
     */
    forceToEol {

      public ByteBuffer apply(ByteBuffer target) {
        while (target.hasRemaining() && '\n' != target.get()) {
        }
        if (!target.hasRemaining()) {
          throw new BufferUnderflowException();
        }
        return target;
      }
    },
    /**
     * makes best-attempt at reaching eol or returns end of buffer
     */
    toEol {

      public ByteBuffer apply(ByteBuffer target) {
        while (target.hasRemaining() && '\n' != target.get()) {
        }
        return target;
      }
    },
    back1 {

      public ByteBuffer apply(ByteBuffer target) {
        int position = target.position();
        return (ByteBuffer) (0 < position ? target.position(position - 1) : target);
      }
    },
    /**
     * reverses position _up to_ 2.
     */
    back2 {

      public ByteBuffer apply(ByteBuffer target) {
        int position = target.position();
        return (ByteBuffer) (1 < position ? target.position(position - 2) : bb(target, back1));
      }
    },
    /**
     * reduces the position of target until the character is non-white.
     */
    rtrim {

      public ByteBuffer apply(ByteBuffer target) {
        int start = target.position(), i = start;
        while (0 <= --i && Character.isWhitespace(target.get(i))) {
        }

        return (ByteBuffer) target.position(++i);
      }
    },

    /**
     * noop
     */
    noop {
      public ByteBuffer apply(ByteBuffer target) {
        return target;
      }
    },
    skipDigits {

      public ByteBuffer apply(ByteBuffer target) {
        while (target.hasRemaining() && Character.isDigit(target.get())) {
        }
        return target;
      }
    }
  }

  enum post implements Cursive {
    compact {
      public ByteBuffer apply(ByteBuffer target) {
        return target.compact();
      }
    },
    reset {

      public ByteBuffer apply(ByteBuffer target) {
        return (ByteBuffer) target.reset();
      }
    },
    rewind {

      public ByteBuffer apply(ByteBuffer target) {
        return (ByteBuffer) target.rewind();
      }
    },
    clear {

      public ByteBuffer apply(ByteBuffer target) {
        return (ByteBuffer) target.clear();
      }

    },
    grow {

      public ByteBuffer apply(ByteBuffer target) {
        return std.grow(target);
      }

    },
    ro {

      public ByteBuffer apply(ByteBuffer target) {
        return target.asReadOnlyBuffer();
      }
    },
    /**
     * fills remainder of buffer to 0's
     */
    pad0 {

      public ByteBuffer apply(ByteBuffer target) {
        while (target.hasRemaining()) {
          target.put((byte) 0);
        }
        return target;
      }
    },
    /**
     * fills prior bytes to current position with 0's
     */
    pad0Until {
      public ByteBuffer apply(ByteBuffer target) {
        int limit = target.limit();
        target.flip();
        while (target.hasRemaining()) {
          target.put((byte) 0);
        }
        return (ByteBuffer) target.limit(limit);
      }
    }
  }
}
