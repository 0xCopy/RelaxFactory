package ds.model;

import rxf.server.CouchService;

import java.util.List;

public interface VendorService extends CouchService<Vendor> {

    @Override
    public Vendor find(String key);

    @View(map = "function(doc) { emit(doc.name, doc); }")
    List<Vendor> findWithName(@Key String name);

    @View(map = "function(doc) { emit(doc._id, doc); }")
    List<Vendor> findAll();
}
