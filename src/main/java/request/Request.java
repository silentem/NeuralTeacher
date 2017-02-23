package request;

import java.util.List;

/**
 * Created by Влад on 14.02.2017.
 */
public interface Request {
    List<String> makeRequest(String uri);
}
