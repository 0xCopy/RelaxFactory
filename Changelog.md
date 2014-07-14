

0.9.7->0.9.9.2:
===

* massive reorg from lesson learned. 
* support for websockets framing of ByteBuffers in WebsocketFrame
* ActionBuilder given new name Tx
    * payload is added here allowing for some refactors of existing CouchDriver complexity with "cursor" and phaser payloads being returned.
    * ported the CouchMetaDriver and CouchDriver to use fewer lines of code for the intended Tx usecase, and return void for visit.
* intellij closure sugar in AsioVisitor (technically 1xio 2.0)
* marker interfaces -> annotations for ProtocolMethodDispatch
* TemplateContentImpl to support oauth usecases
* SecureScope annotation to support cookie based filtering
* borrow viola's Http Proxy for fetchDoc, citing bitrot on the proxyImpl and proxy daemon that lack unit tests.
* total maven module/package reorg to seperate couch from other web features 