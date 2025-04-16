package com.example.LinguaLearn.service;

import com.example.LinguaLearn.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String USERS_COLLECTION = "users";

    @Autowired
    private Firestore firestore;

    public User getUser(String uid) throws ExecutionException, InterruptedException {
        DocumentReference docRef = firestore.collection(USERS_COLLECTION).document(uid);
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        if (document.exists()) {
            return document.toObject(User.class);
        } else {
            return null;
        }
    }

    public User createOrUpdateUser(FirebaseToken decodedToken) throws ExecutionException, InterruptedException {
        String uid = decodedToken.getUid();
        User existingUser = getUser(uid);

        if (existingUser == null) {
            logger.info("Creating new user profile for UID: {}", uid);
            User newUser = new User(uid, decodedToken.getEmail(), decodedToken.getName());
            // Default values are set in constructor
            ApiFuture<WriteResult> future = firestore.collection(USERS_COLLECTION).document(uid).set(newUser);
            logger.info("User profile created at: {}", future.get().getUpdateTime());
            return newUser;
        } else {
            // Update existing user data if needed (e.g., display name or email changes)
            logger.debug("User profile already exists for UID: {}", uid);

            boolean needsUpdate = false;

            // Check if display name changed
            if (decodedToken.getName() != null && !Objects.equals(existingUser.getDisplayName(), decodedToken.getName())) {
                existingUser.setDisplayName(decodedToken.getName());
                needsUpdate = true;
            }

            // Check if email changed
            if (decodedToken.getEmail() != null && !Objects.equals(existingUser.getEmail(), decodedToken.getEmail())) {
                existingUser.setEmail(decodedToken.getEmail());
                needsUpdate = true;
            }

            // Apply updates if needed
            if (needsUpdate) {
                ApiFuture<WriteResult> future = firestore.collection(USERS_COLLECTION)
                        .document(uid)
                        .set(existingUser, SetOptions.merge());
                logger.info("User profile updated at: {}", future.get().getUpdateTime());
            }

            return existingUser;
        }
    }

    public User updateUserSettings(String uid, User updatedUser) throws ExecutionException, InterruptedException {
        User existingUser = getUser(uid);

        if (existingUser == null) {
            logger.error("Cannot update settings for non-existent user: {}", uid);
            return null;
        }

        // Update only specific fields
        if (updatedUser.getDisplayName() != null) {
            existingUser.setDisplayName(updatedUser.getDisplayName());
        }

        if (updatedUser.getPrimaryLanguage() != null) {
            existingUser.setPrimaryLanguage(updatedUser.getPrimaryLanguage());
        }

        if (updatedUser.getDailyGoal() != null) {
            existingUser.setDailyGoal(updatedUser.getDailyGoal());
        }

        if (updatedUser.getPushNotification() != null) {
            existingUser.setPushNotification(updatedUser.getPushNotification());
        }

        // Save updated user
        ApiFuture<WriteResult> future = firestore.collection(USERS_COLLECTION)
                .document(uid)
                .set(existingUser);

        logger.info("User settings updated at: {}", future.get().getUpdateTime());
        return existingUser;
    }

    public User updateUserScore(String uid, int scoreToAdd) throws ExecutionException, InterruptedException {
        try {
            // 트랜잭션을 사용하여 atomic 업데이트 수행
            ApiFuture<User> future = firestore.runTransaction(transaction -> {
                DocumentReference docRef = firestore.collection("Users").document(uid);
                DocumentSnapshot snapshot = transaction.get(docRef).get();

                if (!snapshot.exists()) {
                    throw new RuntimeException("User document does not exist: " + uid);
                }

                User user = snapshot.toObject(User.class);
                int newScore = user.getScore() + scoreToAdd;

                // score 필드만 업데이트
                transaction.update(docRef, "score", newScore);

                // 업데이트된 유저 객체 반환
                user.setScore(newScore);
                return user;
            });

            return future.get();
        } catch (Exception e) {
            logger.error("Failed to update score for user: " + uid, e);
            throw e;
        }
    }
}