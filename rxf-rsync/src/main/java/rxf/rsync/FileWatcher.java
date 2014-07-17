package rxf.rsync;

import com.google.common.hash.Hashing;
import javolution.util.FastMap;
import one.xio.AsioVisitor;
import one.xio.AsyncSingletonServer;
import one.xio.MimeType;
import rxf.couch.gen.CouchDriver;
import rxf.web.inf.ProtocolMethodDispatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.io.BaseEncoding.base64;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;
import static one.xio.Config.get;
import static rxf.couch.driver.CouchMetaDriver.gson;

/**
 * Example to watch a directory (or createTree) for changes to files.
 */

public class FileWatcher {
  public static final Path NORMALIZE = Paths.get(get("FILEWATCHER_DIR", Paths.get(".")
      .toAbsolutePath().normalize().toString()));
  public static final String FILEWATCHER_DB = get("FILEWATCHER_DB", "db");
  public static final String FILEWATCHER_DOCID = get("FILEWATCHER_DOCID", "doc");
  public static String FILEWATCHER_IGNORE_EXAMPLE = get("FILEWATCHER_IGNORE_EXAMPLE",
      ".jar .war .class .java .symbolMap manifest.txt .log .bak compilation-mappings.txt web.xml");
  public static final String IGNORE = get("FILEWATCHER_IGNORE", "").trim();
  public static String[] FILEWATCHER_IGNORE = IGNORE.isEmpty() ? new String[0] : (IGNORE
      .split(" +"));

  public static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors
      .newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
  private final WatchService watcher;
  private final Map<WatchKey, Path> keys;
  private final boolean recursive;
  private boolean trace;
  private Timer timer = new Timer();

