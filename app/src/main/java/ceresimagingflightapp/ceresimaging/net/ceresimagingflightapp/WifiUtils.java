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

    public static String getSBCIp(Context context) {
        int ip = WifiUtils.getWifiInfo(context).getIpAddress();
        return Formatter.formatIpAddress(ip);
    }

    public static String getSBCHostName(Context context) {
        String ssid = WifiUtils.getWifiInfo(context).getSSID();
        return ssid;
    }

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
