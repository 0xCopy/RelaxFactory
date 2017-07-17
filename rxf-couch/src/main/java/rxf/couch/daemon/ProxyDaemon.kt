package rxf.couch.daemon

import one.xio.AsioVisitor
import one.xio.AsyncSingletonServer
import one.xio.HttpHeaders
import rxf.core.Config
import rxf.core.Rfc822HeaderState

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.util.Arrays

import java.nio.channels.SelectionKey.*

/**
 *
 *  * Accepts external socket connections per behalf of Couchdb or other REST couch
 *
 * User: jnorthrup Date: 10/1/13 Time: 7:26 PM
 */
class ProxyDaemon(vararg proxyTask: ProxyTask) : AsioVisitor.Impl() {
    var hdrStream: FileChannel? = null
    /**
     * request lead-in data is placed in this buffer.
     */
    internal var cursor: ByteBuffer? = null

    private val proxyTask: ProxyTask

    private var preallocAddr: InetSocketAddress? = null

    init {
        this.proxyTask = if (proxyTask.isNotEmpty()) proxyTask[0] else ProxyTask( )

        if (PROXY_PORT != 0)
            try {
                preallocAddr = InetSocketAddress(InetAddress.getByName(PROXY_HOST), PROXY_PORT)
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            }

    }

    @Throws(Exception::class)
    override fun onAccept(key: SelectionKey) {
        val c = key.channel() as ServerSocketChannel
        val accept = c.accept()
        accept.configureBlocking(false)
        AsyncSingletonServer.SingleThreadSingletonServer.enqueue(accept, OP_READ, this)
    }

    @Throws(Exception::class)
    override fun onRead(outerKey: SelectionKey) {

        if (cursor == null)
            cursor = ByteBuffer.allocate(4 shl 10)
        val outterChannel = outerKey.channel() as SocketChannel
        val read = AsioVisitor.Helper.read(outterChannel, cursor)
        if (-1 != read) {
            val timeHeaders = RPS_SHOW && counter % 1000 == 0
            var l: Long = 0

            if (timeHeaders)
                l = System.nanoTime()
            val req = Rfc822HeaderState().`$req`().headerInterest(
                    HttpHeaders.Host).read(cursor!!.duplicate().flip() as ByteBuffer) as Rfc822HeaderState.HttpRequest
            val headersBuf = req.headerBuf()
            if (Rfc822HeaderState.suffixMatchChunks(TERMINATOR, headersBuf)) {

                val climit = cursor!!.position()
                if (PROXY_DEBUG) {
                    val decode = StandardCharsets.UTF_8.decode(headersBuf.duplicate().rewind() as ByteBuffer)
                            .toString()
                    val split = decode.split("[\r\n]+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    System.err.println(Arrays.deepToString(split))
                }
                req.headerString(HttpHeaders.Host, proxyTask.prefix)
                val address = outterChannel.socket().remoteSocketAddress as InetSocketAddress

                // grab a frame of int offsets
                val headers = HttpHeaders.getHeaders(headersBuf.flip() as ByteBuffer)
                val hosts = headers["Host"]

                val slice2 = StandardCharsets.UTF_8.encode("Host: " + proxyTask.prefix + "\r\nX-Origin-Host: "
                        + address.toString() + "\r\n")

                val position = cursor!!.limit(climit).position(headersBuf.limit())

                val inwardBuffer = ByteBuffer.allocateDirect(8 shl 10).put(
                        cursor!!.clear().limit(1 + hosts!![0] - HOSTPREFIXLEN) as ByteBuffer).put(
                        cursor!!.limit(headersBuf.limit() - 2).position(hosts[1]) as ByteBuffer).put(slice2)
                        .put(position as ByteBuffer)
                cursor = null

                if (PROXY_DEBUG) {
                    val flip = inwardBuffer.duplicate().flip() as ByteBuffer
                    System.err.println(StandardCharsets.UTF_8.decode(flip).toString() + "-")
                    if (timeHeaders)
                        System.err.println("header decode (ns):" + (System.nanoTime() - l))
                }
                counter++

                val innerChannel = SocketChannel.open().configureBlocking(false) as SocketChannel
                val remote: InetSocketAddress?
                when (PROXY_PORT) {
                    0 -> {
                        val localSocketAddress = (outerKey.channel() as SocketChannel).socket()
                                .localSocketAddress as InetSocketAddress
                        remote = InetSocketAddress(InetAddress.getByName(PROXY_HOST), localSocketAddress
                                .port)
                    }
                    else -> remote = preallocAddr
                }
                innerChannel.connect(remote)
                innerChannel.register(outerKey.selector().wakeup(), OP_CONNECT, object : AsioVisitor.Impl() {

                    @Throws(Exception::class)
                    override fun onConnect(key: SelectionKey) {
                        if (innerChannel.finishConnect())
                            pipe(key, outerKey, inwardBuffer, ByteBuffer.allocateDirect(8 shl 10)
                                    .clear() as ByteBuffer)
                    }
                })
            }
        } else
            outerKey.cancel()
    }

    @Throws(Exception::class)
    override fun onWrite(key: SelectionKey) {
        super.onWrite(key) // To change body of overridden methods use File | Settings | File Templates.
    }

    companion object {
        /**
         * until proven otherwise, all http requests must conform to crlf line-endings, and it is the primary termination
         * token we are seeking in bytebuffer operations.
         */
        val TERMINATOR = "\r\n\r\n".toByteArray()
        /**
         * a shortcut to locating the Host header uses this length
         */
        val HOSTPREFIXLEN = "Host: ".length

        val PROXY_PORT = Integer.parseInt(Config.get("PROXY_PORT", "0")!!)
        val PROXY_HOST = Config.get("PROXY_HOST", "127.0.0.1")
        private val RPS_SHOW = "true" == Config.get("RPS_SHOW", "true")
        private val PROXY_DEBUG = "true" == Config.get("PROXY_DEBUG", "false")
        /**
         * master counter for stats per inbound requests
         */
        var counter = 0

        /**
         * creates a http-specific socket proxy to move bytes between innerKey and outerKey in the async framework.
         *
         * @param outerKey connection to the f5
         * @param innerKey connection to the Distributor
         * @param b the DMA ByteBuffers where applicable
         */
        fun pipe(innerKey: SelectionKey, outerKey: SelectionKey, vararg b: ByteBuffer) {
            val s = "pipe-" + counter
            val ob = if (b.size > 1) b[1] else ByteBuffer.allocate(4 shl 10)
            val ib = HttpPipeVisitor(s + "-in", innerKey, b[0], ob)
            outerKey.interestOps(OP_READ or OP_WRITE).attach(ib)
            innerKey.interestOps(OP_WRITE)
            innerKey.attach(object : HttpPipeVisitor(s + "-out", outerKey, ob, b[0]) {
                var fail: Boolean = false

                @Throws(Exception::class)
                override fun onRead(key: SelectionKey) {
                    if (!ib.isLimit || fail) {
                        val channel = key.channel() as SocketChannel
                        val read = AsioVisitor.Helper.read(key, inBuffer)
                        when (read) {
                            -1 -> {
                                channel.close()
                                return
                            }
                            0 -> return
                            else ->
                                  Rfc822HeaderState().headerInterest(HttpHeaders.`Content$2dLength`).read(
                                    inBuffer.duplicate().flip() as ByteBuffer).`$res`()
                        }// if (BlobAntiPatternObject.suffixMatchChunks(TERMINATOR, httpResponse.headerBuf()
                        // .duplicate())) ;
                    }
                    super.onRead(key)
                }
            })
        }
    }

}
