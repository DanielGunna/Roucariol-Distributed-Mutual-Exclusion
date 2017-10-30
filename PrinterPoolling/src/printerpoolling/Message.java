/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package printerpoolling;

import java.io.Serializable;

/**
 *
 * @author 940437
 */
public class Message  implements Serializable{
    private boolean entryCriticalSection;
    private  MessageType messageType;
    private long timestamp;
    private String nodeId;

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    
    public boolean isEntryCriticalSection() {
        return entryCriticalSection;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timeStamp) {
        this.timestamp = timeStamp;
    }

    public void setEntryCriticalSection(boolean entryCriticalSection) {
        this.entryCriticalSection = entryCriticalSection;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    public static Message getEntryMessage(String nodeId){
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setNodeId(nodeId);
        message.setMessageType(MessageType.ENTRY_CRITICAL_SECTION);
        return message;
    }
    
    public static Message getLeaveMessage(String nodeId){
        Message message = new Message();
        message.setNodeId(nodeId);
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageType(MessageType.LEAVE_CRITICAL_SECTION);
        return message;
    }
}