  private Path root;

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }

  /**
   * Register the given directory with the WatchService
   */
  private void register(Path dir) throws IOException {
    WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    if (trace) {
      Path prev = keys.get(key);
      if (null == prev) {
        System.out.format("register: %s\n", dir);
      } else {
        if (!dir.equals(prev)) {
          System.out.format("update: %s -> %s\n", prev, dir);
        }
      }
    }
    keys.put(key, dir);
  }

  static Map<Path, Boolean> delta = new FastMap<>();

  /**
   * Register the given directory, and all its sub-directories, with the WatchService.
   */
  private void registerAll(Path start) throws IOException {
    // register directory and sub-directories
    Files.walkFileTree(start, new SimpleFileVisitor<Path>() {

      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        register(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Creates a WatchService and registers the given directory
   */
  FileWatcher(Path root, boolean recursive) throws IOException {
    // JIM: Are you OK with this?
    this.root = root;

    this.watcher = FileSystems.getDefault().newWatchService();
    this.keys = new HashMap<>();
    this.recursive = recursive;

    if (recursive) {
      System.out.format("Scanning %s ...\n", root);
      registerAll(root);
      System.out.println("Done.");
    } else {
      register(root);
    }

    // enable trace after initial registration
    this.trace = true;
  }

  /**
   * Process all events for keys queued to the watcher
   */
  void processEvents() {
    System.out.println("FileWatcher.processEvents()");
    while (true) {

      // wait for key to be signalled
      WatchKey key;
      try {
        key = watcher.take();
      } catch (InterruptedException x) {
        return;
      }

      Path dir = keys.get(key);
      if (null == dir) {
        System.err.println("WatchKey not recognized!!");
        continue;
      }

      boolean first = true;
      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind kind = event.kind();

        // TBD - provide example of how OVERFLOW event is handled
        if (kind == OVERFLOW) {
          System.err.println("WatchService Overflow!");
          System.exit(99);
          continue;
        }

        // Context for directory entry event is the file name of entry
        WatchEvent<Path> ev = cast(event);
        Path name = ev.context();
        Path child = (name);

        // print out event
        if (first)
          System.out.format("%s: %s\n", event.kind().name(), child);
        first = false;
        // if directory is created, and watching recursively, then
        // register it and its sub-directories

        if (Files.isDirectory(child)) {
          if (kind == ENTRY_CREATE) {
            try {
              if (recursive && Files.isDirectory(child, NOFOLLOW_LINKS)) {
                registerAll(child);
              }

            } catch (IOException x) {
              // ignore to keep sample readbale
            }

          } else if (kind == ENTRY_DELETE)
            keys.remove(key);
        }
        // Path relativize = child.relativize(NORMALIZE);
        if (Files.isRegularFile(child)) {
          System.out.println("putting child: " + child);
          if (/* kind == ENTRY_CREATE || */kind == ENTRY_MODIFY) {

            delta.put(child, isAvoided(child));

          } else if (kind == ENTRY_DELETE) {
            delta.put(child, false);
          }
        }

        timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {

          public void run() {
            processDelta();

            System.out.println("remaining: " + delta.size());
            if (delta.isEmpty()) {
              timer.cancel();
            }
          }
        }, 2000, 2000);
      }

      // reset key and remove via set if directory no longer accessible
      boolean valid = key.reset();
      if (!valid) {
        keys.remove(key);
        // all directories are inaccessible
        if (keys.isEmpty()) {
          break;
        }
      }
    }
  }

  private void processDelta() {
    if (!delta.isEmpty()) {
      System.err.println("processing " + delta.size());
      String json =
          new CouchDriver.DocFetch().db(FILEWATCHER_DB).docId(FILEWATCHER_DOCID).to().fire().json();
      TreeMap x = gson().fromJson(json, TreeMap.class);
      if (null == x) {
        x = new TreeMap<>();
        x.put("_id", FILEWATCHER_DOCID);
        x.put("_attachments", new TreeMap<>());
      }
      Map<String, Map<String, String>> attachments;

      if (x.containsKey("_attachments")) {
        attachments = (Map<String, Map<String, String>>) x.get("_attachments");
      } else {
        x.put("_attachments", attachments = new TreeMap<>());
      }

      boolean changed = false;
      int c = 0;
      TreeSet<Map.Entry<Path, Boolean>> bySize =
          new TreeSet<Map.Entry<Path, Boolean>>(new Comparator<Map.Entry<Path, Boolean>>() {

            public int compare(Map.Entry<Path, Boolean> o1, Map.Entry<Path, Boolean> o2) {
              try {
                return -(int) ((Files.isRegularFile(o2.getKey()) ? Files.size(o2.getKey()) : -1) - (Files
                    .isRegularFile(o1.getKey()) ? Files.size(o1.getKey()) : -1));
              } catch (IOException e) {

              } finally {
              }
              return 0;

            }

            public boolean equals(Object obj) {
              return false;
            }
          });
      bySize.addAll(delta.entrySet());
      for (Map.Entry<Path, Boolean> entry : bySize) {
        Path key = entry.getKey();
        String s = NORMALIZE.relativize(key).toString();

        Boolean keepOrDelete = entry.getValue();
        if (Boolean.FALSE == keepOrDelete) {
          delta.entrySet().remove(entry);
          attachments.remove(s);
          changed = true;
          if (10 < c++) {
            break;
          }
        } else {
          assert null != attachments : "attachments are null.";
          assert null != s : "null key";
          Map<String, String> fromCouch = attachments.get(s);
          if (null == fromCouch || Boolean.TRUE.equals(keepOrDelete)) {
            try {
              byte[] bytes = Files.readAllBytes(key);
              if (null == fromCouch
                  || !("md5-" + base64().encode(Hashing.md5().hashBytes(bytes).asBytes()))
                      .equals(fromCouch.get("digest"))) {
                changed = true;
                MimeType mimeType = null;
                try {
                  mimeType = MimeType.valueOf(s.substring(s.lastIndexOf(".") + 1));
                } catch (Throwable e) {
                  mimeType = MimeType.bin;
                } finally {
                }
                Map<String, String> map = new TreeMap<>();
                map.put("content_type", mimeType.contentType);
                String data = base64().encode(bytes);

                map.put("data", data);
                attachments.put(('/') != File.separatorChar ? s.replace(File.separatorChar, '/')
                    : s, map);

                c++;
              }
              delta.entrySet().remove(entry);
            } catch (IOException e) {
              e.printStackTrace();
            }
            if (10 < c) {
              break;
            }
          }
        }
      }
      if (changed) {
        new CouchDriver.DocPersist().db(FILEWATCHER_DB).validjson(gson().toJson(x)).to().fire()
            .tx();
      }
    }
  }

  public void provision() {
    System.out.println("FileWatcher.provision()");
    String json =
        new CouchDriver.DocFetch().db(FILEWATCHER_DB).docId(FILEWATCHER_DOCID).to().fire().json();
    TreeMap x = gson().fromJson(json, TreeMap.class);
    if (null == x) {
      x = new TreeMap<>();
      x.put("_id", FILEWATCHER_DOCID);
      x.put("_attachments", new TreeMap<>());
    }
    Map<String, Map<String, String>> attachments;

    if (x.containsKey("_attachments")) {
      attachments = (Map<String, Map<String, String>>) x.get("_attachments");
    } else {
      x.put("_attachments", attachments = new TreeMap<>());
    }

    Map<String, Boolean> existingFiles = new TreeMap<>();
    // Check for new or changed files
    try {
      Files.walkFileTree(root, new ProvisioningFileVisitor(attachments, existingFiles));
    } catch (IOException e) {
      // TODO: What should I do in case of an error?
      e.printStackTrace();
    }

    // This can probably be made more efficient, but I don't want to touch the original keyset and cloning would not be
    // faster
    for (String couchAttachment : attachments.keySet()) {
      if (!existingFiles.containsKey(couchAttachment)) {
        System.out.println("Found removed: " + couchAttachment);
        delta.put(NORMALIZE.resolve(couchAttachment), false);
      }
    }

    while (0 < delta.size()) {
      processDelta();
    }
  }

  private class ProvisioningFileVisitor extends SimpleFileVisitor<Path> {
    private Map<String, Map<String, String>> attachments;
    private Map<String, Boolean> existingFiles;

    public ProvisioningFileVisitor(Map<String, Map<String, String>> attachments,
        Map<String, Boolean> existingFiles) {
      this.attachments = attachments;
      this.existingFiles = existingFiles;
    }

    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      Path relative = NORMALIZE.relativize(file);
      String relativeString = relative.toString();
      if (null != existingFiles) {
        existingFiles.put(relativeString, true);
      }
      if (!attachments.containsKey(relativeString)) {
        System.out.println("Found new: " + relative);
        delta.put(file, isAvoided(relative));
      } else {
        try {
          byte[] bytes = Files.readAllBytes(file);
          String couchDigest = attachments.get(relativeString).get("digest");
          if (null == couchDigest
              || !("md5-" + base64().encode(Hashing.md5().hashBytes(bytes).asBytes()))
                  .equals(couchDigest)) {
            System.out.println("Found changed:  " + relative);
            delta.put(file, isAvoided(file));
          }
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      }
      return FileVisitResult.CONTINUE;
    }
  }

  private Boolean isAvoided(Path file) {
    for (String s : FILEWATCHER_IGNORE) {
      if (file.toString().endsWith(s)) {
        System.err.println("skipping: " + file);
        return false;
      }
    }
    return true;
  }

  static {// boilerplate
    System.setProperty("rxf.couch.realtime.unit", TimeUnit.MINUTES.name());
    SCHEDULED_EXECUTOR_SERVICE.submit(new Runnable() {
      public void run() {
        // class initializers fire here
        AsioVisitor topLevel = new ProtocolMethodDispatch();
        try {
          AsyncSingletonServer.SingleThreadSingletonServer.init(topLevel);
        } catch (Exception ignored) {
        }
      }
    });
  }

  public static void main(String[] args) throws IOException {
    FileWatcher fileWatcher = new FileWatcher(NORMALIZE, true);
    fileWatcher.provision();
    fileWatcher.processEvents();
  }
}