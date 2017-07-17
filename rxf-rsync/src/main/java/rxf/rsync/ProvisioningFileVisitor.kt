package rxf.rsync

import com.google.common.hash.Hashing

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import com.google.common.io.BaseEncoding.base64

internal class ProvisioningFileVisitor(private val fileWatcher: FileWatcher, private val attachments: Map<String, Map<String, String>>,
                                       private val existingFiles: MutableMap<String, Boolean>?) : SimpleFileVisitor<Path>() {

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        val relative = FileWatcher.NORMALIZE.relativize(file)
        val relativeString = relative.toString()
        existingFiles?.put(relativeString, true)
        if (!attachments.containsKey(relativeString)) {
            println("Found new: " + relative)
            FileWatcher.delta.put(file, fileWatcher.isAvoided(relative)!!)
        } else {
            try {
                val bytes = Files.readAllBytes(file)
                val couchDigest = attachments.get(relativeString)!!.get("digest")
                if (null == couchDigest || "md5-" + base64().encode(Hashing.md5().hashBytes(bytes).asBytes()) != couchDigest) {
                    println("Found changed:  " + relative)
                    FileWatcher.delta.put(file, fileWatcher.isAvoided(file))
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
        return FileVisitResult.CONTINUE
    }
}
