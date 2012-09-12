package com.hookmobile.tabui.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

import com.hookmobile.tabui.SmsType;
import com.hookmobile.tabui.TabUI;

public class MainActivity extends Activity {
	
	private String appKey = "Your-App-Key";

	private ToggleButton playToggle;
	private ToggleButton smsTypeToggle;
	private ToggleButton hideTabToggle;
	private Button toggleTabBtn;
	
	private TabUI tab;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		tab = new TabUI(this, savedInstanceState, appKey);
		tab.setPlayButtonVisible(true);
		tab.setTabVisible(true);
		TabUI.setSmsType(SmsType.NATIVE);
		
		playToggle = (ToggleButton) findViewById(R.id.toggleButton1);
		playToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
				tab.setPlayButtonVisible(checked);
			}
		});
		playToggle.setChecked(true);
		
		smsTypeToggle = (ToggleButton) findViewById(R.id.toggleButton2);
		smsTypeToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
				if (checked)
					TabUI.setSmsType(SmsType.NATIVE);
				else
					TabUI.setSmsType(SmsType.VIRTUAL_NUMBER);
			}
		});
		smsTypeToggle.setChecked(true);
		
		hideTabToggle = (ToggleButton) findViewById(R.id.toggleButton3);
		hideTabToggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean checked) {
				tab.setTabVisible(! checked);
			}
		});
		hideTabToggle.setChecked(false);

		toggleTabBtn = (Button) findViewById(R.id.toggletab);
		toggleTabBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				tab.openTabUI();
			}
		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		tab.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		super.onPause();
		tab.pause();
	}
	
}