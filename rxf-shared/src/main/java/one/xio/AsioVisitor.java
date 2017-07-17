package one.xio;

import bbcursive.std;
import one.xio.AsioVisitor.FSM.sslBacklog;
import one.xio.AsyncSingletonServer.SingleThreadSingletonServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static bbcursive.Cursive.pre.flip;
import static bbcursive.lib.log.log;
import static bbcursive.lib.push.push;
import static bbcursive.std.*;
import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static one.xio.AsioVisitor.Helper.REALTIME_CUTOFF;
import static one.xio.AsioVisitor.Helper.REALTIME_UNIT;
import static one.xio.Pair.pair;

/**
 * User: jim Date: 4/15/12 Time: 11:50 PM
 */
public interface AsioVisitor {
  boolean $DBG = "true".equals(Config.get("DEBUG_VISITOR_ORIGINS", "false"));
  Map<Impl, String> $origins = $DBG ? new WeakHashMap<Impl, String>() : null;

  void onRead(SelectionKey key) throws Exception;

  void onConnect(SelectionKey key) throws Exception;

  void onWrite(SelectionKey key) throws Exception;

  void onAccept(SelectionKey key) throws Exception;

  class FSM {
    public static final boolean DEBUG_SENDJSON = Config.get("DEBUG_SENDJSON", "false").equals(
        "true");
    public static Thread selectorThread;
    public static Selector selector;
    public static Map<SelectionKey, SSLEngine> sslState = new WeakHashMap<>();
    /**
     * stores {InterestOps,{attachment}}
     */
    public static Map<SelectionKey, Pair<Integer, Object>> sslGoal = new WeakHashMap<>();
    private static ExecutorService executorService;

