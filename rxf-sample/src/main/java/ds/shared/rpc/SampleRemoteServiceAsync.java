package ds.shared.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface SampleRemoteServiceAsync {
	void getAllNpoNames(AsyncCallback<List<String>> callback);
}
