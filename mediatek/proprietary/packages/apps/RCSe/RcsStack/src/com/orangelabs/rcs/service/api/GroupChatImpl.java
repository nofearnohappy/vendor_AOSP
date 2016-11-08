package com.orangelabs.rcs.service.api;

import java.util.List;

import org.gsma.joyn.JoynServiceException;
import org.gsma.joyn.chat.ChatLog;
import org.gsma.joyn.chat.ChatMessage;
import org.gsma.joyn.chat.ConferenceEventData.ConferenceUser;
import org.gsma.joyn.chat.Geoloc;
import org.gsma.joyn.chat.GroupChat;
import org.gsma.joyn.chat.GroupChat.ReasonCode;
import org.gsma.joyn.chat.IGroupChat;
import org.gsma.joyn.chat.IGroupChatListener;
import org.gsma.joyn.ft.IFileTransfer;
import org.gsma.joyn.ft.IFileTransferListener;


import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OriginatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.RejoinGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.RestartGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Group chat implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChatImpl extends IGroupChat.Stub implements ChatSessionListener {
	
	/**
	 * Core session
	 */
	private GroupChatSession session;
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IGroupChatListener> listeners = new RemoteCallbackList<IGroupChatListener>();

	/**
	 * Lock used for synchronisation
	 */
	private Object lock = new Object();

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	private InstantMessage multiMessage = null;

	/**
	 * Constructor
	 * 
	 * @param session Session
	 */
	public GroupChatImpl(GroupChatSession session) {
		this.session = session;
		
		session.addListener(this);
	}
	
	/**
	 * Get chat ID
	 * 
	 * @return Chat ID
	 */
	public String getChatId() {
		return session.getContributionID();
	}
	
	/**
	 * Get chat ID
	 * 
	 * @return Chat ID
	 */
	public String getChatSessionId() {
		return session.getSessionID();
	}
	
	public void setMultiMessage(InstantMessage message) {
	    this.multiMessage = message;
	}
	
	/**
	 * Get remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return PhoneUtils.extractNumberFromUri(session.getRemoteContact());
	}
	
	/**
	 * Returns the direction of the group chat (incoming or outgoing)
	 * 
	 * @return Direction
	 */
	public int getDirection() {
		if ((session instanceof OriginatingAdhocGroupChatSession) ||
				(session instanceof RejoinGroupChatSession) ||
					(session instanceof RestartGroupChatSession)) {
			return GroupChat.Direction.OUTGOING;
		} else {
			return GroupChat.Direction.INCOMING;
		}
	}		
	
	/**
	 * Returns the state of the group chat
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = GroupChat.State.UNKNOWN;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = GroupChat.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = GroupChat.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				result = GroupChat.State.TERMINATED;
			} else {
				// Session pending
				if ((session instanceof OriginatingAdhocGroupChatSession) ||
						(session instanceof RestartGroupChatSession) ||
						(session instanceof RejoinGroupChatSession)) {
					result = GroupChat.State.INITIATED;
				} else {
					result = GroupChat.State.INVITED;
				}
			}
		}
		if (logger.isActivated()) {
			logger.info("GCM getState:" + result);
		}
		return result;			
	}		
	
	/**
	 * Is Store & Forward
	 * 
	 * @return Boolean
	 */
	public boolean isStoreAndForward() {
		return session.isStoreAndForward();
	}
	
	/**
	 * Get subject associated to the session
	 * 
	 * @return String
	 */
	public String getSubject() {
		return session.getSubject();
	}

	/**
	 * Accepts chat invitation
	 */
	public void acceptInvitation() {
		if (logger.isActivated()) {
			logger.info("GCM Accept session invitation");
		}
				
		// Accept invitation
        Thread t = new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	};
    	t.start();
	}
	
	/**
	 * Rejects chat invitation
	 */ 
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("GCM Reject session invitation");
		}

		// Update rich messaging history
		RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
		
        // Reject invitation
        Thread t = new Thread() {
    		public void run() {
    			session.rejectSession(603);
    		}
    	};
    	t.start();
	}

	/**
	 * Quits a group chat conversation. The conversation will continue between
	 * other participants if there are enough participants.
	 */
	public void quitConversation() {
		if (logger.isActivated()) {
			logger.info("GCM Cancel session");
		}
		
		// Abort the session
        Thread t = new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	};
    	t.start();
	}
	
	/**
	 * Returns the list of connected participants. A participant is identified
	 * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
	 * 
	 * @return List of participants
	 */
	public List<String> getParticipants() {
		if (logger.isActivated()) {
			logger.info("GCM Get list of connected participants in the session");
		}
		return session.getConnectedParticipants().getList();
	}


	/**
	 * Returns the list of connected participants. A participant is identified
	 * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
	 * 
	 * @return List of participants
	 */
	public List<String> getAllParticipants() {
		if (logger.isActivated()) {
			logger.info("GCM Get list of connected participants in the session");
		}
		return session.getParticipants().getList();
	}

	/**
	 * Returns the max number of participants for a group chat from the group
	 * chat info subscription (this value overrides the provisioning parameter)
	 * 
	 * @return Number
	 */
	public int getMaxParticipants() {
        if (logger.isActivated()) {
            logger.info("GCM Get max number of participants in the session");
        }
        return session.getMaxParticipants();
    }

	/**
	 * Adds participants to a group chat
	 * 
	 * @param participants List of participants
	 */
	public void addParticipants(final List<String> participants) {
		if (logger.isActivated()) {
			logger.info("GCM Add " + participants.size() + " participants to the session");
		}

		int max = session.getMaxParticipants()-1;
		int connected = session.getConnectedParticipants().getList().size(); 
        if (connected < max) {
            // Add a list of participants to the session
	        Thread t = new Thread() {
	    		public void run() {
	                session.addParticipants(participants);
	    		}
	    	};
	    	t.start();
        } else {
        	// Max participants achieved
            handleAddParticipantFailed("Maximum number of participants reached");
        }
	}
	
	/**
	 * Sends a text message to the group
	 * 
	 * @param text Message
	 * @return Message ID
	 */
	public String sendMessage(final String text) {
		if (logger.isActivated()) {
			logger.info("GCM sendMessage:" + text);
		}
		// Generate a message Id
		final String msgId = ChatUtils.generateMessageId();

		if(LauncherUtils.supportOP01()) {
			 // Update rich messaging history
			if (!RichMessagingHistory.getInstance().isOne2OneMessageExists(msgId)) {
				if (logger.isActivated()) {
			    	logger.info("CPMS Group sendMessage Add in DB Msgid:" + msgId );
				    }
			InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), text, session.getImdnManager().isImdnActivated() ,null);
			RichMessagingHistory.getInstance().addGroupChatMessage(session.getContributionID(), msg,
					ChatLog.Message.Direction.OUTGOING);
		}
		}

		// Send text message
        Thread t = new Thread() {
    		public void run() {
    			session.sendTextMessage(msgId, text);
    		}
    	};
    	t.start();

		return msgId;
	}
	
	/**
	 * Sends a emoticons message to the group
	 * 
	 * @param text Message
	 * @return Message ID
	 */
	public String sendEmoticonsMessage(final String text) {
		if (logger.isActivated()) {
			logger.info("GCM sendEmoticonsMessage:" + text);
		}
		// Generate a message Id
		final String msgId = ChatUtils.generateMessageId();

		if(LauncherUtils.supportOP01()) {
			 // Update rich messaging history
			InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), text, session.getImdnManager().isImdnActivated() ,null);
			msg.setEmoticonMessage(true);
			RichMessagingHistory.getInstance().addGroupChatMessage(session.getContributionID(), msg,
					ChatLog.Message.Direction.OUTGOING);
		}

		// Send text message
        Thread t = new Thread() {
    		public void run() {
    			session.sendEmoticonsMessage(msgId, text);
    		}
    	};
    	t.start();

		return msgId;
	}
	
	/**
	 * Sends a emoticons message to the group
	 * 
	 * @param text Message
	 * @return Message ID
	 */
	public String sendCloudMessage(final String text) {
		if (logger.isActivated()) {
			logger.info("GCM sendCloudMessage:" + text);
		}
		// Generate a message Id
		final String msgId = ChatUtils.generateMessageId();

		if(LauncherUtils.supportOP01()) {
			 // Update rich messaging history
			InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), text, session.getImdnManager().isImdnActivated() ,null);
			msg.setCloudMessage(true);
			RichMessagingHistory.getInstance().addGroupChatMessage(session.getContributionID(), msg,
					ChatLog.Message.Direction.OUTGOING);
		}

		// Send text message
        Thread t = new Thread() {
    		public void run() {
    			session.sendCloudMessage(msgId, text);
    		}
    	};
    	t.start();

		return msgId;
	}
	
	/**
	 * @param msgId message Id of message
	 * @throws JoynServiceException
	 */
	public int resendMessage(final String msgId)
	{
	    final String message = RichMessagingHistory.getInstance().getMessageText(msgId);
	    if (logger.isActivated()) {
			logger.info("GCM resendMessage:" + message + " msgId:" + msgId);
		}
		
	    if(LauncherUtils.supportOP01()) {
		    // Update rich messaging history
			InstantMessage msg = new InstantMessage(msgId, getRemoteContact(), message, session.getImdnManager().isImdnActivated() ,null);
			//RichMessagingHistory.getInstance().addGroupChatMessage(session.getContributionID(), msg,
				//	ChatLog.Message.Direction.OUTGOING);
	    }
		
		// Send text message
        Thread t = new Thread() {
    		public void run() {
    			session.sendTextMessage(msgId, message);
    		}
    	};
    	t.start();
		
		return getMessageState(msgId);
	}

	 /**	
	 * Returns the state of the group chat message
	 * 
	 * @return State of the message
	 * @see GroupChat.MessageState
	 */
	public int getMessageState(String messageId) {
		int messageStatus = RichMessagingHistory.getInstance().getMessageStatus(messageId);
		 if (logger.isActivated()) {
				logger.info("GCM getMessageState: msgId" + messageId + " StackStatus:" + messageStatus);
		}
		switch(messageStatus){
			case ChatLog.Message.Status.Content.SENDING:
				return GroupChat.MessageState.SENDING;
				
			case ChatLog.Message.Status.Content.SENT:
				return GroupChat.MessageState.SENT;
				
			case ChatLog.Message.Status.Content.UNREAD_REPORT:
			case ChatLog.Message.Status.Content.UNREAD:
			case ChatLog.Message.Status.Content.READ:
				return GroupChat.MessageState.DELIVERED;
				
			case ChatLog.Message.Status.Content.FAILED:
				return GroupChat.MessageState.FAILED;
				
			default:
				return GroupChat.MessageState.FAILED;
		}
	}
	
	
	/**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return Unique message ID or null in case of error
     */
    public String sendGeoloc(Geoloc geoloc) {
		// Generate a message Id
		final String msgId = ChatUtils.generateMessageId();

		// Send geoloc message
		final GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(),
				geoloc.getLatitude(), geoloc.getLongitude(),
				geoloc.getExpiration(), geoloc.getAccuracy());
        Thread t = new Thread() {
    		public void run() {
    			session.sendGeolocMessage(msgId, geolocPush);
    		}
    	};
    	t.start();
		return msgId;
    }	

    /**
	 * Set the new chairman(chairman privilege).
	 * 
	 * @param newChairman new chairman of the group, should be a group member 
	 * @throws JoynServiceException
	 */
    public void transferChairman(final String newChairman) {
        Thread t = new Thread() {
            public void run() {
                session.transferChairman(newChairman);
            }
        };
        t.start();
    }
    
    /**
	 * modify subject of group(chairman privilege).
	 * 
	 * @param newSubject new subject string 
	 * @throws JoynServiceException
	 */
    public void modifySubject(final String newSubject) {
        Thread t = new Thread() {
            public void run() {
                session.modifySubject(newSubject);
            }
        };
        t.start();
    }
    
    /**
	 * modify nickname of participant.
	 * 
	 * @param contact contact of the participant
	 * @param newNickname new nick name of participant 
	 * @throws JoynServiceException
	 */
    public void modifyMyNickName(final String newNickname) {

        final String contact = ImsModule.IMS_USER_PROFILE.getUsername();
        Thread t = new Thread() {
            public void run() {
                session.modifyMyNickName(contact, newNickname);
            }
        };
        t.start();
    }
    
    /**
	 * remove set of participants(chairman privilege).
	 * 
	 * @param participants list of participants to be removed 
	 * @throws JoynServiceException
	 */
    public void removeParticipants(final List<String> participants) {
        Thread t = new Thread() {
            public void run() {
                for(String participant : participants) {
                    session.removeParticipants(participant);
                }
            }
        };
        t.start();
    }
    
    /**
	 * chairman abort(leave) the group, Group session will abort
	 *
	 * @throws JoynServiceException
	 */
    public void abortConversation() {
    	 if (logger.isActivated()) {
				logger.info("GCM abortConversation");
		}
        Thread t = new Thread() {
            public void run() {
                session.abortGroupSession(ImsServiceSession.TERMINATION_BY_USER);
            }
        };
        t.start();
    }
    
    
    /**
     * Block messages in group, stack will not notify application about
     * any received message in this group
     *
     * @param flag true means block the message, false means unblock it
     * @throws JoynServiceException
     */
    public void blockMessages(final boolean flag) {
         if (logger.isActivated()) {
                logger.info("GCM blockMessages flag:" + flag);
        }
        Thread t = new Thread() {
            public void run() {
                session.blockMessages(flag);
            }
        };
        t.start();
    }
    
    /**
     * If myself chairman of the group
     * 
     * @return true/false
     * @throws JoynServiceException
     */
    public boolean isMeChairman() {
        if (logger.isActivated()) {
            logger.info("GCM isMeChairman entry");
        }
        boolean flag = false;
        String chairman = RichMessagingHistory.getInstance().getGroupChairman(getChatId());
        if (logger.isActivated()) {
            logger.info("GCM Current chairman of group "+ chairman);
        }
        String me = ImsModule.IMS_USER_PROFILE.getUsername();
        if (logger.isActivated()) {
            logger.info("GCM me is "+ me);
        }
        //TODO change 'contains' to 'equals' later after verify
        if(me.contains(chairman)){
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Transfers a file to participants. The parameter filename contains the complete
     * path of the file to be transferred.
     * 
     * @param filename Filename to transfer
     * @param fileicon Filename of the file icon associated to the file to be transfered
     * @param listener File transfer event listener
     * @return File transfer
     */
    public IFileTransfer sendFile(String filename, String fileicon, IFileTransferListener listener) {
    	// TODO
    	return null;
    }

    /**
	 * Sends a is-composing event. The status is set to true when typing
	 * a message, else it is set to false.
	 * 
	 * @param status Is-composing status
	 */
	public void sendIsComposingEvent(final boolean status) {
        Thread t = new Thread() {
    		public void run() {
    			session.sendIsComposingStatus(status);
    		}
    	};
    	t.start();
	}
	
    /**
     * Sends a displayed delivery report for a given message ID
     * 
     * @param msgId Message ID
     */
    public void sendDisplayedDeliveryReport(final String msgId) {
		try {
			if (logger.isActivated()) {
				logger.debug("GCM Set displayed delivery report for " + msgId);
			}
			
			// Send MSRP delivery status
	        Thread t = new Thread() {
	    		public void run() {
	    			session.sendMsrpMessageDeliveryStatus(session.getRemoteContact(), msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
	    		}
	    	};
	    	t.start();
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("GCM Could not send MSRP delivery status",e);
			}
		}
    }	
	
	/**
	 * Adds a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 */
	public void addEventListener(IGroupChatListener listener) {
		if (logger.isActivated()) {
			logger.info("GCM Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}
	
	/**
	 * Removes a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 */
	public void removeEventListener(IGroupChatListener listener) {
		if (logger.isActivated()) {
			logger.info("GCM Remove an event listener");
		}

    	synchronized(lock) {
    		listeners.unregister(listener);
    	}
	}
	
    /*------------------------------- SESSION EVENTS ----------------------------------*/

    /**
     * Session is started
     */
    public void handleSessionStarted() {
        synchronized (lock) {
            if (logger.isActivated()) {
                logger.info("GCM Session started");
            }

            // Update rich messaging history
            if (RichMessagingHistory.getInstance().getGroupChatInfo(getChatId()) != null) {
                RichMessagingHistory.getInstance().updateGroupChatStatus(
                    getChatId(), GroupChat.State.STARTED);
                RichMessagingHistory.getInstance().updateGroupChatRejoinId(
                    getChatId(), session.getImSessionIdentity());
            }

            if (!(RichMessagingHistory.getInstance().isGroupMemberExist(
                    getChatId(), ImsModule.IMS_USER_PROFILE.getUsername()))) {
                RichMessagingHistory.getInstance().addGroupMember(
                        session.getContributionID(),
                        RcsSettings.getInstance().getJoynUserAlias(),
                        ImsModule.IMS_USER_PROFILE.getUsername(),
                        null);
            }

            // Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
                logger.info("GCM Session started n: " + N);
            }
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onSessionStarted();
                } catch (Exception e) {
                    if (logger.isActivated()) {
                        logger.error("GCM Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }

        if (logger.isActivated()) {
            logger.info("GCM Session starte multi message ID:" + multiMessage);
        }
        if (multiMessage != null) {
            //If groupchatsession in New DB exists and status id not finished send the message
            // Generate a message Id
            if (logger.isActivated()) {
                logger.info("GCM Session starte multi message ID:"
                    + multiMessage.getMessageId());
            }
            final String msgId = ChatUtils.generateMessageId();

            if (LauncherUtils.supportOP01()) {
                // Update rich messaging history
                InstantMessage msg = new InstantMessage(
                    msgId, getRemoteContact(),
                    multiMessage.getTextMessage(),
                    session.getImdnManager().isImdnActivated(),
                    null);
                RichMessagingHistory.getInstance().addGroupChatMessage(
                    session.getContributionID(), msg,
                    ChatLog.Message.Direction.OUTGOING);
            }

            Thread t = new Thread() {
                public void run() {
                    boolean isCloud = RichMessagingHistory.getInstance().isCloudMessage(
                        multiMessage.getMessageId());
                    if (logger.isActivated()) {
                        logger.info("LMM handleSessionStarted:"
                            + isCloud + ", MsgId:" + multiMessage.getMessageId());
                    }
                    if (isCloud) {
                        session.sendCloudMessage(
                            multiMessage.getMessageId(), multiMessage.getTextMessage());
                    } else {
                        session.sendTextMessage(
                            multiMessage.getMessageId(), multiMessage.getTextMessage());
                    }
                    multiMessage = null;
                }
            };
            t.start();
        }
    }
    
    /**
     * Session has been aborted
     * 
	 * @param reason Termination reason
	 */
    public void handleSessionAborted(int reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("GCM Session aborted (reason " + reason + ")");
			}
	
			// Update rich messaging history
			if (reason == ImsServiceSession.TERMINATION_BY_USER) {
				RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.CLOSED_BY_USER);
			} else {
				if (session.getDialogPath().isSessionCancelled()) {
					RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
				} else {
					RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.TERMINATED);
				}
			}
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
			  if (logger.isActivated()) {
					logger.info("GCM handleSessionAborted N: " + N);
				}
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSessionAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("GCM Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    
    /**
     * Group SIP bye received for group session
     * 
     * @param cause cause in 'cause' parameter
     */
    public void handleSessionTerminatedByGroupRemote(String cause, String text){
        
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("GCM handleSessionTerminatedByGroupRemote Session terminated cause=" + cause + ",text ="+ text);
            }
    
            // Update rich messaging history
            if (session.getDialogPath().isSessionCancelled()) {
                RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
            } else {
                RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.TERMINATED);
            }
            int causeParameter = 0;
            if(cause !=null){
                causeParameter = Integer.valueOf(cause);
            }
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
				logger.info("GCM handleSessionTerminatedByGroupRemote N: " + N);
			}
            if(causeParameter == 200 && text.equalsIgnoreCase("booted")){
                //text 'booted' means participant is kicked out
                //text 'Call completed' means session is aborted by chairman
                //cause is 200 in both case
                
                causeParameter = 201;
                if (logger.isActivated()) {
                    logger.info("GCM participant is kicked out " + cause);
                }
            }
            for (int i=0; i < N; i++) {
                try {
                    switch(causeParameter){
                        case 200:
                            //session aborted by chairman
                            listeners.getBroadcastItem(i).onSessionAbortedbyChairman();
                            break;
                        case 201:
                           listeners.getBroadcastItem(i).onReportMeKickedOut(session.getChairman());
                           break;
                        case 410:
			   //cause 410 means that group chat is dissolved
                           listeners.getBroadcastItem(i).onGroupChatDissolved();
                           break;
                        default:
                            listeners.getBroadcastItem(i).onSessionAborted();
                    }
                    
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("GCM Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
            
            // Remove session from the list
            ChatServiceImpl.removeGroupChatSession(getChatId());
        }
        
    }
    
    
    /**
     * handle Quit Conversation by User(not chairman)
     * 
     * @param code status Code
     */
    public void handleQuitConversationResult(int code) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("GCM handleQuitConversationResult code =" + code);
            }
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onQuitConversationResult(ReasonCode.SUCCESSFUL, 200);
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
     * Abort Conversation successful, only received by chairman after leave group
     * (chairman privilege)
     * @param code reason code
     */
    public void handleAbortConversationResult(int reason, int code) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("Session aborted group reason=" + reason + ",code =" + code);
            }
            if(code == 200){
            // Update rich messaging history
            if (reason == ImsServiceSession.TERMINATION_BY_USER) {
                RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.CLOSED_BY_USER);
            } else {
                if (session.getDialogPath().isSessionCancelled()) {
                    RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
                } else {
                    RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.TERMINATED);
                }
            }
            
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
				logger.info("GCM handleAbortConversationResult N: " + N);
			}
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onAbortConversationResult(ReasonCode.SUCCESSFUL, code);
                        listeners.getBroadcastItem(i).onSessionAborted();
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
            
            // Remove session from the list
            ChatServiceImpl.removeGroupChatSession(getChatId());
            } else {
                // Notify event listeners
                final int N = listeners.beginBroadcast();
                for (int i=0; i < N; i++) {
                    try {
                        listeners.getBroadcastItem(i).onAbortConversationResult(ReasonCode.INTERNAL_ERROR, code);
                    } catch(Exception e) {
                        if (logger.isActivated()) {
                            logger.error("Can't notify listener", e);
                        }
                    }
                }
                listeners.finishBroadcast();
            }
        }
    }
    
    /**
     * Transfer chairman Successful
     * 
     * @param subject subject
     */
    public void handleTransferChairmanSuccessful(String newChairman) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleTransferChairmanSuccessful " + newChairman);
            }
            
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    //listeners.getBroadcastItem(i).onChairmanChanged(newChairman);
                    listeners.getBroadcastItem(i).onSetChairmanResult(GroupChat.ReasonCode.SUCCESSFUL, 200);
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
     * Transfer chairman Failed
     * 
     * @param statusCode statusCode
     */
    public void handleTransferChairmanFailed(int statusCode) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleTransferChairmanFailed " + statusCode);
            }
              
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    //need switch case of statusCode
                    listeners.getBroadcastItem(i).onSetChairmanResult(GroupChat.ReasonCode.INTERNAL_ERROR, statusCode);
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
     * Transfer chairman by other participant
     * 
     * @param newChairman new chairman
     */
    public void handleTransferChairmanByRemote(String newChairman){
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleTransferChairmanByRemote " + newChairman);
            }
            session.setChairman(newChairman);
            // Update chairman in DB
            RichMessagingHistory.getInstance().updateGroupChairman(getChatId(), newChairman);

            
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    
                    listeners.getBroadcastItem(i).onChairmanChanged(newChairman);
                    
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
     * Remove Participant Successful
     * 
     * @param subject subject
     */
    public void handleRemoveParticipantSuccessful(String removedParticipant) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleRemoveParticipantSuccessful " + removedParticipant);
            }

            //remove participant from session
            session.removeParticipantFromSession(removedParticipant);
            //remove participant from database
            RichMessagingHistory.getInstance().removeGroupMember(getChatId(),removedParticipant);
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onRemoveParticipantResult(GroupChat.ReasonCode.SUCCESSFUL, 200, removedParticipant);
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
     * Remove Participant Failed
     * 
     * @param statusCode statusCode
     */
    public void handleRemoveParticipantFailed(int statusCode) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleRemoveParticipantFailed " + statusCode);
            }
              
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    //need switch case of statusCode
                    listeners.getBroadcastItem(i).onRemoveParticipantResult(GroupChat.ReasonCode.INTERNAL_ERROR, statusCode, null);
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
     * Modify Subject Successful
     * 
     * @param subject subject
     */
    public void handleModifySubjectSuccessful(String subject) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleModifySubjectSuccessful " + subject);
            }
            session.setSubject(subject);
            // Update subject in DB
            RichMessagingHistory.getInstance().updateGroupChatSubject(getChatId(), subject);

            
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    //listeners.getBroadcastItem(i).onSubjectChanged(subject);
                    listeners.getBroadcastItem(i).onModifySubjectResult(GroupChat.ReasonCode.SUCCESSFUL, 200);
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
     * Modify Subject Failed
     * 
     * @param statusCode statusCode
     */
    public void handleModifySubjectFailed(int statusCode) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleModifySubjectFailed " + statusCode);
            }
              
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    //need switch case of statusCode
                    listeners.getBroadcastItem(i).onModifySubjectResult(GroupChat.ReasonCode.INTERNAL_ERROR, statusCode);
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
     * Modify Subject BY remote
     * 
     * @param subject subject
     */
    public void handleModifySubjectByRemote(String subject) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleModifySubjectByRemote " + subject);
            }
            session.setSubject(subject);
            // Update subject in DB
            RichMessagingHistory.getInstance().updateGroupChatSubject(getChatId(), subject);

            
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onSubjectChanged(subject);
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
     * Modify Subject Successful
     * 
     * @param subject subject
     */
    public void handleModifyNicknameSuccessful(String contact, String newNickName) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleModifyNicknameSuccessful " + contact + "nickname:"+ newNickName);
            }
            //session.setSubject(subject);
            // Update subject in DB
            //RichMessagingHistory.getInstance().updateGroupChatSubject(getChatId(), subject);
            RichMessagingHistory.getInstance().updateGroupMemberName(session.getContributionID(),contact ,newNickName);
            
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    //listeners.getBroadcastItem(i).onSubjectChanged(subject);
                    listeners.getBroadcastItem(i).onModifyNickNameResult(GroupChat.ReasonCode.SUCCESSFUL, 200);
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
     * Modify Subject Failed
     * 
     * @param statusCode statusCode
     */
    public void handleModifyNicknameFailed(String contact, int statusCode) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleModifyNicknameFailed " + statusCode + ",contact:"+ contact);
            }
              
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    //need switch case of statusCode
                    listeners.getBroadcastItem(i).onModifyNickNameResult(GroupChat.ReasonCode.INTERNAL_ERROR, statusCode);
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
     * Modify Nickname BY remote
     * 
     * @param contact contact
     */
    public void handleModifyNicknameByRemote(String contact, String newNickname) {
        synchronized(lock) {
            if (logger.isActivated()) {
                logger.info("handleModifyNicknameByRemote " + contact + ", newNickname:"+ newNickname);
            }

            RichMessagingHistory.getInstance().updateGroupMemberName(session.getContributionID(),contact ,newNickname);
            
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onNickNameChanged(contact, newNickname);
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
     * Session has been terminated by remote
     */
    public void handleSessionTerminatedByRemote() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("GCM Session terminated by remote");
			}
	
			// Update rich messaging history
			if (session.getDialogPath().isSessionCancelled()) {
				RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
			} else {
				RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.TERMINATED);
			}
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
			if (logger.isActivated()) {
				logger.info("handleSessionTerminatedByRemote N: " + N);
			}
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSessionAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("GCM Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
	/**
	 * New text message received
	 * 
	 * @param text Text message
	 */
    public void handleReceiveMessage(InstantMessage message) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("ABCG New IM received: alias: " + message.getDisplayName() + "Text: " + message.getTextMessage());
			}
			
			// Update rich messaging history
			RichMessagingHistory.getInstance().addGroupChatMessage(session.getContributionID(),
					message, ChatLog.Message.Direction.INCOMING);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
			if (logger.isActivated()) {
				logger.info("handleReceiveMessage n: " + N);
			}
	        for (int i=0; i < N; i++) {
	            try {
	            	ChatMessage msgApi = new ChatMessage(message.getMessageId(),
	            			PhoneUtils.extractNumberFromUri(message.getRemote()),
	            			message.getTextMessage(),
	            			message.getServerDate(), message.isImdnDisplayedRequested(),message.getDisplayName());
	            	if(message.isEmoticonMessage()){
	            		msgApi.setEmoticonMessage(true);
	            	} else if (message.isCloudMessage()){
	            		msgApi.setCloudMessage(true);
	            	}
	            	listeners.getBroadcastItem(i).onNewMessage(msgApi);
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
     * IM session error
     * 
     * @param error Error
     */
    public void handleImError(ChatError error) {
    	synchronized(lock) {
			if (error.getErrorCode() == ChatError.SESSION_INITIATION_CANCELLED) {
				if (logger.isActivated()) {
					logger.info("GCM handleImError IM error1 ");
				}
				// Do nothing here, this is an aborted event
				return;
			}
    		
			if (logger.isActivated()) {
				logger.info("GCM IM error " + error.getErrorCode());
			}
			
			// Update rich messaging history
			switch(error.getErrorCode()){
	    		case ChatError.SESSION_NOT_FOUND:
	    		case ChatError.SESSION_RESTART_FAILED:
	    			// These errors are not logged
	    			break;
		    	default:
					RichMessagingHistory.getInstance().updateGroupChatStatus(session.getContributionID(), GroupChat.State.FAILED);
		    		break;
	    	}
	    	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
			if (logger.isActivated()) {
				logger.info("GCM handleImError N: " + N);
			}
	        for (int i=0; i < N; i++) {
	            try {
	            	int code;
	            	switch(error.getErrorCode()) {
            			case ChatError.SESSION_INITIATION_DECLINED:
	            			code = GroupChat.Error.INVITATION_DECLINED;
	            			break;
            			case ChatError.SESSION_NOT_FOUND:
	            			code = GroupChat.Error.CHAT_NOT_FOUND;
	            			break;
	            		default:
	            			code = GroupChat.Error.CHAT_FAILED;
	            	}
	            	listeners.getBroadcastItem(i).onSessionError(code);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    	
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    /**
	 * Is composing event
	 * 
	 * @param contact Contact
	 * @param status Status
	 */
	public void handleIsComposingEvent(String contact, boolean status) {
    	synchronized(lock) {
        	contact = PhoneUtils.extractNumberFromUri(contact);

        	if (logger.isActivated()) {
				logger.info("GCM " + contact + " is composing status set to " + status);
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onComposingEvent(contact, status);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("GCM Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
		}
	}
	
    /**
     * Conference event
     * 
	 * @param contact Contact
	 * @param contactDisplayname Contact display name
     * @param state State associated to the contact
     */
    public void handleConferenceEvent(String contact, String contactDisplayname, String state, String method, String userStateParameter, String conferenceState) {
    	synchronized(lock) {
        	contact = PhoneUtils.extractNumberFromUri(contact);

        	if (logger.isActivated()) {
				logger.info("GCM New conference event " + state + "method=" + method + " for " + contact + ",Displayname: " + contactDisplayname + ",user State parameter is :" + userStateParameter + ",conf state:"+ conferenceState);
			}
			
	  		// Update history and notify event listeners
			final int N = listeners.beginBroadcast();
			if (logger.isActivated()) {
				logger.info("GCM handleConferenceEvent N: " + N);
			}
	        for (int i=0; i < N; i++) {
	            try {
	                if (state.equals(User.STATE_DISCONNECTED) && method.equals(User.STATE_BOOTED) && userStateParameter.equals(User.STATE_DELETED)) {
                        // Update rich messaging history
	                    RichMessagingHistory.getInstance().addGroupChatSystemMessage(session.getContributionID(), contact, ChatLog.Message.Status.System.GONE);
                        RichMessagingHistory.getInstance().removeGroupMember(session.getContributionID(),contact);
                        // Notify event listener
                        listeners.getBroadcastItem(i).onReportParticipantKickedOut(contact);
                    } else
	            	if (state.equals(User.STATE_CONNECTED)) {
	        			// Update rich messaging history
	        			RichMessagingHistory.getInstance().addGroupChatSystemMessage(session.getContributionID(), contact, ChatLog.Message.Status.System.JOINED);
	        	    	if(!(RichMessagingHistory.getInstance().isGroupMemberExist(getChatId(),contact))){//TODO why not get from IMS profile instead of db
	        			RichMessagingHistory.getInstance().addGroupMember(session.getContributionID(),contactDisplayname,contact,null);
    	        			if(conferenceState.equals("partial")){
    	        			 // Notify event listener with state partial, partial means something changed in group
    	        			    // full conference state means , full list of members
    	                        listeners.getBroadcastItem(i).onParticipantJoined(contact, contactDisplayname);
    	        			}
	        			}
	        	  		
	            	} else
	            	if (state.equals(User.STATE_DISCONNECTED)) {
	        			// Update rich messaging history
	        			RichMessagingHistory.getInstance().addGroupChatSystemMessage(session.getContributionID(), contact, ChatLog.Message.Status.System.DISCONNECTED);
						RichMessagingHistory.getInstance().removeGroupMember(session.getContributionID(),contact);
	        	  		// Notify event listener
	        			listeners.getBroadcastItem(i).onParticipantDisconnected(contact);
	            	} else
	            	if (state.equals(User.STATE_DEPARTED)) {
	        			// Update rich messaging history
	        			RichMessagingHistory.getInstance().addGroupChatSystemMessage(session.getContributionID(), contact, ChatLog.Message.Status.System.GONE);
						RichMessagingHistory.getInstance().removeGroupMember(session.getContributionID(),contact);
	        	  		// Notify event listener
	        			listeners.getBroadcastItem(i).onParticipantLeft(contact);
	            	}
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("GCM Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
    
    /**
     * New message delivery status
     * 
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String contact,String msgId, String status) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("GCM New message delivery status for message " + msgId + ", status " + status + "Contact" + contact);
			}
	
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
        	
			// Notify event listeners
			final int N = listeners.beginBroadcast();
			if (logger.isActivated()) {
				logger.info("GCM handleMessageDeliveryStatus N: " + N);
			}
	        for (int i=0; i < N; i++) {
	            try {
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
	            		listeners.getBroadcastItem(i).onReportMessageDeliveredContact(msgId,contact);
	            	} else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
	        			listeners.getBroadcastItem(i).onReportMessageDisplayedContact(msgId,contact);
	            	} else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
	            		listeners.getBroadcastItem(i).onReportMessageFailedContact(msgId,contact);
	            	}  else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_SENT)) {
	            		listeners.getBroadcastItem(i).onReportSentMessage(msgId);
	            	}
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("GCM Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }

	/**
     * New message delivery status
     * 
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String contact,String msgId, String status ,int errorCode, String statusCode) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("GCM New message delivery status for message " + msgId + ", status " + status + "Contact" + contact);
			}
	
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
        	
			// Notify event listeners
			final int N = listeners.beginBroadcast();
			if (logger.isActivated()) {
				logger.info("GCM handleMessageDeliveryStatus0 N: " + N);
			}
	        for (int i=0; i < N; i++) {
	            try {	            	
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
	            		listeners.getBroadcastItem(i).onReportFailedMessage(msgId,errorCode,statusCode);
	            	}
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
     * New message delivery status
     * 
	 * @param msgId Message ID
     * @param status Delivery status
     */
    public void handleMessageDeliveryStatus(String msgId, String status) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("GCM New message delivery status for message " + msgId + ", status " + status);
			}
	
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status);
        	
			// Notify event listeners
			final int N = listeners.beginBroadcast();
			if (logger.isActivated()) {
				logger.info("GCM handleMessageDeliveryStatus2 N: " + N);
			}
	        for (int i=0; i < N; i++) {
	            try {
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
	            		//listeners.getBroadcastItem(i).onReportMessageDelivered(msgId);
	            	} else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
	        			//listeners.getBroadcastItem(i).onReportMessageDisplayed(msgId);
	            	} else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
	            		//listeners.getBroadcastItem(i).onReportMessageFailed(msgId);
	            	}  else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_SENT)) {
	            		listeners.getBroadcastItem(i).onReportSentMessage(msgId);
	            	}
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
     * Request to add participant is successful
     */
    public void handleAddParticipantSuccessful() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("GCM Add participant request is successful");
			}
	
			// TODO: nothing send over API?
			// Notify event listeners
            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
				logger.info("GCM handleAddParticipantSuccessful N: " + N);
			}
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onInviteParticipantsResult(GroupChat.ParticipantStatus.SUCCESS,"");
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
     * Request to add participant has failed
     * 
     * @param reason Error reason
     */
    public void handleAddParticipantFailed(String reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("GCM Add participant request has failed " + reason);
			}
	
			// TODO: nothing send over API?
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    listeners.getBroadcastItem(i).onInviteParticipantsResult(GroupChat.ParticipantStatus.FAIL,reason);
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
     * Request to add participant has failed
     * 
     * @param reason Error reason
     */
    public void handleNickNameModified(String contact , String displayname) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("GCM handleNickNameModified contact" + contact +  " displayname: " + displayname);
			}
			RichMessagingHistory.getInstance().updateGroupMemberName(session.getContributionID(),contact,displayname);
	
			// TODO: nothing send over API?
            // Notify event listeners
            final int N = listeners.beginBroadcast();
            for (int i=0; i < N; i++) {
                try {
                    //listeners.getBroadcastItem(i).onInviteParticipantsResult(GroupChat.ParticipantStatus.FAIL,reason);
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
	    }  
    }

    /**
     * New geoloc message received
     * 
     * @param geoloc Geoloc message
     */
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New geoloc received");
			}
			
			// Update rich messaging history
			RichMessagingHistory.getInstance().addGroupChatMessage(session.getContributionID(),
					geoloc, ChatLog.Message.Direction.INCOMING);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
	            			geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
	            			geoloc.getGeoloc().getExpiration());
	            	org.gsma.joyn.chat.GeolocMessage msgApi = new org.gsma.joyn.chat.GeolocMessage(geoloc.getMessageId(),
	            			PhoneUtils.extractNumberFromUri(geoloc.getRemote()),
	            			geolocApi, geoloc.getDate(), geoloc.isImdnDisplayedRequested());
	            	listeners.getBroadcastItem(i).onNewGeoloc(msgApi);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();		
	    }
    }

    @Override
    public void handleConferenceNotify(String confState, List<ConferenceUser> users) {
        synchronized(lock) {

            if (logger.isActivated()) {
                logger.info("GCM New conference event op01: " + confState + "users=" + users );
            }

            final int N = listeners.beginBroadcast();
            if (logger.isActivated()) {
                logger.info("GCM handleConferenceEvent N: " + N);
            }
            for (int i=0; i < N; i++) {
                try {
                    
                   listeners.getBroadcastItem(i).onConferenceNotify(confState, users);
                   
                } catch(Exception e) {
                    if (logger.isActivated()) {
                        logger.error("GCM Can't notify listener", e);
                    }
                }
            }
            listeners.finishBroadcast();
        }
        
    }
}