    /**
     * handles SslEngine state NEED_TASK.creates a phaser and launches all threads with invokeAll
     */
    public static void delegateTasks(final Pair<SelectionKey, SSLEngine> state)
        throws InterruptedException {
      SelectionKey key = state.getA();
      List<Callable<Void>> runnables = new ArrayList<>();
      Runnable t;
      final AtomicReference<CyclicBarrier> barrier = new AtomicReference<>();
      SSLEngine sslEngine = state.getB();
      while (null != (t = sslEngine.getDelegatedTask())) {
        final Runnable finalT1 = t;
        runnables.add(new Callable<Void>() {
          public Void call() throws Exception {
            finalT1.run();
            barrier.get().await(REALTIME_CUTOFF, REALTIME_UNIT);
            return null;
          }
        });
      }
      barrier.set(new CyclicBarrier(runnables.size(), new Runnable() {
        @Override
        public void run() {
          try {
            handShake(state);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }));

      assert null != getExecutorService() : "must install FSM executorService!";
      getExecutorService().invokeAll(runnables);

    }

    public static void setExecutorService(ExecutorService svc) {
      executorService = svc;
    }

    /**
     * this is a beast.
     */
    public static void handShake(Pair<SelectionKey, SSLEngine> state) throws Exception {
      if (!state.getA().isValid())
        return;
      SSLEngine sslEngine = state.getB();

      HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
      System.err.println("hs: " + handshakeStatus);

      switch (handshakeStatus) {
        case NEED_TASK:
          delegateTasks(state);
          break;
        case NOT_HANDSHAKING:
        case FINISHED:
          SelectionKey key = state.getA();
          Pair<Integer, Object> integerObjectPair = sslGoal.remove(key);
          sslState.put(key, sslEngine);
          Integer a = integerObjectPair.getA();
          Object b = integerObjectPair.getB();
          key.interestOps(a).attach(b);
          key.selector().wakeup();
          break;
        case NEED_WRAP:
          needWrap(state);
          break;
        case NEED_UNWRAP:
          needUnwrap(state);
          break;
      }
    }

    public static void needUnwrap(final Pair<SelectionKey, SSLEngine> state) throws Exception {
      final ByteBuffer fromNet = sslBacklog.fromNet.resume(state);
      ByteBuffer toApp = sslBacklog.toApp.resume(state);
      SSLEngine sslEngine = state.getB();
      SSLEngineResult unwrap = sslEngine.unwrap((ByteBuffer) fromNet.flip(), toApp);
      System.err.println("" + unwrap);
      fromNet.compact();

      Status status = unwrap.getStatus();
      SelectionKey key = state.getA();
      switch (status) {
        case BUFFER_UNDERFLOW:
          key.interestOps(OP_READ).attach(new Impl() {
            public void onRead(SelectionKey key) throws Exception {
              int read = ((SocketChannel) key.channel()).read(fromNet);
              if (-1 == read) {
                key.cancel();
              } else {
                handShake(state);
              }
            }
          });
          key.selector().wakeup();
          break;
        case OK:
          handShake(state);

          break;
        case BUFFER_OVERFLOW:
          handShake(state);
          break;
        case CLOSED:
          state.getA().cancel();
          break;

      }
    }

    public static void needWrap(Pair<SelectionKey, SSLEngine> state) throws Exception {

      ByteBuffer toNet = sslBacklog.toNet.resume(state);
      ByteBuffer fromApp = sslBacklog.fromApp.resume(state);

      SSLEngine sslEngine = state.getB();
      SSLEngineResult wrap = sslEngine.wrap(bb(fromApp, flip), toNet);
      System.err.println("wrap: " + wrap);
      switch (wrap.getStatus()) {
        case BUFFER_UNDERFLOW:
          throw new Error("not supposed to happen here");
        case OK:
          SocketChannel channel = (SocketChannel) state.getA().channel();
          channel.write((ByteBuffer) toNet.flip());
          toNet.compact();
          fromApp.compact();
          handShake(state);
          return;
        case BUFFER_OVERFLOW:
          throw new Error("buffer size impossible");
        case CLOSED:
          state.getA().cancel();
      }

    }

    public static ExecutorService getExecutorService() {
      return executorService;
    }

    enum sslBacklog {
      fromNet, toNet, fromApp, toApp;
      public Map<SelectionKey, ByteBuffer> per = new WeakHashMap<>();

      public ByteBuffer resume(Pair<SelectionKey, SSLEngine> state) {
        SelectionKey key = state.getA();
        ByteBuffer buffer = on(key);
        if (null == buffer) {
          SSLEngine sslEngine = state.getB();
          on(key, buffer = ByteBuffer.allocateDirect(sslEngine.getSession().getPacketBufferSize()));
        }
        return buffer;
      }

      public ByteBuffer on(SelectionKey key) {
        ByteBuffer byteBufferPairPair = per.get(key);
        return byteBufferPairPair;
      }

      public SelectionKey on(SelectionKey key, ByteBuffer buffer) {
        per.put(key, buffer);
        return key;
      }
    }
  }

  class Helper {
    public static final ByteBuffer[] NIL = new ByteBuffer[] {};
    public static final TimeUnit REALTIME_UNIT = TimeUnit.valueOf(Config.get("REALTIME_UNIT",
        TimeUnit.MINUTES.name()));
    public static final Integer REALTIME_CUTOFF = Integer.parseInt(Config.get("REALTIME_CUTOFF",
        "3"));

    /**
     * called once client is connected, but before any bytes are read or written from socket.
     * 
     * @param host ssl remote host
     * @param port ssl remote port
     * @param asioVisitor
     * @param clientOps ussually OP_WRITE but OP_READ for non-http protocols as well
     * @throws Exception
     */
    public static void sslClient2(final String host, final int port, final Impl asioVisitor,
        final int clientOps) throws Exception {
      SocketChannel open = SocketChannel.open();
      open.configureBlocking(false);
      final InetSocketAddress remote = new InetSocketAddress(host, port);
      open.connect(remote);
      finishConnect(open, new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          log(key, "ssl", remote.toString());
          SSLEngine sslEngine = SSLContext.getDefault().createSSLEngine(host, port);
          sslEngine.setUseClientMode(true);
          sslEngine.setWantClientAuth(false);
          FSM.sslState.put(key, sslEngine);
          FSM.sslGoal.put(key, Pair.<Integer, Object> pair(clientOps, asioVisitor));
          FSM.needWrap(pair(key, sslEngine));
        }
      });

    }

