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
public class Message implements Serializable {

    private MessageType messageType;
    private long timestamp;
    private long incrementedStamp;
    private String nodeId;
    private long OSN;

    public long getOSN() {
        return OSN;
    }

    public void setOSN(long OSN) {
        this.OSN = OSN;
    }

    public String getNodeId() {
        return nodeId;
    }

    public long getIncrementedStamp() {
        return incrementedStamp;
    }

    public void incrementeStamp() {
        this.incrementedStamp++;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timeStamp) {
        this.timestamp = timeStamp;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public static Message getRequestMessage(long osn) {
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setOSN(osn);
        message.setMessageType(MessageType.REQUEST);
        return message;
    }

    public static Message getConnectMessage() {
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageType(MessageType.CONNECT);
        return message;
    }

    public static Message getReplyMessage() {
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageType(MessageType.REPLY);
        return message;
    }
}
