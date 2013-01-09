package ds.shared.rpc;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.List;

@RemoteServiceRelativePath("rpc")
public interface SampleRemoteService extends RemoteService {
    List<String> getAllNpoNames();
}
