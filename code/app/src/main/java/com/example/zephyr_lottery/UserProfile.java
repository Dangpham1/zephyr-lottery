package com.example.zephyr_lottery;

import java.util.ArrayList;

/**
 * This class stores the details of a user
 */
public class UserProfile {

    //attributes to be updated when needed
    private String username;
    private String email;
    private String type;
    private String phone;
    private Boolean receivingNotis;
    private ArrayList<Integer> invitationCodes;
    private String fcmToken;

    // empty constructor for firebase
    public UserProfile() {

    }

    /**
     * Creates a new UserProfile (assume notifications are off)
     * @param username
     *  The username of the profile
     * @param email
     *  The email of the profile
     * @param type
     *  The type of the profile (entrant, organizer, admin)
     */
    public UserProfile(String username, String email, String type) {
        this.username = username;
        this.email = email;
        this.type = type;
        this.receivingNotis = false;
        this.invitationCodes = new ArrayList<Integer>();
    }

    /**
     * Creates a new UserProfile (assume notifications are off)
     * @param username
     *  The username of the profile
     * @param email
     *  The email of the profile
     * @param type
     *  The type of the profile (entrant, organizer, admin)
     * @param phone
     *  The phone number of the profile
     */
    public UserProfile(String username, String email, String type, String phone) {
        this.username = username;
        this.email = email;
        this.type = type;
        this.phone = phone;
        this.receivingNotis = false;
        this.invitationCodes = new ArrayList<Integer>();
    }

    /**
     * Creates a new UserProfile
     * @param username
     *  The username of the profile
     * @param email
     *  The email of the profile
     * @param type
     *  The type of the profile (entrant, organizer, admin)
     * @param phone
     *  The phone number of the profile
     * @param receivingNotis
     *  If the user wants to recieve notifications
     */
    public UserProfile(String username, String email, String type, String phone, Boolean receivingNotis) {
        this.username = username;
        this.email = email;
        this.type = type;
        this.phone = phone;
        this.receivingNotis = receivingNotis;
        this.invitationCodes = new ArrayList<Integer>();
    }

    /**
     * Obtain the user's notification preference
     * @return
     *  Boolean indicating whether the user enabled notifications
     */
    public Boolean getReceivingNotis() {
        return receivingNotis;
    }

    /**
     * Obtains the username of the profile
     * @return
     * The username of the profile, as a String
     */
    public String getUsername() {return username;}

    /**
     * Obtains the email of the profile
     * @return
     * The email of the profile, as a String
     */
    public String getEmail() {return email;}

    /**
     * Obtains the type of the profile (entrant, organizer, admin)
     * @return
     * The type of the profile, as a String
     */
    public String getType() {return type;}

    /**
     * Obtains the phone number of the profile
     * @return
     * The phone number of the profile, as a String
     */
    public String getPhone() {return phone;}

    /**
     * Obtains the user's FCM token
     * @return
     *  The user's FCM token
     */
    public String getFcmToken() {
        return fcmToken;
    }

    /**
     * Sets the user's FCM token
     * @param fcmToken
     *  The FCM token to set
     */
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * get the user's incoming invitations
     * @return
     * array list of hashcodes of the events of any incoming invitations
     */
    public ArrayList<Integer> getInvitationCodes() {
        return invitationCodes;
    }

    /**
     * set the user's incoming invitations
     * @param invitationCodes
     * array list with hashcodes of the events of any incoming invitations
     */
    public void setInvitationCodes(ArrayList<Integer> invitationCodes) {
        this.invitationCodes = invitationCodes;
    }

    /**
     * add one invitation to a user's incoming invitations.
     * @param new_invitation_code
     * the hashcode of the event with the new invitation
     */
    public void addInvitationCode(int new_invitation_code) {
        if (this.invitationCodes == null) {
            this.invitationCodes = new ArrayList<Integer>();
        }
        this.invitationCodes.add(new_invitation_code);
    }
}

