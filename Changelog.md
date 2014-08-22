

0.9.7->0.9.9.3.1:
===

* massive reorg from lesson learned. 
* support for websockets framing of ByteBuffers in WebsocketFrame
* intellij closure sugar in AsioVisitor (technically 1xio 2.0)
* marker interfaces -> annotations for ProtocolMethodDispatch
* TemplateContentImpl to support oauth usecases
* SecureScope annotation to support cookie based filtering
* borrow viola's Http Proxy for fetchDoc, citing bitrot on the proxyImpl and proxy daemon that lack unit tests.
* total maven module/package reorg to seperate couch from other web features 
* ActionBuilder given new name Tx
    * payload is added here allowing for some refactors of existing CouchDriver complexity with "cursor" and phaser 
    payloads being returned.
* CouchMetaDriver.visit() is void.
    * ported the CouchMetaDriver and CouchDriver to use fewer lines of code for the intended Tx usecase, and return 
    void for visit.
    * situatations depending on 
` (visit()==null)!=success ` are now : ` tx=to(); [res=]tx.fire() [.json()]; success=$200==tx.state().statusEnum`
    *   tx.payload() replaces cursor and `ByteBuffer payload=visit()` 

SSL:
===

 * SocketChannel.read() can no longer free-ball in the codebase.  
    * 1xio AsioVisitor.FSM.read/.write methods handle channel and key read/writes.   
    * if a weakref holding the key exists, it refs a SslVisitor object which provides the FSM interruptions to the 
    previous code contracts. SSL should not require new usecases other than where POST and GET convert query params to form fields 
    * if an unintended SSL handshake interrupts a 1xio read/write, read() or write() will 
    return 0
        * rxf code should avoid key.interestOps(int) at all costs when 0 comes back from read or write
        * rxf code should avoid 1-time read or writes that write headers and switch the visitor.  finish{Read,Write} 
        methods should handle these usecases sanely. 
        * if SSLEngine declares NEED_TASK or NEED_WRAP then rxf needs to 
        drop everything and control the key for network activity and then resume the application bytes.  abiding 0 byte 
        returns should stave off sweeping semantic changes to rxf.
        
QUIC
===
* in the midst of resume Kouchdb WebSocket implementation thhe QUIC UDP protocol caught my eye...