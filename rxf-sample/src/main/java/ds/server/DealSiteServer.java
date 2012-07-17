package ds.server;

import ds.model.*;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import rxf.server.BlobAntiPatternObject;
import rxf.server.CouchLocator;
import rxf.server.CouchServiceFactory;
import rxf.server.CouchTx;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ds.server.SecurityImpl.*;
import static java.lang.Math.abs;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.deepToString;
import static rxf.server.CouchNamespace.NAMESPACE;

/**
 * User: jim
 * Date: 5/11/12
 * Time: 4:05 PM
 */
public class DealSiteServer {

    public static final Pattern FRAGMENT = Pattern.compile(".*_escaped_fragment_=([^&]+)");


    public static void registerurlExprAsFirst(HttpMethod method, Pattern passthroughExpr, Class<? extends Impl> value) {
        Map<Pattern, Class<? extends Impl>> linkedHashMap = new LinkedHashMap<Pattern, Class<? extends Impl>>();
        linkedHashMap.put(passthroughExpr, value);
        Map<Pattern, Class<? extends Impl>> patternImplMap = NAMESPACE.get(method);
        if (null != patternImplMap) {
            linkedHashMap.putAll(patternImplMap);
        }
        NAMESPACE.put(method, linkedHashMap);
    }

    public static void main(String... args) throws Exception {
        if (System.getenv().containsKey("DEBUG_DEAL_EDITORS")) {
            BlobAntiPatternObject.EXECUTOR_SERVICE.schedule(new Runnable() {
                public void run() {
                    CouchLocator<Npo> npoLocator = NPO_COUCH_LOCATOR;
                    CouchLocator<Vendor> vlocator = VENDOR_COUCH_LOCATOR;
                    CouchLocator<Deal> dlocator = DEAL_COUCH_LOCATOR;
                    Deal deal = dlocator.create(Deal.class);
                    deal.setProduct(UUID.randomUUID().toString());
                    deal.productDescription = word() + ' ' + word() + ' ' + word() + ' ' + word() + ' ' + word() + ' ' + word() + ' ' + word() + ".  " + word() + ' ' + word() + ' ' + word() + ' ' + word() + ' ' + word() + ' ' + word() + ' ' + word() + '!'; //UUID.randomUUID().toString()+' ' +UUID.randomUUID().toString()+' ' +UUID.randomUUID().toString()+' ' +UUID.randomUUID().toString()+".  " +UUID.randomUUID().toString()+' ' +UUID.randomUUID().toString()+' ' +UUID.randomUUID().toString()+'!' ;
                    deal.setLimit(new Random().nextInt(150) + 50);
                    deal.setMinimum(new Random().nextInt(48) + 2);

                    Vendor v = new Vendor();
                    v.setName(word());
                    v.setContactInfo(new Contact());
                    v.getContactInfo().setAddr1("1234 Main St");
                    v.getContactInfo().setCity("Somewhere");
                    v.getContactInfo().setState("Any State");
                    v.getContactInfo().setEmail("person@company.com");
                    v.getContactInfo().setPhone("321-654-0987");
                    v.setCreation(new Date());
                    v.setDescription(word() + " " + word() + " " + word());
                    v.setPocName("???");

                    Npo npo = new Npo();
                    npo.setContactInfo(new Contact());
                    npo.getContactInfo().setAddr1("987 Main St");
                    npo.getContactInfo().setCity("Elsewhere");
                    npo.getContactInfo().setState("Other State");
                    npo.getContactInfo().setEmail("person@cause.org");
                    npo.getContactInfo().setPhone("890-567-1234");
                    npo.setCreation(new Date());
                    npo.setDescription(word() + " " + word());
                    npo.setName(word());
                    npo.setPocName("???");

                    try {
                        CouchTx persist = npoLocator.persist(npo);
                        System.err.println(persist.toString());
                        String id = persist.id();
                        deal.setNpoId(id);

                        persist = vlocator.persist(v);
                        System.err.println(persist.toString());
                        deal.setVendorId(id);

                        persist = dlocator.persist(deal);
                        System.err.println(persist.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 5, TimeUnit.SECONDS);
            BlobAntiPatternObject.EXECUTOR_SERVICE.schedule(new Runnable() {
                public void run() {
                    DealService service = null;
                    try {
                        service = CouchServiceFactory.get(DealService.class);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    List<Deal> lawnmower = service.findByProduct("test");
                    System.err.println(deepToString(lawnmower));
                }
            }, 5, TimeUnit.SECONDS);
        }

        BlobAntiPatternObject.EXECUTOR_SERVICE.schedule(new Runnable() {
            @Override
            public void run() {
                registerurlExprAsFirst(HttpMethod.GET, FRAGMENT, FragMent.class /*(fragment)*/);

                Pattern authpat = Pattern.compile("/rxf.server.Auth/.*");
                registerurlExprAsFirst(HttpMethod.GET, authpat, OAuthHandler.class /*(fragment)*/);
                registerurlExprAsFirst(HttpMethod.POST, authpat, OAuthHandler.class /*(fragment)*/);

            }
        }, 250, TimeUnit.MILLISECONDS);
        BlobAntiPatternObject.startServer(args);

    }

    private static String word() {
        return Long.toString(abs(new Random().nextLong()), 36);
    }

    static class FragMent extends Impl {

        @Override
        public void onWrite(final SelectionKey key) throws Exception {
            Callable<String> callable = new Callable<String>() {


                public String call() throws Exception {
                    DealSiteServerActivities activities = new DealSiteServerActivities();
                    Object[] args = (Object[]) key.attachment();
                    String path = (String) args[1];
                    Matcher m = FRAGMENT.matcher(path);
                    m.find();
                    String f = m.group(1);

                    try {
                        String results = activities.generateContentForToken(f);


                        final ByteBuffer responseBuffer = UTF8.encode(results);
                        int length = responseBuffer.limit();// counting 16 bit chars could get us in trouble
                        String s1 = "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html ; charset=utf-8\r\n" +
                                "Content-Length: " + length + "\r\n\r\n";

                        final SocketChannel channel = (SocketChannel) key.channel();

                        ByteBuffer respHdrs = UTF8.encode(s1);
                        int written = channel.write(respHdrs);
                        // there is a chance that somehow this line of text will not fully
                        // write.  would be interesting to see when.
                        assert respHdrs.limit() == written;
                        key.selector().wakeup();//removes 1000ms from transitions from READ<->WRITE
                        key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT).attach(new Impl() {
                            @Override
                            public void onWrite(SelectionKey key) throws Exception {
                                int write = channel.write(responseBuffer);
                                if (!responseBuffer.hasRemaining()) {
                                    key.selector().wakeup();//removes 1000ms from transitions from READ<->WRITE
                                }
                                key.interestOps(SelectionKey.OP_READ).attach(null);//http pipeline takes over the selector
                            }
                        });
                    } catch (Throwable e) {
                        e.printStackTrace();
                        key.channel().close();
                    }

                    return null;
                }
            };
            Future<String> submit = BlobAntiPatternObject.EXECUTOR_SERVICE.submit(callable);
            key.interestOps(0);
        }
    }
}