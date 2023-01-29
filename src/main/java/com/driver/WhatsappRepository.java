package com.driver;

import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Repository
public class WhatsappRepository {

    private HashMap<String,User> usersRepo;
    private HashMap<Group,List<User>> groupsRepo;
    private HashMap<Integer,Message> messagesRepo;
    private HashMap<User,List<Message>> userMessageRepo;

    private int messageId;

    private int count;

    public WhatsappRepository() {
        this.usersRepo= new HashMap<>();
        this.groupsRepo= new HashMap<>();
        this.messagesRepo= new HashMap<>();
        this.userMessageRepo= new HashMap<>();
        this.messageId=1;
        this.count=1;
    }

    public String createUser(String mobile, User newUser) throws Exception {
        //If the mobile number exists in database, throw "User already exists" exception
        //Otherwise, create the user and return "SUCCESS"
        if(usersRepo.containsKey(mobile))
            throw new Exception("User already exists");
        usersRepo.put(mobile,newUser);
        return "SUCCESS";
    }

    public Group createGroup(Group newGroup, List<User> users){
        // The list contains at least 2 users where the first user is the admin. A group has exactly one admin.
        // If there are only 2 users, the group is a personal chat and the group name should be kept as the name of the second user(other than admin)
        // If there are 2+ users, the name of group should be "Group count". For example, the name of first group would be "Group 1", second would be "Group 2" and so on.
        // Note that a personal chat is not considered a group and the count is not updated for personal chats.
        // If group is successfully created, return group.

        //For example: Consider userList1 = {Alex, Bob, Charlie}, userList2 = {Dan, Evan}, userList3 = {Felix, Graham, Hugh}.
        //If createGroup is called for these userLists in the same order, their group names would be "Group 1", "Evan", and "Group 2" respectively.
        if(newGroup.getNumberOfParticipants()==2)
            newGroup.setName(users.get(1).getName());
        else
        {
            newGroup.setName("Group "+Integer.toString(count));
            count++;
        }
        groupsRepo.put(newGroup,users);
        return newGroup;
    }

    public int createMessage(Message newMessage){
        // The 'i^th' created message has message id 'i'.
        // Return the message id.
        newMessage.setId(messageId);
        messagesRepo.put(newMessage.getId(),newMessage);
        messageId++;
        return newMessage.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "You are not allowed to send message" if the sender is not a member of the group
        //If the message is sent successfully, return the final number of messages in that group.
        if(!groupsRepo.containsKey(group))
            throw new Exception("Group does not exist");

        List<User> userList= groupsRepo.get(group);

        if(!userList.contains(sender))
            throw new Exception("You are not allowed to send message");

        message.setId(messageId);
        messageId++;

        messagesRepo.put(message.getId(),message);

        List<Message> messageList= null;
        if(!userMessageRepo.containsKey(sender))
            messageList= new ArrayList<>();
        else
            messageList= userMessageRepo.get(sender);

        messageList.add(message);
        userMessageRepo.put(sender,messageList);

        int noOfMessages=0;
        for(User user: userList)
        {
            if(userMessageRepo.containsKey(user))
                noOfMessages+= userMessageRepo.get(user).size();
        }

        return noOfMessages;

    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        //Throw "Group does not exist" if the mentioned group does not exist
        //Throw "Approver does not have rights" if the approver is not the current admin of the group
        //Throw "User is not a participant" if the user is not a part of the group
        //Change the admin of the group to "user" and return "SUCCESS". Note that at one time there is only one admin and the admin rights are transferred from approver to user.
        if(!groupsRepo.containsKey(group))
            throw new Exception("Group does not exist");

        List<User> userList= groupsRepo.get(group);

        if(userList.get(0)!=approver)
            throw new Exception("Approver does not have rights");

        int userCurrIndex=0;
        boolean userFound= false;
        for(User u: userList)
        {
            if(user==u)
            {
                userFound= true;
                break;
            }
            userCurrIndex++;
        }
        if(!userFound)
            throw new Exception("User is not a participant");

        userList.set(0,user);
        userList.set(userCurrIndex,approver);
        groupsRepo.put(group,userList);

        return "SUCCESS";

    }

    public int removeUser(User user) throws Exception{
        //A user belongs to exactly one group
        //If user is not found in any group, throw "User not found" exception
        //If user is found in a group and it is the admin, throw "Cannot remove admin" exception
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        Group grp= null;
        for(Group group: groupsRepo.keySet())
        {
            List<User> userList= groupsRepo.get(group);
            if(userList.contains(user))
            {
                grp= group;
                break;
            }
        }
        if(grp==null)
            throw new Exception("User not found");

        List<User> userList= groupsRepo.get(grp);

        if(userList.get(0)==user)
            throw new Exception("Cannot remove admin");

        List<Message> messageList= userMessageRepo.get(user);
        for(Message message: messageList)
            messagesRepo.remove(message.getId(),message);

        userMessageRepo.remove(user,userMessageRepo.get(user));
        usersRepo.remove(user.getMobile(),user);

        userList.remove(user);
        groupsRepo.put(grp,userList);

        int msgOfGroup=0;
        for(User u: userList)
        {
            if(userMessageRepo.containsKey(u))
            {
                msgOfGroup+= userMessageRepo.get(u).size();
            }
        }
        int count= groupsRepo.get(grp).size()+msgOfGroup+messagesRepo.size();

        return count;

    }
}
