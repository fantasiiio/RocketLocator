package com.frankdev.rocketlocator;

import java.util.Observable;
import java.util.Observer;

import org.broeuschmeul.android.gps.bluetooth.provider.BluetoothGpsManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

public class LogsActivity extends Activity implements Observer {

	private static Handler updateHandler;
	private String logsStr;
	private ScrollView scrollview;	
	private boolean scrollEnable = true;
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_logs);
		scrollview = ((ScrollView) findViewById(R.id.scrollView));
		
		updateHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				updateLogs(msg);
			}
		};
		
		BluetoothGpsManager blueGpsMan = SharedHolder.getInstance().getBlueGpsMan();
		if (blueGpsMan != null) {
			blueGpsMan.addObserver(this);
		}		

		ObservableLogs logs = SharedHolder.getInstance().getLogs();
		if (logs != null) {
			logs.addObserver(this);
		}		
	}

	
	protected void updateLogs(Message msg) {
		if(msg.obj == null)
			return;
		
		logsStr = (String) msg.obj;
		TextView txtLogs = (TextView) findViewById(R.id.txtLogs);
		txtLogs.setText(logsStr);
		if(this.scrollEnable){
			scrollview.fullScroll(ScrollView.FOCUS_DOWN);
		}
		
		//txtLogs.setSelection(txtLogs.getText().length());		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.logs, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// On regarde quel item a été cliqué grâce à son id et on déclenche une
		// action
		switch (item.getItemId()) {
		case R.id.menu_clear_logs:
			SharedHolder.getInstance().getLogs().Clear();
			
			TextView txtLogs = (TextView) findViewById(R.id.txtLogs);
			txtLogs.setText("");
			return true;
		case R.id.menu_toggle_scroll:
			this.scrollEnable = !this.scrollEnable;
			return true;
		}
			
		return false;
	}
	
	@Override
	public void update(Observable observable, Object data) {
		if(observable instanceof ObservableLogs){
			Message msg = new Message();
			msg.obj = SharedHolder.getInstance().getLogs().getLogText();
			updateHandler.sendMessage(msg);			
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		BluetoothGpsManager blueGpsMan = SharedHolder.getInstance().getBlueGpsMan();
		if (blueGpsMan != null) {
			blueGpsMan.deleteObserver(this);
		}		
		ObservableLogs logs = SharedHolder.getInstance().getLogs();
		if (logs != null) {
			logs.deleteObserver(this);
		}		
	}
	
}
