package ds.model;

import rxf.server.CouchService;

import java.util.List;


public interface DealService extends CouchService<Deal> {
    @Override
    Deal find(String key);

    @View(map = "function(doc) { emit(doc.product, doc); }")
    List<Deal> findByProduct(@Key String query);

    @View(map = "function(doc) { emit(doc.vendorId, doc); }")
    List<Deal> findByVendor(@Key String vendorKey);

    @View(map = "function(doc) { emit(doc.npoId, doc); }")
    List<Deal> findByNpo(@Key String npoKey);

    @View(map = "function(doc) { emit(doc._id, doc); }")
    List<Deal> findAll();
}
