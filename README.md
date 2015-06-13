Description
===

[![Join the chat at https://gitter.im/0xCopy/RelaxFactory](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/0xCopy/RelaxFactory?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

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

benchmarks below on my intel core-i7 4771 8-core


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




Latest Benchmark results.
==========

// -D1xio.debug.visitor.origins="false"
---------------------------------------------------------------
RelaxFactory; version: almost.1
---------------------------------------------------------------
// -D1xio.realtime.unit="MINUTES"
// -D1xio.realtime.cutoff="3"
// -D1xio.debug.sendjson="false"

Server Software:		null
Server Hostname:		localhost
Server Port:			8989
Document Path:			http://localhost:8989/rnd?c=2048
Document Length:		2048 bytes

Concurrency Level:		50
Time taken for tests:		23.231000 seconds
Complete requests:		5000000
Failed requests:		0
Write errors:			0
Kept alive:			5000000
Total transferred:		10240000000 bytes
Requests per second:		215,229.65 [#/sec] (mean)
Time per request:		0.232 [ms] (mean)
Time per request:		0.005 [ms] (mean, across all concurrent requests)
Transfer rate:			440,790.32 [Kbytes/sec] received
				-1 kb/s sent
				440,790.32 kb/s total
---------------------------------------------------------------
---------------------------------------------------------------
Jetty (NIO); version: 9.3.0.M2
---------------------------------------------------------------

Server Software:		Jetty(9.3.0.M2)
Server Hostname:		localhost
Server Port:			8989
Document Path:			http://localhost:8989/rnd?c=2048
Document Length:		2048 bytes

Concurrency Level:		50
Time taken for tests:		24.320000 seconds
Complete requests:		5000000
Failed requests:		0
Write errors:			0
Kept alive:			5000000
Total transferred:		10240000000 bytes
Requests per second:		205,592.11 [#/sec] (mean)
Time per request:		0.243 [ms] (mean)
Time per request:		0.005 [ms] (mean, across all concurrent requests)
Transfer rate:			421,052.63 [Kbytes/sec] received
				-1 kb/s sent
				421,052.63 kb/s total
---------------------------------------------------------------
---------------------------------------------------------------
HttpCore (NIO); version: 4.4.1
---------------------------------------------------------------

Server Software:		HttpCore-NIO-Test/1.1
Server Hostname:		localhost
Server Port:			8989
Document Path:			http://localhost:8989/rnd?c=2048
Document Length:		2048 bytes

Concurrency Level:		50
Time taken for tests:		24.626000 seconds
Complete requests:		5000000
Failed requests:		0
Write errors:			0
Kept alive:			5000000
Total transferred:		10240000000 bytes
Requests per second:		203,037.44 [#/sec] (mean)
Time per request:		0.246 [ms] (mean)
Time per request:		0.005 [ms] (mean, across all concurrent requests)
Transfer rate:			415,820.68 [Kbytes/sec] received
				-1 kb/s sent
				415,820.68 kb/s total
---------------------------------------------------------------
---------------------------------------------------------------
Netty; version: 3.6.2
---------------------------------------------------------------

Server Software:		null
Server Hostname:		localhost
Server Port:			8989
Document Path:			http://localhost:8989/rnd?c=2048
Document Length:		2048 bytes

Concurrency Level:		50
Time taken for tests:		22.801000 seconds
Complete requests:		5000000
Failed requests:		0
Write errors:			0
Kept alive:			5000000
Total transferred:		10240000000 bytes
Requests per second:		219,288.63 [#/sec] (mean)
Time per request:		0.228 [ms] (mean)
Time per request:		0.005 [ms] (mean, across all concurrent requests)
Transfer rate:			449,103.11 [Kbytes/sec] received
				-1 kb/s sent
				449,103.11 kb/s total
---------------------------------------------------------------


suppositions:
==
overall profiler shows roughly equivalent benchmark camel humps in the cpu score.

![image](https://cloud.githubusercontent.com/assets/73514/7576808/b132a70e-f7f6-11e4-9780-4847330e0426.png)

benchmarks seem to show allocation of Direct ByteBuffers allocated in force in rxf, with the other 3 implementations presumably using heap or possibly fast "Unsafe" shortcuts.

![image](https://cloud.githubusercontent.com/assets/73514/7576943/f82f7c94-f7f7-11e4-8556-c0ed82daad9a.png)