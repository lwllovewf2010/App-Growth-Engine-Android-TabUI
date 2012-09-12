package com.hookmobile.tabui;

import static com.hookmobile.tabui.TabUIUtils.getResourceId;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

class FriendsAdapter extends BaseAdapter {
	
	private Activity context;
	private TabUI tabui;
	private List<PhoneBookContact> friends;

	
	public FriendsAdapter(Activity context, TabUI tabui, List<PhoneBookContact> friends) {
		this.context = context;
		this.tabui = tabui;
		this.friends = friends;
	}
	
	public View getView(final int position, View view, ViewGroup parent) {
		if (view == null) {
			LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(getResourceId(context, "tabui_mutualcontactitem", "layout"), null);
		}

		final ViewHolder viewHolder = new ViewHolder();
		viewHolder.text = (TextView) view.findViewById(getResourceId(context, "contactname", "id"));
		viewHolder.check = (ImageView) view.findViewById(getResourceId(context, "contactcheck", "id"));

		view.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (tabui.isPlayButtonVisible()) {
					PhoneBookContact element = friends.get(position);
					
					if (element.isInvited()) {
						element.setInvited(false);
						viewHolder.check.setVisibility(View.INVISIBLE);
					} else {
						element.setInvited(true);
						viewHolder.check.setVisibility(View.VISIBLE);
					}
					
					notifyDataSetChanged();
				}
			}
		});

		viewHolder.text.setText(friends.get(position).getName());

		if (friends.get(position).isInvited())
			viewHolder.check.setVisibility(View.VISIBLE);
		else
			viewHolder.check.setVisibility(View.INVISIBLE);
		
		return view;
	}
	
	public int getCount() {
		return friends.size();
	}
	
	public Object getItem(int position) {
		return friends.get(position);
	}
	
	public long getItemId(int position) {
		return position;
	}
	
	public List<PhoneBookContact> getFriends() {
		return friends;
	}

	public void setFriends(List<PhoneBookContact> friends) {
		this.friends = friends;
	}
	
	private class ViewHolder {
		private TextView text;
		private ImageView check;
	}
	
}