    public static void sslClient2(URI uri, Impl asioVisitor, int clientOps) throws Exception {
      log(uri, "sslClient");
      int port = uri.getPort();
      if (port == -1)
        port = 443;
      String host = uri.getHost();
      sslClient2(host, port, asioVisitor, clientOps);

    }

    public static Impl toRead(final F f) {
      return new Impl() {
        public void onRead(SelectionKey key) throws Exception {
          // toRead begin
          f.apply(key);
          // toRead end
        }
      };
    }

    public static void toRead(SelectionKey key, F f) {
      log(key, "toRead", f.toString());
      SSLEngine sslEngine = FSM.sslState.get(key);
      key.interestOps(OP_READ).attach(toRead(f));
      if (null != sslEngine && sslBacklog.toApp.resume(pair(key, sslEngine)).hasRemaining()) {
        try {
          f.apply(key);
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else
        key.selector().wakeup();

    }

    public static void toRead(SelectionKey key, Impl impl) {
      // toRead
      key.interestOps(OP_READ).attach(impl);
      key.selector().wakeup();
    }

    public static void toConnect(SelectionKey key, F f) {
      toConnect(key, toConnect(f));
    }

    public static void toConnect(SelectionKey key, Impl impl) {// toConnect
      log(key, "toConnect", impl.toString());
      key.interestOps(OP_CONNECT).attach(impl);
      key.selector().wakeup();
    }

    public static void finishConnect(String host, int port, F onSuccess) throws Exception { // finishConnect

      SocketChannel open = SocketChannel.open();
      open.configureBlocking(false);
      open.connect(new InetSocketAddress(host, port));
      finishConnect(open, onSuccess);
    }

    public static void finishConnect(final SocketChannel channel, final F onSuccess)
        throws Exception {
      // finishConnect
      SingleThreadSingletonServer.enqueue(channel, OP_CONNECT, toConnect(new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          if (channel.finishConnect()) {
            onSuccess.apply(key);
          }
        }
      }));
    }

    public static void toWrite(SelectionKey key, F f) {
      // toWrite
      toWrite(key, toWrite(f));
    }

    public static void toWrite(SelectionKey key, Impl impl) {
      // toWrite
      key.interestOps(OP_WRITE).attach(impl);
      key.selector().wakeup();
    }

    public static Impl toWrite(final F f) {
      return new Impl() {
        public void onWrite(SelectionKey key) throws Exception {
          f.apply(key);
        }
      };
    }

    public static Impl toConnect(final F f) {
      // toConnect
      return new Impl() {
        public void onConnect(SelectionKey key) throws Exception {
          f.apply(key);
        }
      };
    }

    public static Selector getSelector() {
      return FSM.selector;
    }

    public static void setSelector(Selector selector) {
      FSM.selector = selector;
    }

    public static Impl finishRead(final ByteBuffer payload, final Runnable success) {
      // finishRead
      return toRead(new F() {
        public void apply(SelectionKey key) throws Exception {
          if (payload.hasRemaining()) {
            int read = read(key, payload);
            if (-1 == read) {
              key.cancel();
            }
          }
          if (!payload.hasRemaining()) {
            success.run();// warning, will not remove READ_OP from interest. you are responsible for steering the
                          // outcome
          }
        }
      });
    }

    public static void finishRead(SelectionKey key, ByteBuffer payload, Runnable success) {
      if (payload.hasRemaining())
        toRead(key, finishRead(payload, success));
      else
        success.run();
    }

    public static Impl finishWrite(final Runnable success, ByteBuffer... payload) {
      final ByteBuffer cursor = std.cat(payload);

      return toWrite(new F() {
        public void apply(SelectionKey key) throws Exception {
          int write = write(key, cursor);
          if (-1 == write)
            key.cancel();
          if (!cursor.hasRemaining())
            success.run();
        }
      });
    }

