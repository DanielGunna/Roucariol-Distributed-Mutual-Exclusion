/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package printerpolling;

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
    private String nodeName;

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

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

    public static Message getRequestMessage(String nodeName,String nodeId, long osn) {
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setOSN(osn);
        message.setNodeId(nodeId);
        message.setNodeName(nodeName);
        message.setMessageType(MessageType.REQUEST);
        return message;
    }

    public static Message getConnectMessage(String nodeName,String nodeId) {
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setNodeName(nodeName);
        message.setNodeId(nodeId);
        message.setMessageType(MessageType.CONNECT);
        return message;
    }

    public static Message getReplyMessage(String nodeName,String nodeId) {
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setMessageType(MessageType.REPLY);
        message.setNodeName(nodeName);
        message.setNodeId(nodeId);
        return message;
    }

    public static Message getFinishedMessage(String nodeName,String nodeId) {
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setNodeName(nodeName);
        message.setNodeId(nodeId);
        message.setMessageType(MessageType.FINISHED);
        return message;
    }

    public static Message getStartMessage(String nodeName,String nodeId) {
        Message message = new Message();
        message.setTimestamp(System.currentTimeMillis());
        message.setNodeName(nodeName);
        message.setMessageType(MessageType.START);
        message.setNodeId(nodeId);
        return message;
    }

    public String getMessageTypeName() {
        return messageType.name();
    }

    @Override
    public String toString() {
        return "Message{" + "messageType=" + messageType + ", timestamp=" + timestamp + ", incrementedStamp=" + incrementedStamp + ", nodeId=" + nodeId + ", OSN=" + OSN + ", nodeName=" + nodeName + '}';
    }
    
    
}
