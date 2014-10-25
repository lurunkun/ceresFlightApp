package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC.SingleBoardConnectionEvent;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC.SingleBoardConnectionService;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC.SingleBoardDataEvent;
import ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp.SBC.SingleBoardStatus;


public class SingleBoardStatusActivity extends Activity {

    private TextView mTextStatusName;
    private TextView mTextStatusTime;
    private TextView mTextStatusData;

    private TextView mTextErrorName;
    private TextView mTextErrorTime;
    private TextView mTextErrorData;

    private Button mButtonRestartSBC;

    ArrayList<SingleBoardDataEvent> mErrorList = new ArrayList<SingleBoardDataEvent>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_board_status);
        // status
        mTextStatusName = (TextView) findViewById(R.id.text_status_name);
        mTextStatusTime = (TextView) findViewById(R.id.text_status_time);
        mTextStatusData = (TextView) findViewById(R.id.text_status_data);
        // error
        mTextErrorName = (TextView) findViewById(R.id.text_error_name);
        mTextErrorTime = (TextView) findViewById(R.id.text_error_time);
        mTextErrorData = (TextView) findViewById(R.id.text_error_data);
        // restart button
        mButtonRestartSBC = (Button) findViewById(R.id.button_restart_SBC);

        // display errors
        mErrorList = SingleBoardConnectionService.mErrors;
        if (mErrorList.size() > 0) {
            SingleBoardDataEvent lastError = mErrorList.get(mErrorList.size() - 1);
            mTextErrorName.setText(lastError.name);
            mTextErrorTime.setText(lastError.timeStamp);
            mTextErrorData.setText(lastError.data);
        }

        SingleBoardConnectionService.getEventBus().register(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onClickRestartButton(View view) {
//        SingleBoardConnectionService.requestRestartSBC();
    }

    @Subscribe
    public void onSingleBoardDataEvent(SingleBoardDataEvent event) {
        if (event.type.equals(SingleBoardStatus.ERROR)) {
            mTextErrorName.setText(event.name);
            mTextErrorTime.setText(event.timeStamp);
            mTextErrorData.setText(event.data);
        }
        else if (event.type.equals(SingleBoardStatus.STATUS)) {
            mTextStatusName.setText(event.name);
            mTextStatusTime.setText(event.timeStamp);
            mTextStatusData.setText(event.data);
        }
    }

    @Subscribe
    public void onSingleBoardConnectionEvent(SingleBoardConnectionEvent event) {
    }
}
