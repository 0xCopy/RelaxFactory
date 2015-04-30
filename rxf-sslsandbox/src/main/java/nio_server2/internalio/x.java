///**
// *
// */
//package nio_server2.internalio;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.channels.*;
//
//import java.util.concurrent.*;
//
//import javax.net.ssl.*;
//
//import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;
//
///**
// * Does IO based on a <code>SocketChannel</code> with all data encrypted using
// * SSL.
// *
// * @author ben
// *
// */
//public class SecureIO extends InsecureIO {
//  /**
//   * SSLTasker is responsible for dealing with all long running tasks required
//   * by the SSLEngine
//   *
//   * @author ben
//   *
//   */
//  private class SSLTasker implements Runnable {
//    /**
//     * @inheritDoc
//     */
//    public void run() {
//      Runnable r;
//      while ((r = engine.getDelegatedTask()) != null) {
//        r.run();
//      }
//      if (inNet.position() > 0) {
//        regnow(); // we may already have read what is needed
//      }
//      try {
//        System.out.println(":" + engine.getHandshakeStatus());
//        switch (engine.getHandshakeStatus()) {
//          case NOT_HANDSHAKING:
//            break;
//          case FINISHED:
//            System.err.println("Detected FINISHED in tasker");
//            Thread.dumpStack();
//            break;
//          case NEED_TASK:
//            System.err.println("Detected NEED_TASK in tasker");
//            assert false;
//            break;
//          case NEED_WRAP:
//            rereg(SelectionKey.OP_WRITE);
//            break;
//          case NEED_UNWRAP:
//            rereg(SelectionKey.OP_READ);
//            break;
//        }
//      } catch (IOException e) {
//        e.printStackTrace();
//        try {
//          shutdown();
//        } catch (IOException ex) {
//          ex.printStackTrace();
//        }
//      }
//      hsStatus = engine.getHandshakeStatus();
//      isTasking = false;
//    }
//  }
//
//  private SSLEngine engine;
//
//  private ByteBuffer inNet; // always cleared btwn calls
//
//  private ByteBuffer outNet; // when hasRemaining, has data to write.
//
//  private static final ByteBuffer BLANK = ByteBuffer.allocate(0);
//
//  private boolean initialHandshakeDone = false;
//
//  private volatile boolean isTasking = false;
//
//  private boolean handshaking = true;
//
//  private SSLEngineResult.HandshakeStatus hsStatus = NEED_UNWRAP;
//
//  private boolean shutdownStarted;
//
//  private static Executor executor = getDefaultExecutor();
//
//  private SSLTasker tasker = new SSLTasker();
//
//  private ByteBuffer temp;
//
//  /**
//   * @return the default <code>Executor</code>
//   */
//  public static Executor getDefaultExecutor() {
//    return new ThreadPoolExecutor(3, Integer.MAX_VALUE, 60L,
//        TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
//        new DaemonThreadFactory());
//  }
//
//  private static class DaemonThreadFactory implements ThreadFactory {
//    private static ThreadFactory defaultFactory = Executors
//        .defaultThreadFactory();
//
//    /**
//     * Creates a thread using the default factory, but sets it to be daemon
//     * before returning it
//     *
//     * @param r
//     *            the runnable to run
//     * @return the new thread
//     */
//    public Thread newThread(Runnable r) {
//      Thread t = defaultFactory.newThread(r);
//      t.setDaemon(true);
//      return t;
//    }
//
//  }
//
//  /**
//   * @return the executor currently being used for all long-running tasks
//   */
//  public static Executor getExecutor() {
//    return executor;
//  }
//
//  /**
//   * Changes the executor being used for all long-running tasks. Currently
//   * running tasks will still use the old executor
//   *
//   * @param executor
//   *            the new Executor to use
//   */
//  public static void setExecutor(Executor executor) {
//    SecureIO.executor = executor;
//  }
//
//  /**
//   * Creates a new <code>SecureIO</code>
//   *
//   * @param channel
//   *            the channel to do IO on.
//   * @param sslCtx
//   *            the <code>SSLContext</code> to use
//   */
//  public SecureIO(SocketChannel channel, SSLContext sslCtx) {
//    super(channel);
//    engine = sslCtx.createSSLEngine();
//    engine.setUseClientMode(false);
//    int size = engine.getSession().getPacketBufferSize();
//    inNet = ByteBuffer.allocate(size);
//    outNet = ByteBuffer.allocate(size);
//    outNet.limit(0);
//    temp = ByteBuffer.allocate(engine.getSession()
//        .getApplicationBufferSize());
//  }
//
//  private void doTasks() throws IOException {
//    rereg(0); // don't do anything until the task is done.
//    isTasking = true;
//    SecureIO.executor.execute(tasker);
//  }
//
//  /**
//   * Does all handshaking required by SSL.
//   *
//   * @param dst
//   *            the destination from an application data read
//   * @return true if all needed SSL handshaking is currently complete.
//   * @throws IOException
//   *             if there are errors in handshaking.
//   *
//   */
//  @Override
//  public boolean doHandshake(ByteBuffer dst) throws IOException {
//
//    if (!handshaking) {
//      return true;
//    }
//    if (dst.remaining() < minBufferSize()) {
//      throw new IllegalArgumentException("Buffer has only "
//          + dst.remaining() + " left + minBufferSize is "
//          + minBufferSize());
//    }
//    if (outNet.hasRemaining()) {
//      if (!flush()) {
//        return false;
//      }
//      switch (hsStatus) {
//        case FINISHED:
//          handshaking = false;
//          initialHandshakeDone = true;
//          rereg(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//          return true;
//        case NEED_UNWRAP:
//          rereg(SelectionKey.OP_READ);
//          break;
//        case NEED_TASK:
//          doTasks();
//          return false;
//        case NOT_HANDSHAKING:
//          throw new RuntimeException(
//              "NOT_HANDSHAKING encountered when handshaking");
//      }
//    }
//    SSLEngineResult res;
//    System.out.println(hsStatus + "1" + handshaking);
//    switch (hsStatus) {
//      case NEED_UNWRAP:
//        int i;
//
//        do {
//          rereg(SelectionKey.OP_READ);
//          i = super.read(inNet);
//          if (i < 0) {
//            engine.closeInbound();
//            handshaking = false;
//            shutdown();
//            return true;
//          }
//          if (i == 0 && inNet.position() == 0) {
//            return false;
//          }
//
//          inloop: do {
//            inNet.flip();
//            temp.clear();
//            res = engine.unwrap(inNet, temp);
//            inNet.compact();
//            temp.flip();
//            if (temp.hasRemaining()) {
//              dst.put(temp);
//            }
//            switch (res.getStatus()) {
//              case OK:
//                hsStatus = res.getHandshakeStatus();
//                if (hsStatus == NEED_TASK) {
//                  doTasks();
//                }
//                // if (hsStatus == FINISHED) {
//                // // if (!initialHandshakeDone) {
//                // // throw new RuntimeException(hsStatus
//                // // + " encountered when handshaking");
//                // // }
//                // initialHandshakeDone = true;
//                // handshaking=false;
//                // key.interestOps(SelectionKey.OP_READ
//                // | SelectionKey.OP_WRITE);
//                // }
//                // TODO check others?
//                break;
//              case BUFFER_UNDERFLOW:
//                break inloop;
//              case BUFFER_OVERFLOW:
//              case CLOSED:
//                throw new RuntimeException(res.getStatus()
//                    + " encountered when handshaking");
//            }
//          } while (hsStatus == NEED_UNWRAP
//              && dst.remaining() >= minBufferSize());
//        } while (hsStatus == NEED_UNWRAP
//            && dst.remaining() >= minBufferSize());
//        if (inNet.position() > 0) {
//          System.err.println(inNet);
//        }
//        if (hsStatus != NEED_WRAP) {
//          break;
//        } // else fall through
//        rereg(SelectionKey.OP_WRITE);
//      case NEED_WRAP:
//        do {
//          outNet.clear();
//          res = engine.wrap(BLANK, outNet);
//          switch (res.getStatus()) {
//            case OK:
//              outNet.flip();
//              hsStatus = res.getHandshakeStatus();
//              if (hsStatus == NEED_TASK) {
//                doTasks();
//                return false;
//              }
//
//              // TODO check others?
//              break;
//            case BUFFER_OVERFLOW:
//              outNet.limit(0);
//              int size = engine.getSession()
//                  .getPacketBufferSize();
//              if (outNet.capacity() < size) {
//                outNet = ByteBuffer.allocate(size);
//              } else { // shouldn't happen
//                throw new RuntimeException(res.getStatus()
//                    + " encountered when handshaking");
//              }
//            case BUFFER_UNDERFLOW: // engine shouldn't care
//            case CLOSED:
//              throw new RuntimeException(res.getStatus()
//                  + " encountered when handshaking");
//          }
//        } while (flush() && hsStatus == NEED_WRAP);
//        break;
//      case NEED_TASK:
//        doTasks();
//        return false;
//      case FINISHED:
//        break; // checked below
//      case NOT_HANDSHAKING:
//        System.err.println(hsStatus + " encountered when handshaking");
//        handshaking = false;
//        initialHandshakeDone = true;
//        rereg(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//    }
//    if (hsStatus == FINISHED) {
//      // if (!initialHandshakeDone) {
//      // throw new RuntimeException(hsStatus
//      // + " encountered when handshaking");
//      // }
//      initialHandshakeDone = true;
//      handshaking = false;
//      rereg(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//    }
//    System.out.println(hsStatus + "2" + handshaking);
//    return !handshaking;
//  }
//
//  /**
//   * Attempts to flush all buffered data to the channel.
//   *
//   * @return true if all buffered data has been written.
//   * @throws IOException
//   *             if there are errors writing the data
//   */
//  @Override
//  public boolean flush() throws IOException {
//    if (!outNet.hasRemaining()) {
//      return true;
//    }
//    super.write(outNet);
//    return !outNet.hasRemaining();
//  }
//
//  /**
//   * @return the largest amount of application data that could be read from
//   *         the channel at once.
//   */
//  @Override
//  public int minBufferSize() {
//    return engine.getSession().getApplicationBufferSize();
//  }
//
//  /**
//   * Begins or proceeds with sending an SSL shutdown message to the client.
//   *
//   * @return true if all needed IO is complete
//   * @throws IOException
//   *             if there are errors sending the message.
//   */
//  @Override
//  public boolean shutdown() throws IOException {
//    if (!shutdownStarted) {
//      shutdownStarted = true;
//      engine.closeOutbound();
//    }
//    if (outNet.hasRemaining() && !flush()) {
//      return false;
//    }
//    SSLEngineResult result;
//    do {
//      outNet.clear();
//      result = engine.wrap(BLANK, outNet);
//      if (result.getStatus() != SSLEngineResult.Status.CLOSED) {
//        throw new IOException("Unexpected result in shutdown:"
//            + result.getStatus());
//      }
//      outNet.flip();
//      if (outNet.hasRemaining() && !flush()) {
//        return false;
//      }
//    } while (result.getHandshakeStatus() == NEED_WRAP);
//    return !outNet.hasRemaining();
//  }
//
//  /**
//   * Reads all possible data into the <code>ByteBuffer</code>.
//   *
//   * @param dst
//   *            the buffer to read into.
//   * @return the number of bytes read, or -1 if the channel or
//   *         <code>SSLEngine</code> is closed
//   * @throws IllegalStateException
//   *             if the initial handshake isn't complete *
//   * @throws IOException
//   *             if there are errors.
//   * @throws IllegalStateException
//   *             if the initial handshake isn't complete
//   * @throws IllegalArgumentException
//   *             if the remaining space in dst is less than
//   *             {@link SecureIO#minBufferSize()}
//   */
//
//  @Override
//  public int read(ByteBuffer dst) throws IOException {
//    if (!initialHandshakeDone) {
//      throw new IllegalStateException("Initial handshake incomplete");
//    }
//    if (dst.remaining() < minBufferSize()) {
//      throw new IllegalArgumentException("Buffer has only "
//          + dst.remaining() + " left + minBufferSize is "
//          + minBufferSize());
//    }
//    int sPos = dst.position();
//    int i;
//    while ((i = super.read(inNet)) != 0
//        && dst.remaining() >= minBufferSize()) {
//      if (i < 0) {
//        engine.closeInbound();
//        shutdown();
//        return -1;
//      }
//      do {
//        inNet.flip();
//        temp.clear();
//        SSLEngineResult result = engine.unwrap(inNet, temp);
//        inNet.compact();
//        temp.flip();
//        if (temp.hasRemaining()) {
//          dst.put(temp);
//        }
//        switch (result.getStatus()) {
//          case BUFFER_UNDERFLOW:
//            continue;
//          case BUFFER_OVERFLOW:
//            throw new Error();
//          case CLOSED:
//            return -1;
//          // throw new IOException("SSLEngine closed");
//          case OK:
//            checkHandshake();
//            break;
//        }
//      } while (inNet.position() > 0);
//    }
//    return dst.position() - sPos;
//  }
//
//  /**
//   * Encrypts data and writes it to the channel.
//   *
//   * @param src
//   *            the data to write
//   * @return the number of bytes written
//   * @throws IOException
//   *             if there are errors.
//   * @throws IllegalStateException
//   *             if the initial handshake isn't complete
//   */
//  @Override
//  public int write(ByteBuffer src) throws IOException {
//    if (!initialHandshakeDone) {
//      throw new IllegalStateException("Initial handshake incomplete");
//    }
//    if (!flush()) {
//      return 0;
//    }
//    int written = 0;
//    outer: while (src.hasRemaining()) {
//      outNet.clear(); // we flushed it
//      SSLEngineResult result = engine.wrap(src, outNet);
//      outNet.flip();
//      switch (result.getStatus()) {
//        case BUFFER_UNDERFLOW:
//          break outer; // not enough left to send (prob won't
//        // happen - padding)
//        case BUFFER_OVERFLOW:
//
//          if (!flush()) {
//            break outer; // can't remake while still have
//            // stuff to write
//          }
//
//          int size = engine.getSession().getPacketBufferSize();
//          if (outNet.capacity() < size) {
//            outNet = ByteBuffer.allocate(size);
//          } else { // shouldn't happen
//            throw new RuntimeException(hsStatus
//                + " encountered when handshaking");
//          }
//          continue; // try again
//        case CLOSED:
//          throw new IOException("SSLEngine closed");
//        case OK:
//          checkHandshake();
//          break;
//      }
//      if (!flush()) {
//        break;
//      }
//    }
//    return written;
//  }
//
//  private boolean hasRemaining(ByteBuffer[] src) {
//    for (ByteBuffer b : src) {
//      if (b.hasRemaining()) {
//        return true;
//      }
//    }
//    return false;
//  }
//
//  /**
//   * Encrypts data and writes it to the channel.
//   *
//   * @param src
//   *            the data to write
//   * @return the number of bytes written
//   * @throws IOException
//   *             if there are errors.
//   * @throws IllegalStateException
//   *             if the initial handshake isn't complete
//   */
//  @Override
//  public long write(ByteBuffer[] src) throws IOException {
//    if (!initialHandshakeDone) {
//      throw new IllegalStateException("Initial handshake incomplete");
//    }
//    if (!flush()) {
//      return 0;
//    }
//    int written = 0;
//    outer: while (hasRemaining(src)) {
//      outNet.clear(); // we flushed it
//      SSLEngineResult result = engine.wrap(src, outNet);
//      outNet.flip();
//      switch (result.getStatus()) {
//        case BUFFER_UNDERFLOW:
//          break outer; // not enough left to send (prob won't
//        // happen - padding)
//        case BUFFER_OVERFLOW:
//
//          if (!flush()) {
//            break outer; // can't remake while still have
//            // stuff to write
//          }
//
//          int size = engine.getSession().getPacketBufferSize();
//          if (outNet.capacity() < size) {
//            outNet = ByteBuffer.allocate(size);
//          } else { // shouldn't happen
//            throw new RuntimeException(hsStatus
//                + " encountered when handshaking");
//          }
//          continue; // try again
//        case CLOSED:
//          throw new IOException("SSLEngine closed");
//        case OK:
//          checkHandshake();
//          break;
//      }
//      if (!flush()) {
//        break;
//      }
//    }
//    return written;
//  }
//
//  private void checkHandshake() throws IOException {
//    // Thread.dumpStack();
//    // System.out.println(engine.getHandshakeStatus());
//    outer: while (true) {
//      switch (engine.getHandshakeStatus()) {
//        case NOT_HANDSHAKING:
//          initialHandshakeDone = true;
//          handshaking = false;
//          rereg(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//          return;
//        case FINISHED:
//          // this shouldn't happen, I don't think. If it does, say
//          // where.
//          System.err.println("Detected FINISHED in checkHandshake");
//          Thread.dumpStack();
//          break outer;
//        case NEED_TASK:
//          if (isTasking) {
//            while (isTasking) { // TODO: deal with by reg?
//              Thread.yield();
//              try {
//                Thread.sleep(1);
//              } catch (InterruptedException ex) {
//                // TODO Auto-generated catch block
//                ex.printStackTrace();
//              }
//            }
//            break;
//          }
//          doTasks();
//          break;
//        case NEED_WRAP:
//          rereg(SelectionKey.OP_WRITE);
//          break outer;
//        case NEED_UNWRAP:
//          rereg(SelectionKey.OP_READ);
//          break outer;
//
//      }
//    }
//    handshaking = true;
//    hsStatus = engine.getHandshakeStatus();
//  }
//
//  /**
//   * @return true if the channel is open and no shutdown message has been
//   *         recieved.
//   */
//  @Override
//  public boolean isOpen() {
//    return super.isOpen() && !engine.isInboundDone();
//  }
//}