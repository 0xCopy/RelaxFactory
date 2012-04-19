Description:
GWT Requestfactory middle-tier for couchdb.  provides an archetype for round trip Requestfactory communication with couchdb documents.

Developer Usecases:
 * edge node high-throughput/low-resource design
 * streaming java async events on couchdb _changes
 * GWT with long-poll/channels via couch REST _changes
 * HA RequestFactory concurrency via bigcouch

2 minute quick start:
 * install couchdb
 * install maven 2+

git clone git://github.com/jnorthrup/RelaxFactory.git

cd RelaxFactory

bin/start.sh

browse http://localhost:8080/index.html