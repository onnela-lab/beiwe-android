package org.beiwe.app.listeners;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/**
 * Records outgoing SMS messages when they're sent
 * 
 * The listeners for incoming and outgoing SMS messages are in different
 * classes because they need to use different methods.  You CAN register
 * a BroadcastReceiver to listen for INCOMING texts, but if you register a
 * BroadcastReceiver to listen for OUTGOING texts, it WON'T pick them up if
 * they're sent with a typical Android default text-messaging app.
 * 
 * Because of that, this class listens for outgoing texts by registering a
 * ContentObserver that watches the file "content://sms" to see when it's
 * changed.
 * 
 * @author Josh Zagorsky June 2014
 */
public class SmsSentLogger extends ContentObserver {	

	public static String header = "timestamp, phone_number, message_body\n";

	private Handler handler = null;
	Context appContext = null;
	String idOfLastSmsSent = null;

	
	public SmsSentLogger(Handler theHandler, Context context) {
		super(theHandler);
		theHandler = handler;
		appContext = context;
	}
	
	
	@Override
	public void onChange(boolean selfChange) {
		super.onChange(selfChange);
		
		Uri uriSmsUri = Uri.parse("content://sms");
		
		Cursor cursor = appContext.getContentResolver().query(uriSmsUri, null, null, null, null);
		cursor.moveToNext();
		
		String id = cursor.getString(cursor.getColumnIndex("_id"));
		String protocol = cursor.getString(cursor.getColumnIndex("protocol"));
		String address = cursor.getString(cursor.getColumnIndex("address"));
		String body = cursor.getString(cursor.getColumnIndex("body"));
		String date = cursor.getString(cursor.getColumnIndex("date"));
		
		if ((protocol == null) && (idIsNew(id))) {
			// Message was just sent
			Log.i("SmsSentLogger", "Message just sent.");
			Log.i("SmsSentLogger", "Phone number: " + address);
			Log.i("SmsSentLogger", "Message body: " + body);
			Log.i("SmsSentLogger", "Date/time: " + date);
			
			// TODO: figure out if we need to log more data, like "type"
			// TODO: write messages to the appropriate SMS log file
			// TODO: hash the phone numbers
			// TODO: figure out what MESSAGE_TYPE means and if it's important: http://stackoverflow.com/a/18873822
		}
		
		// TODO: also log incoming SMS messages this way
		// TODO: check how MMS messages are handled
	}
	
	
	/* Returns true if the ID doesn't match idOfLastSmsSent.
	 * This check is because every time an SMS goes out, the ContentObserver on
	 * "content://sms" registers about 5 changes, because the message gets
	 * changed and/or moved around. See: http://stackoverflow.com/a/8242090
	 * This check is to make SmsSentLogger only register one text for every
	 * one text that's sent out. */
	private boolean idIsNew(String id) {
		if ((idOfLastSmsSent == null) || (!idOfLastSmsSent.equals(id))) {
			idOfLastSmsSent = id; // Reset idOfLastSmsSent
			return true;
		}
		return false; // This is not a new/unique ID; return false
	}

}