package com.hookmobile.tabui;

import java.util.List;

/**
 * Interface for classes to implement play action.
 */
public interface Playable {
	
	/**
	 * This play method will be invoked when the play button clicked.
	 * Registering the play action on the TabUI is required.
	 * 
	 * @param selectedFriends the phone numbers of the selected friends.
	 */
	public void play(List<String> selectedFriends);
	
}
