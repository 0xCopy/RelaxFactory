package rxf.server.guice;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.CouchNamespace.NAMESPACE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import rxf.server.BlobAntiPatternObject;
import rxf.server.PreRead;
import rxf.server.Rfc822HeaderState;
import rxf.server.Rfc822HeaderState.HttpRequest;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class InjectedTopLevelVisitor extends AsioVisitor.Impl {
	private final Map<HttpMethod, Map<String, Key<? extends AsioVisitor>>> bindings = new EnumMap<HttpMethod, Map<String, Key<? extends AsioVisitor>>>(HttpMethod.class);

	private Injector injector;

	@Inject
	public void init(Injector injector) {
		for (Binding<VisitorDef> entry : injector
				.findBindingsByType(TypeLiteral.get(VisitorDef.class))) {
			VisitorDef def = entry.getProvider().get();
			System.out.println(def.getMethod() + " " + def.getPattern() + ": "
					+ def.getVisitorKey());
			Map<String, Key<? extends AsioVisitor>> method = bindings.get(def
					.getMethod());
			if (method == null) {
				method = new LinkedHashMap<String, Key<? extends AsioVisitor>>();
				bindings.put(def.getMethod(), method);
			}
			method.put(def.getPattern(), def.getVisitorKey());
		}
		System.out.println(bindings);
		this.injector = injector;
	}

	public void onAccept(SelectionKey key) throws IOException {
		ServerSocketChannel channel = (ServerSocketChannel) key.channel();
		SocketChannel accept = channel.accept();
		accept.configureBlocking(false);
		HttpMethod.enqueue(accept, OP_READ, this);

	}

	public void onRead(SelectionKey key) throws Exception {
		final SocketChannel channel = (SocketChannel) key.channel();

		ByteBuffer cursor = ByteBuffer.allocateDirect(BlobAntiPatternObject
				.getReceiveBufferSize());
		int read = channel.read(cursor);
		if (-1 == read) {
			((SocketChannel) key.channel()).socket().close();//cancel();
			return;
		}

		HttpMethod method = null;
		HttpRequest httpRequest = null;
		try {
			//find the method to dispatch
			Rfc822HeaderState state = new Rfc822HeaderState()
					.apply((ByteBuffer) cursor.flip());
			httpRequest = state.$req();
			if (BlobAntiPatternObject.DEBUG_SENDJSON) {
				System.err.println(BlobAntiPatternObject.deepToString(UTF8
						.decode((ByteBuffer) httpRequest.headerBuf()
								.duplicate().rewind())));
			}
			String method1 = httpRequest.method();
			method = HttpMethod.valueOf(method1);

		} catch (Exception e) {
		}

		if (null == method) {
			((SocketChannel) key.channel()).socket().close();//cancel();

			return;
		}

		Set<Entry<String, Key<? extends AsioVisitor>>> entries = bindings.get(
				method.name()).entrySet();
		String path = httpRequest.path();
		for (Entry<String, Key<? extends AsioVisitor>> visitorEntry : entries) {
			if (path.matches(visitorEntry.getKey())) {
				if (BlobAntiPatternObject.DEBUG_SENDJSON) {
					System.err.println("+?+?+? using "
							+ visitorEntry.getValue());
				}
				AsioVisitor visitor = injector.getInstance(visitorEntry
						.getValue());

				Object a[] = {visitor, httpRequest, cursor};
				key.attach(a);
				if (visitor instanceof PreRead) {
					visitor.onRead(key);
				}

				key.selector().wakeup();

				return;
			}
		}
		// Failed to find a matching visitor, 404
		key.selector().wakeup();
		key.interestOps(OP_WRITE).attach(new Impl() {
			@Override
			public void onWrite(SelectionKey key) throws Exception {
				String response = "HTTP/1.1 404 Not Found\n"
						+ "Content-Length: 0\n\n";
				int write = channel.write(UTF8.encode(response));
				key.selector().wakeup();
				key.interestOps(OP_READ).attach(null);
			}
		});
		System.err.println(BlobAntiPatternObject.deepToString("!!!1!1!!",
				"404", path, "using", NAMESPACE));
	}
}
