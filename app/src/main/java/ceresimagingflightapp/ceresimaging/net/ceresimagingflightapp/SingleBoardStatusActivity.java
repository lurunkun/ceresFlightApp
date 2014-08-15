package ceresimagingflightapp.ceresimaging.net.ceresimagingflightapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.squareup.otto.Subscribe;


public class SingleBoardStatusActivity extends Activity {
    private TextView mTextStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_board_status);
        mTextStatus = (TextView) findViewById(R.id.text_status);
        SingleBoardConnectionService.getEventBus().register(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Subscribe
    public void onSingleBoardDataEvent(SingleBoardDataEvent event) {
        mTextStatus.setText(event.message);
    }
}
