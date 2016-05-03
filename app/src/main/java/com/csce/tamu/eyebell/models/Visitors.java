package com.csce.tamu.eyebell.models;

import java.util.Date;

/**
 * Created by codytaylor on 3/12/16.
 */
public class Visitors {
    private String objectId;
    private String Image;
    private String SerialNum;
    private String VisitorName;
    private Date VisitDate;

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    private Date created;

    public String getObjectId()
    {
        return objectId;
    }

    public void setObjectId( String objectId )
    {
        this.objectId = objectId;
    }

    public Date getVisitDate() {
        return VisitDate;
    }

    public void setVisitDate(Date visitDate) {
        VisitDate = visitDate;
    }

    public String getVisitorName() {
        return VisitorName;
    }

    public void setVisitorName(String visitorName) {
        VisitorName = visitorName;
    }

    public String getSerialNum() {
        return SerialNum;
    }

    public void setSerialNum(String serialNum) {
        SerialNum = serialNum;
    }

    public String getImage() {
        return Image;
    }

    public void setImage(String image) {
        Image = image;
    }
}
