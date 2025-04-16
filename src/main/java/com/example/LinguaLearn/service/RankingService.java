package com.example.LinguaLearn.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.LinguaLearn.model.RankingEntry;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;

@Service
public class RankingService {

    private static final Logger logger = LoggerFactory.getLogger(RankingService.class);
    private static final String USERS_COLLECTION = "Users";

    @Autowired
    private Firestore firestore;
    
    /**
     * 상위 랭킹을 가져옵니다.
     * @param limit 가져올 랭킹 수
     * @return 상위 랭킹 목록
     */
    public List<RankingEntry> getTopRankings(int limit) throws ExecutionException, InterruptedException {
        // score 필드를 기준으로 내림차순 정렬하여 쿼리 실행
        ApiFuture<QuerySnapshot> future = firestore.collection(USERS_COLLECTION)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
                
        List<RankingEntry> rankings = new ArrayList<>();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        
        for (QueryDocumentSnapshot document : documents) {
            // Firestore 문서에서 필요한 필드 추출
            String uid = document.getId();
            String email = document.getString("email");
            String displayName = document.getString("displayName");
            
            // score 필드가 없을 경우 기본값 0으로 설정
            Long scoreValue = document.getLong("score");
            int score = (scoreValue != null) ? scoreValue.intValue() : 0;
            
            RankingEntry entry = new RankingEntry(uid, email, displayName, score);
            rankings.add(entry);
            
            logger.debug("Ranking entry: {}", entry);
        }
        
        logger.info("Retrieved {} ranking entries", rankings.size());
        return rankings;
    }
    
    /**
     * 특정 사용자의 점수를 업데이트합니다.
     * @param uid 사용자 ID
     * @param score 새 점수
     */
    public void updateUserScore(String uid, int score) throws ExecutionException, InterruptedException {
        firestore.collection(USERS_COLLECTION)
                .document(uid)
                .update("score", score)
                .get();
        
        logger.info("Updated score for user {}: {}", uid, score);
    }
}