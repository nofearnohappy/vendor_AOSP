package com.orangelabs.rcs.service.api;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.gsma.joyn.Build;
import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.JoynServiceNotAvailableException;
import org.gsma.joyn.chat.Chat;
import org.gsma.joyn.chat.ChatIntent;
import org.gsma.joyn.chat.ChatListener;
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ChatServiceConfiguration;
import org.gsma.joyn.chat.Geoloc;
import org.gsma.joyn.chat.GroupChat;
import org.gsma.joyn.chat.GroupChatIntent;
import org.gsma.joyn.chat.IChat;
import org.gsma.joyn.chat.IChatListener;
import org.gsma.joyn.chat.IChatService;
import org.gsma.joyn.chat.IGroupChat;
import org.gsma.joyn.chat.IGroupChatListener;
import org.gsma.joyn.chat.IGroupChatSyncingListener;
import org.gsma.joyn.chat.INewChatListener;
import org.gsma.joyn.chat.ISpamReportListener;
import org.gsma.joyn.chat.ConferenceEventData;
import org.gsma.joyn.chat.ConferenceEventData.ConferenceUser;
import org.xml.sax.InputSource;

import android.text.TextUtils;


import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.SubscribeRequest;
import com.orangelabs.rcs.core.ims.service.SubscriptionManager;
import com.orangelabs.rcs.core.ims.service.im.OriginatingSpamSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.ContributionIdGenerator;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OneOneChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.event.ConferenceInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.event.ConferenceInfoParser;
import com.orangelabs.rcs.core.ims.service.im.chat.event.GroupListDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.event.GroupListParser;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.core.ims.service.im.chat.event.GroupListDocument.BasicGroupInfo;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.core.ims.network.registration.RegistrationInfo;
import com.orangelabs.rcs.core.ims.network.registration.RegistrationInfoParser;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chat service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatServiceImpl extends IChatService.Stub {
	/**
	 * List of service event listeners
	 */
	private RemoteCallbackList<IJoynServiceRegistrationListener> serviceListeners = new RemoteCallbackList<IJoynServiceRegistrationListener>();

	/**
	 * List of chat sessions
	 */
	private static Hashtable<String, IChat> chatSessions = new Hashtable<String, IChat>();  

	/**
	 * List of chat sessions
	 */
    private static Hashtable<String, IChat> publicChatSessions = new Hashtable<String, IChat>();  

	/**
	 * List of chat sessions
	 */
	private static Hashtable<String, IChat> storeForwardChatSessions = new Hashtable<String, IChat>();  

	/**
	 * List of group chat sessions
	 */
	private static Hashtable<String, IGroupChat> groupChatSessions = new Hashtable<String, IGroupChat>();  

	/**
	 * List of file chat invitation listeners
	 */
	private RemoteCallbackList<INewChatListener> listeners = new RemoteCallbackList<INewChatListener>();

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(ChatServiceImpl.class.getName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * List of spam sessions
	 */
	private static ArrayList<OriginatingSpamSession> spamSessions = new ArrayList<OriginatingSpamSession>(); 

	/**
	 * Constructor
	 */
	public ChatServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Chat service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear list of sessions
		chatSessions.clear();
		storeForwardChatSessions.clear();
		groupChatSessions.clear();
		publicChatSessions.clear();
		
		if (logger.isActivated()) {
			logger.info("Chat service API is closed");
		}
	}
	
    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
	 * @return Returns true if registered else returns false
     */
    public boolean isServiceRegistered() {
    	return ServerApiUtils.isImsConnected();
    }

	/**
	 * Registers a listener on service registration events
	 * 
	 * @param listener Service registration listener
	 */
	public void addServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add a service listener");
			}

			serviceListeners.register(listener);
		}
	}
	
	/**
	 * Unregisters a listener on service registration events
	 * 
	 * @param listener Service registration listener
	 */
	public void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Remove a service listener");
			}
			
			serviceListeners.unregister(listener);
    	}	
	}    
    
    /**
     * Receive registration event
     * 
     * @param state Registration state
     */
    public void notifyRegistrationEvent(boolean state) {
    	// Notify listeners
    	synchronized(lock) {
			final int N = serviceListeners.beginBroadcast();
			if (logger.isActivated()) {
	            logger.info("notifyRegistrationEvent N: " + N);
	        }
	        for (int i=0; i < N; i++) {
	            try {
	            	if (state) {
	            		serviceListeners.getBroadcastItem(i).onServiceRegistered();
	            	} else {
	            		serviceListeners.getBroadcastItem(i).onServiceUnregistered();
	            	}
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        serviceListeners.finishBroadcast();
	    }    	    	
    }
    
    /**
	 * Receive a new chat invitation
	 * 
	 * @param session Chat session
	 */
    public void receiveOneOneStandAloneChatInvitation(OneOneChatSession session) {
        if (logger.isActivated()) {
            logger.info("Receive stand alone chat invitation from " + session.getRemoteContact() + 
            		"  Display name: " + session.getRemoteDisplayName());
        }
        
        if (logger.isActivated()) {
            logger.info("Receive stand alone chat invitation Conversation: " 
            		+ session.getConversationID());
        }

		session.setLargeMessageMode(true);

        // Extract number from contact 
        String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

       //The message itself will be added in database. No need to add system message in DB.
        if(RcsSettings.getInstance().isCPMSupported()) {
        	if(!(RichMessagingHistory.getInstance().getCoversationID(session.getRemoteContact(), 1).
        			equals(session.getConversationID()))){
        		if(session.getConversationID() != null) {
	        		if (logger.isActivated()) {
	        	       logger.info("Receive stand alone chat invitation OldId: "  + RichMessagingHistory.
	        	    		   getInstance().getCoversationID(session.getRemoteContact(),1) + "NewId: " +
	        	    		   session.getConversationID());
	        	    }
	                RichMessagingHistory.getInstance().UpdateCoversationID(session.getRemoteContact(),
	                		session.getConversationID(), 1);
        		} else {
        			if (logger.isActivated()) {
 	        	       logger.info("Receive stand alone chat invitation Conversation Id is null");
 	        	    }
        		}
        	} else {
        		// Create a text message
		        InstantMessage conversationMsg = ChatUtils.createTextMessage(session.getRemoteContact(), "system",
		                Core.getInstance().getImService().getImdnManager().isImdnActivated());
				 RichMessagingHistory.getInstance().addChatSystemMessage(conversationMsg, ChatLog.Message.Direction.INCOMING);
				 RichMessagingHistory.getInstance().UpdateCoversationID(session.getRemoteContact(),session.getConversationID(),1);
        	}
        }
        
        // Add session in the list
        ChatImpl sessionApi = new ChatImpl(number, session);
        ChatServiceImpl.addChatSession(number, sessionApi);

        // Broadcast intent related to the received invitation
        Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
        intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        intent.putExtra(ChatIntent.EXTRA_CONTACT, number);
        intent.putExtra(ChatIntent.EXTRA_CLOUD_MESSAGE, session.isCloudMessage());
        intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
        AndroidFactory.getApplicationContext().sendBroadcast(intent);
        
        // Notify chat invitation listeners
        synchronized(lock) {
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
	            logger.info("receiveOneOneStandAloneChatInvitation N: " + N);
	        }
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onNewSingleChat(number, null);
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }               

    }
    
    /**
	 * Receive a new chat invitation
	 * 
	 * @param session Chat session
	 */
    public void receiveOneOneChatInvitation(OneOneChatSession session) {
		if (logger.isActivated()) {
			logger.info("Receive chat invitation from " + session.getRemoteContact() + "  Display name: " + session.getRemoteDisplayName());
		}

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich messaging history
		// Nothing done in database
		// Update rich messaging history with o2o chat message
		
		InstantMessage msg = session.getFirstMessage();
		msg.setDisplayName(session.getRemoteDisplayName());
		if (logger.isActivated()) {
			logger.info("Receive chat invitation from first message display: " + msg.getDisplayName());
		}
		
		RichMessagingHistory.getInstance().addChatSystemMessage(msg, ChatLog.Message.Direction.INCOMING);
		 if(RcsSettings.getInstance().isCPMSupported()) {
	        	if(!(RichMessagingHistory.getInstance().getCoversationID(session.getRemoteContact(), 1).equals(session.getConversationID()))){
	        		if(session.getConversationID() != null) {
		        		if (logger.isActivated()) {
		        	       logger.info("Receive O2O chat invitation OldId: "  + RichMessagingHistory.getInstance().getCoversationID(session.getRemoteContact(),1) + " NewId: " + session.getConversationID());
		        	    }
		                RichMessagingHistory.getInstance().UpdateCoversationID(session.getRemoteContact(),session.getConversationID(), 1);
	        		} else {
	        			if (logger.isActivated()) {
	 	        	       logger.info("Receive O2O chat invitation Conversation Id is null");
	 	        	    }
	        		}
	        	} else {
	        		// Create a text message
			        InstantMessage conversationMsg = ChatUtils.createTextMessage(session.getRemoteContact(), "system",
			                Core.getInstance().getImService().getImdnManager().isImdnActivated());
					 RichMessagingHistory.getInstance().addChatSystemMessage(conversationMsg, ChatLog.Message.Direction.INCOMING);
					 RichMessagingHistory.getInstance().UpdateCoversationID(session.getRemoteContact(),session.getConversationID(),1);
	        	}
	        }		

		// Add session in the list
		ChatImpl sessionApi = new ChatImpl(number, session);
		ChatServiceImpl.addChatSession(number, sessionApi);

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(ChatIntent.ACTION_NEW_CHAT);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(ChatIntent.EXTRA_CONTACT, number);
    	intent.putExtra(ChatIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	ChatMessage msgApi;
    	if (msg instanceof GeolocMessage) {
    		GeolocMessage geoloc = (GeolocMessage)msg;
        	Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
        			geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
        			geoloc.getGeoloc().getExpiration());
        	msgApi = new org.gsma.joyn.chat.GeolocMessage(geoloc.getMessageId(),
        			PhoneUtils.extractNumberFromUri(geoloc.getRemote()),
        			geolocApi, geoloc.getDate(), geoloc.isImdnDisplayedRequested());
	    	intent.putExtra(ChatIntent.EXTRA_MESSAGE, msgApi);
    	} else {
        	msgApi = new ChatMessage(msg.getMessageId(),
        			PhoneUtils.extractNumberFromUri(msg.getRemote()),
        			msg.getTextMessage(), msg.getServerDate(),
        			msg.isImdnDisplayedRequested(),msg.getDisplayName());
        	intent.putExtra(ChatIntent.EXTRA_MESSAGE, msgApi);	
    	}
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	
    	// Notify chat invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewSingleChat(number, msgApi);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }    	    	
    }  
    
    
    public IChat openMultiChat(List<String> participants, IChatListener listener)throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Open a 1-* chat session");
		}

		
		// Test IMS connection
		ServerApiUtils.testIms();
		
		try {
			// Extract contactlist from participants List
			
			String participantList = "";
	        participantList = TextUtils.join(",",participants);
			

			// Check if there is an existing chat or not
			ChatImpl sessionApi = (ChatImpl)ChatServiceImpl.getChatSession(participantList);
			
			if (sessionApi != null && sessionApi.getCoreSession() != null && !sessionApi.getCoreSession().isStoreAndForward()) {
				if (logger.isActivated()) {
					logger.debug("Chat session already exist for " + participantList);
				}
				
				// Add session listener
				sessionApi.addEventListener(listener);

				// Check core session state
				final OneOneChatSession coreSession = sessionApi.getCoreSession();
				if (coreSession != null) {
					if (logger.isActivated()) {
						logger.debug("Core chat session already exist: " + coreSession.getSessionID());
					}

					if (coreSession.getDialogPath().isSessionTerminated() ||
							coreSession.getDialogPath().isSessionCancelled()) {
						if (logger.isActivated()) {
							logger.debug("Core chat session is terminated: reset it");
						}
						
						// Session has expired, remove it
						sessionApi.resetCoreSession();
					} else
					if (!coreSession.getDialogPath().isSessionEstablished()) {
						if (logger.isActivated()) {
							logger.debug("Core chat session is pending: auto accept it");
						}
						
						// Auto accept the pending session
				        Thread t = new Thread() {
				    		public void run() {
								coreSession.acceptSession();
				    		}
				    	};
				    	t.start();
					} else {
						if (logger.isActivated()) {
							logger.debug("Core chat session is already established");
						}
					}
				}
			} else {
				if (logger.isActivated()) {
					logger.debug("Create a new chat session with " + participantList);
				}

				// Add session listener
				sessionApi = new ChatImpl(participantList);
				sessionApi.addEventListener(listener);
	
				// Add session in the list
				ChatServiceImpl.addChatSession(participantList, sessionApi);
			}
		
			return sessionApi;
		
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Open a single chat with a given contact and returns a Chat instance.
     * The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @param contact Contact
     * @param listener Chat event listener
     * @return Chat
	 * @throws ServerApiException
     */
    public IChat openSingleChat(String contact, IChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Open a 1-1 chat session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();
		
		try {
			// Extract number from contact 
			String number = PhoneUtils.extractNumberFromUri(contact);

			// Check if there is an existing chat or not
			ChatImpl sessionApi = (ChatImpl)ChatServiceImpl.getChatSession(number);
			//if(sessionApi != null){
			//	OneOneChatSession tempSession = sessionApi.getCoreSession();
			//}
			
			if (sessionApi != null && sessionApi.getCoreSession() != null && !sessionApi.getCoreSession().isStoreAndForward()) {
			//if (sessionApi != null ){
				if (logger.isActivated()) {
					logger.debug("Chat session already exist for " + number);
				}
				
				// Add session listener
				sessionApi.addEventListener(listener);

				// Check core session state
				final OneOneChatSession coreSession = sessionApi.getCoreSession();
				if (coreSession != null) {
					if (logger.isActivated()) {
						logger.debug("Core chat session already exist: " + coreSession.getSessionID());
					}

					if (coreSession.getDialogPath().isSessionTerminated() ||
							coreSession.getDialogPath().isSessionCancelled()) {
						if (logger.isActivated()) {
							logger.debug("Core chat session is terminated: reset it");
						}
						
						// Session has expired, remove it
						sessionApi.resetCoreSession();
					} else
					if (!coreSession.getDialogPath().isSessionEstablished()) {
						if (logger.isActivated()) {
							logger.debug("Core chat session is pending: auto accept it");
						}
						
						// Auto accept the pending session
				        Thread t = new Thread() {
				    		public void run() {
								coreSession.acceptSession();
				    		}
				    	};
				    	t.start();
					} else {
						if (logger.isActivated()) {
							logger.debug("Core chat session is already established");
						}
					}
				}
			} else {
				if (logger.isActivated()) {
					logger.debug("Create a new chat session with " + number);
				}

				// Add session listener
				sessionApi = new ChatImpl(number);
				sessionApi.addEventListener(listener);
	
				// Add session in the list
				ChatServiceImpl.addChatSession(number, sessionApi);
			}
		
			return sessionApi;
		
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }    

    /**
     * Init a Publicaccount chat with a given contact and returns a Chat instance.
     * The parameter contact supports the following formats: MSISDN in national
     * or international format, SIP address, SIP-URI or Tel-URI.
     * 
     * @param contact Contact
     * @param listener Chat event listener
     * @return Chat
     * @throws ServerApiException
     */
    public IChat initPublicAccountChat(String contact, IChatListener listener) throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("PAM Open a 1-1 public chat  with " + contact);
        }

        // Test IMS connection
        ServerApiUtils.testIms();
        
        try {
            // Extract number from contact 
            //String number = PhoneUtils.extractNumberFromUri(contact);
        	String number = contact;

            // Check if there is an existing chat or not
            ChatImpl sessionApi = new ChatImpl(number);                 
            // Add session listener
            sessionApi.addEventListener(listener);

            // Add session in the list
            ChatServiceImpl.addPublicChatSession(number, sessionApi);
        
            return sessionApi;
        
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("PAM Unexpected error", e);
            }
            throw new ServerApiException(e.getMessage());
        }
    }
    
    /**
     * Receive message delivery status
     * 
	 * @param contact Contact
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void receiveMessageDeliveryStatus(String contact, String msgId, String status) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("LMM Receive message delivery status for message " + msgId + ", status " + status);
			}

			String number = PhoneUtils.extractNumberFromUri(contact);

			if (logger.isActivated()) {
				logger.info("LMM receiveMessageDeliveryStatus " + contact + ", number " + number);
			}
	
	  		// Notify message delivery listeners
			ChatImpl chat = (ChatImpl)ChatServiceImpl.getChatSession(number);
			if (chat != null) {
				if (logger.isActivated()) {
					logger.info("LMM chat is not null");
				}
            	chat.handleMessageDeliveryStatus(msgId, status);
	    	}
			else{
				if (logger.isActivated()) {
					logger.info("LMM chat is null");
				}
				// Update rich messaging history
	            RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
	            
				Intent intent = new Intent(ChatIntent.ACTION_DELIVERY_STATUS);
		    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
		    	intent.putExtra(ChatIntent.EXTRA_CONTACT, number);
				intent.putExtra("msgId", msgId);
				intent.putExtra("status", status);
				AndroidFactory.getApplicationContext().sendBroadcast(intent);
			}
    	}
    }
    
	/**
	 * Add a chat session in the list
	 * 
	 * @param contact Contact
	 * @param session Chat session
	 */
	public static void addChatSession(String contact, ChatImpl session) {
		if (logger.isActivated()) {
			logger.debug("LMM a chat session in the list (size=" + chatSessions.size() + ") for " + contact);
		}

		if((session.getCoreSession() != null) && session.getCoreSession().isStoreAndForward()){
			if (logger.isActivated()) {
				logger.debug("LMM ChatSession s&f");
			}
			if (logger.isActivated()) {
				logger.debug("AddRemove add storeForwardChatSessions " + session.getCoreSession().getSessionID());
			}
			storeForwardChatSessions.put(contact, session);
		}
		else{
			if (logger.isActivated()) {
				//logger.debug("LMM AddRemove add chatSessions " + session.getCoreSession().getSessionID());
			}
			chatSessions.put(contact, session);
		}
	}

	/**
     * Add a chat session in the list
     * 
     * @param contact Contact
     * @param session Chat session
     */
    public static void addPublicChatSession(String contact, ChatImpl session) {
        if (logger.isActivated()) {
            logger.debug("PAM Add a chat session in the list (size=" + chatSessions.size() + ") for " + contact);
        }
        if (logger.isActivated()) {
//			logger.debug("PAM AddRemove addPublicChatSession " + session.getCoreSession().getSessionID());
		}
        publicChatSessions.put(contact, session);
    }

	/**
	 * Get a chat session from the list for a given contact
	 * 
	 * @param contact Contact
	 * @param GroupChat session
	 */
	protected static IChat getStoreChatSession(String contact) {
		if (logger.isActivated()) {
			logger.debug("LMM Get ChatSession s&f " + contact);
		}
		
		return storeForwardChatSessions.get(contact);
	}

	/**
	 * Remove a chat session from the list
	 * 
	 * @param contact Contact
	 */
	protected static void removeStoreChatSession(String contact) {
		if (logger.isActivated()) {
			logger.debug("LMM Remove removeStoreChatSession (size=" + storeForwardChatSessions.size() + ") for " + contact);
		}		 
		
		if ((storeForwardChatSessions != null) && (contact != null)) {
			storeForwardChatSessions.remove(contact);
		}
	}

	/**
	 * Get a chat session from the list for a given contact
	 * 
	 * @param contact Contact
	 * @param GroupChat session
	 */
	protected static IChat getChatSession(String contact) {
		if (logger.isActivated()) {
			logger.debug("LMM Get a chat session for " + contact);
		}
		
		return chatSessions.get(contact);
	}

	/**
	 * Remove a chat session from the list
	 * 
	 * @param contact Contact
	 */
	protected static void removeChatSession(String contact) {
		if (logger.isActivated()) {
			logger.debug("LMM Remove a chat session from the list (size=" + chatSessions.size() + ") for " + contact);
		}
		
		if ((chatSessions != null) && (contact != null)) {
			chatSessions.remove(contact);
		}
	}
	
    /**
     * Get a publicAccount chat session from the list for a given contact
     * 
     * @param contact Contact
     * @param PublicAccountChat session
     */
    protected static IChat getPublicAccountChatSession(String contact) {
        if (logger.isActivated()) {
            logger.debug("PAM Get a public account chat session for " + contact);
        }
        
        return publicChatSessions.get(contact);
    }

    /**
     * Remove a chat session from the list
     * 
     * @param contact Contact
     */
    protected static void removePublicAccountChatSession(String contact) {
        if (logger.isActivated()) {
            logger.debug("PAM Remove a public account chat session from the list (size=" + publicChatSessions.size() + ") for " + contact);
        }
        
        if ((publicChatSessions != null) && (contact != null)) {
            publicChatSessions.remove(contact);
        }
    }
	
    /**
     * Returns the list of single chats in progress
     * 
     * @return List of chats
     * @throws ServerApiException
     */
    public List<IBinder> getChats() throws ServerApiException {
    	int size = chatSessions.size() + storeForwardChatSessions.size();
		if (logger.isActivated()) {
			logger.info("Get chat sessions sze: " + size);
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(size);
			for (Enumeration<IChat> e = chatSessions.elements() ; e.hasMoreElements() ;) {
				IChat sessionApi = (IChat)e.nextElement() ;
				result.add(sessionApi.asBinder());
			}
			//ArrayList<IBinder> result = new ArrayList<IBinder>(storeForwardChatSessions.size());
			for (Enumeration<IChat> e = storeForwardChatSessions.elements() ; e.hasMoreElements() ;) {
				IChat sessionApi = (IChat)e.nextElement() ;
				result.add(sessionApi.asBinder());
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Returns a chat in progress from its unique ID
     * 
     * @param contact Contact
     * @return Chat or null if not found
     * @throws ServerApiException
     */
    public IChat getChat(String contact) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get chat session with " + contact);
		}

		// Return a session instance
		return chatSessions.get(contact);
    }
    
    /**
     * Returns a public account chat in progress from its unique ID
     * 
     * @param contact Contact
     * @return Chat or null if not found
     * @throws ServerApiException
     */
    public IChat getPublicAccountChat(String contact) throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("PAM getPublicAccountChat with " + contact);
        }

        // Return a session instance
        return publicChatSessions.get(contact);
    }
    
    /**
	 * Receive a new group chat invitation
	 * 
	 * @param session Chat session
	 */
    public void receiveGroupChatInvitation(GroupChatSession session) {
		if (logger.isActivated()) {
			logger.info("Receive group chat invitation from " + session.getRemoteContact() + "  Display name: " + session.getGroupRemoteDisplayName() + "new name: " + SipUtils.getDisplayNameFromUri(session.getRemoteContact()));
		}

		// Extract number from contact 
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		Boolean isChatExist = false;
		isChatExist = RichMessagingHistory.getInstance().isGroupChatExists(session.getContributionID());

		// Update rich messaging history if not present , to stop same chatId to come , TODO add update function and call in else
		if(!isChatExist){
		RichMessagingHistory.getInstance().addGroupChat(session.getContributionID(),session.getSubject(), session.getParticipants().getList(), GroupChat.State.INVITED, GroupChat.Direction.INCOMING);
		}
		int blocked = RichMessagingHistory.getInstance().getGroupBlockedStatus(session.getContributionID());
        if(blocked == 1){
           session.setMessageBlocked(true);
        } else if(blocked == 0){
           session.setMessageBlocked(false);
        }
		//session.setChairman(session.getRemoteContact());//chairman already set in session
		//RichMessagingHistory.getInstance().updateGroupChairman(session.getContributionID(), session.getChairman());
		if(RcsSettings.getInstance().isCPMSupported()) {
			if (logger.isActivated()) {
				logger.info("receiveGroupChatInvitation conversationId: " + session.getConversationID() + " contributionId: " + session.getContributionID());
			}
		    RichMessagingHistory.getInstance().UpdateCoversationID(session.getContributionID(),session.getConversationID(), 2);
		}
		
		// Add session in the list
		GroupChatImpl sessionApi = new GroupChatImpl(session);
		ChatServiceImpl.addGroupChatSession(sessionApi);		

		if (logger.isActivated()) {
			logger.info("receiveGroupChatInvitation ischatexists: " + isChatExist);
		}

		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(GroupChatIntent.ACTION_NEW_INVITATION);
    	intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    	intent.putExtra(GroupChatIntent.EXTRA_CONTACT, number);
    	intent.putExtra(GroupChatIntent.EXTRA_DISPLAY_NAME, session.getGroupRemoteDisplayName());
    	intent.putExtra(GroupChatIntent.EXTRA_CHAT_ID, sessionApi.getChatId());
    	intent.putExtra(GroupChatIntent.EXTRA_SUBJECT, sessionApi.getSubject());
    	intent.putExtra("autoAccept", RcsSettings.getInstance().isGroupChatAutoAccepted());
		intent.putExtra("isGroupChatExist", isChatExist);
		intent.putExtra(GroupChatIntent.EXTRA_SESSION_IDENTITY, session.getImSessionIdentity());
		 /**
         * M: managing extra local chat participants that are 
         * not present in the invitation for sending them invite request.@{
         */
        String participants = "";
        List<String> ListParticipant = session.getParticipants().getList();
        for(String currentPartc : ListParticipant){
         participants += currentPartc + ";";	
        }
        /**
    	 * @}
    	 */      
        intent.putExtra("participantList", participants);
		
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	
    	// Notify chat invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
			if (logger.isActivated()) {
				logger.info("receiveGroupChatInvitation N: " + N);
			}
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewGroupChat(sessionApi.getChatId());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }    	
    }

	/**
	 * Extend a 1-1 chat session
	 * 
     * @param groupSession Group chat session
     * @param oneoneSession 1-1 chat session
	 */
    public void extendOneOneChatSession(GroupChatSession groupSession, OneOneChatSession oneoneSession) {
		if (logger.isActivated()) {
			logger.info("extendOneOneChatSession ReplaceId: " + groupSession.getSessionID());
		}

		// Add session in the list
		GroupChatImpl sessionApi = new GroupChatImpl(groupSession);
		ChatServiceImpl.addGroupChatSession(sessionApi);
		
		if (logger.isActivated()) {
			logger.info("extendOneOneChatSession ExtraChatId: " + sessionApi.getChatId());
		}
		
		// Broadcast intent related to the received invitation
		Intent intent = new Intent(GroupChatIntent.ACTION_SESSION_REPLACED);
		intent.putExtra("sessionId", groupSession.getSessionID());
		intent.putExtra(GroupChatIntent.EXTRA_CHAT_ID, sessionApi.getChatId());
		AndroidFactory.getApplicationContext().sendBroadcast(intent);
    }
	
	/**
	 * Add a group chat session in the list
	 * 
	 * @param session Chat session
	 */
	protected static void addGroupChatSession(GroupChatImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add a group chat session in the list (size=" + groupChatSessions.size() + ")");
		}
		
		groupChatSessions.put(session.getChatId(), session);
	}

	/**
	 * Remove a group chat session from the list
	 * 
	 * @param chatId Chat ID
	 */
	protected static void removeGroupChatSession(String chatId) {
		if (logger.isActivated()) {
			logger.debug("Remove a group chat session from the list (size=" + groupChatSessions.size() + ")");
		}
		
		groupChatSessions.remove(chatId);
	}
	
	public String ListToString(List<String> contacts){
		String newContact = "";
		for (int i = 0; i < contacts.size(); i++) {
		    String element = contacts.get(i);
		    newContact = newContact + element + ",";
		}
		return newContact;
	}
	
	public int resendOne2MultiMessage(String msgId,IGroupChatListener listener) throws ServerApiException {
		String text = RichMessagingHistory.getInstance().getMessageText(msgId);
		if (logger.isActivated()) {
            logger.info("resendOne2MultiMessage text:" + text);
        }
		List<String> contacts =  RichMessagingHistory.getInstance().getMultiMessageParticipants(msgId);
		if (logger.isActivated()) {
            logger.info("resendOne2MultiMessage contacts:" + contacts);
        }
	    sendOne2MultiMessage(contacts,text,listener,msgId);
	    return getState(msgId);
	    
	}
	    
	/**
     * Returns the state of the group chat message
     * 
     * @return State of the message
     * @see GroupChat.MessageState
     */
    public int getState(String messageId) {
        //int messageStatus = RichMessagingHistory.getInstance().getMessageStatus(messageId);
        int messageStatus = 0;
        switch(messageStatus){
            case ChatLog.Message.Status.Content.SENDING:
                return Chat.MessageState.SENDING;
                
            case ChatLog.Message.Status.Content.SENT:
                return Chat.MessageState.SENT;
                
            case ChatLog.Message.Status.Content.UNREAD_REPORT:
            case ChatLog.Message.Status.Content.UNREAD:
            case ChatLog.Message.Status.Content.READ:
                return Chat.MessageState.DELIVERED;
                
            case ChatLog.Message.Status.Content.FAILED:
                return Chat.MessageState.FAILED;
                
            default:
                return Chat.MessageState.FAILED;
        }
    }
	
	public String sendOne2MultiMessage(List<String> contacts, String message, IGroupChatListener listener) throws ServerApiException {
		return sendOne2MultiMessage(contacts,message,listener,null);
	}
	
	/**
     * Initiates a group chat with a group of contact and returns a GroupChat
     * instance. The subject is optional and may be null.
     * 
     * @param contact List of contacts
     * @param subject Subject
     * @param listener Chat event listener
     * @throws ServerApiException
     */
    public String sendOne2MultiMessage(List<String> contacts, String message, IGroupChatListener listener,String resendId) throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("sendOne2MultiMessage Contacts:" + contacts);
        }
        if (logger.isActivated()) {
            logger.info("sendOne2MultiMessage message:" + message);
        }
        
        // Test IMS connection
        ServerApiUtils.testIms();

        try {
            String newContact = ListToString(contacts);
            InstantMessage msg = ChatUtils.createTextMessage(newContact, message,
        			Core.getInstance().getImService().getImdnManager().isImdnActivated());
            if(resendId != null){
            	msg.setMessageId(resendId);
            }
            // Initiate the session
            final ChatSession session = Core.getInstance().getImService().initiateOneToMultiStandAloneChatSession(contacts, null);
            session.setImdnMessageId(msg.getMessageId());           
            
            // Add session listener
            GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
            sessionApi.addEventListener(listener);
            sessionApi.setMultiMessage(msg);

            if(resendId == null){            	
            RichMessagingHistory.getInstance().addChatMessage(msg, ChatLog.Message.Direction.OUTGOING);
            }
            String text = RichMessagingHistory.getInstance().getMessageText(msg.getMessageId());
    		if (logger.isActivated()) {
                logger.info("resendOne2MultiMessage text:" + text);
            }
    		List<String> contacts2 =  RichMessagingHistory.getInstance().getMultiMessageParticipants(msg.getMessageId());
    		if (logger.isActivated()) {
                logger.info("resendOne2MultiMessage contacts:" + contacts2);
            }
            // Update rich messaging history
           // RichMessagingHistory.getInstance().addMultiMessageChat(session.getContributionID(),msg.getTextMessage(),msg.getMessageId(),contacts,GroupChat.State.INITIATED,GroupChat.Direction.OUTGOING);
            

            // Start the session
            Thread t = new Thread() {
                public void run() {
                    session.startSession();
                }
            };
            t.start();
                        
            // Add session in the list
            ChatServiceImpl.addGroupChatSession(sessionApi);
            return msg.getMessageId();
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Unexpected error", e);
            }
            throw new ServerApiException(e.getMessage());
        }
    }

    /**
     * Initiates a group chat with a group of contact and returns a GroupChat
     * instance. The subject is optional and may be null.
     * 
     * @param contact List of contacts
     * @param subject Subject
     * @param listener Chat event listener
	 * @throws ServerApiException
     */
    public String sendOne2MultiCloudMessageLargeMode(List<String> contacts, String message, IGroupChatListener listener) throws ServerApiException {
        if (logger.isActivated()) {
            logger.info("sendOne2MultiCloudMessage Contacts:" + contacts);
        }
        if (logger.isActivated()) {
            logger.info("sendOne2MultiCloudMessage message:" + message);
        }
        
        // Test IMS connection
        ServerApiUtils.testIms();

        try {
            // Initiate the session
            final ChatSession session = Core.getInstance().getImService().initiateOneToMultiStandAloneChatSession(contacts, null);
            session.setCloudMessage(true);

            String newContact = ListToString(contacts);
            InstantMessage msg = ChatUtils.createTextMessage(newContact, message,
        			Core.getInstance().getImService().getImdnManager().isImdnActivated());

            
            // Add session listener
            GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
            sessionApi.addEventListener(listener);
            sessionApi.setMultiMessage(msg);

            RichMessagingHistory.getInstance().addCloudMessage(msg, ChatLog.Message.Direction.OUTGOING);
            // Update rich messaging history
           // RichMessagingHistory.getInstance().addMultiMessageChat(session.getContributionID(),msg.getTextMessage(),msg.getMessageId(),contacts,GroupChat.State.INITIATED,GroupChat.Direction.OUTGOING);
            

            // Start the session
            Thread t = new Thread() {
                public void run() {
                    session.startSession();
                }
            };
            t.start();
                        
            // Add session in the list
            ChatServiceImpl.addGroupChatSession(sessionApi);
            return msg.getMessageId();
        } catch(Exception e) {
            if (logger.isActivated()) {
                logger.error("Unexpected error", e);
            }
            throw new ServerApiException(e.getMessage());
        }
    }

    /**
     * Initiates a group chat with a group of contact and returns a GroupChat
     * instance. The subject is optional and may be null.
     * 
     * @param contact List of contacts
     * @param subject Subject
     * @param listener Chat event listener
	 * @throws ServerApiException
     */
    public IGroupChat initiateGroupChat(List<String> contacts, String subject, IGroupChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an ad-hoc group chat session Subject:" + subject);
		}
		
		if (logger.isActivated()) {
			logger.info("initiateGroupChat contacts:" + contacts);
		}
		
		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			final ChatSession session = Core.getInstance().getImService().initiateAdhocGroupChatSession(contacts, subject);

			// Add session listener
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
			sessionApi.addEventListener(listener);

			// Update rich messaging history
            RichMessagingHistory.getInstance().addGroupChat(
                    session.getContributionID(),
                    session.getSubject(),
                    session.getParticipants().getList(),
                    GroupChat.State.INITIATED,
                    GroupChat.Direction.OUTGOING);

            if (RcsSettings.getInstance().isCPMSupported()) {
                if (logger.isActivated()) {
                    logger.info("initiateGroupChat conversationId: "
                            + session.getConversationID() 
                            + " contributionId: " 
                            + session.getContributionID());
                }
                RichMessagingHistory.getInstance().UpdateCoversationID(
                        session.getContributionID(),
                        session.getConversationID(),
                        2);
            }
			session.setChairman(ImsModule.IMS_USER_PROFILE.getUsername());
			RichMessagingHistory.getInstance().updateGroupChairman(session.getContributionID(), session.getChairman());

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
					session.startSession();
	    		}
	    	};
	    	t.start();
						
			// Add session in the list
			ChatServiceImpl.addGroupChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Rejoins an existing group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws ServerApiException
     */
    public IGroupChat rejoinGroupChat(String chatId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Rejoin group chat session related to the conversation " + chatId);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			final ChatSession session = Core.getInstance().getImService().rejoinGroupChatSession(chatId);
			
			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();

			// Add session in the list
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
			ChatServiceImpl.addGroupChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Rejoins an existing group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws ServerApiException
     */
    public IGroupChat rejoinGroupChatId(String chatId, String rejoinId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Rejoin group chat session related to the conversation " + chatId + "; rejoinId: " + rejoinId);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			final ChatSession session = Core.getInstance().getImService().rejoinGroupChatSession(chatId,rejoinId);
			
			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();

			// Add session in the list
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
			ChatServiceImpl.addGroupChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Restarts a previous group chat from its unique chat ID
     * 
     * @param chatId Chat ID
     * @return Group chat
     * @throws ServerApiException
     */
    public IGroupChat restartGroupChat(String chatId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Restart group chat session related to the conversation " + chatId);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Initiate the session
			final ChatSession session = Core.getInstance().getImService().restartGroupChatSession(chatId);

			// Start the session
	        Thread t = new Thread() {
	    		public void run() {
	    			session.startSession();
	    		}
	    	};
	    	t.start();
			
			// Add session in the list
			GroupChatImpl sessionApi = new GroupChatImpl((GroupChatSession)session);
			ChatServiceImpl.addGroupChatSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    public void syncAllGroupChats(IGroupChatSyncingListener listener) {
        if (logger.isActivated()) {
            logger.info("Sync all group chats from server");
        }
        new GrouplistSubscriber(listener).start();
    }
    
    public void syncGroupChat(String chatId, IGroupChatSyncingListener listener) {
        if (logger.isActivated()) {
            logger.info("Sync one group chat from server");
        }
        new GroupInfoSubscriber(chatId, listener).start();
    }
    
    public void initiateSpamReport(String contact, String msgID) {	
    	if (logger.isActivated()) {
			logger.info("initiateSpamReport Impl");
		}
    	
    	// Initiate a new session
		final OriginatingSpamSession session = new OriginatingSpamSession(Core.getInstance().getImService(), PhoneUtils.formatNumberToSipUri("+86100869999"), msgID);
		spamSessions.add(session);
		// Start the session
        Thread t = new Thread() {
    		public void run() {
				session.startSession();
    		}
    	};
    	t.start();
		
	}
    
    public void removeSpamReportListener(ISpamReportListener listener) {
    	if (logger.isActivated()) {
			logger.info("removeSpamReportListener spamSessions.size is" + spamSessions.size());
		}
    	for(int i = 0; i < spamSessions.size() ; i++)
    	{
    		spamSessions.get(i).removeListener(listener);
    	}
    }
    
    public void addSpamReportListener(ISpamReportListener listener) {
    	
    	if (logger.isActivated()) {
			logger.info("addSpamReportListener spamSessions.size is" + spamSessions.size());
		}
    	
    	for(int i = 0; i < spamSessions.size() ; i++)
    	{
    		spamSessions.get(i).addSpamListener(listener);
    	}
    }
    
    /**
     * Returns the list of group chats in progress
     * 
     * @return List of group chat
     * @throws ServerApiException
     */
    public List<IBinder> getGroupChats() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get group chat sessions");
		}

		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(groupChatSessions.size());
			for (Enumeration<IGroupChat> e = groupChatSessions.elements() ; e.hasMoreElements() ;) {
				IGroupChat sessionApi = (IGroupChat)e.nextElement() ;
				result.add(sessionApi.asBinder());
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }
    
    /**
     * Returns a group chat in progress from its unique ID
     * 
     * @param chatId Chat ID
     * @return Group chat or null if not found
     * @throws ServerApiException
     */
    public IGroupChat getGroupChat(String chatId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get group chat session " + chatId);
		}

		// Return a session instance
		return groupChatSessions.get(chatId);
	}
    
    /**
     * Block messages in group, stack will not notify application about
     * any received message in this group
     *
     * @param chatId chatId of the group
     * @param flag true means block the message, false means unblock it
     * @throws JoynServiceException
     */
    public void blockGroupMessages(String chatId, boolean flag) {
         if (logger.isActivated()) {
                logger.info("GCM blockGroupMessages flag:" + flag);
        }
        GroupChatImpl groupImpl = (GroupChatImpl)groupChatSessions.get(chatId);
        if(groupImpl != null){
            groupImpl.blockMessages(flag);
        } else{
            // Only Update flag in DB, can't update in session
            if(flag == true) {
                RichMessagingHistory.getInstance().updateGroupBlockedStatus(chatId, 1);
            } else {
                RichMessagingHistory.getInstance().updateGroupBlockedStatus(chatId, 0);
            }
        }
    }
    
    /**
     * Adds a listener on new chat invitation events
     * 
     * @param listener Chat invitation listener
     * @throws ServerApiException
     */
    public void addEventListener(INewChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a chat invitation listener");
		}
		
		listeners.register(listener);
    }
    
    /**
     * Removes a listener on new chat invitation events
     * 
     * @param listener Chat invitation listener
     * @throws ServerApiException
     */
    public void removeEventListener(INewChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a chat invitation listener");
		}
		
		listeners.unregister(listener);
    }
    
    /**
     * Returns the configuration of the chat service
     * 
     * @return Configuration
     */
    public ChatServiceConfiguration getConfiguration() {
    	return new ChatServiceConfiguration(
    			RcsSettings.getInstance().isStoreForwardWarningActivated(),
    			RcsSettings.getInstance().getChatIdleDuration(),
    			RcsSettings.getInstance().getIsComposingTimeout(),
    			RcsSettings.getInstance().getMaxChatParticipants(),
    			RcsSettings.getInstance().getMaxChatMessageLength(),
    			RcsSettings.getInstance().getMaxGroupChatMessageLength(),
    			RcsSettings.getInstance().getMaxChatSessions(),
    			RcsSettings.getInstance().isSmsFallbackServiceActivated(),
    			RcsSettings.getInstance().isChatAutoAccepted(),
    			RcsSettings.getInstance().isGroupChatAutoAccepted(),
    			RcsSettings.getInstance().isImReportsActivated(),
    			RcsSettings.getInstance().getMaxGeolocLabelLength(),
    			RcsSettings.getInstance().getGeolocExpirationTime(),
    			RcsSettings.getInstance().isImAlwaysOn());
	}    

    /**
	 * Registers a new chat invitation listener
	 * 
	 * @param listener New file transfer listener
	 * @throws ServerApiException
	 */
	public void addNewChatListener(INewChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add a new chat invitation listener");
		}
		
		listeners.register(listener);
	}

	/**
	 * Unregisters a chat invitation listener
	 * 
	 * @param listener New file transfer listener
	 * @throws ServerApiException
	 */
	public void removeNewChatListener(INewChatListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove a chat invitation listener");
		}
		
		listeners.unregister(listener);
	}

	/**
	 * Returns IM always on or not
	 * 
	 * @return boolean value
	 * @throws ServerApiException
	 */
	public boolean isImCapAlwaysOn(){
		return RcsSettings.getInstance().isImAlwaysOn();
	}

	/**
	 * Returns service version
	 * 
	 * @return Version
	 * @see Build.VERSION_CODES
	 * @throws ServerApiException
	 */
	public int getServiceVersion() throws ServerApiException {
		return Build.API_VERSION;
	}

    private static class GrouplistSubscriber extends Thread {
        public GrouplistSubscriber(IGroupChatSyncingListener listener) {
            this.listener = listener;
        }

        public void terminate() {
            beenCanceled = true;
        }

        @Override
        public void run() {
            boolean missing = false;
            synchronized(listLock) {
                subscribeGrouplist();
                try {
                    listLock.wait();
                    if (logger.isActivated()) {
                        logger.info("sync group list done!");
                    }
                    listener.onSyncStart(basicGroupInfos.size());
                    if (basicGroupInfos.size() == 0) {
                        listener.onSyncDone(0);
                    } else {
                        while (basicGroupInfos.size() > 0) {
                            synchronized (infoLock) {
                                String rejoinId = basicGroupInfos.get(0).getUri();
                                subscribeGroupInfo(rejoinId);
                                try {
                                    infoLock.wait();
                                    if (eventData == null) {
                                        missing = true;
                                    }
                                    else {
                                        logger.info("callback to group info handler");
                                        String chatId = RichMessagingHistory.getInstance()
                                                .getChatIdbyRejoinId(rejoinId);
                                        listener.onSyncInfo(chatId, eventData);
                                    }
                                } catch (InterruptedException e) {
                                    missing = true;
                                } finally {
                                    basicGroupInfos.remove(0);
                                }
                            }
                        }
                        if (missing)
                            listener.onSyncDone(-1);
                        else
                            listener.onSyncDone(0);
                    }
                } catch (InterruptedException e) {
                    try {
                        listener.onSyncDone(-1);
                    } catch (Exception e2) {

                    }
                } catch (Exception e1) {

                }
            }
        }

        private void subscribeGrouplist() {
            SubscriptionManager manager = SubscriptionManager.getInstance();

            SubscriptionManager.EventCallback eventCallback = new SubscriptionManager.EventCallback() {
                @Override
                protected void handleEventNotify(byte[] content) {
                    if (logger.isActivated()) {
                        logger.info("subscribe group list receive notify");
                    }
                    if (content == null || content.length == 0)
                        return;
                    try {
                        if (logger.isActivated()) {
                            logger.info("group list not null");
                        }
                        InputSource input = new InputSource(new ByteArrayInputStream(content));
                        GroupListParser parser = new GroupListParser(input);
                        GroupListDocument info = parser.getGroupList();

                        basicGroupInfos = info.getGroups();
                        if (logger.isActivated()) {
                            logger.info("group list size: " + basicGroupInfos.size());
                        }
                    } catch (Exception e) {
                        if (logger.isActivated()) {
                            logger.error("Can't parse XML notification", e);
                        }
                    } finally {
                        synchronized (listLock) {
                            listLock.notify();
                        }
                    }
                }

                @Override
                protected void onActive(String identity) {
                }

                @Override
                protected void onPending(String identity) {
                }

                @Override
                protected void onTerminated(String reason, int retryAfter) {
                }
            };

            String contentType = "application/grouplist-ver+xml";
            String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + SipUtils.CRLF +
                             "<grouplist-ver version=0\">" + SipUtils.CRLF +
                             "</grouplist-ver>";

            SubscribeRequest request = new SubscribeRequest.Builder()
                .setRequestUri(RcsSettings.getInstance().getImConferenceUri())
                .setSubscibeEvent("grouplist")
                .setAcceptContent("application/conference-info+xml")
                .setContent(contentType, content.getBytes())
                .build();
            manager.pollStatus(request, eventCallback);
        }

        private void subscribeGroupInfo(final String groupId) {
            SubscriptionManager manager = SubscriptionManager.getInstance();

            SubscriptionManager.EventCallback callback = new SubscriptionManager.EventCallback() {
                @Override
                protected void handleEventNotify(byte[] content) {
                    logger.info("handle group info notify");
                    if (content == null || content.length == 0)
                        return;
                    try {
                        InputSource input = new InputSource(new ByteArrayInputStream(content));
                        ConferenceInfoParser parser = new ConferenceInfoParser(input);
                        ConferenceInfoDocument info = parser.getConferenceInfo();

                        if (info == null)
                            return;

                        String chatId = updateGroupChat(groupId, info);
                        ArrayList<ConferenceUser> confUsers =
                                new ArrayList<ConferenceUser>();

                        Vector<User> users = info.getUsers();
                        for (User user:users) {
                            ConferenceUser confUser =
                                    new ConferenceUser(
                                            user.getEntity(),
                                            user.getUserState(),
                                            user.getState(),
                                            user.getDisconnectionMethod(),
                                            user.getRole(),
                                            user.getEtype(),
                                            user.getDisplayName());
                            confUsers.add(confUser);
                        }
                        eventData = new ConferenceEventData(
                                "full",
                                info.getSubject(),
                                info.getChairman(),
                                confUsers);
                    } catch (Exception e) {
                        if (logger.isActivated()) {
                            logger.error("Can't parse XML notification", e);
                        }
                    } finally {
                        synchronized (infoLock) {
                            infoLock.notify();
                        }
                    }
                }

                @Override
                protected void onActive(String identity) {
                }

                @Override
                protected void onPending(String identity) {
                }

                @Override
                protected void onTerminated(String reason, int retryAfter) {
                }
            };

            eventData = null;
            SubscribeRequest request = new SubscribeRequest.Builder()
                .setRequestUri(groupId)
                .setSubscibeEvent("groupinfo")
                .setAcceptContent("application/conference-info+xml")
                .build();
            manager.pollStatus(request, callback);
        }

        private String updateGroupChat(String rejoinId, ConferenceInfoDocument info) {
            RichMessagingHistory msgHistory = RichMessagingHistory.getInstance();
            GroupChatInfo chatInfo = msgHistory.getGroupChatInfoByRejoinId(rejoinId);
            String chatId = null;

            if (chatInfo == null) {
                String callId = Core.getInstance().getImsModule().getSipManager().getSipStack().generateCallId();
                chatId = ContributionIdGenerator.getContributionId(callId);

                Vector<User> users = info.getUsers();
                ArrayList<String> participants = new ArrayList<String>();

                for (User user:users) {
                    participants.add(user.getEntity());
                }

                msgHistory.addGroupChat(
                            chatId,
                            info.getSubject(),
                            participants,
                            GroupChat.State.TERMINATED,
                            GroupChat.Direction.INCOMING);

                msgHistory.updateGroupChatRejoinId(chatId, rejoinId);
                msgHistory.updateGroupChairman(chatId, info.getChairman());
            } else {
                chatId = chatInfo.getContributionId();
                msgHistory.updateGroupChatSubject(chatId, info.getSubject());
                msgHistory.updateGroupChairman(chatId, info.getChairman());
            }
            return chatId;
        }

        private IGroupChatSyncingListener listener;
        private List<BasicGroupInfo> basicGroupInfos;
        ConferenceEventData eventData;

        Object listLock = new Object();
        Object infoLock = new Object();
        boolean beenCanceled = false;
    }


    private static class GroupInfoSubscriber extends Thread {
        private String chatId;
        private IGroupChatSyncingListener listener;

        public GroupInfoSubscriber(String chatId, IGroupChatSyncingListener listener) {
            this.chatId = chatId;
            this.listener = listener;
        }

        @Override
        public void run() {
            GroupChatInfo chatInfo = RichMessagingHistory.getInstance().getGroupChatInfo(chatId);
            subscribeGroupInfo(chatInfo.getRejoinId());
        }

        private void subscribeGroupInfo(final String groupId) {
            SubscriptionManager manager = SubscriptionManager.getInstance();

            SubscriptionManager.EventCallback callback = new SubscriptionManager.EventCallback() {
                @Override
                protected void handleEventNotify(byte[] content) {
                    if (content == null || content.length == 0)
                        return;
                    try {
                        InputSource input = new InputSource(new ByteArrayInputStream(content));
                        ConferenceInfoParser parser = new ConferenceInfoParser(input);
                        ConferenceInfoDocument info = parser.getConferenceInfo();

                        if (info == null)
                            return;

                        String chatId = updateGroupChat(groupId, info);
                        ArrayList<ConferenceUser> confUsers =
                                new ArrayList<ConferenceUser>();

                        Vector<User> users = info.getUsers();
                        for (User user:users) {
                            ConferenceUser confUser =
                                    new ConferenceUser(
                                            user.getEntity(),
                                            user.getUserState(),
                                            user.getState(),
                                            user.getDisconnectionMethod(),
                                            user.getRole(),
                                            user.getEtype(),
                                            user.getDisplayName());
                            confUsers.add(confUser);
                        }
                        listener.onSyncInfo(chatId, new ConferenceEventData(
                                "full",
                                info.getSubject(),
                                info.getChairman(),
                                confUsers));
                    } catch (Exception e) {
                        if (logger.isActivated()) {
                            logger.error("Can't parse XML notification", e);
                        }
                    } finally {
                    }
                }

                @Override
                protected void onActive(String identity) {
                }

                @Override
                protected void onPending(String identity) {
                }

                @Override
                protected void onTerminated(String reason, int retryAfter) {
                }
            };

            SubscribeRequest request = new SubscribeRequest.Builder()
                .setRequestUri(groupId)
                .setSubscibeEvent("groupinfo")
                .setAcceptContent("application/conference-info+xml")
                .build();
            manager.pollStatus(request, callback);
        }

        private String updateGroupChat(String rejoinId, ConferenceInfoDocument info) {
            RichMessagingHistory msgHistory = RichMessagingHistory.getInstance();
            GroupChatInfo chatInfo = msgHistory.getGroupChatInfoByRejoinId(rejoinId);
            String chatId = null;

            if (chatInfo == null) {
                String callId = Core.getInstance().getImsModule().getSipManager().getSipStack().generateCallId();
                chatId = ContributionIdGenerator.getContributionId(callId);

                Vector<User> users = info.getUsers();
                ArrayList<String> participants = new ArrayList<String>();

                for (User user:users) {
                    participants.add(user.getEntity());
                }

                msgHistory.addGroupChat(
                            chatId,
                            info.getSubject(),
                            participants,
                            GroupChat.State.TERMINATED,
                            GroupChat.Direction.INCOMING);

                msgHistory.updateGroupChatRejoinId(chatId, rejoinId);
                msgHistory.updateGroupChairman(chatId, info.getChairman());
            } else {
                chatId = chatInfo.getContributionId();
                msgHistory.updateGroupChatSubject(chatId, info.getSubject());
                msgHistory.updateGroupChairman(chatId, info.getChairman());
            }
            return chatId;
        }
    }
}
