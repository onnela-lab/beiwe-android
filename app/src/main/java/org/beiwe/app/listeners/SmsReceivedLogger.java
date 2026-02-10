package org.beiwe.app.listeners;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import org.beiwe.app.storage.EncryptionEngine;
import org.beiwe.app.storage.TextFileManager;

/* Improvement idea: we could also read the messages in the inbox, and possibly
 * get more data from that, such as whether a message was opened. */

/**
 * Listens for Broadcast saying that an SMS message has been received, and records
 * their timestamp to the log.
 * 
 * Note: the timestamp is the time at which the phone received the message.
 * For SMS messages (but not MMS messages), it is also possible to record the
 * time the message was sent; this is recorded in the last column ("time sent")
 * 
 * @author Josh Zagorsky June 2014 */
public class SmsReceivedLogger extends BroadcastReceiver {

	/** onReceive, splits incoming texts into SMS and MMS. */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
			handleIncomingSMS(intent);
		}
		else if (intent.getAction().equals("android.provider.Telephony.WAP_PUSH_RECEIVED")) {
			handleIncomingMMS(intent);
		}
	}

	/** pulls out source phone number from an MMS, writes info to texts log */
	private void handleIncomingMMS(Intent intent) {
		 Bundle bundle = intent.getExtras();

		 if (bundle != null) {
			 // MMS detection code based on http://stackoverflow.com/q/14452808
			 byte[] buffer = bundle.getByteArray("data");
			 String incomingNumber = new String(buffer);
			 int indx = incomingNumber.indexOf("/TYPE");
			 if(indx>0 && (indx-15)>0){
				 int newIndx = indx - 15;
				 incomingNumber = incomingNumber.substring(newIndx, indx);
				 indx = incomingNumber.indexOf("+");
				 if(indx>0){
					 incomingNumber = incomingNumber.substring(indx);
//                     "timestamp,hashed phone number,sent vs received,message length,time sent";
					 String data = "" + System.currentTimeMillis() + TextFileManager.DELIMITER;
					 data += EncryptionEngine.hashPhoneNumber(incomingNumber) + TextFileManager.DELIMITER;
					 data += "received MMS" + TextFileManager.DELIMITER;
//                     TODO: Feature. determine if we can get the length of the text, if it has an attachment.
					Log.i("SMSReceivedLogger(SMS)", "data = " + data);
					 TextFileManager.getTextsLogFile().writeEncrypted(data);
				 }
			 }
		 }
	}
	
	/** pulls out source phone number and length from an SMS, writes info to texts log. */
	@SuppressLint("InlinedApi") @SuppressWarnings("deprecation") //these yell at you in the old/new code paths.
	private void handleIncomingSMS(Intent intent) {
		Bundle bundle = intent.getExtras();
		SmsMessage[] messages = null;
		String messageFrom;
		if (bundle != null) {
			try {
				Object[] pdus = (Object[]) bundle.get("pdus");
				String format = bundle.getString("format");
				messages = new SmsMessage[pdus.length];
				for (int i = 0; i < pdus.length; i++) {
					if ( android.os.Build.VERSION.SDK_INT < 23 ) { messages[i] = SmsMessage.createFromPdu( (byte[]) pdus[i] ); }
					else { messages[i] = SmsMessage.createFromPdu( (byte[]) pdus[i], format); }
					messageFrom = messages[i].getOriginatingAddress();
					String messageBody = messages[i].getMessageBody();
					long timestamp = messages[i].getTimestampMillis();
//                  "timestamp,hashed phone number,sent vs received,message length,time sent";
					String data = "" + System.currentTimeMillis() + TextFileManager.DELIMITER;
					data += EncryptionEngine.hashPhoneNumber(messageFrom) + TextFileManager.DELIMITER;
					data += "received SMS" + TextFileManager.DELIMITER;
					data += messageBody.length() + TextFileManager.DELIMITER;
					data += timestamp;

					Log.i("SMSReceivedLogger (MMS)", "data = " + data);
					TextFileManager.getTextsLogFile().writeEncrypted(data);
				}
			}
			catch (Exception e) { Log.e("SMSReceivedLogger", "SMS_RECEIVED Caught exception: " + e.getCause() + ", " + e.getMessage()); }

		}
	}
}
