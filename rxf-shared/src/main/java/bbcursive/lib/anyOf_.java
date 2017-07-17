package bbcursive.lib;

import bbcursive.Cursive.pre;
import bbcursive.ann.Backtracking;
import bbcursive.vtables._edge;
import bbcursive.vtables._ptr;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import static bbcursive.std.*;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.deepToString;

/**
 * Created by jim on 1/17/16.
 */
public class anyOf_ {

  public static final EnumSet<traits> NONE_OF = EnumSet.noneOf(traits.class);

  public static UnaryOperator<ByteBuffer> anyOf(UnaryOperator<ByteBuffer>... anyOf) {


        return new UnaryOperator<ByteBuffer>() {

            @Override
            public String toString() {
                return "any" + deepToString(anyOf);
            }

            @Override
            public ByteBuffer apply(ByteBuffer buffer) {
                int mark = buffer.position();
                if (flags.get().contains(traits.skipper)) {
                    ByteBuffer apply = pre.skipWs.apply(buffer);
                    buffer = apply == null ? (ByteBuffer) buffer.position(mark) : apply;
                    if (!buffer.hasRemaining()) {
                        return null;
                    }
                }
                mark = buffer.position();
                int[] offsets = {mark, mark};
                Set[] flaggs = {NONE_OF};


                ByteBuffer[] r = {null};
                final ByteBuffer[] finalBuffer = {buffer};

                Arrays.stream(anyOf)/*.parallel()*/
                        .map((Function<UnaryOperator<ByteBuffer>, _edge<_edge<Set<traits>,
                                _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr>>)
                                op -> new _edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr>() {
                                    private final ByteBuffer buffer = finalBuffer[0];

                                    @Override
                                    protected _ptr at() {
                                        return r$();
                                    }

                                    @Override
                                    protected _ptr goTo(_ptr ptr) {
                                        throw new Error("trifling with an immutable pointer");
                                    }

                                    /**
                                     * this binds a pointer to a pair of ByteBuffer and Integer.  note the bytebuffer
                                     * is mutated by this operation and will corrupt the source stream if this isn't
                                     * a slice or a duplicate
                                     *
                                     * @return the _ptr
                                     */
                                    @Override

                                    protected _ptr r$() {

                                        return (_ptr) new _ptr().bind(
                                                (ByteBuffer) buffer.duplicate().position(offsets[1]), offsets[0]);
                                    }

                                    @Override
                                    public _edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>> core(_edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr>... e) {
                                        return new _edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>() {
                                            @Override
                                            public Set<traits> core(_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>... e) {
                                                return flaggs[0];
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
                                                        return op;
                                                    }

                                                    @Override
                                                    protected Integer r$() {
                                                        return offsets[1];
                                                    }
                                                };
                                            }
                                        };
                                    }
                                }).filter(
                        ed -> {
                            UnaryOperator<ByteBuffer> op = ed.core().location().core();
                            Integer newPosition = ed.location().location();
                            ByteBuffer byteBuffer = (ByteBuffer) ed.location().core().duplicate().position(newPosition);
                            ByteBuffer res = op.apply(byteBuffer);
                            if (null != res) {
                                offsets[1] = res.position();
                                flaggs[0] = EnumSet.copyOf(flags.get());
                                return true;
                            }
                            return false;
                        })
                        .findFirst().ifPresent(
                        edge_ptr_edge -> {
                            Consumer<_edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer>, Integer>>, _ptr>> edgeConsumer = outbox.get();
                            edgeConsumer.accept(edge_ptr_edge);
                            r[0] = (ByteBuffer) finalBuffer[0].position(offsets[1]);
                        });

                return r[0];
            }
        };
    }

  @Backtracking
    public static UnaryOperator<ByteBuffer> anyIn(CharSequence s) {
        int[] ints = s.chars().sorted().toArray();
        return new UnaryOperator<ByteBuffer>() {
            @Override
            public String toString() {
                StringBuilder b=new StringBuilder();
                IntStream.of(ints).forEach(i -> b.append((char) (i & 0xffff)));
                return "in"+Arrays.deepToString(new String[]{b.toString()});
            }

            @Override
            public ByteBuffer apply(ByteBuffer b) {
                ByteBuffer r = null;
                if (null != b && b.hasRemaining()) {
                    byte b1 = b.get();
                    if (-1 < binarySearch(ints, b1 & 0xff))
                        r = b;
                }
                return r;
            }
        };
    }
}
