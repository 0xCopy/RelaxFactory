package ds.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import ds.model.Npo;
import ds.model.NpoService;
import ds.shared.rpc.SampleRemoteService;
import rxf.server.GwtRpcVisitor;

import java.util.ArrayList;
import java.util.List;

public class SampleRemoteServiceImpl extends GwtRpcVisitor implements SampleRemoteService {

    @Override
    public List<String> getAllNpoNames() {
        //Cheating horrifically, should never do this:
        Injector i = Guice.createInjector(new DealModule());
        // Instead, we should have more guice to create this visitor,
        // and inject dependencies

        NpoService dealService = i.getInstance(NpoService.class);
        List<Npo> all = dealService.findAll();

        List<String> allNames = new ArrayList<String>();
        for (Npo npo : all) {
            allNames.add(npo.getName());
        }

        return allNames;
    }
}
