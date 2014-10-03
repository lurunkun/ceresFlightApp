package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by huaruiwu on 10/2/14.
 */
public class SendSocketThread extends Thread{
        private Socket mSocket;
        public Socket getSocket() {
            return mSocket;
        }
        @Override
        public void run() {
            try {
                mSocket = new Socket(SingleBoardConnectionService.SBC_URL, SingleBoardConnectionService.SEND_PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
}
