package com.hookmobile.tabui;

import static com.hookmobile.tabui.TabUIUtils.getResourceId;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.hookmobile.age.AgeException;
import com.hookmobile.age.AgeUtils;
import com.hookmobile.age.Discoverer;

class PhoneBookContactsAdapter extends BaseAdapter {
	
	private PhoneBookContactsActivity context;
	private List<PhoneBookContact> phoneBookContacts;

	
	public PhoneBookContactsAdapter(PhoneBookContactsActivity context, List<PhoneBookContact> phoneBookContacts) {
		this.context = context;
		this.phoneBookContacts = phoneBookContacts;
	}
	
	public View getView(final int position, View view, ViewGroup parent) {
		if (view == null) {
			LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(getResourceId(context, "tabui_contactsitem", "layout"), null);
		}

		ViewHolder viewHolder = new ViewHolder();
		viewHolder.text = (TextView) view.findViewById(getResourceId(context, "contactname", "id"));
		viewHolder.inviteBtn = (Button) view.findViewById(getResourceId(context, "invitebutton", "id"));

		viewHolder.inviteBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				PhoneBookContact contact = phoneBookContacts.get(position);
				ArrayList<String> numberList = new ArrayList<String>();
				numberList.add(contact.getPhoneNumber());
				
				try {
					if (TabUI.getSmsType() == SmsType.NATIVE) {
						if (AgeUtils.isOnline(context)) {
							int id = TabUIUtils.getResourceId(context, "app_name", "string");
							Discoverer.getInstance().newReferral(numberList, false, context.getString(id));
							context.showInvitationSentDialog();
						} else {
							context.showMessageDialog(TabUIConstants.EMPTY_STRING, TabUIConstants.MSG_UNABLE_TO_SEND, TabUIConstants.MSG_OK);
						}
						contact.setInvited(true);
					} else {
						if (AgeUtils.isOnline(context)) {
							PhoneBookContactsAdapter.this.context.sendServerSMS(numberList, position);
						} else {
							context.showMessageDialog(TabUIConstants.EMPTY_STRING, TabUIConstants.MSG_UNABLE_TO_SEND, TabUIConstants.MSG_OK);
						}
					}
				} catch (AgeException e) {
					context.displayError(e);
				}

				notifyDataSetChanged();
			}
		});

		PhoneBookContact contact = phoneBookContacts.get(position);
		viewHolder.text.setText(contact.getName());

		return view;
	}

	public int getCount() {
		return phoneBookContacts.size();
	}

	public Object getItem(int position) {
		return phoneBookContacts.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public List<PhoneBookContact> getPhoneBookContacts() {
		return phoneBookContacts;
	}

	public void setPhoneBookContacts(List<PhoneBookContact> phoneBookContacts) {
		this.phoneBookContacts = phoneBookContacts;
	}

	private class ViewHolder {
		private TextView text;
		private Button inviteBtn;
	}
	
}
