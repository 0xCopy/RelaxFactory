package rxf.rsync

import com.google.common.hash.Hashing
import javolution.util.FastMap
import one.xio.AsioVisitor
import one.xio.AsyncSingletonServer
import one.xio.MimeType
import rxf.couch.gen.CouchDriver
import rxf.web.inf.ProtocolMethodDispatch

import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import com.google.common.io.BaseEncoding.base64
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.StandardWatchEventKinds.*
import rxf.core.Config.get
import rxf.couch.driver.CouchMetaDriver.gson

/**
 * Example to watch a directory (or createTree) for changes to files.
 */

class FileWatcher
/**
 * Creates a WatchService and registers the given directory
 */
@Throws(IOException::class)
internal constructor(private val root: Path, private val recursive: Boolean) {
    private val watcher: WatchService
    private val keys: MutableMap<WatchKey, Path>
    private val trace: Boolean
    private var timer = Timer()

    /**
     * Register the given directory with the WatchService
     */
    @Throws(IOException::class)
    private fun register(dir: Path) {
        val key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        if (trace) {
            val prev = keys[key]
            if (null == prev) {
                System.out.format("register: %s\n", dir)
            } else {
                if (dir != prev) {
                    System.out.format("update: %s -> %s\n", prev, dir)
                }
            }
        }
        keys.put(key, dir)
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    @Throws(IOException::class)
    private fun registerAll(start: Path) {
        // register directory and sub-directories
        Files.walkFileTree(start, object : SimpleFileVisitor<Path>() {

            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                register(dir)
                return FileVisitResult.CONTINUE
            }
        })
    }

    init {

        this.watcher = FileSystems.getDefault().newWatchService()
        this.keys = HashMap()

        if (recursive) {
            System.out.format("Scanning %s ...\n", root)
            registerAll(root)
            println("Done.")
        } else {
            register(root)
        }

        // enable trace after initial registration
        this.trace = true
    }// JIM: Are you OK with this?

    /**
     * Process all events for keys queued to the watcher
     */
    internal fun processEvents() {
        println("FileWatcher.processEvents()")
        while (true) {

            // wait for key to be signalled
            val key: WatchKey
            try {
                key = watcher.take()
            } catch (x: InterruptedException) {
                return
            }

            val dir = keys[key]
            if (null == dir) {
                System.err.println("WatchKey not recognized!!")
                continue
            }

            var first = true
            for (event in key.pollEvents()) {
                val kind = event.kind()

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    System.err.println("WatchService Overflow!")
                    System.exit(99)
                    continue
                }

                // Context for directory entry event is the file name of entry
                val ev = cast<Path>(event)
                val name = ev.context()
                val child = name

                // print out event
                if (first)
                    System.out.format("%s: %s\n", event.kind().name(), child)
                first = false
                // if directory is created, and watching recursively, then
                // register it and its sub-directories

                if (Files.isDirectory(child)) {
                    if (kind == ENTRY_CREATE) {
                        try {
                            if (recursive && Files.isDirectory(child, NOFOLLOW_LINKS)) {
                                registerAll(child)
                            }

                        } catch (x: IOException) {
                            // ignore to keep sample readbale
                        }

                    } else if (kind == ENTRY_DELETE)
                        keys.remove(key)
                }
                // Path relativize = child.relativize(NORMALIZE);
                if (Files.isRegularFile(child)) {
                    println("putting child: " + child)
                    if (/* kind == ENTRY_CREATE || */kind == ENTRY_MODIFY) {

                        delta.put(child, isAvoided(child))

                    } else if (kind == ENTRY_DELETE) {
                        delta.put(child, false)
                    }
                }

                timer.cancel()
                timer = Timer()
                timer.scheduleAtFixedRate(object : TimerTask() {

                    override fun run() {
                        processDelta()

                        println("remaining: " + delta.size)
                        if (delta.isEmpty()) {
                            timer.cancel()
                        }
                    }
                }, 2000, 2000)
            }

            // reset key and remove via set if directory no longer accessible
            val valid = key.reset()
            if (!valid) {
                keys.remove(key)
                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break
                }
            }
        }
    }

    private fun processDelta() {
        if (!delta.isEmpty()) {
            System.err.println("processing " + delta.size)
            val json = CouchDriver.DocFetch().db(FILEWATCHER_DB).docId(FILEWATCHER_DOCID).to().fire().json()
            var x  = gson().fromJson(
                    json,
                    MutableMap::class.java
            )
            if (null == x) {
                x = TreeMap<String, Any>()
                x.put("_id", FILEWATCHER_DOCID)
                x.put("_attachments", TreeMap<Any, Any>())
            }
            val attachments:  MutableMap<String, MutableMap<String, String>>?

            when {
                x.containsKey("_attachments") -> attachments = x["_attachments"] as  MutableMap<String, MutableMap<String, String>>
                else -> {
                    attachments = TreeMap<String,  MutableMap<String, String>>()
                    (x as MutableMap<String,Any> )["_attachments"] = attachments

                }
            }

            var changed = false
            var c = 0
            val bySize = TreeSet<Map.Entry<Path, Boolean>>(object : Comparator<Map.Entry<Path, Boolean>> {

                override fun compare(o1: Map.Entry<Path, Boolean>, o2: Map.Entry<Path, Boolean>): Int {
                    try {
                        return -((if (Files.isRegularFile(o2.key)) Files.size(o2.key) else -1) - if (Files
                                .isRegularFile(o1.key))
                            Files.size(o1.key)
                        else
                            -1).toInt()
                    } catch (e: IOException) {

                    } finally {
                    }
                    return 0

                }

                override fun equals(obj: Any?): Boolean {
                    return false
                }
            })
            bySize.addAll(delta.entries)
            for (entry in bySize) {
                val key = entry.key
                val s = NORMALIZE.relativize(key).toString()

                val keepOrDelete = entry.value
                if (java.lang.Boolean.FALSE == keepOrDelete) {
                    delta.entries.remove(entry)
                    attachments.remove(s)
                    changed = true
                    if (10 < c++) {
                        break
                    }
                } else {
                    assert(attachments != null!!) { "attachments are null." }
                    assert(s==null!!) { "null key" }
                    val fromCouch = attachments!![s]
                    if (null == fromCouch || java.lang.Boolean.TRUE == keepOrDelete) {
                        try {
                            val bytes = Files.readAllBytes(key)
                            if (null == fromCouch || "md5-" + base64().encode(Hashing.md5().hashBytes(bytes).asBytes()) != fromCouch["digest"]) {
                                changed = true
                                var mimeType: MimeType? = null
                                try {
                                    mimeType = MimeType.valueOf(s.substring(s.lastIndexOf(".") + 1))
                                } catch (e: Throwable) {
                                    mimeType = MimeType.bin
                                } finally {
                                }
                                val map = TreeMap<String, String>()
                                map.put("content_type", mimeType!!.contentType)
                                val data = base64().encode(bytes)

                                map.put("data", data)
                                attachments.put(if ('/' != File.separatorChar)
                                    s.replace(File.separatorChar, '/')
                                else
                                    s, map)

                                c++
                            }
                            delta.entries.remove(entry)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }

                        if (10 < c) {
                            break
                        }
                    }
                }
            }
            if (changed) {
                CouchDriver.DocPersist().db(FILEWATCHER_DB).validjson(gson().toJson(x)).to().fire()
                        .tx()
            }
        }
    }

    fun provision() {
        println("FileWatcher.provision()")
        val json = CouchDriver.DocFetch().db(FILEWATCHER_DB).docId(FILEWATCHER_DOCID).to().fire().json()
        var x : TreeMap<*, *>? = gson().fromJson(json, TreeMap  ::class.java)
        if (null == x) {
            x = TreeMap<String, Any>()
            x.put("_id", FILEWATCHER_DOCID)
            x.put("_attachments", TreeMap<Any, Any>())
        }
        val attachments: MutableMap<String, MutableMap<String, String>>

        if (x.containsKey("_attachments")) {
            attachments = x["_attachments"] as MutableMap<String, MutableMap<String, String>>
        } else {
            attachments = TreeMap<String, MutableMap<String, String>>()
            (x as TreeMap<String, Any>) ["_attachments"]= attachments

        }

        val existingFiles = TreeMap<String, Boolean>()
        // Check for new or changed files
        try {
            Files.walkFileTree(root, ProvisioningFileVisitor(this, attachments, existingFiles))
        } catch (e: IOException) {
            // TODO: What should I do in case of an error?
            e.printStackTrace()
        }

        // This can probably be made more efficient, but I don't want to touch the original keyset and cloning would not be
        // faster
        for (couchAttachment in attachments.keys) {
            if (!existingFiles.containsKey(couchAttachment)) {
                println("Found removed: " + couchAttachment)
                delta.put(NORMALIZE.resolve(couchAttachment), false)
            }
        }

        while (delta.isNotEmpty()) processDelta()
    }

    fun isAvoided(file: Path): Boolean {
        for (s in FILEWATCHER_IGNORE) if (file.  endsWith(s )) {
            System.err.println("skipping: " + file)
            return false
        }
        return true
    }

    companion object {
        val NORMALIZE = Paths.get(get("FILEWATCHER_DIR", Paths.get(".")
                .toAbsolutePath().normalize().toString()))
        val FILEWATCHER_DB = get("FILEWATCHER_DB", "db")
        val FILEWATCHER_DOCID = get("FILEWATCHER_DOCID", "doc")
        var FILEWATCHER_IGNORE_EXAMPLE = get("FILEWATCHER_IGNORE_EXAMPLE",
                ".jar .war .class .java .symbolMap manifest.txt .log .bak compilation-mappings.txt web.xml")
        val IGNORE = get("FILEWATCHER_IGNORE", "")!!.trim { it <= ' ' }
        var FILEWATCHER_IGNORE = if (IGNORE.isEmpty())
            arrayOfNulls<String>(0)
        else
            IGNORE
                    .split(" +".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val SCHEDULED_EXECUTOR_SERVICE = Executors
                .newScheduledThreadPool(Runtime.getRuntime().availableProcessors())

        internal fun <T> cast(event: WatchEvent<*>): WatchEvent<T> {
            return event as WatchEvent<T>
        }

        internal var delta: MutableMap<Path, Boolean> = FastMap()

        init {// boilerplate
            System.setProperty("rxf.couch.realtime.unit", TimeUnit.MINUTES.name)
            SCHEDULED_EXECUTOR_SERVICE.submit {
                // class initializers fire here
                val topLevel = ProtocolMethodDispatch()
                try {
                    AsyncSingletonServer.SingleThreadSingletonServer.init(topLevel)
                } catch (ignored: Exception) {
                }
            }
        }

        @Throws(IOException::class)
        @JvmStatic fun main(args: Array<String>) {
            val fileWatcher = FileWatcher(NORMALIZE, true)
            fileWatcher.provision()
            fileWatcher.processEvents()
        }
    }
}