Description
===

RelaxFactory is a Server Toolkit for implementing web protocols 
like HTTP, QUIC, WebSockets, and REST middleware using Java 
language but without a java resource cost associated with 
application containers.  This is accomplished by carefully 
proceeding in whatever way avoids object allocation and object 
reference longevity.  The C10k Reactor pattern implentation 
imposes no overhead on inbound sockets and in fact, knows nothing 
about the sockets outside of the spartan protocol dispatch 
routines.

The first implementation features of RelaxFactory produced a high 
quality GWT middle-tier for couchdb data persistence.  Smaller 
professional engagements have brought the toolkit to bear on 
iterations bridging hadoop, natural language processing tools, 
and RelaxFactory powers development web sites on major cloud 
providers.    


Developer Usecases:

 * edge node high-throughput/low-resource design
 * streaming java async events on couchdb _changes
 * hosting GWT with long-poll/channels via couch REST _changes
 * HA RequestFactory concurrency via "The Big Merge" couchdb
 * basis for KouchDb platform for re-implementing known-good web based document store features
 * Suitable platform to build java QUIC serverside
 * _new!_ ssl client and server options via 1xio

Maven
===

RelaxFactory is dependent upon 1xio (https://github.com/jnorthrup/1xio) and potentially other projects that have not yet become hosted in maven central.  to use RelaxFactory without building it add 

```
<repository>
  <id>misc-repo</id>
  <url>http://raw.github.com/jnorthrup/misc-repo/master/</url>
</repository>
```

to your pom.xml

IRC
===
while working on the toolkit the author(s) can likely be found at irc://chat.freenode.net/#relaxfactory