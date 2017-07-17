package rxf.couch.daemon

import one.xio.AsioVisitor
import rxf.core.Config
import rxf.shared.PreRead

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets

import java.nio.channels.SelectionKey.OP_READ
import java.nio.channels.SelectionKey.OP_WRITE

/**
 * this visitor shovels data from the outward selector to the inward selector, and vice versa. once the headers are sent
 * inward the only state monitored is when one side of the connections close.
 */
@PreRead
open class HttpPipeVisitor(protected var name: String, // public AtomicInteger remaining;
                           internal var otherKey: SelectionKey,vararg val  b: ByteBuffer) : AsioVisitor.Impl() {
     var isLimit: Boolean = false


    @Throws(Exception::class)
    override fun onRead(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        if (otherKey.isValid) {
            val read = AsioVisitor.Helper.read(key, inBuffer)
            if (read == -1)
            /* key.cancel(); */ {
                channel.shutdownInput()
                key.interestOps(OP_WRITE)
                AsioVisitor.Helper.write(key, ByteBuffer.allocate(0))
            } else {
                // if buffer fills up, stop the read option for a bit
                otherKey.interestOps(OP_READ or OP_WRITE)
                AsioVisitor.Helper.write(key, ByteBuffer.allocate(0))
            }
        } else {
            key.cancel()
        }
    }

    @Throws(Exception::class)
    override fun onWrite(key: SelectionKey) {
        val channel = key.channel() as SocketChannel
        val flip = outBuffer.flip() as ByteBuffer
        if (PROXY_DEBUG) {
            val decode = StandardCharsets.UTF_8.decode(flip.duplicate())
            System.err.println("writing to $name: $decode-")
        }
        val write = channel.write(flip)

        if (-1 == write || isLimit /* && null != remaining && 0 == remaining.get() */) {
            key.cancel()
        } else {
            // if (isLimit() /*&& null != remaining*/) {
            // /*this.remaining.getAndAdd(-write);*//*
            // if (1 > remaining.get()) */{
            // key.channel().close();
            // otherKey.channel().close();
            // return;
            // }
            // }
            key.interestOps(OP_READ or OP_WRITE)// (getOutBuffer().hasRemaining() ? OP_WRITE : 0));
            outBuffer.compact()
        }
    }

    val inBuffer: ByteBuffer
        get() = b[0]

    val outBuffer: ByteBuffer
        get() = b[1]

    companion object {
        val PROXY_DEBUG = "true" == Config.get("PROXY_DEBUG", false.toString())
    }
}
