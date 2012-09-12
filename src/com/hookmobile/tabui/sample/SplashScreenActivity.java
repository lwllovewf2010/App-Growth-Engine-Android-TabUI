package com.hookmobile.tabui.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

public class SplashScreenActivity extends Activity {

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
			finish();
		};
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.splashscreen);
		
		new Thread() {
			@Override
			public void run() {
				super.run();

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					
				}
				handler.sendEmptyMessage(1);
			}
		}.start();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}
	
}
