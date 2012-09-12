package com.hookmobile.tabui;

import java.io.Serializable;

class PhoneBookContact implements Serializable {

	private static final long serialVersionUID = 7850469092842777724L;
	
	private String name;
	private String id;
	private String phoneNumber;
	private boolean invited;

	
	public PhoneBookContact(String name, String id, String phoneNumber) {
		this(name, id, phoneNumber, false);
	}
	
	public PhoneBookContact(String name, String id, String phoneNumber, boolean invited) {
		this.name = name;
		this.id = id;
		this.phoneNumber = phoneNumber;
		this.invited = invited;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public boolean isInvited() {
		return invited;
	}

	public void setInvited(boolean invited) {
		this.invited = invited;
	}
	
}
