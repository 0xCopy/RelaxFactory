package rxf.couch.daemon

import one.xio.AsyncSingletonServer
import rxf.rpc.RpcHelper

import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel

/**
 * this launches the main service thread and assigns the proxy port to socketservers. User: jnorthrup Date: 10/1/13
 * Time: 7:27 PM
 */
open class ProxyTask(vararg val proxyPorts: String) : Runnable {
    var prefix: String? = null

    override fun run() {
        try {
            for (proxyPort in proxyPorts) {
                AsyncSingletonServer.SingleThreadSingletonServer.enqueue(ServerSocketChannel.open().bind(
                        InetSocketAddress(Integer.parseInt(proxyPort)), 4096).setOption(
                        StandardSocketOptions.SO_REUSEADDR, java.lang.Boolean.TRUE).configureBlocking(false),
                        SelectionKey.OP_ACCEPT, ProxyDaemon(this))
            }

        } catch (e: Throwable) {
            e.printStackTrace()
        }

    }

    companion object {

        @JvmStatic fun main(vararg args: String) {
            RpcHelper.getEXECUTOR_SERVICE().submit(object : ProxyTask(*args) {})
        }
    }

}
