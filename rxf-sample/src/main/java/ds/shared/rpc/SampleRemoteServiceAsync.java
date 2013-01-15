package ds.shared.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.List;

public interface SampleRemoteServiceAsync {
    void getAllNpoNames(AsyncCallback<List<String>> callback);
}
