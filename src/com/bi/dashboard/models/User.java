/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bi.dashboard.models;

public final class User {

    private final String username;
    private String password;
    private String secretAnswer;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public final boolean authenticUser(String username,String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
    public final boolean resetPassword(String newPassword,String secretAnswer) {
        if(this.secretAnswer.equals(secretAnswer)){
            if (newPassword.length() > 4) {
                if (this.password.equals(newPassword) == false) {
                    this.password = newPassword;
                    return true;
                }
            }
        }
        return false;
    }
    //For Testing
    @Override
    public final String toString(){
        return this.username + ":" + this.password;
    }
}
