package com.example.LinguaLearn.service;

import com.example.LinguaLearn.repository.FirestoreRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class FirestoreService {
    private final FirestoreRepository firestoreRepository;

    public FirestoreService(FirestoreRepository firestoreRepository) {
        this.firestoreRepository = firestoreRepository;
    }

    public List<String> getWrongProblem(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot snapshot = firestoreRepository.getProgress(uid);

        // Firestore에 저장된 오답 목록 조회 (없는 경우 빈 리스트)
        List<String> wrongSentences = snapshot.contains("wrong_sentence")
                ? (List<String>) snapshot.get("wrong_sentence") : new ArrayList<>();
        List<String> englishSentences = new ArrayList<>();
        for (Object obj : wrongSentences) {
            if (obj instanceof HashMap) {
                HashMap<?, ?> map = (HashMap<?, ?>) obj;
                Object sentenceObj = map.get("sentence");
                if (sentenceObj instanceof String) {
                    englishSentences.add((String) sentenceObj);
                }
            }
        }
        return englishSentences;
    }
    public boolean checkAnswer(String uid, String result,
                                              String wrongSentence, String userTranslation)
            throws ExecutionException, InterruptedException {
        if (result.contains("정답")) {
            DocumentReference progressDoc = firestoreRepository.getProgressDocument(uid);
            DocumentSnapshot snapshot = progressDoc.get().get();
            List<?> wrongSentenceRaw = snapshot.contains("wrong_sentence")
                    ? (List<?>) snapshot.get("wrong_sentence")
                    : new ArrayList<>();
            HashMap<String, Object> wrongMapToRemove = null;

            for (Object obj : wrongSentenceRaw) {
                if (obj instanceof HashMap) {
                    HashMap<?, ?> map = (HashMap<?, ?>) obj;
                    Object sentenceObj = map.get("sentence");
                    if (sentenceObj instanceof String && ((String) sentenceObj).trim().equals(wrongSentence.trim())) {
                        // 찾은 Map 객체를 그대로 사용할 수도 있지만, 새 Map을 만들어 동일한 값을 넣으면 안전합니다.
                        wrongMapToRemove = new HashMap<>();
                        wrongMapToRemove.put("sentence", map.get("sentence"));
                        wrongMapToRemove.put("transResult", map.get("transResult"));
                        break;
                    }
                }
            }
            // 새 correct_sentence에 추가할 Map 객체 생성 (영어 문장과 새 번역 결과 포함)
            HashMap<String, Object> newCorrectMap = new HashMap<>();
            newCorrectMap.put("sentence", wrongSentence);
            newCorrectMap.put("transResult", userTranslation);

            // Firestore 업데이트를 위한 HashMap 생성
            HashMap<String, Object> updates = new HashMap<>();
            if (wrongMapToRemove != null) {
                updates.put("wrong_sentence", FieldValue.arrayRemove(wrongMapToRemove));
            }
            updates.put("correct_sentence", FieldValue.arrayUnion(newCorrectMap));
            updates.put("wrong_cnt", FieldValue.increment(-1));
            updates.put("correct_cnt", FieldValue.increment(1));

            progressDoc.set(updates, SetOptions.merge());
            return true;
        }
        else {
            DocumentReference progressDoc = firestoreRepository.getProgressDocument(uid);
            DocumentSnapshot snapshot = progressDoc.get().get();

            // 현재 wrong_sentence 배열을 가져오거나 없으면 빈 리스트로 초기화합니다.
            List<?> wrongSentenceRaw = snapshot.contains("wrong_sentence")
                    ? (List<?>) snapshot.get("wrong_sentence")
                    : new ArrayList<>();

            // 새 오답 Map 생성 (오답 문장과 번역 결과 포함)
            HashMap<String, Object> newWrongMap = new HashMap<>();
            newWrongMap.put("sentence", wrongSentence);
            newWrongMap.put("transResult", userTranslation);

            // Firestore 업데이트를 위한 HashMap 생성
            HashMap<String, Object> updates = new HashMap<>();
            updates.put("wrong_sentence", FieldValue.arrayUnion(newWrongMap));
            updates.put("wrong_cnt", FieldValue.increment(1));

            return false;
        }
    }
}