    public static Impl finishWrite(ByteBuffer payload, Runnable onSuccess) {
      return finishWrite(onSuccess, payload);
    }

    public static void finishWrite(SelectionKey key, Runnable onSuccess, ByteBuffer... payload) {
      toWrite(key, finishWrite(onSuccess, payload));
    }

    public static Impl finishRead(final ByteBuffer payload, final F success) {
      return toRead(new F() {
        public void apply(SelectionKey key) throws Exception {
          if (payload.hasRemaining()) {
            int read = read(key, payload);
            if (-1 == read)
              key.cancel();
          }
          if (!payload.hasRemaining())
            success.apply(key);// warning, will not remove READ_OP from interest. you are responsible for steering the
                               // outcome
        }
      });
    }

    public static void finishRead(SelectionKey key, ByteBuffer payload, F success) {
      log(key, "finishRead");
      if (!payload.hasRemaining()) {
        try {
          success.apply(key);
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        toRead(key, finishRead(payload, success));
      }
    }

    public static Impl finishWrite(final F success, ByteBuffer... src1) {
      log(success, "finishWrite");
      final ByteBuffer src = std.cat(src1);
      return toWrite(new F() {
        public void apply(SelectionKey key) throws Exception {
          int write = write(key, src);
          if (-1 == write)
            key.cancel();
          if (!src.hasRemaining())
            success.apply(key);
        }
      });
    }

    public static void finishWrite(ByteBuffer payload, F onSuccess) {
      finishWrite(onSuccess, payload);
    }

    public static void finishWrite(SelectionKey key, F onSuccess, ByteBuffer... payload) {
      log(onSuccess, "finishWrite-pre");
      ByteBuffer cursor = std.cat(payload);
      try {
        SocketChannel channel = (SocketChannel) key.channel();
        if (cursor.hasRemaining())
          channel.write(cursor);
        if (cursor.hasRemaining())
          toWrite(key, finishWrite(onSuccess, cursor));
        else
          try {
            onSuccess.apply(key);
          } catch (Exception e) {
            e.printStackTrace();
          }
      } catch (IOException e) {
        e.printStackTrace();
      }

    }

    public static int read(Channel channel, ByteBuffer fromNet) throws Exception {
      return read(((SocketChannel) channel).keyFor(getSelector()), fromNet);
    }

    public static int write(Channel channel, ByteBuffer fromApp) throws Exception {
      return write(((SocketChannel) channel).keyFor(getSelector()), fromApp);
    }

    public static int write(SelectionKey key, ByteBuffer src) throws Exception {
      SSLEngine sslEngine = FSM.sslState.get(key);
      int write = 0;
      if (null == sslEngine) {
        write = ((SocketChannel) key.channel()).write(src);
        return write;
      }
      ByteBuffer toNet = sslBacklog.toNet.resume(pair(key, sslEngine));
      ByteBuffer fromApp = sslBacklog.fromApp.resume(pair(key, sslEngine));
      ByteBuffer origin = src.duplicate();
      push(src, fromApp);
      SSLEngineResult wrap = sslEngine.wrap((ByteBuffer) fromApp.flip(), toNet);
      fromApp.compact();
      log("write:wrap: " + wrap);

      switch (wrap.getHandshakeStatus()) {
        case NOT_HANDSHAKING:
        case FINISHED:
          Status status = wrap.getStatus();
          switch (status) {
            case BUFFER_OVERFLOW:
            case OK:
              SocketChannel channel = (SocketChannel) key.channel();
              int ignored = channel.write((ByteBuffer) toNet.flip());
              toNet.compact();
              int i = src.position() - origin.position();
              return i;
            case CLOSED:
              key.cancel();
              return -1;
          }
          break;
        case NEED_TASK:
        case NEED_WRAP:
        case NEED_UNWRAP:
          sslPush(key, sslEngine);
          break;
      }
      return 0;
    }

    public static int read(SelectionKey key, ByteBuffer toApp) throws Exception {
      SSLEngine sslEngine = FSM.sslState.get(key);
      int read = 0;
      if (null == sslEngine) {
        read = ((SocketChannel) key.channel()).read(toApp);
        return read;
      }
      ByteBuffer fromNet = sslBacklog.fromNet.resume(pair(key, sslEngine));
      read = ((SocketChannel) key.channel()).read(fromNet);
      ByteBuffer overflow = sslBacklog.toApp.resume(pair(key, sslEngine));
      ByteBuffer origin = toApp.duplicate();
      SSLEngineResult unwrap = sslEngine.unwrap(bb(fromNet, flip), overflow);
      push(bb(overflow, flip), toApp);
      if (overflow.hasRemaining())
        log("**!!!* sslBacklog.toApp retaining " + overflow.remaining() + " bytes");
      overflow.compact();
      fromNet.compact();
      log("read:unwrap: " + unwrap);
      Status status = unwrap.getStatus();
      switch (unwrap.getHandshakeStatus()) {
        case NOT_HANDSHAKING:
        case FINISHED:
          switch (status) {
            case BUFFER_UNDERFLOW:
              if (-1 == read)
                key.cancel();

            case OK:
              int i = toApp.position() - origin.position();
              return i;

            case CLOSED:
              key.cancel();
              return -1;

          }

          break;
        case NEED_TASK:
        case NEED_WRAP:
        case NEED_UNWRAP:
          sslPush(key, sslEngine);
          break;
      }

      return 0;
    }

    public static void sslPop(SelectionKey key) {
      Pair<Integer, Object> remove = FSM.sslGoal.remove(key);
      key.interestOps(remove.getA()).attach(remove.getB());
      key.selector().wakeup();
    }

    public static void sslPush(SelectionKey key, SSLEngine engine) throws Exception {
      log(key, "sslPush");
      FSM.sslGoal.put(key, pair(key.interestOps(), key.attachment()));
      FSM.handShake(pair(key, engine));
    }

    /**
     * like finishWrite however does not coalesce buffers together
     * 
     * @param key
     * @param success
     * @param payload
     */
    public static void finishWriteSeq(SelectionKey key, final Runnable success,
        final ByteBuffer... payload) {
      finishWriteSeq(key, new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          success.run();
        }
      }, payload);
    }

