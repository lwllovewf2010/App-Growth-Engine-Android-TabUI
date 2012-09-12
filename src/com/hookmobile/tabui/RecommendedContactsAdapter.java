package com.hookmobile.tabui;

import static com.hookmobile.age.AgeUtils.lookupNameByPhone;
import static com.hookmobile.tabui.TabUIUtils.getResourceId;
import static com.hookmobile.tabui.TabUIConstants.TABUI_LOG;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hookmobile.age.Lead;

class RecommendedContactsAdapter extends BaseAdapter {
	
	private Activity context;
	private List<Lead> leads;
	private List<RecommendedContact> recommendedContacts;
	
	
	public RecommendedContactsAdapter(Activity context, List<Lead> leads) {
		this.context = context;
		this.leads = leads;
		this.recommendedContacts = new ArrayList<RecommendedContact>();
	}
	
	public View getView(final int position, View view, ViewGroup parent) {
		if (view == null) {
			LayoutInflater inflator = context.getLayoutInflater();
			view = inflator.inflate(getResourceId(context, "tabui_contactitem",	"layout"), null);
		}
		
		final ViewHolder viewHolder = new ViewHolder();
		viewHolder.text = (TextView) view.findViewById(getResourceId(context, "contactname", "id"));
		viewHolder.icon = (ImageView) view.findViewById(getResourceId(context, "imageView1", "id"));
		viewHolder.check = (ImageView) view.findViewById(getResourceId(context, "contactcheck", "id"));

		view.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				RecommendedContact contact = recommendedContacts.get(position);
				
				if (contact.isSelected()) {
					contact.setSelected(false);
					viewHolder.check.setVisibility(View.INVISIBLE);
				} else {
					contact.setSelected(true);
					viewHolder.check.setVisibility(View.VISIBLE);
				}
				
				notifyDataSetChanged();
			}
		});

		try {
			viewHolder.text.setText(recommendedContacts.get(position).getName());

			if (leads.get(position).getOsType().equals("ios")) {
				viewHolder.icon.setImageResource(getResourceId(context, "tabui_apple_icon", "drawable"));
			} else {
				viewHolder.icon.setImageResource(getResourceId(context, "tabui_android_icon", "drawable"));
			}

			if (recommendedContacts.get(position).isSelected())
				viewHolder.check.setVisibility(View.VISIBLE);
			else
				viewHolder.check.setVisibility(View.INVISIBLE);
		} catch (Exception e) {
			Log.e(TABUI_LOG, e.getMessage());
		}
		
		return view;
	}
	
	public void populateLeads() {
		if (leads != null) {
			int count = leads.size();

			if (count > 0) {
				for (int i = 0; i < count; i++) {
					String name = lookupNameByPhone(RecommendedContactsAdapter.this.context, leads.get(i).getPhone());
					recommendedContacts.add(new RecommendedContact(name));
				}
			}
		}
	}

	public int getCount() {
		return recommendedContacts.size();
	}

	public Object getItem(int position) {
		return recommendedContacts.get(position);
	}

	public long getItemId(int position) {
		return position;
	}
	
	public List<RecommendedContact> getRecommendedContacts() {
		return recommendedContacts;
	}

	public void setRecommendedContacts(List<RecommendedContact> recommendedContacts) {
		this.recommendedContacts = recommendedContacts;
	}

	public void clear() {
		recommendedContacts.clear();
	}

	public List<Lead> getLeads() {
		return leads;
	}

	public void setLeads(List<Lead> leads) {
		this.leads = leads;
	}

	private class ViewHolder {
		private TextView text;
		private ImageView icon;
		private ImageView check;
	}
	
}
