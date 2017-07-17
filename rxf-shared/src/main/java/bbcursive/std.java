package bbcursive;

import bbcursive.ann.Backtracking;
import bbcursive.ann.ForwardOnly;
import bbcursive.ann.Infix;
import bbcursive.ann.Skipper;
import bbcursive.lib.u8tf;
import bbcursive.vtables._edge;
import bbcursive.vtables._ptr;
import com.databricks.fastbuffer.ByteBufferReader;
import com.databricks.fastbuffer.JavaByteBufferReader;
import com.databricks.fastbuffer.UnsafeDirectByteBufferReader;
import com.databricks.fastbuffer.UnsafeHeapByteBufferReader;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.lang.Character.isDigit;
import static java.lang.Character.isWhitespace;
import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.copyOfRange;
import static java.util.Arrays.deepToString;
import static java.util.EnumSet.copyOf;
import static java.util.EnumSet.noneOf;


/**
 * Created by jim on 8/8/14.
 */
public class std {


    final private static boolean debug_bbcursive = true;// Objects.equals("true", System.getenv("debug_bbcursive"));
    private static Allocator allocator;

    /**
     * the outbox -- when a parse term successfully returns and a {@link Consumer}is installed as the outbox the
     * following state is published allowing for a recreation of the event elsewhere within the jvm
     * <p>
     * in reverse order of resolution:
     * <p>
     * flags -- from annotations from lambda class
     * UnaryOperator -- the lambda that fired,
     * Integer -- length, to save time moving and scoring the artifact
     * _ptr -- _edge[ByteBuffer,Integer] state pair
     */
    public static ThreadLocal<Consumer<_edge<_edge<Set<traits>,
            _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr>>>
            outbox = ThreadLocal.withInitial(() -> new Consumer<_edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr>>() {
        @Override
        public void accept(_edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr> edge_ptr_edge) {
            // exhaust core()+location() fanout in intellij for a representational constant
            // automate later.
            _edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr> edge_ptr_edge1 = edge_ptr_edge;
            _ptr location = edge_ptr_edge1.location();
            Integer startPosition = location.location();
            _edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>> set_edge_edge = edge_ptr_edge1.core();
            Set<traits> traitsSet = set_edge_edge.core();
            _edge<UnaryOperator<ByteBuffer>, Integer> operatorIntegerEdge = set_edge_edge.location();
            Integer endPosition = operatorIntegerEdge.location();
            UnaryOperator<ByteBuffer> unaryOperator = operatorIntegerEdge.core();
            String s = deepToString(new Integer[]{startPosition, endPosition});
            System.err.println("+++ " + s + unaryOperator + " " + traitsSet);

        }
    });

    /**
     * when you want to change the behaviors of the main IO parser, insert a new {@link BiFunction} to intercept
     * parameters and returns to fire events and clean up using {@link ThreadLocal#set(Object)}
     */
    public enum traits {
        debug, backtracking, skipper;

    }


    public static final ThreadLocal<Set<traits>> flags = ThreadLocal.withInitial((Supplier<? extends Set<traits>>) () -> noneOf(traits.class));


    /**
     * this is the main bytebuffer io parser most easily coded for.
     *
     * @param b   the bytebuffer
     * @param ops
     * @return
     */
    public static ByteBuffer bb(ByteBuffer b, UnaryOperator<ByteBuffer>... ops) {
        ByteBuffer r = null;
        Set<traits> restoration = null;
        UnaryOperator<ByteBuffer> op = null;
        if (null != b && 0 < ops.length && null != (op = ops[0])) {
            ;
            if (debug_bbcursive) System.err.println("??? " + op);
            int startPosition = b.position();

            if (flags.get().contains(traits.skipper)) {
                boolean rem=false;
                while ((rem = b.hasRemaining()) && isWhitespace(((ByteBuffer) b.mark()).get() & 0xff));
                if (rem) b.reset();
            }
            restoration = induct(op.getClass());
            switch (ops.length) {
                case 0:
                    r = b;
                    break;
                case 1:
                    r = op.apply(b);
                    break;

/*save
                case 2:
                    r = bb(bb(b, op), ops[1]);
                    break;
                case 3:
                    r = bb(bb(bb(b, op), ops[1]), ops[2]);
                    break;
                case 4:
                    r = bb(bb(bb(bb(b, op), ops[1]), ops[2]), ops[3]);
                    break;
                case 5:
                    r = bb(bb(bb(bb(bb(b, op), ops[1]), ops[2]), ops[3]), ops[4]);
                    break;
                case 6:
                    r = bb(bb(bb(bb(bb(bb(b, op), ops[1]), ops[2]), ops[3]), ops[4]), ops[5]);
                    break;
*/

                default:
                    r = /*bb(bb(bb(bb(*/bb(bb(b, op), copyOfRange(ops, 1, ops.length));
                    break;
            }

            if (null == r && flags.get().contains(traits.backtracking)) {
                if (debug_bbcursive)
                    System.err.println("--- " + deepToString(new Integer[]{startPosition, b.position()}) + " " + String.valueOf(op));
                r = (ByteBuffer) b.position(startPosition);

            } else if (null != outbox.get()) {
                onSuccess(b, op, startPosition);
            }

        }
        if (restoration != null)
            flags.set(restoration);
        return r;
    }