    /**
     * like finishWrite however does not coalesce buffers together
     * 
     * @param key
     * @param success
     * @param payload
     */
    public static void finishWriteSeq(final SelectionKey key, final F success,
        final ByteBuffer... payload) {
      log(key, "finishWriteSeq");
      key.interestOps(OP_WRITE).attach(toWrite(new F() {
        public int c;

        public void apply(SelectionKey key1) throws Exception {
          for (;;) {
            final ByteBuffer cursor = payload[c];
            int write = write(key1, cursor);
            if (-1 == write) {
              key1.cancel();
              return;
            }
            if (cursor.hasRemaining())
              return;
            payload[c] = null;
            c++;
            if (payload.length == c) {
              success.apply(key);
              return;
            }
          }
        }
      }));
      key.selector().wakeup();
    }

    public static void toConnect(InetSocketAddress socketAddress, F f) throws IOException {
      SocketChannel open = SocketChannel.open();
      open.configureBlocking(false);
      open.connect(socketAddress);
      SelectionKey register = open.register(getSelector(), OP_CONNECT);
      toConnect(register, f);

    }

    /**
     * makes a key/channel go buh-bye.
     */
    public static void bye(SelectionKey key) {
      log(key, "bye");
      try {
        try {
          // SSLEngine sslEngine = FSM.sslState.get(key);
          // if (null != sslEngine) {
          // // log(sslEngine.isInboundDone(), "sslEngine.isInboundDone()");
          // // log(sslEngine.isOutboundDone(), "sslEngine.isOutboundDone()");
          // // Pair<SelectionKey, SSLEngine> pair = pair(key, sslEngine);
          // // log(sslBacklog.toApp.resume(pair).toString(), "sslBacklog.toApp");
          // // log(sslBacklog.fromNet.resume(pair).toString(), "sslBacklog.fromNet");
          // // log(sslBacklog.toNet.resume(pair).toString(), "sslBacklog.toNet");
          // // log(sslBacklog.fromApp.resume(pair).toString(), "sslBacklog.fromApp");
          // // sslEngine.closeInbound();
          // // sslEngine.closeOutbound();
          // // FSM.needWrap(pair( key, sslEngine));
          // }
          key.cancel();
        } catch (Throwable e) {

        }
        key.channel().close();
      } catch (Throwable e) {

      }
    }

