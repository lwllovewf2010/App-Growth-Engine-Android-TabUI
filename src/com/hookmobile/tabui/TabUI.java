package com.hookmobile.tabui;

import static com.hookmobile.tabui.TabUIConstants.TABUI_LOG;
import static com.hookmobile.tabui.TabUIUtils.convertParcelableContacts;
import static com.hookmobile.tabui.TabUIUtils.convertParcelableLead;
import static com.hookmobile.tabui.TabUIUtils.getResourceId;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.hookmobile.age.AgeException;
import com.hookmobile.age.AgeUtils;
import com.hookmobile.age.Direction;
import com.hookmobile.age.Discoverer;
import com.hookmobile.age.Lead;

/**
 * This is the main Tab UI component. You can also refer to Hook
 * Mobile's <a href="" target="_blank">Android Plug-in Tutorial</a> for more information.
 */
public class TabUI {
	
	private static final int SHOW_PREPARE_DIALOG = 0;
	private static final int HIDE_PREPARE_DIALOG = 1;
	private static final int SHOW_DISCOVER_MESSAGE_DIALOG = 2;
	private static final int HIDE_MESSAGE_DIALOG = 3;
	private static final int SHOW_MESSAGE_DIALOG = 4;
	private static final int SHOW_LIST_LEADS = 5;
	private static final int SHOW_SMS_TOAST = 6;
	private static final int SHOW_NAME_DIALOG_SMS = 7;
	private static final int SHOW_SMS_CONFIRMATION = 8;
	
	private static final int DIALOG_ACTION_DEFAULT = 0;
	private static final int DIALOG_ACTION_DISCOVER_YES = 1;
	private static final int DIALOG_ACTION_DISCOVER_NO = 2;
	private static final int DIALOG_ACTION_DISCOVER_FINISHED = 3;
	private static final int DIALOG_ACTION_OTHER = 4;
	private static final int DIALOG_ACTION_QUERYLEADS = 5;
	
	private static final String LEADS_TAB_ID = "leads_tab";
	private static final String FRIENDS_TAB_ID = "friends_tab";
	
	private static SmsType smsType = SmsType.VIRTUAL_NUMBER;
	
	private boolean tabVisible = true;
	private boolean playButtonVisible = true;
	private Playable action;
	private String phoneBookPermissionMessage = "To find family and friends, AGE needs to send your contacts to our server.";
	
	private Activity context;
	private SharedPreferences prefs;
	private Editor editor;
	private int dialogActionValue = DIALOG_ACTION_DEFAULT;
	
	private LayoutInflater layoutInflater;
	private ViewGroup mainLayout;
	private LinearLayout drawableLayout;
	private SlidingDrawer slidingDrawer;
	private TabHost tabHost;
	private TabSpec leadsTab;
	private TabSpec friendsTab;
	private ListView leadsView;
	private ListView friendsView;
	private RecommendedContactsAdapter leadsAdapter;
	private FriendsAdapter friendsAdapter;
	private RelativeLayout headerLayout;
	private Button inviteBtn;
	private Button sendNplayBtn;
	private ProgressDialog progressDialog;
	private AlertDialog messageDialog;
	
	private Dialog inviterNameDialog;
	private Button inviterNameSendBtn;
	private EditText inviterNameText;
	
	private Dialog invitationSentDialog;
	private Button invitationDialogBtn;
	