    public
    static void onSuccess(ByteBuffer b, UnaryOperator<ByteBuffer> byteBufferUnaryOperator, int startPosition) {
        int endPos = b.position();
        Set<traits> immutableTraits = copyOf(flags.get());

        /**
         * creates a slice.  probably a bad idea due to array() b000gz
         */
        std.outbox.get().accept(createSuccessTuple(b, byteBufferUnaryOperator, startPosition, endPos, immutableTraits));
    }

    @NotNull
    public static _edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr> createSuccessTuple(final ByteBuffer b, final UnaryOperator<ByteBuffer> byteBufferUnaryOperator, final int startPosition, final int endPos, final Set<traits> immutableTraits) {
        return new _edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr>() {
            @Override
            protected _ptr at() {
                return r$();
            }

            @Override
            protected _ptr goTo(_ptr ptr) {
                throw new Error("trifling with an immutable pointer");
            }

            /**
             * this binds a pointer to a pair of ByteBuffer and Integer.  note the bytebuffer is mutated by this
             * operation and will corrupt the source stream if this isn't a slice or a duplicate
             *
             *
             * @return the _ptr
             */
            @Override

            protected _ptr r$() {

                return (_ptr) new _ptr().bind(
                        (ByteBuffer) b.duplicate().limit(endPos), startPosition);
            }

            @Override
            public _edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>> core(_edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr>... e) {
                return new _edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>() {
                    @Override
                    public Set<traits> core(_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>... e) {
                        return immutableTraits;
                    }

                    @Override
                    protected _edge<UnaryOperator<ByteBuffer>, Integer> at() {
                        return r$();
                    }

                    @Override
                    protected _edge<UnaryOperator<ByteBuffer>, Integer> goTo(_edge<UnaryOperator<ByteBuffer>, Integer> unaryOperatorInteger_edge) {
                        throw new Error("cant move this");
                    }

                    @Override
                    protected _edge<UnaryOperator<ByteBuffer>, Integer> r$() {
                        return new _edge<UnaryOperator<ByteBuffer>, Integer>() {
                            @Override
                            protected Integer at() {
                                return r$();
                            }

                            @Override
                            protected Integer goTo(Integer integer) {
                                throw new Error("immutable");
                            }

                            @Override
                            public UnaryOperator<ByteBuffer> core(_edge<UnaryOperator<ByteBuffer>, Integer>... e) {
                                return byteBufferUnaryOperator;
                            }

                            @Override
                            protected Integer r$() {
                                return endPos;
                            }
                        };
                    }
                };
            }
        };
    }


    static Map<Class, Set<traits>> termCache = new WeakHashMap<>();

    /**
     * cache terminal flags and use them by class.
     * <p>
     * if class is gc'd, no leak.
     *
     * @param aClass
     * @return the previous (restoration) state
     */
    static Set<traits> induct(Class<? extends UnaryOperator> aClass) {
        Set<traits> c = flags.get();
        Set<traits> traitses = copyOf(c);
        AtomicBoolean dirty = new AtomicBoolean(false);
        if (aClass.isAnnotationPresent(Skipper.class)) {
            dirty.set(true);
            c.add(traits.skipper);
        } else if (aClass.isAnnotationPresent(Infix.class)) {
            dirty.set(true);
            c.remove(traits.skipper);
        }
        if (aClass.isAnnotationPresent(Backtracking.class)) {
            dirty.set(true);
            c.add(traits.backtracking);
        } else if (aClass.isAnnotationPresent(ForwardOnly.class)) {
            dirty.set(true);
            c.remove(traits.backtracking);
        }
        return !dirty.get() ? null : traitses;
    }


    public static <S extends WantsZeroCopy> ByteBuffer bb(S b, UnaryOperator<ByteBuffer>... ops) {
        ByteBuffer b1 = b.asByteBuffer();
        for (int i = 0, opsLength = ops.length; i < opsLength; i++) {
            UnaryOperator<ByteBuffer> op = ops[i];
            if (null == op) {
                b1 = null;
                break;
            }
            b1 = op.apply(b1);
        }
        return b1;
    }

