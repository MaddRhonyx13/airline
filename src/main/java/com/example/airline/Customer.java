package com.example.airline;

import java.time.LocalDate;

public class Customer {
    private int custId;
    private String pnrNumber;
    private LocalDate travelDate;
    private String custName;
    private String fatherName;
    private String gender;
    private LocalDate dateOfBirth;
    private String address;
    private String telNo;
    private String profession;
    private String security;
    private String concession;

    public Customer(String pnrNumber, LocalDate travelDate, String custName, String fatherName, String gender, LocalDate dateOfBirth, String address, String telNo, String profession, String security, String concession){
        this.pnrNumber = pnrNumber;
        this.travelDate = travelDate;
        this.custName = custName;
        this.fatherName = fatherName;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.telNo = telNo;
        this.profession = profession;
        this.security = security;
        this.concession = concession;
    }

    public Customer() {

    }

    public int getCustId(){return custId;}
    public void setCustId(int custId){this.custId = custId;}

    public String getPnrNumber(){return pnrNumber;}
    public void setPnrNumber(String pnrNumber){this.pnrNumber = pnrNumber;}

    public LocalDate getTravelDate() {
        return travelDate;
    }
    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public String getCustName() {
        return custName;
    }
    public void setCustName(String custName) {
        this.custName = custName;
    }

    public String getFatherName() {
        return fatherName;
    }
    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }

    public String getTelNo() {
        return telNo;
    }
    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public String getProfession() {
        return profession;
    }
    public void setProfession(String profession) {
        this.profession = profession;
    }

    public void setSecurity(String security) {
        this.security = security;
    }
    public String getSecurity() {
        return security;
    }

    public String getConcession() {
        return concession;
    }
    public void setConcession(String concession) {
        this.concession = concession;
    }

    @Override
    public String toString(){
        return "Customer{" +
                "custId" + custId +
                ", pnrNumber" + pnrNumber +
                ", custName" + custName +
                ", travelDate" + travelDate +
                "}";
    }
}
