package com.hookmobile.tabui;

import java.io.Serializable;

class RecommendedContact implements Serializable {
	
	private static final long serialVersionUID = 8929387534297890842L;
	
	private String name;
	private boolean selected;
	
	
	public RecommendedContact(String name) {
		this(name, false);
	}
	
	public RecommendedContact(String name, boolean selected) {
		this.name = name;
		this.selected = selected;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
}