    public static <S extends WantsZeroCopy> ByteBufferReader fast(S zc) {
        return fast(zc.asByteBuffer());
    }

    public static ByteBufferReader fast(ByteBuffer buf) {
        ByteBufferReader r;
        try {
            if (buf.hasArray())
                r = new UnsafeHeapByteBufferReader(buf);
            else
                r = new UnsafeDirectByteBufferReader(buf);

        } catch (UnsupportedOperationException e) {
            r = new JavaByteBufferReader(buf);
        }
        return r;
    }

    /**
     * convenience method
     *
     * @param bytes
     * @param operations
     * @return
     */
    public static String str(ByteBuffer bytes, UnaryOperator<ByteBuffer>... operations) {
        ByteBuffer bb = bb(bytes, operations);
        return UTF_8.decode(bb).toString();
    }

    /**
     * just saves a few chars
     *
     * @param something toString will run on this
     * @param atoms
     * @return
     */
    public static String str(WantsZeroCopy something, UnaryOperator<ByteBuffer>... atoms) {
        return str(something.asByteBuffer(), atoms);
    }

    /**
     * just saves a few chars
     *
     * @param something toString will run on this
     * @param atoms
     * @return
     */
    public static String str(AtomicReference<? extends WantsZeroCopy> something, UnaryOperator<ByteBuffer>... atoms) {
        return str(something.get(), atoms);
    }

    /**
     * just saves a few chars
     *
     * @param something toString will run on this
     * @return
     */
    public static String str(Object something) {
        return String.valueOf(something);
    }

    /**
     * convenience method
     *
     * @param src
     * @param operations
     * @return
     */
    public static <T extends CharSequence> ByteBuffer bb(T src, UnaryOperator<ByteBuffer>... operations) {
        return bb(u8tf.c2b(String.valueOf(src)), operations);
    }

    public static ByteBuffer grow(ByteBuffer src) {
        return allocateDirect(src.capacity() << 1).put(src);
    }

    public static ByteBuffer cat(List<ByteBuffer> byteBuffers) {
        ByteBuffer[] byteBuffers1 = byteBuffers.toArray(new ByteBuffer[byteBuffers.size()]);
        return cat(byteBuffers1);
    }

    public static ByteBuffer cat(ByteBuffer... src) {
        ByteBuffer cursor;
        int total = 0;
        if (1 >= src.length) {
            cursor = src[0];
        } else {
            for (int i = 0, payloadLength = src.length; i < payloadLength; i++) {
                ByteBuffer byteBuffer = src[i];
                total += byteBuffer.remaining();
            }
            cursor = alloc(total);
            for (int i = 0, payloadLength = src.length; i < payloadLength; i++) {
                ByteBuffer byteBuffer = src[i];
                cursor.put(byteBuffer);
            }
            cursor.rewind();
        }
        return cursor;
    }

    public static ByteBuffer alloc(int size) {
        return null != getAllocator() ? getAllocator().allocate(size) : allocateDirect(size);
    }

    public static ByteBufferReader alloca(int size) {
        return fast(alloc(size));
    }

    public static ByteBuffer consumeString(ByteBuffer buffer) {
        //TODO unicode wat?
        while (buffer.hasRemaining()) {
            byte current = buffer.get();
            switch (current) {
                case '"':
                    return buffer;
                case '\\':
                    byte next = buffer.get();
                    switch (next) {
                        case 'u':
                            buffer.position(buffer.position() + 4);
                        default:
                    }
            }
        }
        return buffer;
    }

    public static ByteBuffer consumeNumber(ByteBuffer slice) {
        byte b = ((ByteBuffer) slice.mark()).get();

        boolean sign = '-' == b || '+' == b;
        if (!sign) {
            slice.reset();
        }

        boolean dot = false;
        boolean etoken = false;
        boolean esign = false;
        ByteBuffer r = null;
        while (slice.hasRemaining()) {
            while (slice.hasRemaining() && isDigit(b = ((ByteBuffer) slice.mark()).get())) ;
            switch (b) {
                case '.':
                    assert !dot : "extra dot";
                    dot = true;
                case 'E':
                case 'e':
                    assert !etoken : "missing digits or redundant exponent";
                    etoken = true;
                case '+':
                case '-':
                    assert !esign : "bad exponent sign";
                    esign = true;
                default:
                    if (!isDigit(b)) r = (ByteBuffer) slice.reset();
                    break;
            }
        }
        return r;
    }

    public static Allocator getAllocator() {
        return allocator;
    }

    public static void setAllocator(Allocator allocator) {
        std.allocator = allocator;
    }


}