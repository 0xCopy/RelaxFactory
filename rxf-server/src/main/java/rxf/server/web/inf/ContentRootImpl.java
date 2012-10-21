package rxf.server.web.inf;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.server.*;
import rxf.server.Rfc822HeaderState.HttpRequest;
import rxf.server.Rfc822HeaderState.HttpResponse;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static java.lang.Math.min;
import static java.nio.channels.SelectionKey.*;
import static one.xio.HttpHeaders.*;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.HEADER_TERMINATOR;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;

/**
 * User: jim
 * Date: 6/4/12
 * Time: 1:42 AM
 */
public class ContentRootImpl extends Impl implements PreRead {

	public static final String SLASHDOTSLASH = File.separator + "."
			+ File.separator;
	public static final String DOUBLESEP = File.separator + File.separator;
	private String rootPath = CouchNamespace.COUCH_DEFAULT_FS_ROOT;
	private ByteBuffer cursor;
	private HttpRequest request;
	private SocketChannel channel;
	private HttpRequest req;

	public ContentRootImpl() {
		init();
	}

	File file;

	public ContentRootImpl(String rootPath) {
		this.rootPath = rootPath;
		init();
	}

	public static String fileScrub(String scrubMe) {
		final char inverseChar = '/' == File.separatorChar ? '\\' : '/';
		return null == scrubMe ? null : scrubMe.trim().replace(inverseChar,
				File.separatorChar).replace(DOUBLESEP, "" + File.separator)
				.replace("..", ".");
	}

	public void init() {
		File dir = new File(rootPath);
		if (!dir.isDirectory() && dir.canRead())
			throw new IllegalAccessError("can't verify readable dir at "
					+ rootPath);
	}

	@Override
	public void onRead(SelectionKey key) throws Exception {
		channel = (SocketChannel) key.channel();
		if (cursor == null) {
			if (key.attachment() instanceof Object[]) {
				Object[] ar = (Object[]) key.attachment();
				for (Object o : ar) {
					if (o instanceof ByteBuffer) {
						cursor = (ByteBuffer) o;
						continue;
					}
					if (o instanceof Rfc822HeaderState) {
						req = ((Rfc822HeaderState) o).$req();
						continue;
					}
				}
			}
			key.attach(this);
		}
		cursor = null == cursor ? ByteBuffer
				.allocateDirect(getReceiveBufferSize()) : cursor.hasRemaining()
				? cursor
				: ByteBuffer.allocateDirect(cursor.capacity() << 1).put(
						(ByteBuffer) cursor.rewind());
		int read = channel.read(cursor);
		if (read == -1)
			key.cancel();
		Buffer flip = cursor.duplicate().flip();
		req = (HttpRequest) ActionBuilder.get().state().$req().apply(
				(ByteBuffer) flip);
		if (!BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, req
				.headerBuf())) {
			return;
		}
		cursor = ((ByteBuffer) flip).slice();
		/*  int remaining = Integer.parseInt(req.headerString(HttpHeaders.Content$2dLength));
		      final Impl prev = this;
		      if (cursor.remaining() != remaining) key.attach(new Impl() {
		        @Override
		        public void onRead(SelectionKey key) throws Exception {
		          int read1 = channel.read(cursor);
		          if (read1 == -1)
		            key.cancel();
		          if (!cursor.hasRemaining()) {
		            key.interestOps(SelectionKey.OP_WRITE).attach(prev);
		            return;
		          }
		        }

		      });*/
		key.interestOps(SelectionKey.OP_WRITE);
	}

	public void onWrite(SelectionKey key) throws Exception {

		String accepts = req.headerString(Accept$2dEncoding);
		String ceString = null;
		String finalFname = fileScrub(rootPath + SLASHDOTSLASH
				+ req.path().split("\\?")[0]);
		file = new File(finalFname);
		if (file.isDirectory()) {
			file = new File((finalFname + "/index.html"));
		}
		if (null != accepts) {

			for (CompressionTypes compType : CompressionTypes.values()) {
				if (accepts.contains(compType.name())) {
					File f = new File(finalFname + "." + compType.suffix);
					if (f.isFile() && f.canRead()) {
						if (BlobAntiPatternObject.DEBUG_SENDJSON) {
							System.err.println("sending compressed archive: "
									+ f.getAbsolutePath());
						}
						ceString = (compType.name());
						file = f;
						break;
					}
				}
			}
		}
		boolean send200 = file.canRead() && file.isFile();
		finalFname = (file.getCanonicalPath());

		if (send200) {
			final RandomAccessFile randomAccessFile = new RandomAccessFile(
					file, "r");
			final long total = randomAccessFile.length();
			final FileChannel fileChannel = randomAccessFile.getChannel();

			String substring = finalFname
					.substring(finalFname.lastIndexOf('.') + 1);
			MimeType mimeType = MimeType.valueOf(substring);
			long length = randomAccessFile.length();

			final HttpResponse responseHeader = new Rfc822HeaderState().$res();

			responseHeader.status(HttpStatus.$200).headerString(Content$2dType,
					(null == mimeType ? MimeType.bin : mimeType).contentType)
					.headerString(Content$2dLength, String.valueOf(length));
			if (null != ceString)
				responseHeader.headerString(Content$2dEncoding, ceString);
			ByteBuffer response = responseHeader.as(ByteBuffer.class);
			int write = channel.write(response);
			final int sendBufferSize = BlobAntiPatternObject
					.getSendBufferSize();
			final long[] progress = {fileChannel.transferTo(0, sendBufferSize,
					channel)};
			key.interestOps(OP_WRITE | OP_CONNECT);
			key.selector().wakeup();
			key.attach(new Impl() {

				public void onWrite(SelectionKey key) throws Exception {
					long remaining = total - progress[0];
					progress[0] += fileChannel.transferTo(progress[0], min(
							sendBufferSize, remaining), channel);
					remaining = total - progress[0];
					if (0 == remaining) {
						fileChannel.close();
						randomAccessFile.close();
						key.selector().wakeup();
						key.interestOps(OP_READ);
						key.attach(null);
					}
				}
			});
		} else {
			key.selector().wakeup();
			key.interestOps(OP_WRITE).attach(new Impl() {

				public void onWrite(SelectionKey key) throws Exception {

					String response = "HTTP/1.1 404 Not Found\n"
							+ "Content-Length: 0\n\n";
					System.err.println("!!! " + file.getAbsolutePath());
					int write = channel.write(UTF8.encode(response));
					key.selector().wakeup();
					key.interestOps(OP_READ).attach(null);
				}
			});
		}
	}
}
