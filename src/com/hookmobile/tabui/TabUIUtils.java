package com.hookmobile.tabui;

import java.util.ArrayList;

import com.hookmobile.age.Lead;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcelable;

class TabUIUtils {
	
	static String appPackage = TabUIConstants.EMPTY_STRING;
	
	
	static int getResourceId(Context context, String resName, String className) {
		Resources r = context.getResources();
		
		return r.getIdentifier(resName, className, appPackage);
	}	
	
	static ArrayList<Lead> convertParcelableLead(ArrayList<Parcelable> parcelableLeads) {
		ArrayList<Lead> leads = new ArrayList<Lead>();
		
		for (int i = 0; i < parcelableLeads.size(); i++) {
			leads.add((Lead) parcelableLeads.get(i));
		}
		
		return leads;
	}

	static ArrayList<PhoneBookContact> convertParcelableContacts(ArrayList<Parcelable> parcelableContacts) {
		ArrayList<PhoneBookContact> contacts = new ArrayList<PhoneBookContact>();
		
		for (int i = 0; i < parcelableContacts.size(); i++) {
			contacts.add((PhoneBookContact) parcelableContacts.get(i));
		}
		
		return contacts;
	}
	
}
