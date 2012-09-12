package com.hookmobile.tabui;

import static com.hookmobile.tabui.TabUIConstants.TABUI_LOG;
import static com.hookmobile.tabui.TabUIUtils.convertParcelableContacts;
import static com.hookmobile.tabui.TabUIUtils.getResourceId;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.hookmobile.age.AgeException;
import com.hookmobile.age.AgeUtils;
import com.hookmobile.age.Discoverer;

/**
 * Activity which lists the phone book contacts, where contacts are 
 * searchable and they can be invited individually.
 */
public class PhoneBookContactsActivity extends Activity {

	private static final int SHOW_DIALOG = 0;
	private static final int HIDE_DIALOG = 1;
	
	private ListView contactsView;
	private PhoneBookContactsAdapter phoneBookContactsAdapter;
	private ProgressDialog progressDialog;
	private EditText filterEditText;
	
	private Dialog dialog;
	private Button dialogSendBtn;
	private EditText dialogEdtText;
	
	private Dialog invitationSentDialog;
	private Button invitationDialogBtn;
	
	private ArrayList<PhoneBookContact> phoneBookContacs;
	private ArrayList<String> cacheInvites;
	private ArrayList<String> inviteePhoneNumbers;
	private String inviterName;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch (msg.what) {
			case SHOW_DIALOG:
				progressDialog.show();
				break;
			case HIDE_DIALOG:
				progressDialog.dismiss();
				phoneBookContactsAdapter = new PhoneBookContactsAdapter(PhoneBookContactsActivity.this, phoneBookContacs);
				contactsView.setAdapter(phoneBookContactsAdapter);
				break;
			}
		}
	};
	
	
	void sendServerSMS(ArrayList<String> inviteePhoneNumbers, int position) {
		this.inviteePhoneNumbers = inviteePhoneNumbers;
		
		if (AgeUtils.isEmptyStr(inviterName)) {
			dialog.show();
		} else {
			sendSMS();
		}
	}
	
	void showInvitationSentDialog(){
		invitationSentDialog.show();
	}

	void displayError(AgeException e) {
		String body = TabUIConstants.MSG_SERVER_ERROR;

		if (e.getMessage() != null) {
			body += e.getMessage();
		} else {
			body += TabUIConstants.MSG_UNKNOWN_ERROR;
		}

		showMessageDialog(TabUIConstants.MSG_FINISHED, body, TabUIConstants.MSG_DISMISS);
	}
	
	void showMessageDialog(String title, String message, String buttonText) {
		new AlertDialog.Builder(this)
			.setMessage(message)
			.setPositiveButton(buttonText,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

					}
				}).show();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(getResourceId(this, "tabui_contactsview_container",	"layout"));
		contactsView = (ListView) findViewById(getResourceId(this, "listview", "id"));
		
		phoneBookContacs = new ArrayList<PhoneBookContact>();
		cacheInvites = new ArrayList<String>();
		
		progressDialog = new ProgressDialog(this);
		progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		progressDialog.setTitle(TabUIConstants.MSG_REFRESHING);
		progressDialog.setMessage(TabUIConstants.MSG_PLEASE_WAIT);
		progressDialog.setCancelable(false);
			
		SharedPreferences prefs = getSharedPreferences(TabUIConstants.TABUI_PREF, MODE_PRIVATE);
		final Editor editor = prefs.edit();

		int inviteSize = prefs.getInt(TabUIConstants.PREF_INVITE_SIZE, 0);
		for (int i = 0; i < inviteSize; i++) {
			String ids = prefs.getString(TabUIConstants.PREF_INVITE_VALUE_PREFIX + i, TabUIConstants.NUMBER);
			cacheInvites.add(ids);
		}
		inviterName = prefs.getString(TabUIConstants.PREF_INVITER_NAME, null);
		
		invitationSentDialog = new Dialog(this);
		invitationSentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		invitationSentDialog.setContentView(getResourceId(this, "tabui_invitationsent_dialog", "layout"));
		invitationDialogBtn = (Button) invitationSentDialog.findViewById(getResourceId(this, "dismissBtn", "id"));	
		invitationDialogBtn.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				invitationSentDialog.dismiss();				
			}
		});
		
		dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		dialog.setContentView(getResourceId(this, "tabui_entername_dialog", "layout"));
		dialogEdtText = (EditText) dialog.findViewById(getResourceId(this, "editText1", "id"));
		dialogSendBtn = (Button) dialog.findViewById(getResourceId(this, "button1", "id"));

		dialogEdtText.requestFocus();
		((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(dialogEdtText, InputMethodManager.SHOW_IMPLICIT);
		dialogSendBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				inviterName = dialogEdtText.getText().toString();
				editor.putString(TabUIConstants.PREF_INVITER_NAME, inviterName);
				editor.commit();
				sendSMS();
				dialog.dismiss();
			}
		});
		filterEditText = (EditText) findViewById(getResourceId(this, "filter_text", "id"));
		filterEditText.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String str = s.toString();
				final List<PhoneBookContact> tempHistoryList = new ArrayList<PhoneBookContact>();
				tempHistoryList.addAll(phoneBookContacs);
				PhoneBookContact data;
				
				for (int i = 0; i < phoneBookContacs.size(); i++) {
					data = phoneBookContacs.get(i);
					String name = data.getName().toLowerCase();
					
					if (name.indexOf((str.toLowerCase())) == -1) {
						tempHistoryList.remove(data);
					}
				}
				setUIElements(tempHistoryList);
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {

			}

			public void afterTextChanged(Editable s) {

			}
		});
		Button clearButton = (Button) findViewById(getResourceId(this, "button1", "id"));
		clearButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				filterEditText.setText(TabUIConstants.EMPTY_STRING);
			}
		});
		clearButton.requestFocus();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		if (savedInstanceState != null) {
			phoneBookContacs = convertParcelableContacts(savedInstanceState.getParcelableArrayList(TabUIConstants.STATE_PHONE_BOOK_CONTACTS));
			handler.sendMessage(handler.obtainMessage(HIDE_DIALOG));
			boolean dialogCon = savedInstanceState.getBoolean(TabUIConstants.STATE_DIALOG_VALUE, false);

			if (dialogCon) {
				inviteePhoneNumbers = savedInstanceState.getStringArrayList(TabUIConstants.STATE_PHONES);
				dialog.show();
			}
		} else {
			new Thread() {
				public void run() {
					handler.sendMessage(handler.obtainMessage(SHOW_DIALOG));
					getContacts();
					handler.sendMessage(handler.obtainMessage(HIDE_DIALOG));
				};
			}.start();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(TabUIConstants.STATE_PHONE_BOOK_CONTACTS, (ArrayList<PhoneBookContact>) phoneBookContacs);
		
		if (dialog.isShowing()) {
			outState.putStringArrayList(TabUIConstants.STATE_PHONES, inviteePhoneNumbers);
			outState.putBoolean(TabUIConstants.STATE_DIALOG_VALUE, dialog.isShowing());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		if (dialog != null)
			dialog.dismiss();
		
		if (progressDialog != null)
			progressDialog.dismiss();
	}
	
	private void setUIElements(List<PhoneBookContact> historyLists) {
		phoneBookContactsAdapter = new PhoneBookContactsAdapter(this, historyLists);
		contactsView.setAdapter(phoneBookContactsAdapter);
	}
	
	private void sendSMS() {
		try {
			if (AgeUtils.isOnline(this)) {
				Discoverer.getInstance().newReferral(inviteePhoneNumbers, true, inviterName);
				phoneBookContactsAdapter.notifyDataSetChanged();
				showInvitationSentDialog();
			}
		} catch (AgeException e) {
			Log.e(TABUI_LOG, e.getMessage());
			showMessage(e);
		}
	}

	private void showMessage(AgeException e) {
		String body = TabUIConstants.MSG_SERVER_ERROR;

		if (e.getMessage() != null) {
			body += e.getMessage();
		} else {
			body += TabUIConstants.MSG_UNKNOWN_ERROR;
		}
		
		showMessageDialog(TabUIConstants.MSG_FINISHED, body, TabUIConstants.MSG_DISMISS);
	}

	private void getContacts() {
		ContentResolver resolver = getContentResolver();
		Cursor cur = resolver.query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						new String[] {
							ContactsContract.CommonDataKinds.Phone._ID,
							ContactsContract.CommonDataKinds.Phone.NUMBER,
							ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
						},
						ContactsContract.CommonDataKinds.Phone.NUMBER + " IS NOT NULL",
						null,
						ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
		
		try {
			while (cur.moveToNext()) {
				String phone = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				String id = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));
				String name = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
				
				if (cacheInvites.contains(phone))
					phoneBookContacs.add(new PhoneBookContact(name, id, phone, true));
				else
					phoneBookContacs.add(new PhoneBookContact(name, id, phone, false));
			}
		}
		finally {
			cur.close();
		}
	}
	
}
