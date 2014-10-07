package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.util.List;


/**
 * Created by huaruiwu on 10/7/14.
 */
public class WifiUtils {

    private static WifiInfo getWifiInfo(Context context) {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        return wifiMgr.getConnectionInfo();
    }

    // returns IP of current wifi connection
    @SuppressWarnings("depreciation")
    public static String getConnectedIp(Context context) {
        int ip = WifiUtils.getWifiInfo(context).getIpAddress();
        return Formatter.formatIpAddress(ip);
    }

    // returns SSID of current wifi connection
    public static String getSBCHostName(Context context) {
        String ssid = WifiUtils.getWifiInfo(context).getSSID();
        return ssid;
    }

    // connects to a wifi host by SSID and pw
    public static void ConnectBySSID(Context context, String SSID, String pw) {
        // SSID and Password must be in quotes
        SSID = "\"" + SSID + "\"";
        WifiConfiguration conf = new WifiConfiguration();
        conf.preSharedKey = "\"" + pw + "\"";
        WifiManager wifiMgr = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        wifiMgr.addNetwork(conf);

        List<WifiConfiguration> list = wifiMgr.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
                wifiMgr.disconnect();
                wifiMgr.enableNetwork(i.networkId, true);
                wifiMgr.reconnect();
                break;
            }
        }
    }

}
