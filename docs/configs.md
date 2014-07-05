| name     |      default  |  description|
|----------|+-------------+|-------------|
|RXF_CONTENT_ROOT|./|docroot relative to the java execution.  can be absolute, of course.
|RXF_ORGNAME|rxf_|view generation and requestfactory automatic prefixes, overridable
|RXF_DEBUG_SENDJSON|false| used for unit test and debugging output, probably misnamed as time progresses-- this debugs most io. 
|RXF_CONNECTION_POOL_SIZE|20|couchdb createConnection keeps a list of hot sockets.  
|RXF_COUCH_PREFIX|http://localhost:5984|this is prefixed to couch REST requests, or consumed for host/port info as needed.
|PROXY_DEBUG|String|part of deprecated socket proxy daemon 
|PROXY_PORT|0|part of deprecated socket proxy daemon 
|PROXY_HOST|127.0.0.1|part of deprecated socket proxy daemon 
|RPS_SHOW|true|part of deprecated socket proxy daemon 
|RXF_REALTIME_UNIT|DEBUG_SENDJSON ? HOURS : SECONDS|realtime cutoff on couchdb operations. obviously in a debugger 3 seconds is too short so DEBUG_SENDJSON is important to set 
|RXF_REALTIME_CUTOFF|3|realtime cutoff count of units, whatever units may be chosen
|GSON_DATEFORMAT|yyyy-MM-dd'T'HH:mm:ss.SSSZ|*Relaxfactory dont care*  gson date formatter chosen for ISO-like sortability and preservation of java timezone where carelessness may not always record GMT in java services.
|GSON_FIELDNAMINGPOLICY|IDENTITY|*Relaxfactory dont care* gson internal tweak.  
|GSON_PRETTY|true|*Relaxfactory dont care* gson internal tweak.
|GSON_NULLS|false|*Relaxfactory dont care* gson internal tweak.
|GSON_NANS|false|*Relaxfactory dont care* gson internal tweak.
|RXF_CACHED_THREADPOOL|false|if true, this gives unlimitted threadworkers.  if false, the default, RelaxFactory assumes a simple realtime server is intended without much consideration for gwt rpc or re-entrant logic  
|FILEWATCHER_DIR|.| rxf-rsync knob to define a VFS directory to mirror attachments into/out of
|FILEWATCHER_DB|db|rxf-rsync knob to define a couch db to mirror attachments into/out of
|FILEWATCHER_DOCID|doc|rxf-rsync knob to define a document to mirror attachments into/out of
|FILEWATCHER_IGNORE_EXAMPLE|.jar .war .class .java .symbolMap manifest.txt .log .bak compilation-mappings.txt web.xml|rxf-rsync knob which shows a reasonable set of ignores for mirroring a gwt app in realtime
|FILEWATCHER_IGNORE| |rxf-rsync knob typically makes sense to use the example, but is blank by default
