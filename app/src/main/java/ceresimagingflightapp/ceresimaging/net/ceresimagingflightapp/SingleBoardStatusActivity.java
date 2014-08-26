package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.squareup.otto.Subscribe;


public class SingleBoardStatusActivity extends Activity {

    private TextView mTextStatusName;
    private TextView mTextStatusTime;
    private TextView mTextStatusData;

    private TextView mTextErrorName;
    private TextView mTextErrorTime;
    private TextView mTextErrorData;

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

        SingleBoardConnectionService.getEventBus().register(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
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
}
