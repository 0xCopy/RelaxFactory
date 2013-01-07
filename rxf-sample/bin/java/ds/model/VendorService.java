package ds.model;

import java.util.List;

import rxf.server.CouchService;

public interface VendorService extends CouchService<Vendor> {

  @Override
  public Vendor find(String key);

  @View(map = "function(doc) { emit(doc.name, doc); }")
  List<Vendor> findWithName(@Key String name);

  @View(map = "function(doc) { emit(doc._id, doc); }")
  List<Vendor> findAll();
}
