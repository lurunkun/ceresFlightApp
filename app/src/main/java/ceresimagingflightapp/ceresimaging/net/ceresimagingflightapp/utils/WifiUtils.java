package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.utils;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.util.List;


/**
 * Created by huaruiwu on 10/7/14.
 */
public class WifiUtils {

    private static final String SBC_SSID = "CeresSBC";
    private static final String SBC_PW = "12345";

    public static WifiInfo getWifiInfo(WifiManager wifiMgr) {
        return wifiMgr.getConnectionInfo();
    }

    // returns IP of current wifi connection
    @SuppressWarnings("depreciation")
    public static String getConnectedIp(WifiManager wifiMgr) {
        int ip = getWifiInfo(wifiMgr).getIpAddress();
        return Formatter.formatIpAddress(ip);
    }

    // returns SSID of current wifi connection
    public static String getSBCHostName(WifiManager wifiMgr) {
        return getWifiInfo(wifiMgr).getSSID();
    }

    // connect using a configuration
    public static void connectToConfiguration(WifiManager wifiMgr, WifiConfiguration conf) {
        wifiMgr.disconnect();
        wifiMgr.enableNetwork(conf.networkId, true);
        wifiMgr.reconnect();
    }

    // configure a wifi host by SSID and pw
    public static void configureConnection(WifiManager wifiMgr, String SSID, String pw) {
        // SSID and Password must be in quotes
        SSID = "\"" + SSID + "\"";
        WifiConfiguration conf = new WifiConfiguration();
        conf.preSharedKey = "\"" + pw + "\"";
        wifiMgr.addNetwork(conf);
    }

    // returns true if there is a SBC wifi connection that is configured
    public static boolean isSBCWifiConfigured(WifiManager wifiMgr) {
        List<WifiConfiguration> list = wifiMgr.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.substring(0, SBC_SSID.length()).equals("\"" + SBC_SSID + "\"")) {
                return true;
            }
        }
        return false;
    }

    // configure available SBC connections
    // returns false if no SBC connections are available
    public static boolean configSBCWifiConnections(WifiManager wifiMgr) {
        List<ScanResult> list = wifiMgr.getScanResults();
        for (ScanResult res : list) {
            if (res.SSID != null && res.SSID.substring(0, SBC_SSID.length()).equals("\"" + SBC_SSID + "\"")) {
                configureConnection(wifiMgr, res.SSID, SBC_PW);
                return true;
            }
        }
        return false;
    }

    // Connects to the first SBC wifi that is configured
    public static void connectToFirstSBC(WifiManager wifiMgr) {
        List<WifiConfiguration> list = wifiMgr.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.substring(0, SBC_SSID.length()).equals("\"" + SBC_SSID + "\"")) {
                connectToConfiguration(wifiMgr, i);
            }
        }
    }

}