	private List<Lead> leads;
	private List<PhoneBookContact> friends;
	private ArrayList<String> inviteSelectedPhoneNumbers;
	private String inviterName;
	private int contactCheck = -1;
	private boolean dialogCheck = true;
	
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			
			switch (msg.what) {
			case SHOW_PREPARE_DIALOG:
				progressDialog.show();
				break;
			case HIDE_PREPARE_DIALOG:
				progressDialog.dismiss();
				break;
			case SHOW_DISCOVER_MESSAGE_DIALOG:
				dialogActionValue = DIALOG_ACTION_DEFAULT;
				showMessageDialog(TabUIConstants.MSG_FINISHED, TabUIConstants.MSG_DISCOVER, TabUIConstants.MSG_DISMISS, null);
				break;
			case HIDE_MESSAGE_DIALOG:
				dialogActionValue = DIALOG_ACTION_DEFAULT;
				progressDialog.dismiss();
				break;
			case SHOW_MESSAGE_DIALOG:
				String[] content = (String[]) msg.obj;
				showMessageDialog(content[0], content[1], content[2], null);
				break;
			case SHOW_LIST_LEADS:
				leadsAdapter.notifyDataSetChanged();
				progressDialog.dismiss();
				leadsView.setAdapter(leadsAdapter);
				setFriendAdapter();
				break;
			case SHOW_SMS_TOAST:
				break;
			case SHOW_NAME_DIALOG_SMS:
				sendServerSMS(null, TabUIConstants.INVALID);
				break;
			case SHOW_SMS_CONFIRMATION:
				dialogActionValue = DIALOG_ACTION_QUERYLEADS;				
				invitationSentDialog.show();
				break;
			default:
				break;
			}
		}
	};
	
	
	/**
	 * Constructor.
	 * 
	 * @param context the Android context.
	 * @param savedInstanceState the Bundle parameter received during the onCreate method in Activity.
	 * @param appKey the App Key for this app.
	 */
	public TabUI(Activity context, Bundle savedInstanceState, String appKey) {
		this.context = context;
		TabUIUtils.appPackage = context.getPackageName();
		Discoverer.activate(context, appKey);
		
		init();
		
		if (savedInstanceState == null) {
			int val = prefs.getInt(TabUIConstants.PREF_LEAD_SIZE, TabUIConstants.INVALID);

			leads = new ArrayList<Lead>();
			Lead lead;
			for (int i = 0; i < val; i++) {
				String phone = prefs.getString(TabUIConstants.PREF_PHONE_VALUE_PREFIX + i, TabUIConstants.NUMBER);
				String os = prefs.getString(TabUIConstants.PREF_OS_VALUE_PREFIX + i, TabUIConstants.NUMBER);
				lead = new Lead();
				lead.setPhone(phone);
				lead.setOsType(os);
				leads.add(lead);
			}
			
			friends = new ArrayList<PhoneBookContact>();
			val = prefs.getInt(TabUIConstants.PREF_FRIEND_SIZE, TabUIConstants.INVALID);
			for (int i = 0; i < val; i++) {
				String phone = prefs.getString(TabUIConstants.PREF_FRIEND_VALUE_PREFIX + i, "0");
				String name = AgeUtils.lookupNameByPhone(context, phone);
				friends.add(new PhoneBookContact(name, null, phone));
			}
		} else {
			leads = convertParcelableLead(savedInstanceState.getParcelableArrayList(TabUIConstants.STATE_LEADS));
			friends = convertParcelableContacts(savedInstanceState.getParcelableArrayList(TabUIConstants.STATE_FRIENDS));

			boolean slidingDrawerOpened = savedInstanceState.getBoolean(TabUIConstants.PREF_SLIDING_DRAWER_OPENED);
			if (slidingDrawerOpened) {
				slidingDrawer.open();
			}

			int currentTab = savedInstanceState.getInt(TabUIConstants.STATE_CURRENT_TAB);
			tabHost.setCurrentTab(currentTab);

			if (currentTab == 0) {
				sendNplayBtn.setBackgroundResource(getResourceId(context, "tabui_send_button", "drawable"));
			} else {
				sendNplayBtn.setBackgroundResource(getResourceId(context, "tabui_play_button", "drawable"));
			}

			leadsAdapter = new RecommendedContactsAdapter(context, leads);
			boolean[] selected = savedInstanceState.getBooleanArray(TabUIConstants.STATE_LEADS_SELECTED);
			leadsAdapter.populateLeads();
			List<RecommendedContact> leadList = leadsAdapter.getRecommendedContacts();
			for (int i = 0; i < selected.length; i++) {
				leadList.get(i).setSelected(selected[i]);
			}
			leadsAdapter.setRecommendedContacts(leadList);
			setFriendAdapter();

			boolean dialogCon = savedInstanceState.getBoolean(TabUIConstants.STATE_DIALOG_VALUE, false);
			if (dialogCon) {
				inviteSelectedPhoneNumbers = savedInstanceState.getStringArrayList(TabUIConstants.STATE_PHONES);
				inviterNameDialog.show();
			}
		}
		
		if (leadsAdapter == null) {
			leadsAdapter = new RecommendedContactsAdapter(context, leads);
			leadsAdapter.populateLeads();
		}
		handler.sendMessage(handler.obtainMessage(SHOW_LIST_LEADS));
		
		if (!AgeUtils.isOnline(context)) {
			dialogActionValue = DIALOG_ACTION_DEFAULT;
			showMessageDialog(TabUIConstants.EMPTY_STRING, TabUIConstants.MSG_INTERNET_REQUIRED, TabUIConstants.MSG_OK, null);
		}

		setupScreenOrientationUIValues();
	}

	/**
	 * Sets SMS delivery method for invitation.
	 * 
	 * @param smsType the SMS type.
	 */
	public static void setSmsType(SmsType smsType) {
		TabUI.smsType = smsType;
	}
	
	/**
	 * Gets SMS delivery method.
	 * 
	 * @return the SMS type.
	 */
	public static SmsType getSmsType() {
		return TabUI.smsType;
	}
	
	/**
	 * Makes the tab visible or invisible.
	 * 
	 * @param visible whether the tab is visible or not.
	 */
	public void setTabVisible(boolean visible) {
		this.tabVisible = visible;
		
		if (visible) {
			slidingDrawer.setVisibility(View.VISIBLE);
		}
		else {
			slidingDrawer.setVisibility(View.INVISIBLE);
		}
	}
	
	/**
	 * Checks whether the tab is visible.
	 * 
	 * @return true if visible; false otherwise.
	 */
	public boolean isTabVisible() {
		return tabVisible;
	}

	/**
	 * Makes the play button visible or invisible.
	 * 
	 * @param visible whether play button is visible or not.
	 */
	public void setPlayButtonVisible(boolean visible) {
		this.playButtonVisible = visible;
		
		if(! visible) {
			for(int i=0; i<friends.size(); i++)
				friends.get(i).setInvited(false);
		}
		
		updatePlayButtonOnTab(visible);
		
		if (tabHost.getCurrentTab() == TabUIConstants.CHK_VALUE_FALSE) {
			sendNplayBtn.setVisibility(View.VISIBLE);
		} else {
			if (visible) {
				sendNplayBtn.setVisibility(View.VISIBLE);
				headerLayout.setBackgroundResource(getResourceId(context, "tabui_header_play_p", "drawable"));
			} else {
				sendNplayBtn.setVisibility(View.INVISIBLE);
				headerLayout.setBackgroundResource(getResourceId(context, "tabui_header_friends_p", "drawable"));
			}			
		}
	}
	
	/**
	 * Checks whether the play button is visible.
	 * 
	 * @return true if visible; false otherwise.
	 */
	public boolean isPlayButtonVisible() {
		return playButtonVisible;
	}
	
	/**
	 * Registers the implementation for play button.
	 */
	public void registerPlayAction(Playable action) {
		this.action = action;
	}
	
	/**
	 * Sets the phone book permission message.
	 * 
	 *  @param phoneBookPermissionMessage the permission message.
	 */
	public void setPhoneBookPermissionMessage(String phoneBookPermissionMessage) {
		this.phoneBookPermissionMessage = phoneBookPermissionMessage;
	}
	
	/**
	 * Gets the phone book permission message;
	 * 
	 * @return the permission message.
	 */
	public String getPhoneBookPermissionMessage() {
		return phoneBookPermissionMessage;
	}

	/**
	 * Opens the TabUI.
	 */
	public void openTabUI() {
		if (slidingDrawer.isOpened()) {
			slidingDrawer.animateClose();
		} else {
			if (!slidingDrawer.isShown())
				slidingDrawer.setVisibility(View.VISIBLE);
			slidingDrawer.animateOpen();
		}
	}
	
	/**
	 * Saves the data and state of TabUI. All the stored data will be reused when the TabUI is again created.
	 * 
	 * @param outState the Bundle object in which all the data of TabUI are stored.
	 */
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(TabUIConstants.STATE_LEADS, (ArrayList<Lead>)leads);
		outState.putSerializable(TabUIConstants.STATE_FRIENDS, (ArrayList<PhoneBookContact>)friends);
		outState.putBoolean(TabUIConstants.PREF_SLIDING_DRAWER_OPENED, slidingDrawer.isOpened());
		outState.putInt(TabUIConstants.STATE_CURRENT_TAB, tabHost.getCurrentTab());
		
		List<RecommendedContact> leadlist = leadsAdapter.getRecommendedContacts();
		boolean[] selected = new boolean[leadlist.size()];

		for (int i = 0; i < leadlist.size(); i++) {
			selected[i] = leadlist.get(i).isSelected();
		}
		outState.putBooleanArray(TabUIConstants.STATE_LEADS_SELECTED, selected);

		if (inviterNameDialog.isShowing()) {
			outState.putStringArrayList(TabUIConstants.STATE_PHONES, inviteSelectedPhoneNumbers);
			outState.putBoolean(TabUIConstants.STATE_DIALOG_VALUE, inviterNameDialog.isShowing());
		}
	}

	/**
	 * Eliminates all the asynchronous components before closing the activity.
	 */
	public void pause() {
		if (inviterNameDialog != null)
			inviterNameDialog.dismiss();
		
		if (progressDialog != null)
			progressDialog.dismiss();
	}
	
	private void init() {
		layoutInflater = context.getLayoutInflater();
		slidingDrawer = (SlidingDrawer) context.findViewById(getResourceId(context, "drawer", "id"));
		mainLayout = (ViewGroup) slidingDrawer.getParent();

		drawableLayout = new LinearLayout(context);
		drawableLayout.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.FILL_PARENT));
		drawableLayout.setBackgroundColor(Color.argb(150, 0, 0, 0));

		setupDialogs();
		setupTabs();
		loadPreferences();

		tabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				if (tabId.equals(LEADS_TAB_ID)) {
					headerLayout.setBackgroundResource(getResourceId(context, "tabui_header_suggested_p", "drawable"));
					sendNplayBtn.setBackgroundResource(getResourceId(context, "tabui_send_button", "drawable"));
					sendNplayBtn.setVisibility(View.VISIBLE);
				} else if (tabId.equals(FRIENDS_TAB_ID)) {
					try {
						sendNplayBtn.setBackgroundResource(getResourceId(context, "tabui_play_button", "drawable"));
						
						if (! playButtonVisible) {
							sendNplayBtn.setVisibility(View.INVISIBLE);
							headerLayout.setBackgroundResource(getResourceId(context, "tabui_header_friends_p", "drawable"));
						} else {
							sendNplayBtn.setVisibility(View.VISIBLE);
							headerLayout.setBackgroundResource(getResourceId(context, "tabui_header_play_p", "drawable"));
						}
							
						setFriendAdapter();
					} catch (Exception e) {
						Log.e(TABUI_LOG, e.getMessage());
					}
				}
			}
		});
		
		slidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {
			public void onDrawerOpened() {
				discoverWithCheck();
				mainLayout.addView(drawableLayout);
				mainLayout.bringChildToFront(slidingDrawer);
			}
		});
		slidingDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener() {
			public void onDrawerClosed() {
				if (! tabVisible) {
					slidingDrawer.setVisibility(View.INVISIBLE);
				}
				mainLayout.removeView(drawableLayout);
			}
		});
	}
	
	private void sendSMS() {
		try {
			if (AgeUtils.isOnline(context)) {
				Discoverer.getInstance().newReferral(inviteSelectedPhoneNumbers, true, inviterName);

				dialogActionValue = DIALOG_ACTION_QUERYLEADS;
				handler.sendMessage(handler.obtainMessage(SHOW_SMS_CONFIRMATION));
			}
		} catch (AgeException e) {
			Log.e(TABUI_LOG, e.getMessage());
			showMessage(e);
		}
	}

	private void sendServerSMS(ArrayList<String> numberList, int position) {
		inviterName = prefs.getString(TabUIConstants.PREF_INVITER_NAME, null);
		
		if (AgeUtils.isEmptyStr(inviterName)) {
			inviterNameDialog.show();
		} else {
			sendSMS();
		}
	}
	
	private void updatePlayButtonOnTab(boolean visible) {
		if (visible) {
			tabHost.getTabWidget().getChildAt(1).setBackgroundResource(getResourceId(context, "tabui_play_tab_selector_p", "drawable"));
		}
		else {
			tabHost.getTabWidget().getChildAt(1).setBackgroundResource(getResourceId(context, "tabui_friends_tab_selector_p", "drawable"));
		}
		
		setFriendAdapter();
	}
	
	private void setFriendAdapter() {
		if (friendsView != null) {
			friendsAdapter = new FriendsAdapter(context, this, friends);
			friendsView.setAdapter(friendsAdapter);
		}
	}
	
	private void loadPreferences() {
		prefs = context.getSharedPreferences(TabUIConstants.TABUI_PREF, Context.MODE_PRIVATE);
		editor = prefs.edit();
		
		int val = prefs.getInt(TabUIConstants.PREF_CONTACT_CHECK, TabUIConstants.INVALID);
		if (val != TabUIConstants.INVALID) {
			contactCheck = val;
		}
		inviterName = prefs.getString(TabUIConstants.PREF_INVITER_NAME, null);
		
		editor.commit();
	}
	
	private void setupDialogs() {
		progressDialog = new ProgressDialog(context);
		progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		progressDialog.setTitle(TabUIConstants.MSG_FINDING);
		progressDialog.setMessage(TabUIConstants.MSG_FRIENDS_AND_FAMILY);
		progressDialog.setCancelable(false);

		messageDialog = new AlertDialog.Builder(context).create();
		messageDialog.setCancelable(false);

		inviterNameDialog = new Dialog(context);
		inviterNameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		inviterNameDialog.setContentView(getResourceId(context, "tabui_entername_dialog", "layout"));
		inviterNameDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
		inviterNameText = (EditText) inviterNameDialog.findViewById(getResourceId(context, "editText1", "id"));
		inviterNameSendBtn = (Button) inviterNameDialog.findViewById(getResourceId(context, "button1", "id"));

		inviterNameText.requestFocus();
		((InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(inviterNameText, InputMethodManager.SHOW_IMPLICIT);
		inviterNameSendBtn.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				inviterName = inviterNameText.getText().toString();
				editor.putString(TabUIConstants.PREF_INVITER_NAME, inviterName);
				editor.commit();
				sendSMS();
				inviterNameDialog.dismiss();
			}
		});
		
		invitationSentDialog = new Dialog(context);
		invitationSentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		invitationSentDialog.setContentView(getResourceId(context, "tabui_invitationsent_dialog", "layout"));
		invitationDialogBtn = (Button) invitationSentDialog.findViewById(getResourceId(context, "dismissBtn", "id"));	
		invitationDialogBtn.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
				invitationSentDialog.dismiss();
				new Thread() {
					public void run() {
						queryLeads();
					};
				}.start();
			}
		});
	}
	
	private void setupTabs() {
		try {
			tabHost = (TabHost) slidingDrawer.findViewById(getResourceId(context, "th_set_menu_tabhost", "id"));
			tabHost.setup();

			leadsTab = tabHost.newTabSpec(LEADS_TAB_ID);
			leadsTab.setIndicator(TabUIConstants.EMPTY_STRING);
			leadsTab.setContent(new TabHost.TabContentFactory() {
				public View createTabContent(String tag) {
					int id = TabUIUtils.getResourceId(context, "ListView01", "id");
					leadsView = (ListView) tabHost.findViewById(id);
					id = TabUIUtils.getResourceId(context, "tabui_contactview_footer", "layout");
					View footer = layoutInflater.inflate(id, null);

					footer.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							context.startActivity(getContactsStartIntent());
						}
					});
					id = TabUIUtils.getResourceId(context, "imageView1", "id");
					ImageView im = (ImageView) footer.findViewById(id);
					im.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							try {
								context.startActivity(getContactsStartIntent());
							} catch (Exception e) {
								Log.e(TABUI_LOG, e.getMessage());
							}
						}
					});
					id = TabUIUtils.getResourceId(context, "imageView2", "id");
					im = (ImageView) footer.findViewById(id);
					im.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							context.startActivity(getContactsStartIntent());
						}
					});
					id = TabUIUtils.getResourceId(context, "textView1", "id");
					TextView tv = (TextView) footer.findViewById(id);
					tv.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							context.startActivity(getContactsStartIntent());
						}
					});
					leadsView.addFooterView(footer);
					leadsView.setAdapter(leadsAdapter);

					id = TabUIUtils.getResourceId(context, "tabrl1", "id");
					headerLayout = (RelativeLayout) tabHost.findViewById(id);
					id = TabUIUtils.getResourceId(context, "tabui_header_suggested_p", "drawable");
					headerLayout.setBackgroundResource(id);

					id = TabUIUtils.getResourceId(context, "button1", "id");
					inviteBtn = (Button) headerLayout.findViewById(id);
					inviteBtn.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							if (!discoverWithCheck()) {
								runDiscover();
							}
						}
					});
					id = TabUIUtils.getResourceId(context, "button2", "id");
					sendNplayBtn = (Button) headerLayout.findViewById(id);
					sendNplayBtn.setOnClickListener(new OnClickListener() {
						public void onClick(View v) {
							sendInvitationAction();
						}
					});
					return leadsView;
				}
			});
			tabHost.addTab(leadsTab);
			
			friendsTab = tabHost.newTabSpec(FRIENDS_TAB_ID);
			friendsTab.setContent(new TabHost.TabContentFactory() {
				public View createTabContent(String tag) {
					int id = TabUIUtils.getResourceId(context, "ListView02", "id");
					friendsView = (ListView) slidingDrawer.findViewById(id);
					return friendsView;
				}
			});
			friendsTab.setIndicator(TabUIConstants.EMPTY_STRING);
			tabHost.addTab(friendsTab);
			
			tabHost.getTabWidget().getChildAt(0).setBackgroundResource(getResourceId(context, "tabui_invite_tab_selector_p", "drawable"));
		} catch (Exception e) {
			Log.e(TABUI_LOG, e.getMessage());
		}
	}
	
	private void setupScreenOrientationUIValues() {
		int orientation = context.getResources().getConfiguration().orientation;
		Display display = context.getWindowManager().getDefaultDisplay();
		int height = display.getHeight();
		float h = 480;
		float fh = height / h;

		DisplayMetrics displayMetrics = new DisplayMetrics();
		display.getMetrics(displayMetrics);
		int density = displayMetrics.densityDpi;
		
		if (orientation == Configuration.ORIENTATION_PORTRAIT) {
			if (density == DisplayMetrics.DENSITY_HIGH) {
				tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = (int) (43 * fh);
				tabHost.getTabWidget().getChildAt(1).getLayoutParams().height = (int) (43 * fh);
			} else if (density == DisplayMetrics.DENSITY_MEDIUM) {
				tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = (int) (50 * fh);
				tabHost.getTabWidget().getChildAt(1).getLayoutParams().height = (int) (50 * fh);
			}
		} else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
			fh += 0.25;
			if (density == DisplayMetrics.DENSITY_HIGH) {
				fh += 0.25;
				tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = (int) (43 * fh);
				tabHost.getTabWidget().getChildAt(1).getLayoutParams().height = (int) (43 * fh);
			} else if (density == DisplayMetrics.DENSITY_MEDIUM) {
				tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = (int) (50 * fh);
				tabHost.getTabWidget().getChildAt(1).getLayoutParams().height = (int) (50 * fh);
			}
			
			RelativeLayout.LayoutParams layoutParams = (android.widget.RelativeLayout.LayoutParams) slidingDrawer.getLayoutParams();
			layoutParams.width = height;
			slidingDrawer.setLayoutParams(layoutParams);
		}
	}
	
	private void sendInvitationAction(){
		if (tabHost.getCurrentTab() == 0) {
			new Thread() {
				public void run() {
					ArrayList<String> invitees = new ArrayList<String>();
					
					for (int i = 0; i < leadsAdapter.getRecommendedContacts().size(); i++){
						if (leadsAdapter.getRecommendedContacts().get(i).isSelected()) {
							invitees.add(leadsAdapter.getLeads().get(i).getPhone());
						}
					}
					
					if (invitees.size() > 0) {
						if (AgeUtils.isOnline(context)) {
							try {
								progressDialog.setTitle(TabUIConstants.MSG_REFRESHING);
								progressDialog.setMessage(TabUIConstants.MSG_PLEASE_WAIT);
								handler.sendMessage(handler.obtainMessage(SHOW_PREPARE_DIALOG));
								dialogActionValue = DIALOG_ACTION_DEFAULT;
								
								if (smsType == SmsType.NATIVE) {
									int id = TabUIUtils.getResourceId(context,"app_name","string");
									Discoverer.getInstance().newReferral(invitees, false, context.getString(id));

									dialogActionValue = DIALOG_ACTION_QUERYLEADS;
									handler.sendMessage(handler.obtainMessage(SHOW_SMS_CONFIRMATION));
								} else {
									inviteSelectedPhoneNumbers = invitees;
									handler.sendMessage(handler.obtainMessage(SHOW_NAME_DIALOG_SMS));
								}
							} catch (AgeException e) {
								dialogActionValue = DIALOG_ACTION_DEFAULT;
								showMessage(e);
							} finally {
								handler.sendMessage(handler.obtainMessage(HIDE_PREPARE_DIALOG));
							}
						} else {
							dialogActionValue = DIALOG_ACTION_DEFAULT;
							Message msg = handler.obtainMessage();
							msg.what = SHOW_MESSAGE_DIALOG;
							msg.obj = new String[] { TabUIConstants.EMPTY_STRING, TabUIConstants.MSG_UNABLE_TO_SEND, TabUIConstants.MSG_OK };
							handler.sendMessage(msg);
							handler.sendMessage(handler.obtainMessage(HIDE_PREPARE_DIALOG));
						}
					} else {
						dialogActionValue = DIALOG_ACTION_DEFAULT;
						Message msg = handler.obtainMessage();
						msg.what = SHOW_MESSAGE_DIALOG;
						msg.obj = new String[] { TabUIConstants.EMPTY_STRING, TabUIConstants.MSG_SELECT_CONTACTS, TabUIConstants.MSG_DISMISS };
						handler.sendMessage(msg);
						handler.sendMessage(handler.obtainMessage(HIDE_PREPARE_DIALOG));
					}
				};
			}.start();
		} else {
			ArrayList<String> selectedFriends = new ArrayList<String>();

			for (int i = 0; i < friendsAdapter.getFriends().size(); i++){
				if (friendsAdapter.getFriends().get(i).isInvited()) {
					selectedFriends.add(friendsAdapter.getFriends().get(i).getPhoneNumber());
				}
			}
			
			if (action == null) {
				dialogActionValue = DIALOG_ACTION_DEFAULT;
				Message msg = handler.obtainMessage();
				msg.what = SHOW_MESSAGE_DIALOG;
				msg.obj = new String[] { TabUIConstants.TITLE_CALL_TO_ACTION, TabUIConstants.MSG_CALL_TO_ACTION, TabUIConstants.MSG_DISMISS };
				handler.sendMessage(msg);
			} else {
				action.play(selectedFriends);
			}
		}
	}

	private Intent getContactsStartIntent() {
		return new Intent(new Intent(context, PhoneBookContactsActivity.class));
	}

	private boolean discoverWithCheck() {
		boolean value = false;
		
		try {
			if (!AgeUtils.isOnline(context)) {
				dialogActionValue = DIALOG_ACTION_DEFAULT;
				if (dialogCheck)
					showMessageDialog(TabUIConstants.EMPTY_STRING, TabUIConstants.MSG_INTERNET_REQUIRED, TabUIConstants.MSG_OK, null);
				dialogCheck = false;
				value = true;
			} else {
				if (contactCheck != TabUIConstants.CHK_VALUE_TRUE && contactCheck == TabUIConstants.INVALID) {
					dialogActionValue = DIALOG_ACTION_DISCOVER_YES;
					showMessageDialog(TabUIConstants.TITLE_PHONE_BOOK_PERMISSION, phoneBookPermissionMessage, TabUIConstants.MSG_OK, TabUIConstants.MSG_DONT_ALLOW);
					value = true;
				}
			}
		} catch (Exception e) {
			Log.e(TABUI_LOG, e.getMessage());
		}
		
		return value;
	}

	private void queryLeads() {
		progressDialog.setTitle(TabUIConstants.MSG_FINDING);
		progressDialog.setMessage(TabUIConstants.MSG_FRIENDS_AND_FAMILY);
		handler.sendMessage(handler.obtainMessage(SHOW_PREPARE_DIALOG));
		
		try {
			List<Lead> updatedLeads = Discoverer.getInstance().queryLeads();

			leads.clear();
			leadsAdapter.clear();
			editor.putInt(TabUIConstants.PREF_LEAD_SIZE, updatedLeads.size());
			Lead lead;
			
			for (int i = 0; i < updatedLeads.size(); i++) {
				lead = updatedLeads.get(i);
				leads.add(lead);
				editor.putString(TabUIConstants.PREF_PHONE_VALUE_PREFIX + i, lead.getPhone());
				editor.putString(TabUIConstants.PREF_OS_VALUE_PREFIX + i, lead.getOsType());
			}
			
			leadsAdapter.populateLeads();
			contactCheck = TabUIConstants.CHK_VALUE_TRUE;
			editor.putInt(TabUIConstants.PREF_CONTACT_CHECK, contactCheck);
			editor.commit();
			handler.sendMessage(handler.obtainMessage(SHOW_LIST_LEADS));
		} catch (AgeException e) {
			dialogActionValue = DIALOG_ACTION_DEFAULT;
			showMessage(e);
		} finally {
			handler.sendMessage(handler.obtainMessage(HIDE_PREPARE_DIALOG));
		}
	}

	private void runDiscover() {
		new Thread() {
			public void run() {
				try {
					progressDialog.setTitle(TabUIConstants.MSG_FINDING);
					progressDialog.setMessage(TabUIConstants.MSG_FRIENDS_AND_FAMILY);
					handler.sendMessage(handler.obtainMessage(SHOW_PREPARE_DIALOG));
					Discoverer.getInstance().discover();

					List<Lead> updatedLeads = Discoverer.getInstance().queryLeads();
					leads.clear();
					leadsAdapter.clear();
					editor.putInt(TabUIConstants.PREF_LEAD_SIZE, updatedLeads.size());
					Lead lead;
					for (int i = 0; i < updatedLeads.size(); i++) {
						lead = updatedLeads.get(i);
						leads.add(lead);
						editor.putString(TabUIConstants.PREF_PHONE_VALUE_PREFIX + i, lead.getPhone());
						editor.putString(TabUIConstants.PREF_OS_VALUE_PREFIX + i, lead.getOsType());
					}
					leadsAdapter.populateLeads();
					
					contactCheck = TabUIConstants.CHK_VALUE_TRUE;
					editor.putInt(TabUIConstants.PREF_CONTACT_CHECK, contactCheck);

					List<String> updatedFriends = Discoverer.getInstance().queryInstalls(Direction.FORWARD);
					editor.putInt(TabUIConstants.PREF_FRIEND_SIZE, updatedFriends.size());
					friends.clear();
					for (int i = 0; i < updatedFriends.size(); i++) {
						String num = updatedFriends.get(i);
						String name = AgeUtils.lookupNameByPhone(context, num);
						friends.add(new PhoneBookContact(name, null, num));
						editor.putString(TabUIConstants.PREF_FRIEND_VALUE_PREFIX + i, num);
					}

					editor.commit();
					handler.sendMessage(handler.obtainMessage(SHOW_LIST_LEADS));
				} catch (AgeException e) {
					dialogActionValue = DIALOG_ACTION_DEFAULT;
					handler.sendMessage(handler.obtainMessage(HIDE_PREPARE_DIALOG));
					showMessage(e);
				}
			};
		}.start();
	}
	
	private void dialogAction() {
		switch (dialogActionValue) {
		case DIALOG_ACTION_DISCOVER_YES:
			dialogActionValue = DIALOG_ACTION_DEFAULT;
			runDiscover();
			break;
		case DIALOG_ACTION_DISCOVER_NO:
		case DIALOG_ACTION_DISCOVER_FINISHED:
		case DIALOG_ACTION_OTHER:
			break;
		case DIALOG_ACTION_QUERYLEADS:
			dialogActionValue = DIALOG_ACTION_DEFAULT;
			new Thread() {
				public void run() {
					queryLeads();
				};
			}.start();
			break;
		default:
			break;
		}
	}

	private void showMessage(AgeException e) {
		String body = TabUIConstants.MSG_SERVER_ERROR;

		if (e.getMessage() != null) {
			body += e.getMessage();
		} else {
			body += TabUIConstants.MSG_UNKNOWN_ERROR;
		}

		Message msg = handler.obtainMessage();
		msg.what = SHOW_MESSAGE_DIALOG;
		msg.obj = new String[] { TabUIConstants.TITLE_FINISHED, body, TabUIConstants.MSG_DISMISS };
		handler.sendMessage(msg);
	}
	
	private void showMessageDialog(String title, String message, String buttonText1, String buttonText2) {
		messageDialog = new AlertDialog.Builder(context).create();
		messageDialog.setCancelable(false);
		messageDialog.setMessage(message);
		if(!AgeUtils.isEmptyStr(title)){
			messageDialog.setTitle(title);
		}
		messageDialog.setButton(buttonText1,
			new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialogAction();
				}
			});
		if (buttonText2 != null)
			messageDialog.setButton2(buttonText2,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

					}
				});
		messageDialog.show();
	}
	
}
