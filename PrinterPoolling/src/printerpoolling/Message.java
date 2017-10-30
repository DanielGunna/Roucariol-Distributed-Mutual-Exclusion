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

    public boolean isEntryCriticalSection() {
        return entryCriticalSection;
    }

    public void setEntryCriticalSection(boolean entryCriticalSection) {
        this.entryCriticalSection = entryCriticalSection;
    }
   
}
