package com.example.LinguaLearn.model;

public class RankingEntry {
    private String uid;
    private String email;
    private String displayName;
    private int score;

    // Firestore requires a no-arg constructor
    public RankingEntry() {}

    public RankingEntry(String uid, String email, String displayName, int score) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.score = score;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    @Override
    public String toString() {
        return "RankingEntry{" +
               "uid='" + uid + '\'' +
               ", email='" + email + '\'' +
               ", displayName='" + displayName + '\'' +
               ", score=" + score +
               '}';
    }
}