    public static void park(SelectionKey key, final F then) throws Exception {
      key.interestOps(0);
      then.apply(key);
    }

    public static <T extends F> T park(final F... then) throws Exception {
      return (T) new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          key.interestOps(0);
          if (then.length > 0) {
            then[0].apply(key);
          }
        }
      };
    }

    public static F terminate(final F... then) {
      log(then.length > 0 ? then[0] : "-", "terminate");
      return new F() {
        @Override
        public void apply(SelectionKey deadKeyWalking) throws Exception {

          bye(deadKeyWalking);
          if (then.length > 0) {
            F f = then[0];
            f.apply(deadKeyWalking);
          }
        }
      };
    }

    /**
     * barriers and locks sometimes want thier own thread.
     * 
     * @param onSuccess
     * @return
     */
    public static F isolate(final F onSuccess) {
      return new F() {
        @Override
        public void apply(final SelectionKey key) throws Exception {
          FSM.getExecutorService().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              onSuccess.apply(key);

              return null;
            }
          });
        }
      };
    }

    public interface F {
      void apply(SelectionKey key) throws Exception;
    }
  }

  class Impl implements AsioVisitor {
    {
      if ($DBG)
        $origins.put(this, wheresWaldo(4));
    }

    /**
     * tracking aid
     * 
     * @param depth typically 2 is correct
     * @return a stack trace string that intellij can hyperlink
     */
    public static String wheresWaldo(int... depth) {
      int d = 0 < depth.length ? depth[0] : 2;
      Throwable throwable = new Throwable();
      Throwable throwable1 = throwable.fillInStackTrace();
      StackTraceElement[] stackTrace = throwable1.getStackTrace();
      StringBuilder ret = new StringBuilder();
      for (int i = 2, end = min(stackTrace.length - 1, d); i <= end; i++) {
        StackTraceElement stackTraceElement = stackTrace[i];
        ret.append("\tat ").append(stackTraceElement.getClassName()).append(".").append(
            stackTraceElement.getMethodName()).append("(").append(stackTraceElement.getFileName())
            .append(":").append(stackTraceElement.getLineNumber()).append(")\n");

      }
      return ret.toString();
    }

    public void onRead(SelectionKey key) throws Exception {
      System.err.println("fail: " + key.toString());
      int receiveBufferSize = 4 << 10;
      String trim = UTF_8.decode(ByteBuffer.allocateDirect(receiveBufferSize)).toString().trim();

      throw new UnsupportedOperationException("found " + trim + " in "
          + getClass().getCanonicalName());
    }

    /**
     * this doesn't change very often for outbound web connections
     * 
     * @param key
     * @throws Exception
     */
    public void onConnect(SelectionKey key) throws Exception {
      if (((SocketChannel) key.channel()).finishConnect())
        key.interestOps(OP_WRITE);
    }

    public void onWrite(SelectionKey key) throws Exception {
      SocketChannel channel = (SocketChannel) key.channel();
      System.err.println("buffer underrun?: " + channel.socket().getRemoteSocketAddress());
      throw new UnsupportedOperationException("found in " + getClass().getCanonicalName());
    }

    /**
     * HIGHLY unlikely to solve a problem with OP_READ | OP_WRITE, each network socket protocol typically requires one
     * or the other but not both.
     * 
     * @param key the serversocket key
     * @throws Exception
     */
    public void onAccept(SelectionKey key) throws Exception {

      ServerSocketChannel c = (ServerSocketChannel) key.channel();
      SocketChannel accept = c.accept();
      accept.configureBlocking(false);
      accept.register(key.selector(), OP_READ | OP_WRITE, key.attachment());
    }
  }
}
