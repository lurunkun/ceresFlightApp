package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by huarui on 8/11/14.
 */
public class SingleBoardDataEvent {

    public String message;

    public JSONObject statusObject;
    public String type;
    public String timeStamp;
    public String name;
    public String data;

    public SingleBoardDataEvent(String message) {
        this.message = message;
    }

    public SingleBoardDataEvent(JSONObject statusObject) throws JSONException {
        this.statusObject = statusObject;
        this.type = statusObject.getString("type");
        this.timeStamp = statusObject.getString("timeStamp");
        this.name  = statusObject.getString("name");
        this.data = statusObject.getString("data");
    }
}
