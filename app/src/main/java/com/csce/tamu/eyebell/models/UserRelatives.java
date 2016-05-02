package com.csce.tamu.eyebell.models;


/**
 * Created by codytaylor on 3/12/16.
 */
public class UserRelatives {
    private String objectId;
    private String RelativeName;
    private String SerialNum;
    private String RelativePath;
    private String Greeting;

    public String getFaceID() {
        return FaceID;
    }

    public void setFaceID(String faceID) {
        FaceID = faceID;
    }

    private String FaceID;

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getRelativeName() {
        return RelativeName;
    }

    public void setRelativeName(String relativeName) {
        RelativeName = relativeName;
    }

    public String getSerialNum() {
        return SerialNum;
    }

    public void setSerialNum(String serialNum) {
        SerialNum = serialNum;
    }

    public String getRelativePath() {
        return RelativePath;
    }

    public void setRelativePath(String relativePath) {
        RelativePath = relativePath;
    }

    public String getGreeting() {
        return Greeting;
    }

    public void setGreeting(String greeting) {
        Greeting = greeting;
    }
}
